package com.lge.systemservice.core;
import android.content.Context;
import android.os.PowerManager;
/** Approximates LG thermal actions with the AOSP thermal status. */
public class LGThermalManager {
    private final Context mContext;
    LGThermalManager(Context c) { mContext = c; }
    public boolean isThermalActionTriggered(String action) {
        try {
            PowerManager pm = mContext.getSystemService(PowerManager.class);
            return pm != null && pm.getCurrentThermalStatus() >= PowerManager.THERMAL_STATUS_SEVERE;
        } catch (Throwable t) {
            return false;
        }
    }
}
