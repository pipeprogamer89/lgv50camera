package com.lge.content.pm;
import android.content.Context;
import android.graphics.drawable.Drawable;
public class PackageManagerEx {
    private static final PackageManagerEx INSTANCE = new PackageManagerEx();
    public static PackageManagerEx getDefault() { return INSTANCE; }
    public Drawable getIconDrawableAsIconFrameTheme(Context c, Drawable d, String pkg, int flags) { return d; }
}
