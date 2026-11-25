package com.lge.camera.port;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Cine Video support on AOSP.
 *
 * LG applies the Cine Video looks in the camera HAL (com.lge.camera.cine.lut_level), but only
 * inside its 10-bit HDR10 usecase. LineageOS cannot allocate those buffers, so the HAL node logs
 * "CHILGCameraSolutionCineEffect: Unknown type" and leaves the frames untouched.
 *
 * The app ships its own GPU film engine (FilmEmulatorEngine) which applies 3D LUTs to preview and
 * recording in 8-bit. This class drives it from the Cine Video filter strip:
 *   - CinemaModuleBase.initializeSettingMenus(): select the identity LUT so the engine is running
 *     before the camera preview starts.
 *   - CinemaFilterManager.setCinemaLUT(): map the selected Cine look to the matching film LUT.
 *
 * Called from DexPatcher-injected entry hooks, so everything here is reflection: the shim class
 * lives in its own dex and cannot link against the (patched) app classes.
 */
public final class CineCompat {
    private static final String TAG = "LGCameraPort";
    private static final String DIR = "/product/etc/lgcamera/cinefilters/";
    private static final String IDENTITY = DIR + "10_Cine 01_.dat";
    private static final String PREFS = "lgcamera_port";
    private static final String PREF_LUT = "cine_lut";

    private static boolean sInLut;
    private static android.view.Surface sRecorderSurface;
    private static String sCurrentLutPath;
    private static int sStrength = 100;

    private CineCompat() {}

    /** Hook at CinemaModuleBase.initializeSettingMenus(): runs before the camera preview opens. */
    public static void onCinemaInit(Object module) {
        try {
            // Start the GPU engine with the look the Cine strip is already showing, so the
            // preview and the strip agree from the first frame. If the strip is on "none"
            // (index 0 -> LUT 1), use the identity LUT: the engine has to stay alive so later
            // selections can be switched live.
            int lut = 1;
            Object manager = field(module, "mCinemaFilterManager");
            if (manager != null) {
                lut = intField(manager, "mCurrentLUTIndex") + 1;
            }
            if (lut < 1) lut = 1;
            String path = lutPath(lut);
            sCurrentLutPath = path;
            sStrength = 100;
            setFilm(module, path);
            prefs().edit().putInt(PREF_LUT, lut).apply();
            Log.i(TAG, "cine init: film LUT " + lut + " -> " + path + gate(module));
            String dbg = Props.get("debug.lgcamera.postfile", "");
            if (!dbg.isEmpty()) {
                String dlut = Props.get("debug.lgcamera.postlut", path);
                final String f = dbg, l = dlut;
                Log.i(TAG, "cine debug post: " + f + " <- " + l);
                new Thread(() -> CineVideoPost.processFile(Compat.sApp, f, l, 100), "cine-dbg").start();
            }
        } catch (Throwable t) {
            Log.e(TAG, "cine init failed", t);
        }
    }

    /**
     * Hook at CinemaFilterManager.setCinemaLUT(). The Cine strip has already updated
     * mCurrentLUTIndex; mirror the choice on the app's film engine.
     */
    public static void onCinemaLut(Object manager) {
        if (sInLut) return;
        sInLut = true;
        try {
            int lut = intField(manager, "mCurrentLUTIndex") + 1;
            int strength = 100;
            try {
                Object v = call(manager, "getCurrentLUTStrength", new Class<?>[]{int.class}, 0);
                if (v instanceof Integer) strength = (Integer) v;
            } catch (Throwable ignored) {}
            Object module = field(manager, "mCineFilterGet");
            if (module == null) return;
            String path = lutPath(lut);
            sCurrentLutPath = path;
            sStrength = strength;
            prefs().edit().putInt(PREF_LUT, lut).apply();
            setFilm(module, path);

            Object film = field(module, "mAdvancedFilmManager");
            if (film == null) {
                Log.w(TAG, "cine lut " + lut + ": no film manager");
                return;
            }
            boolean running = Boolean.TRUE.equals(call(film, "isRunningFilmEmulator", new Class<?>[]{}));
            if (!running) {
                call(film, "runFilmEmulator", new Class<?>[]{String.class}, path);
            }
            call(film, "setCurIndex", new Class<?>[]{String.class}, path);
            call(film, "setFilmStrength", new Class<?>[]{float.class, boolean.class},
                    strength / 100.0f, false);
            Log.i(TAG, "cine lut " + lut + " strength " + strength + " -> " + path
                    + (running ? " (live)" : " (started)"));
        } catch (Throwable t) {
            Log.e(TAG, "cine lut failed", t);
        } finally {
            sInLut = false;
        }
    }

