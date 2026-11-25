package com.lge.appbox.updateproxy;
import java.util.Collections;
import java.util.Iterator;
public class AppList implements Iterable<AppInfo> {
    public boolean isSuccess() { return false; }
    public Iterator<AppInfo> iterator() { return Collections.<AppInfo>emptyList().iterator(); }
}
