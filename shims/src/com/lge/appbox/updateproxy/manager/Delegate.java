package com.lge.appbox.updateproxy.manager;
import com.lge.appbox.updateproxy.AppInfo;
import com.lge.appbox.updateproxy.RequestForm;
public interface Delegate {
    void downloadAndInstallAppAsync(AppInfo info);
    void registeSelfUpdateCallback(ISelfUpdateServiceCallback cb);
    void retrieveAppListAsync(RequestForm form);
    void unregisteSelfUpdateCallback(ISelfUpdateServiceCallback cb);
}