    /**
     * Hook at AdvancedSelfieManager.prepareRecording(). In Cine Video the GPU engine only feeds
     * the preview: the HAL already applies the look to the encoded stream, and the app's recorder
     * surface path is broken there (MediaRecorder SurfaceMediaSource). Skip it only in cinema.
     */
    public static void prepareFilmRecording(Object manager, String size, boolean flag) {
        try {
            if (isCinema(manager)) {
                Log.i(TAG, "cine: skip GPU film recording (preview-only engine)");
                return;
            }
            Method m = manager.getClass().getMethod("prepareRecording", String.class, boolean.class);
            m.invoke(manager, size, flag);
        } catch (Throwable t) {
            Log.e(TAG, "prepare film recording failed", t);
        }
    }

    public static void startFilmRecording(Object manager) {
        try {
            if (isCinema(manager)) {
                Log.i(TAG, "cine: skip GPU startRecorder (preview-only engine)");
                return;
            }
            manager.getClass().getMethod("startRecorder").invoke(manager);
        } catch (Throwable t) {
            Log.e(TAG, "start film recording failed", t);
        }
    }

    public static void stopFilmRecording(Object manager, boolean direct) {
        try {
            if (isCinema(manager)) {
                Log.i(TAG, "cine: skip GPU stopRecorder (preview-only engine)");
                return;
            }
            manager.getClass().getMethod("stopRecorder", boolean.class).invoke(manager, direct);
        } catch (Throwable t) {
            Log.e(TAG, "stop film recording failed", t);
        } finally {
            sRecorderSurface = null;
        }
    }

    /**
     * Replacement for MediaRecorder.prepare() at the VideoRecorder.init() call site.
     * MediaRecorder.getSurface() must be called *before* prepare(); afterwards AOSP throws
     * "SurfaceMediaSource could not be initialized". LG asks for the surface later (after
     * prepare), so grab it here and let the GPU film engine render into it.
     */
    public static void prepareRecorder(Object recorder) {
        try {
            if (sRecorderSurface == null) {
                try {
                    Object s = recorder.getClass().getMethod("getSurface").invoke(recorder);
                    if (s instanceof android.view.Surface) {
                        sRecorderSurface = (android.view.Surface) s;
                        Log.i(TAG, "captured recorder surface before prepare: " + s);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "MediaRecorder.getSurface before prepare failed: " + t);
                }
            }
        } finally {
            try {
                recorder.getClass().getMethod("prepare").invoke(recorder);
            } catch (Throwable t) {
                Log.e(TAG, "MediaRecorder.prepare failed", t);
            }
        }
    }

    /**
     * Replacement for VideoRecorder.getSurface() at all call sites. In Cine Video the
     * MediaRecorder surface cannot be created on AOSP ("SurfaceMediaSource could not be
     * initialized"), but VideoRecorder.init() already created a persistent input surface and
     * called MediaRecorder.setInputSurface() with it. Fall back to that surface so the GPU engine
     * can render the graded frames straight into the encoder.
     */
    public static android.view.Surface getRecordingSurface(boolean useRecorder, int index) {
        try {
            if (useRecorder && sRecorderSurface != null) {
                return sRecorderSurface;
            }
            android.view.Surface persistent = persistentSurface(index);
            if (!useRecorder) return persistent;
            Object recorder = staticField("com.lge.camera.app.VideoRecorder", "sMediaRecorder");
            Object second = staticField("com.lge.camera.app.VideoRecorder", "sSecondMediaRecorder");
            boolean init = Boolean.TRUE.equals(staticField("com.lge.camera.app.VideoRecorder", "sInit"));
            if (recorder != null && (init || second != null)) {
                try {
                    Method m = recorder.getClass().getMethod("getSurface");
                    Object s = m.invoke(recorder);
                    if (s != null) return (android.view.Surface) s;
                } catch (Throwable t) {
                    Log.w(TAG, "MediaRecorder.getSurface failed, using persistent surface: " + t);
                }
            }
            return persistent;
        } catch (Throwable t) {
            Log.e(TAG, "getRecordingSurface failed", t);
            return null;
        }
    }

    private static android.view.Surface persistentSurface(int index) {
        Object v = staticField("com.lge.camera.app.VideoRecorder", "sPersistentSurface");
        if (v instanceof android.view.Surface[] && index >= 0 && index < ((android.view.Surface[]) v).length) {
            return ((android.view.Surface[]) v)[index];
        }
        return null;
    }

