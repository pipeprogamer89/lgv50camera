package com.lge.sysprop;
import java.util.Optional;
import com.lge.camera.port.Props;
public final class ExportedVendorProperties {
    private static Optional<String> p(String k) {
        String v = Props.get(k, "");
        return v.isEmpty() ? Optional.empty() : Optional.of(v);
    }
    public static Optional<String> build_default_country() { return p("ro.vendor.lge.build.default_country"); }
    public static Optional<String> data_front_minfps() { return p("ro.vendor.lge.data.front_minfps"); }
    public static Optional<String> data_rear_minfps() { return p("ro.vendor.lge.data.rear_minfps"); }
    public static Optional<String> dev_fmode() { return p("ro.vendor.lge.dev.fmode"); }
    public static Optional<String> dev_fmode_exif() { return p("ro.vendor.lge.dev.fmode_exif"); }
    public static Optional<String> dev_fmode_exif_atnt() { return p("ro.vendor.lge.dev.fmode_exif_atnt"); }
    public static Optional<String> livedemounit() { return p("ro.vendor.lge.livedemounit"); }
    public static Optional<String> opensw() { return p("ro.vendor.lge.opensw"); }
}
