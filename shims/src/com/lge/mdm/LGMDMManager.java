package com.lge.mdm;
import android.content.ComponentName;
import java.util.List;
/** No device policy: everything allowed. */
public class LGMDMManager {
    private static final LGMDMManager INSTANCE = new LGMDMManager();
    public static LGMDMManager getInstance() { return INSTANCE; }
    public boolean getAllowCameraWithWhitelist(ComponentName who) { return true; }
    public boolean getAllowMicrophone(ComponentName who) { return true; }
    public List getApplicationState(ComponentName who) { return new java.util.ArrayList(); }
    public List getCameraWhitelist(ComponentName who) { return null; }
    public List getMicrophoneWhitelist(ComponentName who) { return null; }
}
