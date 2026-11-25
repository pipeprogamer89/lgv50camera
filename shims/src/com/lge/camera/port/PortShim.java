package com.lge.camera.port;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Entry point injected at the start of LGCameraApplication.onCreate().
 * Makes the subset of LG framework resources (package 0x02 "com.lge") available to the
 * app through a ResourcesLoader, so hard-coded 0x02xxxxxx ids and
 * getIdentifier(..., "com.lge") keep working on AOSP.
 */
public final class PortShim {
    private static final String TAG = "LGCameraPort";
    private static final String ASSET = "lge-res-lite.apk";
    private static final String[] SYSTEM_PATHS = {
        "/product/etc/lgcamera/lge-res-lite.apk",
        "/system_ext/etc/lgcamera/lge-res-lite.apk",
        "/system/etc/lgcamera/lge-res-lite.apk",
    };
    private static ResourcesLoader sLoader;
    static volatile Activity sResumed;

    public static void init(Application app) {
        Compat.sApp = app;
        try {
            sLoader = createLoader(app);
            attach(app.getResources());
            app.registerActivityLifecycleCallbacks(new Callbacks());
            Log.i(TAG, "LG resources loader installed");
        } catch (Throwable t) {
            Log.e(TAG, "LG resources loader failed", t);
        }
    }

    public static void attach(Resources res) {
        if (sLoader == null || res == null) return;
        try {
            res.addLoaders(sLoader); // no-op if already present
        } catch (Throwable t) {
            Log.w(TAG, "addLoaders failed", t);
        }
    }

    private static ResourcesLoader createLoader(Context ctx) throws Exception {
        File src = null;
        for (String p : SYSTEM_PATHS) {
            File f = new File(p);
            if (f.canRead()) { src = f; break; }
        }
        if (src == null) {
            // Fall back to the copy bundled in the APK's assets.
            File dst = new File(ctx.getCodeCacheDir(), ASSET);
            try (InputStream in = ctx.getAssets().open(ASSET)) {
                if (!dst.exists() || dst.length() != in.available()) {
                    try (OutputStream out = new FileOutputStream(dst)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }
                }
            }
            src = dst;
        }
        ResourcesLoader loader = new ResourcesLoader();
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(src, ParcelFileDescriptor.MODE_READ_ONLY)) {
            loader.addProvider(ResourcesProvider.loadFromApk(pfd));
        }
        Log.i(TAG, "loaded " + src);
        return loader;
    }

    /** Replacement for android.util.FloatMath.sqrt, removed from the platform. */
    public static float sqrt(float v) { return (float) Math.sqrt(v); }

    private static final class Callbacks implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityPreCreated(Activity a, Bundle b) { attach(a.getResources()); }
        @Override public void onActivityCreated(Activity a, Bundle b) {}
        @Override public void onActivityStarted(Activity a) {}
        @Override public void onActivityResumed(Activity a) { sResumed = a; }
        @Override public void onActivityPaused(Activity a) { if (sResumed == a) sResumed = null; }
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        @Override public void onActivityDestroyed(Activity a) {}
    }
}
