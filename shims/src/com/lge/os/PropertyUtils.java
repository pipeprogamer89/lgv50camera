package com.lge.os;

import android.util.SparseArray;
import com.lge.camera.port.Props;

/**
 * LG resolves numeric property ids to names in its framework. Ids used by the camera
 * (see com.lge.camera.util.SysPropUtils) with the names found on V50 stock firmware.
 * Missing numeric properties fall back to "0" (the app parses them with Integer.parseInt).
 */
public class PropertyUtils {
    private static final PropertyUtils INSTANCE = new PropertyUtils();
    private static final SparseArray<String[]> NAMES = new SparseArray<>();
    static {
        // id -> { property, fallback when unset }
        NAMES.put(0x898, new String[]{"persist.vendor.lge.service.main.enable", "0"});
        NAMES.put(0x4b1, new String[]{"persist.sys.store_demo_enabled", "0"});
        NAMES.put(0x76c, new String[]{"vendor.lge.hw.camcorder.fpsrange", null});
        NAMES.put(0x770, new String[]{"ro.vendor.lge.dev.fmode", "0"});
        NAMES.put(0x76d, new String[]{"ro.vendor.lge.dev.fmode_exif", "0"});
        NAMES.put(0x76e, new String[]{"ro.vendor.lge.dev.fmode_exif_atnt", "0"});
        NAMES.put(0x70a, new String[]{"ro.vendor.lge.livedemounit", "0"});
        NAMES.put(0x771, new String[]{"ro.vendor.lge.data.front_minfps", null});
        NAMES.put(0x772, new String[]{"ro.vendor.lge.data.rear_minfps", null});
        NAMES.put(0xabb, new String[]{"ro.vendor.lge.build.default_country", null});
        NAMES.put(0xaca, new String[]{"ro.vendor.lge.opensw", null});
    }

    public static PropertyUtils getInstance() { return INSTANCE; }

    public String get(int id, String def) {
        String[] e = NAMES.get(id);
        if (e == null) return def;
        // vendor_lge_misc_prop is not readable by apps under AOSP sepolicy: allow enabling
        // LG's CamLog via a debug property instead (adb shell setprop debug.lgcamera.log 1).
        if (id == 0x898 && "1".equals(Props.get("debug.lgcamera.log", ""))) return "1";
        String v = Props.get(e[0], "");
        if (!v.isEmpty()) return v;
        if (def != null && !def.isEmpty()) return def;
        return e[1] != null ? e[1] : def;
    }
}
