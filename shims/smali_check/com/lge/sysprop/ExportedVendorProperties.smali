.class public final Lcom/lge/sysprop/ExportedVendorProperties;
.super Ljava/lang/Object;
.source "ExportedVendorProperties.java"


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 4
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static build_default_country()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 9
    const-string v0, "ro.vendor.lge.build.default_country"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static data_front_minfps()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 10
    const-string v0, "ro.vendor.lge.data.front_minfps"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static data_rear_minfps()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 11
    const-string v0, "ro.vendor.lge.data.rear_minfps"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static dev_fmode()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 12
    const-string v0, "ro.vendor.lge.dev.fmode"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static dev_fmode_exif()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 13
    const-string v0, "ro.vendor.lge.dev.fmode_exif"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static dev_fmode_exif_atnt()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 14
    const-string v0, "ro.vendor.lge.dev.fmode_exif_atnt"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static livedemounit()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 15
    const-string v0, "ro.vendor.lge.livedemounit"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method public static opensw()Ljava/util/Optional;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 16
    const-string v0, "ro.vendor.lge.opensw"

    invoke-static {v0}, Lcom/lge/sysprop/ExportedVendorProperties;->p(Ljava/lang/String;)Ljava/util/Optional;

    move-result-object v0

    return-object v0
.end method

.method private static p(Ljava/lang/String;)Ljava/util/Optional;
    .registers 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            ")",
            "Ljava/util/Optional<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .line 6
    const-string v0, ""

    invoke-static {p0, v0}, Lcom/lge/camera/port/Props;->get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    .line 7
    invoke-virtual {p0}, Ljava/lang/String;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_11

    invoke-static {}, Ljava/util/Optional;->empty()Ljava/util/Optional;

    move-result-object p0

    goto :goto_15

    :cond_11
    invoke-static {p0}, Ljava/util/Optional;->of(Ljava/lang/Object;)Ljava/util/Optional;

    move-result-object p0

    :goto_15
    return-object p0
.end method
