package android.widget;
import android.content.Context;
import android.content.Intent;
import java.util.LinkedHashMap;
/** Stub of LG's share-target ranking provider (QuickClip). Not available on AOSP. */
public class ShareActionProviderEx {
    private Intent mIntent;
    public ShareActionProviderEx(Context c) {}
    public void setShareIntent(Intent i) { mIntent = i; }
    public LinkedHashMap getSortedListAndWeight() { return new LinkedHashMap(); }
    public Intent activitySelected(String component) { return null; }
}
