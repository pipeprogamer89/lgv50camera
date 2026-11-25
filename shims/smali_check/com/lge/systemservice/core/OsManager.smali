.class public Lcom/lge/systemservice/core/OsManager;
.super Ljava/lang/Object;
.source "OsManager.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "OsManagerShim"


# instance fields
.field private final mContext:Landroid/content/Context;


# direct methods
.method constructor <init>(Landroid/content/Context;)V
    .registers 2

    .line 12
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/lge/systemservice/core/OsManager;->mContext:Landroid/content/Context;

    return-void
.end method


# virtual methods
.method public goToSleepWithForce(JI)V
    .registers 7

    .line 15
    :try_start_0
    iget-object p1, p0, Lcom/lge/systemservice/core/OsManager;->mContext:Landroid/content/Context;

    const-class p2, Landroid/os/PowerManager;

    invoke-virtual {p1, p2}, Landroid/content/Context;->getSystemService(Ljava/lang/Class;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/os/PowerManager;

    .line 16
    const-class p2, Landroid/os/PowerManager;

    const-string p3, "goToSleep"

    const/4 v0, 0x1

    new-array v0, v0, [Ljava/lang/Class;

    sget-object v1, Ljava/lang/Long;->TYPE:Ljava/lang/Class;

    const/4 v2, 0x0

    aput-object v1, v0, v2

    invoke-virtual {p2, p3, v0}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p2

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v0

    invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object p3

    filled-new-array {p3}, [Ljava/lang/Object;

    move-result-object p3

    invoke-virtual {p2, p1, p3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_29
    .catchall {:try_start_0 .. :try_end_29} :catchall_2a

    .line 19
    goto :goto_47

    .line 17
    :catchall_2a
    move-exception p1

    .line 18
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p1

    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    const-string p3, "goToSleep unavailable: "

    invoke-virtual {p2, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string p2, "OsManagerShim"

    invoke-static {p2, p1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 20
    :goto_47
    return-void
.end method

.method public makeNonPersistent(Landroid/os/IBinder;)V
    .registers 2

    .line 22
    return-void
.end method

.method public makeNonPersistentUsingPackageName(Ljava/lang/String;)V
    .registers 2

    .line 23
    return-void
.end method

.method public makePersistent(Ljava/lang/String;)Landroid/os/IBinder;
    .registers 2

    .line 21
    new-instance p1, Landroid/os/Binder;

    invoke-direct {p1}, Landroid/os/Binder;-><init>()V

    return-object p1
.end method

.method public setSystemProperty(Ljava/lang/String;Ljava/lang/String;)V
    .registers 3

    .line 24
    invoke-static {p1, p2}, Lcom/lge/camera/port/Props;->set(Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method
