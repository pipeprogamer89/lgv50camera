package com.lge.systemservice.core;
/** The V50 has no LG "emotion" LED controllable from AOSP. */
public class LEDManager {
    public int startPattern(String pkg, int id, LGLedRecord rec) { return 0; }
    public int stopPattern(String pkg, int id) { return 0; }
}
