package com.lge.systemservice.core;
import android.content.Context;
import android.util.Log;
public class LGContext {
    private final Context mContext;
    public LGContext(Context c) { mContext = c; }
    public Object getLGSystemService(String name) {
        if (name == null) return null;
        switch (name) {
            case "osservice": return new OsManager(mContext);
            case "lgthermal": return new LGThermalManager(mContext);
            case "lgpowermanagerhelper": return new LGPowerManagerHelper();
            case "smartcover": return new SmartCoverManager();
            case "volumevibrator": return new VolumeVibratorManager();
            case "emotionled": return new LEDManager();
            case "wfdService": return new WfdManager();
            default:
                Log.d("LGContextShim", "unbacked LG system service: " + name);
                return null;
        }
    }
}
