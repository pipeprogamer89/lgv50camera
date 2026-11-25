package com.lge.appbox.updateproxy.manager;
import android.content.Context;
/** LG App Box self-update is not available outside LG firmware: never connects. */
public class SelfUpdateBinder {
    public interface ISelfupdateConnection {
        void onConnected(Delegate d);
        void onDisConnected();
    }
    private static final SelfUpdateBinder INSTANCE = new SelfUpdateBinder();
    public static SelfUpdateBinder getInstance(Context c) { return INSTANCE; }
    public boolean bind(ISelfupdateConnection c) { return false; }
}
