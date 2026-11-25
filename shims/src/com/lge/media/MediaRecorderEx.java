package com.lge.media;
import android.media.MediaRecorder;
import android.util.Log;
import java.lang.reflect.Method;
public class MediaRecorderEx extends MediaRecorder {
    private static final String TAG = "MediaRecorderExShim";
    private static Method sSetParameter;
    public MediaRecorderEx() { super(); }
    /** Forwards to the (hidden) AOSP MediaRecorder.setParameter; unknown LG keys are ignored by stagefright. */
    public void setParameter(String nameValuePair) {
        try {
            if (sSetParameter == null) {
                sSetParameter = MediaRecorder.class.getDeclaredMethod("setParameter", String.class);
                sSetParameter.setAccessible(true);
            }
            sSetParameter.invoke(this, nameValuePair);
        } catch (Throwable t) {
            Log.w(TAG, "setParameter failed: " + nameValuePair, t);
        }
    }
    /**
     * Cine Video / HDR10 ask for HEVC Main10 HDR10 (0x1000) or Main10 (2). The port records
     * 8-bit (the 10-bit camera path does not work on LineageOS), so encode HEVC Main instead.
     */
    @Override
    public void setVideoEncodingProfileLevel(int profile, int level) {
        if (profile == 0x1000 || profile == 2 || profile == 0x2000) {
            Log.i(TAG, "HEVC 10-bit profile " + profile + " -> Main (8-bit)");
            profile = 1;
        }
        super.setVideoEncodingProfileLevel(profile, level);
    }

    public void setAudioZooming() { Log.d(TAG, "setAudioZooming ignored"); }
    public void changeMaxFileSize(long size) {
        try { setMaxFileSize(size); } catch (Throwable t) { Log.d(TAG, "changeMaxFileSize ignored: " + t); }
    }
}
