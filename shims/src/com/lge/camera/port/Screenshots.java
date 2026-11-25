package com.lge.camera.port;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Replacement for LG's com.lge.camera.util.Utils.getScreenShot*(): on LG firmware they read
 * the camera window through LG's IWindowManagerEx + SurfaceControl.screenshotUpToLayer, which
 * AOSP does not have. Live Photo builds its clip from these frames.
 *
 * Here the preview is read back from the app's own visible SurfaceView/TextureView(s) with
 * PixelCopy (public API, no permission needed) and composed at their on-screen position, so
 * the result matches a screenshot of the preview layers without the UI drawn on top.
 */
public final class Screenshots {
    private static final String TAG = "LGCameraPort";
    private static Handler sCopyHandler;
    private static long sLastWarn;

    static synchronized Handler copyHandler() {
        if (sCopyHandler == null) {
            HandlerThread t = new HandlerThread("lgport-pixelcopy");
            t.start();
            sCopyHandler = new Handler(t.getLooper());
        }
        return sCopyHandler;
    }

    static Activity activityOf(Context c) {
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        return PortShim.sResumed;
    }

    /** Utils.getScreenShot(Context, w, h, useLayers, crop, quiet, rotation, secondDisplay, scale). */
    public static Bitmap getScreenShot(Context ctx, int w, int h, boolean useLayers, Rect crop, boolean quiet,
                                       int rotation, boolean secondDisplay, boolean scale) {
        Rect region = crop != null && !crop.isEmpty() ? new Rect(crop) : new Rect(0, 0, w, h);
        int outW = w > 0 ? w : region.width(), outH = h > 0 ? h : region.height();
        return capture(ctx, region, outW, outH);
    }

    /** Utils.getScreenShot(Context, w, h, useLayers, crop, quiet). */
    public static Bitmap getScreenShot(Context ctx, int w, int h, boolean useLayers, Rect crop, boolean quiet) {
        return getScreenShot(ctx, w, h, useLayers, crop, quiet, 0, false, true);
    }

    /** Utils.getScreenShotWithCropRect(Activity, ?, ?, crop, ?, ?, log). */
    public static Bitmap getScreenShotWithCropRect(Activity a, int i1, int i2, Rect crop, int i3, boolean b1, boolean b2) {
        Rect region = crop != null && !crop.isEmpty() ? new Rect(crop) : screenRect(a);
        return capture(a, region, region.width(), region.height());
    }

    static Rect screenRect(Context ctx) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        return new Rect(0, 0, dm.widthPixels, dm.heightPixels);
    }

    /** Renders the preview views that intersect {@code region} (screen coords) into an outW x outH bitmap. */
    static Bitmap capture(Context ctx, Rect region, int outW, int outH) {
        if (outW <= 0 || outH <= 0 || region.isEmpty()) return null;
        Activity a = activityOf(ctx);
        if (a == null || a.getWindow() == null) return warn("no activity");
        List<View> views = new ArrayList<>();
        collect(a.getWindow().getDecorView(), views);
        if (views.isEmpty()) return warn("no preview view");

        float sx = outW / (float) region.width(), sy = outH / (float) region.height();
        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.BLACK);
        boolean drew = false;
        int[] loc = new int[2];
        for (View v : views) { // traversal order == z order for siblings: later draws on top
            v.getLocationOnScreen(loc);
            Rect vr = new Rect(loc[0], loc[1], loc[0] + v.getWidth(), loc[1] + v.getHeight());
            if (!Rect.intersects(vr, region)) continue;
            int bw = Math.max(1, Math.round(vr.width() * sx)), bh = Math.max(1, Math.round(vr.height() * sy));
            Bitmap frame = copy(v, bw, bh);
            if (frame == null) continue;
            RectF dst = new RectF((vr.left - region.left) * sx, (vr.top - region.top) * sy,
                    (vr.right - region.left) * sx, (vr.bottom - region.top) * sy);
            canvas.drawBitmap(frame, null, dst, null);
            frame.recycle();
            drew = true;
        }
        if (!drew) { out.recycle(); return warn("preview copy failed"); }
        return out;
    }

    static void collect(View v, List<View> out) {
        if (v == null || v.getVisibility() != View.VISIBLE || v.getWidth() == 0 || v.getHeight() == 0) return;
        if (v instanceof SurfaceView || v instanceof TextureView) { out.add(v); return; }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out);
        }
    }

    static Bitmap copy(View v, int bw, int bh) {
        if (v instanceof TextureView) {
            TextureView tv = (TextureView) v;
            if (!tv.isAvailable()) return null;
            if (Looper.myLooper() == Looper.getMainLooper()) return tv.getBitmap(bw, bh);
            final Bitmap[] res = new Bitmap[1];
            CountDownLatch done = new CountDownLatch(1);
            v.post(() -> { try { res[0] = tv.getBitmap(bw, bh); } finally { done.countDown(); } });
            await(done);
            return res[0];
        }
        SurfaceView sv = (SurfaceView) v;
        if (sv.getHolder().getSurface() == null || !sv.getHolder().getSurface().isValid()) return null;
        Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        final int[] result = {PixelCopy.ERROR_UNKNOWN};
        CountDownLatch done = new CountDownLatch(1);
        try {
            PixelCopy.request(sv, bmp, r -> { result[0] = r; done.countDown(); }, copyHandler());
        } catch (IllegalArgumentException e) {
            bmp.recycle();
            return null;
        }
        if (!await(done) || result[0] != PixelCopy.SUCCESS) { bmp.recycle(); return null; }
        return bmp;
    }

    static boolean await(CountDownLatch l) {
        try { return l.await(500, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { return false; }
    }

    static Bitmap warn(String why) {
        long now = System.currentTimeMillis();
        if (now - sLastWarn > 5000) { sLastWarn = now; Log.w(TAG, "screenshot: " + why); }
        return null;
    }
}
