.class public final Lcom/lge/camera/port/PortShim;
.super Ljava/lang/Object;
.source "PortShim.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/lge/camera/port/PortShim$Callbacks;
    }
.end annotation


# static fields
.field private static final ASSET:Ljava/lang/String; = "lge-res-lite.apk"

.field private static final SYSTEM_PATHS:[Ljava/lang/String;

.field private static final TAG:Ljava/lang/String; = "LGCameraPort"

.field private static sLoader:Landroid/content/res/loader/ResourcesLoader;


# direct methods
.method static constructor <clinit>()V
    .registers 3

    .line 26
    const-string v0, "/system_ext/etc/lgcamera/lge-res-lite.apk"

    const-string v1, "/system/etc/lgcamera/lge-res-lite.apk"

    const-string v2, "/product/etc/lgcamera/lge-res-lite.apk"

    filled-new-array {v2, v0, v1}, [Ljava/lang/String;

    move-result-object v0

    sput-object v0, Lcom/lge/camera/port/PortShim;->SYSTEM_PATHS:[Ljava/lang/String;

    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .line 23
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static attach(Landroid/content/res/Resources;)V
    .registers 4

    .line 45
    sget-object v0, Lcom/lge/camera/port/PortShim;->sLoader:Landroid/content/res/loader/ResourcesLoader;

    if-eqz v0, :cond_1c

    if-nez p0, :cond_7

    goto :goto_1c

    .line 47
    :cond_7
    const/4 v0, 0x1

    :try_start_8
    new-array v0, v0, [Landroid/content/res/loader/ResourcesLoader;

    sget-object v1, Lcom/lge/camera/port/PortShim;->sLoader:Landroid/content/res/loader/ResourcesLoader;

    const/4 v2, 0x0

    aput-object v1, v0, v2

    invoke-virtual {p0, v0}, Landroid/content/res/Resources;->addLoaders([Landroid/content/res/loader/ResourcesLoader;)V
    :try_end_12
    .catchall {:try_start_8 .. :try_end_12} :catchall_13

    .line 50
    goto :goto_1b

    .line 48
    :catchall_13
    move-exception p0

    .line 49
    const-string v0, "LGCameraPort"

    const-string v1, "addLoaders failed"

    invoke-static {v0, v1, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 51
    :goto_1b
    return-void

    .line 45
    :cond_1c
    :goto_1c
    return-void
.end method

.method private static createLoader(Landroid/content/Context;)Landroid/content/res/loader/ResourcesLoader;
    .registers 7
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .line 54
    nop

    .line 55
    sget-object v0, Lcom/lge/camera/port/PortShim;->SYSTEM_PATHS:[Ljava/lang/String;

    array-length v1, v0

    const/4 v2, 0x0

    move v3, v2

    :goto_6
    if-ge v3, v1, :cond_19

    aget-object v4, v0, v3

    .line 56
    new-instance v5, Ljava/io/File;

    invoke-direct {v5, v4}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 57
    invoke-virtual {v5}, Ljava/io/File;->canRead()Z

    move-result v4

    if-eqz v4, :cond_16

    goto :goto_1a

    .line 55
    :cond_16
    add-int/lit8 v3, v3, 0x1

    goto :goto_6

    :cond_19
    const/4 v5, 0x0

    .line 59
    :goto_1a
    if-nez v5, :cond_74

    .line 61
    new-instance v5, Ljava/io/File;

    invoke-virtual {p0}, Landroid/content/Context;->getCodeCacheDir()Ljava/io/File;

    move-result-object v0

    const-string v1, "lge-res-lite.apk"

    invoke-direct {v5, v0, v1}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 62
    invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object p0

    invoke-virtual {p0, v1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object p0

    .line 63
    :try_start_2f
    invoke-virtual {v5}, Ljava/io/File;->exists()Z

    move-result v0

    if-eqz v0, :cond_42

    invoke-virtual {v5}, Ljava/io/File;->length()J

    move-result-wide v0

    invoke-virtual {p0}, Ljava/io/InputStream;->available()I

    move-result v3

    int-to-long v3, v3

    cmp-long v0, v0, v3

    if-eqz v0, :cond_58

    .line 64
    :cond_42
    new-instance v0, Ljava/io/FileOutputStream;

    invoke-direct {v0, v5}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_47
    .catchall {:try_start_2f .. :try_end_47} :catchall_68

    .line 65
    const/high16 v1, 0x10000

    :try_start_49
    new-array v1, v1, [B

    .line 67
    :goto_4b
    invoke-virtual {p0, v1}, Ljava/io/InputStream;->read([B)I

    move-result v3

    if-lez v3, :cond_55

    invoke-virtual {v0, v1, v2, v3}, Ljava/io/OutputStream;->write([BII)V
    :try_end_54
    .catchall {:try_start_49 .. :try_end_54} :catchall_5e

    goto :goto_4b

    .line 68
    :cond_55
    :try_start_55
    invoke-virtual {v0}, Ljava/io/OutputStream;->close()V
    :try_end_58
    .catchall {:try_start_55 .. :try_end_58} :catchall_68

    .line 70
    :cond_58
    if-eqz p0, :cond_5d

    invoke-virtual {p0}, Ljava/io/InputStream;->close()V

    .line 71
    :cond_5d
    goto :goto_74

    .line 64
    :catchall_5e
    move-exception v1

    :try_start_5f
    invoke-virtual {v0}, Ljava/io/OutputStream;->close()V
    :try_end_62
    .catchall {:try_start_5f .. :try_end_62} :catchall_63

    goto :goto_67

    :catchall_63
    move-exception v0

    :try_start_64
    invoke-virtual {v1, v0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_67
    throw v1
    :try_end_68
    .catchall {:try_start_64 .. :try_end_68} :catchall_68

    .line 62
    :catchall_68
    move-exception v0

    if-eqz p0, :cond_73

    :try_start_6b
    invoke-virtual {p0}, Ljava/io/InputStream;->close()V
    :try_end_6e
    .catchall {:try_start_6b .. :try_end_6e} :catchall_6f

    goto :goto_73

    :catchall_6f
    move-exception p0

    invoke-virtual {v0, p0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :cond_73
    :goto_73
    throw v0

    .line 73
    :cond_74
    :goto_74
    new-instance p0, Landroid/content/res/loader/ResourcesLoader;

    invoke-direct {p0}, Landroid/content/res/loader/ResourcesLoader;-><init>()V

    .line 74
    const/high16 v0, 0x10000000

    invoke-static {v5, v0}, Landroid/os/ParcelFileDescriptor;->open(Ljava/io/File;I)Landroid/os/ParcelFileDescriptor;

    move-result-object v0

    .line 75
    :try_start_7f
    invoke-static {v0}, Landroid/content/res/loader/ResourcesProvider;->loadFromApk(Landroid/os/ParcelFileDescriptor;)Landroid/content/res/loader/ResourcesProvider;

    move-result-object v1

    invoke-virtual {p0, v1}, Landroid/content/res/loader/ResourcesLoader;->addProvider(Landroid/content/res/loader/ResourcesProvider;)V
    :try_end_86
    .catchall {:try_start_7f .. :try_end_86} :catchall_a8

    .line 76
    if-eqz v0, :cond_8b

    invoke-virtual {v0}, Landroid/os/ParcelFileDescriptor;->close()V

    .line 77
    :cond_8b
    invoke-static {v5}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v0

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "loaded "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "LGCameraPort"

    invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 78
    return-object p0

    .line 74
    :catchall_a8
    move-exception p0

    if-eqz v0, :cond_b3

    :try_start_ab
    invoke-virtual {v0}, Landroid/os/ParcelFileDescriptor;->close()V
    :try_end_ae
    .catchall {:try_start_ab .. :try_end_ae} :catchall_af

    goto :goto_b3

    :catchall_af
    move-exception v0

    invoke-virtual {p0, v0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :cond_b3
    :goto_b3
    throw p0
.end method

.method public static init(Landroid/app/Application;)V
    .registers 4

    .line 35
    const-string v0, "LGCameraPort"

    :try_start_2
    invoke-static {p0}, Lcom/lge/camera/port/PortShim;->createLoader(Landroid/content/Context;)Landroid/content/res/loader/ResourcesLoader;

    move-result-object v1

    sput-object v1, Lcom/lge/camera/port/PortShim;->sLoader:Landroid/content/res/loader/ResourcesLoader;

    .line 36
    invoke-virtual {p0}, Landroid/app/Application;->getResources()Landroid/content/res/Resources;

    move-result-object v1

    invoke-static {v1}, Lcom/lge/camera/port/PortShim;->attach(Landroid/content/res/Resources;)V

    .line 37
    new-instance v1, Lcom/lge/camera/port/PortShim$Callbacks;

    const/4 v2, 0x0

    invoke-direct {v1, v2}, Lcom/lge/camera/port/PortShim$Callbacks;-><init>(Lcom/lge/camera/port/PortShim-IA;)V

    invoke-virtual {p0, v1}, Landroid/app/Application;->registerActivityLifecycleCallbacks(Landroid/app/Application$ActivityLifecycleCallbacks;)V

    .line 38
    const-string p0, "LG resources loader installed"

    invoke-static {v0, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_1d
    .catchall {:try_start_2 .. :try_end_1d} :catchall_1e

    .line 41
    goto :goto_24

    .line 39
    :catchall_1e
    move-exception p0

    .line 40
    const-string v1, "LG resources loader failed"

    invoke-static {v0, v1, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 42
    :goto_24
    return-void
.end method

.method public static sqrt(F)F
    .registers 3

    .line 82
    float-to-double v0, p0

    invoke-static {v0, v1}, Ljava/lang/Math;->sqrt(D)D

    move-result-wide v0

    double-to-float p0, v0

    return p0
.end method
