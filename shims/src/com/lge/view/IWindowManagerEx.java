package com.lge.view;
import android.os.IBinder;
import java.util.Collections;
import java.util.List;
public interface IWindowManagerEx {
    List getWindowInfoList(int displayId, boolean visibleOnly);
    abstract class Stub {
        public static IWindowManagerEx asInterface(IBinder b) {
            return (d, v) -> Collections.emptyList();
        }
    }
}
