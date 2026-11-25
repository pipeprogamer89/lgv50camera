package com.lge.systemservice.core;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import com.lge.camera.port.Props;
public class OsManager {
    private static final String TAG = "OsManagerShim";
    private final Context mContext;
    OsManager(Context c) { mContext = c; }
    public void goToSleepWithForce(long time, int reason) {
        try {
            PowerManager pm = mContext.getSystemService(PowerManager.class);
            PowerManager.class.getMethod("goToSleep", long.class).invoke(pm, SystemClock.uptimeMillis());
        } catch (Throwable t) {
            Log.w(TAG, "goToSleep unavailable: " + t);
        }
    }
    public IBinder makePersistent(String pkg) { return new Binder(); }
    public void makeNonPersistent(IBinder token) {}
    public void makeNonPersistentUsingPackageName(String pkg) {}
    public void setSystemProperty(String key, String value) { Props.set(key, value); }
}