    private static Object staticField(String className, String name) {
        try {
            Class<?> c = Class.forName(className);
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isCinema(Object manager) {
        Object mGet = field(manager, "mGet");
        if (mGet == null) return false;
        try {
            return String.valueOf(call(mGet, "getShotMode", new Class<?>[]{})).contains("cinema");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Hook at DefaultCameraModule.checkToUseFilmEffect(): diagnostics + make sure it can run. */
    public static void beforeFilmGate(Object module) {
        try {
            String mode = String.valueOf(call(module, "getShotMode", new Class<?>[]{}));
            boolean cinema = mode.contains("cinema");
            if (cinema) {
                // The film preference must be enabled in this mode for checkToUseFilmEffect().
                call(module, "setSettingMenuEnable", new Class<?>[]{String.class, boolean.class},
                        "key_film_emulator", true);
            }
            Log.i(TAG, "film gate: mode=" + mode + gate(module));
        } catch (Throwable t) {
            Log.e(TAG, "film gate failed", t);
        }
    }

    /**
     * Replacement for MediaRecorderEx.setAudioEncoder(). LG's "Hi-Fi" recording uses encoder 12,
     * which does not exist in AOSP and makes MediaRecorder throw "Invalid audio encoder" and kill
     * the app. Record AAC instead (the 24-bit parameters are ignored by the AOSP stack anyway).
     */
    public static void setAudioEncoder(Object recorder, int value) {
        int mapped = (value == 12) ? 3 : value;
        if (mapped != value) Log.i(TAG, "audio encoder " + value + " -> " + mapped + " (AAC)");
        try {
            recorder.getClass().getMethod("setAudioEncoder", int.class).invoke(recorder, mapped);
        } catch (Throwable t) {
            Log.e(TAG, "setAudioEncoder failed", t);
        }
    }

    /** Hook at CinemaModuleBase.stopRecorder(): burn the selected look into the saved MP4. */
    public static void onCinemaStopRecorder(Object module) {
        try {
            int lut = prefs().getInt(PREF_LUT, 1);
            String path = sCurrentLutPath;
            int strength = sStrength;
            Log.i(TAG, "cine stop: lut=" + lut + " strength=" + strength + " path=" + path);
            if (lut <= 1 || path == null) return;
            String video = null;
            try {
                Class<?> vr = Class.forName("com.lge.camera.app.VideoRecorder");
                Object v = vr.getMethod("getFilePath").invoke(null);
                if (v instanceof String) video = (String) v;
            } catch (Throwable ignored) {}
            final String videoPath = video;
            Thread t = new Thread(() -> CineVideoPost.processLatest(Compat.sApp, videoPath, path, strength), "cine-post");
            t.start();
        } catch (Throwable t) {
            Log.e(TAG, "cine stop hook failed", t);
        }
    }

    public static int currentLut() {
        try { return prefs().getInt(PREF_LUT, 1); } catch (Throwable t) { return 1; }
    }

    public static String currentLutPath() { return sCurrentLutPath; }

    public static int currentStrength() { return sStrength; }

    // ---------------------------------------------------------------- helpers

    private static void setFilm(Object module, String path) throws Exception {
        call(module, "setSetting", new Class<?>[]{String.class, String.class, boolean.class},
                "key_film_emulator", path, false);
        call(module, "setSettingMenuEnable", new Class<?>[]{String.class, boolean.class},
                "key_film_emulator", true);
        // The film menu keeps its own "last selected" copy and would otherwise restore 0_film.dat.
        Context ctx = Compat.sApp;
        if (ctx != null) {
            ctx.getSharedPreferences("Main_CameraAppConfig", Context.MODE_PRIVATE)
                    .edit().putString("last_select_filter", path).apply();
        }
    }

    private static String lutPath(int lut) {
        if (lut <= 1) return IDENTITY;
        int n = Math.min(lut, 16);
        return String.format(Locale.US, "%s%d_Cine %02d_.dat", DIR, n + 9, n);
    }

    private static SharedPreferences prefs() {
        Context ctx = Compat.sApp;
        if (ctx == null) {
            throw new IllegalStateException("no app context yet");
        }
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Object field(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                Log.w(TAG, "field " + name + " failed: " + t);
                return null;
            }
        }
        return null;
    }

    private static int intField(Object o, String name) {
        Object v = field(o, name);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private static Object call(Object o, String name, Class<?>[] sig, Object... args) throws Exception {
        Method m = null;
        for (Class<?> c = o.getClass(); c != null && m == null; c = c.getSuperclass()) {
            try {
                m = c.getDeclaredMethod(name, sig);
            } catch (NoSuchMethodException ignored) {}
        }
        if (m == null) throw new NoSuchMethodException(name);
        m.setAccessible(true);
        return m.invoke(o, args);
    }

    private static String gate(Object module) {
        try {
            Object film = field(module, "mAdvancedFilmManager");
            return " [enabled=" + call(module, "getSettingMenuEnable", new Class<?>[]{String.class}, "key_film_emulator")
                    + " value=" + call(module, "getSettingValue", new Class<?>[]{String.class}, "key_film_emulator")
                    + " running=" + (film != null && Boolean.TRUE.equals(call(film, "isRunningFilmEmulator", new Class<?>[]{})))
                    + " uhd=" + call(module, "isUHDmode", new Class<?>[]{})
                    + " fhd60=" + call(module, "isFHD60", new Class<?>[]{})
                    + "]";
        } catch (Throwable t) {
            return " [gate log failed: " + t + "]";
        }
    }
}
