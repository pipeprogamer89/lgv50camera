package com.lge.media;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * LG's CamcorderProfileEx on top of AOSP CamcorderProfile.
 * LG-only qualities (60fps, HFR, HEVC variants) were served by LG's native media framework;
 * here they are synthesized from the AOSP profile with the same resolution.
 * Constants are copied from the V50 stock framework.jar.
 */
public class CamcorderProfileEx {
    private static final String TAG = "CamcorderProfileExShim";
    public static final int QUALITY_1080P = 0x6;
    public static final int QUALITY_2160P = 0x8;
    public static final int QUALITY_480P = 0x4;
    public static final int QUALITY_4kDCI = 0xd;
    public static final int QUALITY_4kUHD = 0x8;
    public static final int QUALITY_720P = 0x5;
    public static final int QUALITY_8KUHD = 0xbb9;
    public static final int QUALITY_CIF = 0x3;
    public static final int QUALITY_FWVGA = 0xc;
    public static final int QUALITY_HEVC1080P = 0xf;
    public static final int QUALITY_HEVC4kDCI = 0x11;
    public static final int QUALITY_HEVC4kUHD = 0x10;
    public static final int QUALITY_HEVC720P = 0xe;
    public static final int QUALITY_HEVC8KUHD = 0x12;
    public static final int QUALITY_HFR720P = 0x7d3;
    public static final int QUALITY_HIGH = 0x1;
    public static final int QUALITY_HIGH_SPEED_1080P = 0x7d4;
    public static final int QUALITY_HIGH_SPEED_480P = 0x7d2;
    public static final int QUALITY_HIGH_SPEED_720P = 0x7d3;
    public static final int QUALITY_HIGH_SPEED_HIGH = 0x7d1;
    public static final int QUALITY_HIGH_SPEED_LOW = 0x7d0;
    public static final int QUALITY_LGE_1080P_120 = 0x271d;
    public static final int QUALITY_LGE_1080P_240 = 0x271e;
    public static final int QUALITY_LGE_1080P_480 = 0x271f;
    public static final int QUALITY_LGE_1080P_60 = 0x271c;
    public static final int QUALITY_LGE_2160P_120 = 0x272f;
    public static final int QUALITY_LGE_2160P_60 = 0x2725;
    public static final int QUALITY_LGE_720P_120 = 0x2721;
    public static final int QUALITY_LGE_720P_240 = 0x2722;
    public static final int QUALITY_LGE_720P_480 = 0x2723;
    public static final int QUALITY_LGE_720P_60 = 0x2720;
    public static final int QUALITY_LGE_720P_960 = 0x2724;
    public static final int QUALITY_LGE_HEVC1080P_120 = 0x2728;
    public static final int QUALITY_LGE_HEVC1080P_240 = 0x2729;
    public static final int QUALITY_LGE_HEVC1080P_480 = 0x272a;
    public static final int QUALITY_LGE_HEVC1080P_60 = 0x2727;
    public static final int QUALITY_LGE_HEVC2160P_120 = 0x2730;
    public static final int QUALITY_LGE_HEVC2160P_60 = 0x2726;
    public static final int QUALITY_LGE_HEVC720P_120 = 0x272b;
    public static final int QUALITY_LGE_HEVC720P_240 = 0x272c;
    public static final int QUALITY_LGE_HEVC720P_480 = 0x272d;
    public static final int QUALITY_LGE_HEVC720P_960 = 0x272e;
    public static final int QUALITY_LOW = 0x0;
    public static final int QUALITY_QCIF = 0x2;
    public static final int QUALITY_QVGA = 0x7;
    public static final int QUALITY_TIME_LAPSE_1080P = 0x3ee;
    public static final int QUALITY_TIME_LAPSE_2160P = 0x3f0;
    public static final int QUALITY_TIME_LAPSE_480P = 0x3ec;
    public static final int QUALITY_TIME_LAPSE_720P = 0x3ed;
    public static final int QUALITY_TIME_LAPSE_CIF = 0x3eb;
    public static final int QUALITY_TIME_LAPSE_FWVGA = 0x3f1;
    public static final int QUALITY_TIME_LAPSE_HEVC1080P = 0x3f6;
    public static final int QUALITY_TIME_LAPSE_HEVC2160P = 0x3f7;
    public static final int QUALITY_TIME_LAPSE_HEVC4320P = 0x3f8;
    public static final int QUALITY_TIME_LAPSE_HEVC720P = 0x3f5;
    public static final int QUALITY_TIME_LAPSE_HIGH = 0x3e9;
    public static final int QUALITY_TIME_LAPSE_LOW = 0x3e8;
    public static final int QUALITY_TIME_LAPSE_QCIF = 0x3ea;
    public static final int QUALITY_TIME_LAPSE_QVGA = 0x3ef;
    public static final int QUALITY_TIME_LAPSE_VGA = 0x3f3;
    public static final int QUALITY_TIME_LAPSE_WQVGA = 0x3f4;
    public static final int QUALITY_TIME_LAPSE_WVGA = 0x3f2;
    public static final int QUALITY_VGA = 0xa;
    public static final int QUALITY_WQVGA = 0xb;
    public static final int QUALITY_WVGA = 0x9;

    public int duration, quality, fileFormat, videoCodec, videoBitRate, videoFrameRate,
            videoFrameWidth, videoFrameHeight, audioCodec, audioBitRate, audioSampleRate, audioChannels;

