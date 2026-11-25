package com.lge.app.permission;
import android.content.Context;
public class DefaultUiProvider implements GuideUiProvider {
    public DefaultUiProvider() {}
    public CharSequence getReasonForRequestingPermissions(Context context, String[] permissions) { return null; }
}
