package com.lge.appbox.updateproxy.manager;
import com.lge.appbox.updateproxy.AppList;
public class ISelfUpdateServiceCallback {
    public ISelfUpdateServiceCallback() {}
    public void onDownloadStatus(String pkg, int a, int b, long c, long d) {}
    public void onInstallStatus(String pkg, int a, int b) {}
    public void onReceiveAppList(AppList list) {}
}
