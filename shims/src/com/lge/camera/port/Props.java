package com.lge.camera.port;
import android.util.Log;
import java.lang.reflect.Method;
/** android.os.SystemProperties via reflection (available to platform-signed apps). */
public final class Props {
    private static Method sGet, sSet;
    public static String get(String key, String def) {
        try {
            if (sGet == null) sGet = Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class);
            return (String) sGet.invoke(null, key, def);
        } catch (Throwable t) {
            return def;
        }
    }
    public static void set(String key, String value) {
        try {
            if (sSet == null) sSet = Class.forName("android.os.SystemProperties").getMethod("set", String.class, String.class);
            sSet.invoke(null, key, value);
        } catch (Throwable t) {
            Log.d("LGCameraPort", "cannot set " + key + ": " + t.getCause());
        }
    }
}
