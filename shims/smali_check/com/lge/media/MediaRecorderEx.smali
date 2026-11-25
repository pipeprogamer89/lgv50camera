.class public Lcom/lge/media/MediaRecorderEx;
.super Landroid/media/MediaRecorder;
.source "MediaRecorderEx.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "MediaRecorderExShim"

.field private static sSetParameter:Ljava/lang/reflect/Method;


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 8
    invoke-direct {p0}, Landroid/media/MediaRecorder;-><init>()V

    return-void
.end method


# virtual methods
.method public changeMaxFileSize(J)V
    .registers 4

    .line 23
    :try_start_0
    invoke-virtual {p0, p1, p2}, Lcom/lge/media/MediaRecorderEx;->setMaxFileSize(J)V
    :try_end_3
    .catchall {:try_start_0 .. :try_end_3} :catchall_4

    goto :goto_21

    :catchall_4
    move-exception p1

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p1

    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "changeMaxFileSize ignored: "

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string p2, "MediaRecorderExShim"

    invoke-static {p2, p1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 24
    :goto_21
    return-void
.end method

.method public setAudioZooming()V
    .registers 3

    .line 21
    const-string v0, "MediaRecorderExShim"

    const-string v1, "setAudioZooming ignored"

    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    return-void
.end method

.method public setParameter(Ljava/lang/String;)V
    .registers 8

    .line 12
    :try_start_0
    sget-object v0, Lcom/lge/media/MediaRecorderEx;->sSetParameter:Ljava/lang/reflect/Method;

    if-nez v0, :cond_1b

    .line 13
    const-class v0, Landroid/media/MediaRecorder;

    const-string v1, "setParameter"

    const/4 v2, 0x1

    new-array v3, v2, [Ljava/lang/Class;

    const-class v4, Ljava/lang/String;

    const/4 v5, 0x0

    aput-object v4, v3, v5

    invoke-virtual {v0, v1, v3}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    sput-object v0, Lcom/lge/media/MediaRecorderEx;->sSetParameter:Ljava/lang/reflect/Method;

    .line 14
    sget-object v0, Lcom/lge/media/MediaRecorderEx;->sSetParameter:Ljava/lang/reflect/Method;

    invoke-virtual {v0, v2}, Ljava/lang/reflect/Method;->setAccessible(Z)V

    .line 16
    :cond_1b
    sget-object v0, Lcom/lge/media/MediaRecorderEx;->sSetParameter:Ljava/lang/reflect/Method;

    filled-new-array {p1}, [Ljava/lang/Object;

    move-result-object v1

    invoke-virtual {v0, p0, v1}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_24
    .catchall {:try_start_0 .. :try_end_24} :catchall_25

    .line 19
    goto :goto_3e

    .line 17
    :catchall_25
    move-exception v0

    .line 18
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "setParameter failed: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v1, "MediaRecorderExShim"

    invoke-static {v1, p1, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 20
    :goto_3e
    return-void
.end method
