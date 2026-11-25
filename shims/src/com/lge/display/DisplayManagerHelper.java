package com.lge.display;
import android.content.Context;
/** No LG Dual Screen / cover support on AOSP: always "no accessory attached". */
public class DisplayManagerHelper {
    public static final int STATE_COVER_OPENED = 0;
    public static final int STATE_UNMOUNT = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_ENABLED = 3;
    public static class CoverDisplayCallback {
        public CoverDisplayCallback() {}
        public void onCoverDisplayEnabledChangedCallback(int state) {}
    }
    public static class SmartCoverCallback {
        public SmartCoverCallback() {}
        public void onStateChanged(int state) {}
    }
    public DisplayManagerHelper(Context c) {}
    public int getCoverDisplayState() { return STATE_UNMOUNT; }
    public int getCoverState() { return STATE_COVER_OPENED; }
}
