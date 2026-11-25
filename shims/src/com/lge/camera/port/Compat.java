package com.lge.camera.port;

import android.content.Context;
import android.graphics.Rect;
import android.view.SurfaceControl;
import java.util.Collections;
import java.util.List;

/**
 * Static replacements for platform methods that only existed in LG's modified framework
 * or were removed after Android 11. Calls are redirected here by DexPatcher; for instance
 * methods the receiver is passed as the first argument.
 */
public final class Compat {
    static Context sApp;

    // IActivityManager.getAllStackInfos() (removed in Android 12)
    public static List getAllStackInfos(Object am) { return Collections.emptyList(); }
    // IWindowManager LG extensions
    public static boolean isOneHandUxOn(Object wm) { return false; }
    public static boolean isDesktopModeEnabled(Object wm) { return false; }
    public static void setForcedDisplayRotationForUser(Object wm, int rotation, boolean freeze) {}
    public static void setNavigationBarFromApps(Object wm, int displayId, boolean visible) {}
    // IActivityTaskManager LG extension
    public static void minimizeAllFreeforms(Object atm, int displayId) {}
    // SurfaceControl (Android 11 hidden API, removed later)
    public static long[] getPhysicalDisplayIds() { return new long[0]; }
    public static android.view.SurfaceControl$ScreenshotGraphicBuffer captureLayers(SurfaceControl sc, Rect crop, float scale) {
        return null;
    }
    // CameraManager.getCameraIdList(): on LG firmware the camera app only saw the physical
    // sensors (0-4 on the V50) and assigned the following ids (5-9) to its own logical
    // cameras (rear/front dual, optic zoom). AOSP lists every HAL id, so the app computed
    // logical ids 10-12 that do not exist. Return one id per physical sensor, like stock.
    private static String[] sCameraIds;
    public static synchronized String[] getCameraIdList(Object cm) throws android.hardware.camera2.CameraAccessException {
        if (sCameraIds != null) return sCameraIds.clone();
        android.hardware.camera2.CameraManager m = (android.hardware.camera2.CameraManager) cm;
        java.util.ArrayList<String> ok = new java.util.ArrayList<>();
        java.util.HashSet<String> sensors = new java.util.HashSet<>();
        for (String id : m.getCameraIdList()) {
            try {
                android.hardware.camera2.CameraCharacteristics c = m.getCameraCharacteristics(id);
                String sig = c.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) + "/"
                        + java.util.Arrays.toString(c.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)) + "/"
                        + c.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) + "/"
                        + c.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                if (sensors.add(sig)) ok.add(id);
            } catch (Throwable t) {
                android.util.Log.w("LGCameraPort", "skipping camera id " + id + ": " + t.getMessage());
            }
        }
        sCameraIds = ok.toArray(new String[0]);
        android.util.Log.i("LGCameraPort", "physical camera ids for LG app: " + ok);
        return sCameraIds.clone();
    }

    // SystemProperties.get(String): LG build properties that do not exist on AOSP ROMs.
    // The app parses some of them without a default (Integer.parseInt("") crashes).
    private static final java.util.Map<String, String> LG_PROP_DEFAULTS = new java.util.HashMap<>();
    static {
        LG_PROP_DEFAULTS.put("product.lge.secpolicy.camera.disabled", "0");
        LG_PROP_DEFAULTS.put("ro.product.lge.5g_networks", "true");
        LG_PROP_DEFAULTS.put("ro.product.lge.fingerprint.mode", "nav");
        // Open-market values: KR/JP targets force the shutter sound and change features.
        LG_PROP_DEFAULTS.put("ro.vendor.lge.build.target_country", "COM");
        LG_PROP_DEFAULTS.put("ro.vendor.lge.build.target_operator", "OPEN");
        LG_PROP_DEFAULTS.put("ro.vendor.lge.build.target_region", "COM");
    }
    public static String getProp(String key) {
        String v = Props.get(key, "");
        if (v.isEmpty()) {
            String d = LG_PROP_DEFAULTS.get(key);
            if (d != null) return d;
        }
        return v;
    }

    // android.security.KeyStore.getApplicationContext() (class removed in Android 12)
    public static Context keyStoreApplicationContext() { return sApp; }

    // CameraCaptureSession.close(): when a session closes without a replacement, AOSP's
    // framework "unconfigures" the device, and cameraserver sends the HAL a fake stream with
    // operation mode 0. The LG CHI override crashes on that (null PerfLockManager in
    // UsecaseFactory::CreateUsecaseObject), killing the camera provider; LG's own cameraserver
    // skipped it ("skipDummyStreamCreate = true"). replaceSessionClose() is the framework's
    // close-without-unconfigure, used when a new session follows or the device is closing.
    private static java.lang.reflect.Method sReplaceClose;
    private static boolean sReplaceCloseLooked;
    public static void closeSession(Object session) {
        android.hardware.camera2.CameraCaptureSession s = (android.hardware.camera2.CameraCaptureSession) session;
        synchronized (Compat.class) {
            if (!sReplaceCloseLooked) {
                sReplaceCloseLooked = true;
                for (Class<?> c = s.getClass(); c != null && sReplaceClose == null; c = c.getSuperclass()) {
                    try {
                        sReplaceClose = c.getDeclaredMethod("replaceSessionClose");
                        sReplaceClose.setAccessible(true);
                    } catch (Throwable ignored) {}
                }
                if (sReplaceClose == null) android.util.Log.w("LGCameraPort", "replaceSessionClose not found");
            }
        }
        if (sReplaceClose != null && sReplaceClose.getDeclaringClass().isInstance(s)) {
            try {
                sReplaceClose.invoke(s);
                return;
            } catch (Throwable t) {
                android.util.Log.w("LGCameraPort", "replaceSessionClose failed", t);
            }
        }
        s.close();
    }

    // BlockingCameraManager.createRecordingSession(): the video session carries two JPEG (BLOB)
    // outputs for snapshots while recording. With them, the Android 12 LG/QTI HAL shipped by
    // LineageOS builds a deferred offline snapshot session (UsecaseMultiCamera::DeferSnapSession)
    // whose EIS extra-buffer setup dereferences a null stream and kills the camera provider on
    // most attempts. Drop the BLOB outputs so recording works (no photos while recording).
    private static java.lang.reflect.Method sRecSession, sSurfaceFormat;
    public static android.hardware.camera2.CameraCaptureSession createRecordingSession(Object mgr, Boolean hfr,
            android.hardware.camera2.CameraDevice dev, java.util.ArrayList outputs, android.os.Handler h, int opMode,
            android.hardware.camera2.CaptureRequest params) throws Throwable {
        java.util.ArrayList filtered = new java.util.ArrayList();
        int dropped = 0;
        for (Object o : outputs) {
            android.view.Surface s = o instanceof android.view.Surface ? (android.view.Surface) o
                    : o instanceof android.hardware.camera2.params.OutputConfiguration
                    ? ((android.hardware.camera2.params.OutputConfiguration) o).getSurface() : null;
            if (s != null && surfaceFormat(s) == 0x21 /* HAL_PIXEL_FORMAT_BLOB */) { dropped++; continue; }
            filtered.add(o);
        }
        if (dropped > 0) android.util.Log.i("LGCameraPort", "recording session: dropped " + dropped + " JPEG output(s)");
        if (sRecSession == null) {
            sRecSession = mgr.getClass().getMethod("createRecordingSession", Boolean.class,
                    android.hardware.camera2.CameraDevice.class, java.util.ArrayList.class, android.os.Handler.class,
                    int.class, android.hardware.camera2.CaptureRequest.class);
        }
        try {
            return (android.hardware.camera2.CameraCaptureSession) sRecSession.invoke(mgr, hfr, dev, filtered, h, opMode, params);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static int surfaceFormat(android.view.Surface s) {
        try {
            if (sSurfaceFormat == null) {
                sSurfaceFormat = Class.forName("android.hardware.camera2.utils.SurfaceUtils")
                        .getMethod("getSurfaceFormat", android.view.Surface.class);
            }
            return (Integer) sSurfaceFormat.invoke(null, s);
        } catch (Throwable t) {
            return -1;
        }
    }

    // FilmEmulatorManager.setCurLutAndValue(String): looks the LUT up in mFilmIndexHashMap and,
    // if missing, retries "0_film.dat" and unboxes it. In AI CAM the map only holds the scene's
    // smartcam LUTs, which stays empty without LG's EllieVision service -> NPE crash. Fall back
    // to any LUT in the map, or keep the current one.
    private static java.lang.reflect.Field sFilmMap;
    private static java.lang.reflect.Method sSetLut;
    public static void setCurLutAndValue(Object mgr, String value) {
        try {
            if (sFilmMap == null) {
                Class<?> c = mgr.getClass();
                while (c != null && sSetLut == null) {
                    try { sSetLut = c.getDeclaredMethod("setCurLutAndValue", String.class); } catch (NoSuchMethodException e) { c = c.getSuperclass(); }
                }
                for (c = mgr.getClass(); c != null && sFilmMap == null; c = c.getSuperclass()) {
                    try { sFilmMap = c.getDeclaredField("mFilmIndexHashMap"); } catch (NoSuchFieldException e) {}
                }
                sFilmMap.setAccessible(true);
                sSetLut.setAccessible(true);
            }
            java.util.Map map = (java.util.Map) sFilmMap.get(mgr);
            String v = value;
            if (map == null || map.isEmpty()) {
                android.util.Log.w("LGCameraPort", "film LUT map empty, ignoring " + value);
                return;
            }
            if (!map.containsKey(v) && !map.containsKey("0_film.dat")) {
                v = (String) map.keySet().iterator().next();
                android.util.Log.w("LGCameraPort", "film LUT " + value + " not available, using " + v);
            }
            sSetLut.invoke(mgr, v);
        } catch (java.lang.reflect.InvocationTargetException e) {
            android.util.Log.w("LGCameraPort", "setCurLutAndValue failed", e.getCause());
        } catch (Throwable t) {
            android.util.Log.w("LGCameraPort", "setCurLutAndValue shim failed", t);
        }
    }

    // ---- Cine Video on the normal video usecase -------------------------------------------
    // BuilderSet.applyCineEffect / applyFilmParam run as usual; then, while Cine Video is active
    // with a look selected ("cinema-lut" = "<lut>-<strength>", 1 = none), the look is sent to the
    // HAL's film emulator as /vendor/etc/camera/film/cine_<lut>.lut (see tools/cine2film.py).
    private static java.lang.reflect.Method sApplyCine, sApplyFilm, sApplyBuilder, sParamGet;
    private static java.lang.reflect.Field sParams, sShotMode;
    private static Object sKeyFilmType, sKeyFilmStrength;

    public static void applyCineEffect(Object bs, android.hardware.camera2.CaptureRequest.Builder b) {
        try {
            initBuilderSet(bs);
            sApplyCine.invoke(bs, b);
        } catch (Throwable t) {
            android.util.Log.w("LGCameraPort", "applyCineEffect failed", t);
        }
        applyCineLook(bs, b);
    }

    public static void applyFilmParam(Object bs, android.hardware.camera2.CaptureRequest.Builder b) {
        try {
            initBuilderSet(bs);
            sApplyFilm.invoke(bs, b);
        } catch (Throwable t) {
            android.util.Log.w("LGCameraPort", "applyFilmParam failed", t);
        }
        applyCineLook(bs, b);
    }

    private static synchronized void initBuilderSet(Object bs) throws Exception {
        if (sApplyCine != null) return;
        Class<?> c = bs.getClass();
        Class<?> bc = android.hardware.camera2.CaptureRequest.Builder.class;
        sApplyCine = findMethod(c, "applyCineEffect", bc);
        sApplyFilm = findMethod(c, "applyFilmParam", bc);
        sApplyBuilder = findMethod(c, "applyBuilder", bc, Object.class, Object.class);
        sParams = findField(c, "mParameters");
        sShotMode = findField(c, "mShotMode");
        for (java.lang.reflect.AccessibleObject o : new java.lang.reflect.AccessibleObject[]{
                sApplyCine, sApplyFilm, sApplyBuilder, sParams, sShotMode}) o.setAccessible(true);
        Class<?> pc = Class.forName("com.lge.camera.device.ParamConstants", true, c.getClassLoader());
        sKeyFilmType = pc.getField("KEY_FILM_FILTER_TYPE").get(null);
        sKeyFilmStrength = pc.getField("KEY_FILM_FILTER_STRENGTH").get(null);
    }

    static java.lang.reflect.Method findMethod(Class<?> c, String name, Class<?>... args) throws NoSuchMethodException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredMethod(name, args); } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(c.getName() + "." + name);
    }

    static java.lang.reflect.Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(c.getName() + "." + name);
    }

    /** "cine_N.lut" while Cine Video shows a look (cinema-lut "N-strength", N > 1), else null. */
    private static String cineLook(Object bs, int[] strengthOut) {
        try {
            initBuilderSet(bs);
            String shot = (String) sShotMode.get(bs);
            if (shot == null || !shot.contains("cinema")) return null;
            Object p = sParams.get(bs);
            if (p == null) return null;
            if (sParamGet == null) sParamGet = p.getClass().getMethod("get", String.class);
            String cam = (String) sParamGet.invoke(p, "cinema-camera");
            String lut = (String) sParamGet.invoke(p, "cinema-lut");
            if (cam == null || "cinema-off".equals(cam) || lut == null) return null;
            String[] parts = lut.split("-");
            int n = Integer.parseInt(parts[0].trim());
            if (strengthOut != null) strengthOut[0] = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 100;
            String name = "cine_" + n + ".lut";
            if (n <= 1 || !new java.io.File("/vendor/etc/camera/film/" + name).exists()) return null;
            return name;
        } catch (Throwable t) {
            android.util.Log.w("LGCameraPort", "cine look failed", t);
            return null;
        }
    }

    private static void applyCineLook(Object bs, android.hardware.camera2.CaptureRequest.Builder b) {
        if (b == null) return;
        int[] strength = {100};
        String name = cineLook(bs, strength);
        if (name == null) return;
        try {
            sApplyBuilder.invoke(bs, b, sKeyFilmType, name);
            sApplyBuilder.invoke(bs, b, sKeyFilmStrength, strength[0]);
        } catch (Throwable t) {
            android.util.Log.w("LGCameraPort", "cine look apply failed", t);
        }
    }

    // BuilderSet.applySolution(builder, enabledSolutions, maxFrames): the HAL only runs its film
    // emulator when SolutionConstants.FILM_EMULATOR (0x20) is in the enabled list.
    private static java.lang.reflect.Method sApplySolution;
    public static void applySolution(Object bs, android.hardware.camera2.CaptureRequest.Builder b, int solutions, int frames) {
        try {
            initBuilderSet(bs);
            if (sApplySolution == null) {
                sApplySolution = findMethod(bs.getClass(), "applySolution",
                        android.hardware.camera2.CaptureRequest.Builder.class, int.class, int.class);
                sApplySolution.setAccessible(true);
            }
            if (cineLook(bs, null) != null) solutions |= 0x20;
            sApplySolution.invoke(bs, b, solutions, frames);
        } catch (Throwable t) {
            android.util.Log.w("LGCameraPort", "applySolution failed", t);
        }
    }

    // LG "downloaded filters" folder -> the Cine Video looks shipped by the Magisk module
    // (tools/cine2film.py output, named NN_<label>_.dat; the label is the 2nd "_" field).
    public static String filterDownloadPath() {
        for (String d : new String[]{"/product/etc/lgcamera/cinefilters/", "/system_ext/etc/lgcamera/cinefilters/",
                "/system/etc/lgcamera/cinefilters/"}) {
            if (new java.io.File(d).isDirectory()) return d;
        }
        return "/storage/emulated/0/Download/LGWorld/Camera/Filter/";
    }

    // android.hardware.Camera.getCameraInfo(): throws "Unknown camera ID" for the LG logical
    // ids or while the camera provider restarts, which crashed the app from the orientation
    // listener. Fill from camera2 characteristics instead, or sane defaults.
    public static void getCameraInfo(int id, android.hardware.Camera.CameraInfo info) {
        try {
            android.hardware.Camera.getCameraInfo(id, info);
            return;
        } catch (RuntimeException ignored) {}
        info.facing = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
        info.orientation = 90;
        info.canDisableShutterSound = true;
        try {
            android.hardware.camera2.CameraManager cm = sApp.getSystemService(android.hardware.camera2.CameraManager.class);
            android.hardware.camera2.CameraCharacteristics ch = cm.getCameraCharacteristics(String.valueOf(id));
            Integer f = ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
            Integer o = ch.get(android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION);
            if (f != null && f == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT)
                info.facing = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
            if (o != null) info.orientation = o;
        } catch (Throwable ignored) {
            // V50: 1 and 4 are the front sensors, 6 the front dual
            if (id == 1 || id == 4 || id == 6) {
                info.facing = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
                info.orientation = 270;
            }
        }
    }
}