    /** {base AOSP quality, fps (0 = keep), hevc (0/1), high-speed AOSP quality or -1} */
    private static int[] spec(int q) {
        switch (q) {
            case QUALITY_LGE_720P_60:   return new int[]{QUALITY_720P, 60, 0, -1};
            case QUALITY_LGE_720P_120:  return new int[]{QUALITY_720P, 120, 0, CamcorderProfile.QUALITY_HIGH_SPEED_720P};
            case QUALITY_LGE_720P_240:  return new int[]{QUALITY_720P, 240, 0, CamcorderProfile.QUALITY_HIGH_SPEED_720P};
            case QUALITY_LGE_1080P_60:  return new int[]{QUALITY_1080P, 60, 0, -1};
            case QUALITY_LGE_1080P_120: return new int[]{QUALITY_1080P, 120, 0, CamcorderProfile.QUALITY_HIGH_SPEED_1080P};
            case QUALITY_LGE_1080P_240: return new int[]{QUALITY_1080P, 240, 0, CamcorderProfile.QUALITY_HIGH_SPEED_1080P};
            case QUALITY_LGE_2160P_60:  return new int[]{QUALITY_2160P, 60, 0, -1};
            case QUALITY_LGE_HEVC1080P_60:  return new int[]{QUALITY_1080P, 60, 1, -1};
            case QUALITY_LGE_HEVC1080P_120: return new int[]{QUALITY_1080P, 120, 1, CamcorderProfile.QUALITY_HIGH_SPEED_1080P};
            case QUALITY_LGE_HEVC1080P_240: return new int[]{QUALITY_1080P, 240, 1, CamcorderProfile.QUALITY_HIGH_SPEED_1080P};
            case QUALITY_LGE_HEVC2160P_60:  return new int[]{QUALITY_2160P, 60, 1, -1};
            case QUALITY_LGE_HEVC720P_240:  return new int[]{QUALITY_720P, 240, 1, CamcorderProfile.QUALITY_HIGH_SPEED_720P};
            case QUALITY_LGE_HEVC720P_480:  return null;
            case QUALITY_LGE_720P_480:      return null;
            case QUALITY_HEVC720P:   return new int[]{QUALITY_720P, 0, 1, -1};
            case QUALITY_HEVC1080P:  return new int[]{QUALITY_1080P, 0, 1, -1};
            case QUALITY_HEVC4kUHD:  return new int[]{QUALITY_2160P, 0, 1, -1};
            case QUALITY_HEVC4kDCI:
            case QUALITY_HEVC8KUHD:
            case QUALITY_8KUHD:
            case QUALITY_4kDCI:
            case QUALITY_FWVGA:
            case QUALITY_TIME_LAPSE_FWVGA:
            case QUALITY_TIME_LAPSE_HEVC720P:
            case QUALITY_TIME_LAPSE_HEVC1080P:
            case QUALITY_TIME_LAPSE_HEVC2160P:
            case QUALITY_TIME_LAPSE_HEVC4320P:
                return null;
            default:
                return new int[]{q, 0, 0, -1};   // standard AOSP quality id
        }
    }

    private static boolean aospHas(int cameraId, int q) {
        try { return CamcorderProfile.hasProfile(cameraId, q); } catch (Throwable t) { return false; }
    }

    public static boolean hasProfile(int quality) { return hasProfile(0, quality); }

    public static boolean hasProfile(int cameraId, int quality) {
        int[] s = spec(quality);
        boolean r = s != null && aospHas(cameraId, s[0]) && (s[3] < 0 || aospHas(cameraId, s[3]));
        Log.d(TAG, "hasProfile cam=" + cameraId + " q=" + quality + " -> " + r);
        return r;
    }

    public static CamcorderProfileEx get(int quality) { return get(0, quality); }

    public static CamcorderProfileEx get(int cameraId, int quality) {
        int[] s = spec(quality);
        if (s == null || !aospHas(cameraId, s[0])) {
            Log.w(TAG, "no profile cam=" + cameraId + " q=" + quality);
            return null;
        }
        CamcorderProfile p = CamcorderProfile.get(cameraId, s[0]);
        CamcorderProfileEx e = new CamcorderProfileEx();
        e.duration = p.duration; e.quality = quality; e.fileFormat = p.fileFormat;
        e.videoCodec = p.videoCodec; e.videoBitRate = p.videoBitRate; e.videoFrameRate = p.videoFrameRate;
        e.videoFrameWidth = p.videoFrameWidth; e.videoFrameHeight = p.videoFrameHeight;
        e.audioCodec = p.audioCodec; e.audioBitRate = p.audioBitRate;
        e.audioSampleRate = p.audioSampleRate; e.audioChannels = p.audioChannels;
        if (s[1] > 0 && p.videoFrameRate > 0 && s[1] != p.videoFrameRate) {
            // scale bitrate with frame rate (capped at 2x for >60 fps, as LG did for HFR)
            float k = Math.min((float) s[1] / p.videoFrameRate, s[1] > 60 ? 2f : 2f);
            e.videoBitRate = (int) (p.videoBitRate * k);
            e.videoFrameRate = s[1];
        }
        if (s[2] == 1) {
            e.videoCodec = MediaRecorder.VideoEncoder.HEVC;
            e.videoBitRate = (int) (e.videoBitRate * 0.6f);
        }
        return e;
    }

    /** LG manual-video fps/bitrate table (native LG data); not available on AOSP. */
    public static String getManualSupportedList(int cameraId, int quality, float ratio) { return "NotSupport"; }
}
