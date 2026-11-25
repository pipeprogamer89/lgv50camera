package com.lge.app.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import java.util.ArrayList;
public final class RequestPermissionsHelper {
    public static boolean hasPermissions(Context ctx, String[] perms) {
        if (perms == null) return true;
        for (String p : perms) {
            if (p != null && ctx.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }
    /** Returns true when a request was started (caller must wait), false when all are granted. */
    public static boolean requestPermissionsIfNeeded(Activity a, String[] perms, GuideUiProvider ui) {
        if (hasPermissions(a, perms)) return false;
        ArrayList<String> missing = new ArrayList<>();
        for (String p : perms) {
            if (p != null && a.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
        }
        a.requestPermissions(missing.toArray(new String[0]), 0);
        return true;
    }
}
