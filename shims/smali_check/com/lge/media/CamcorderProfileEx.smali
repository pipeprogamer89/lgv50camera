.class public Lcom/lge/media/CamcorderProfileEx;
.super Ljava/lang/Object;
.source "CamcorderProfileEx.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "CamcorderProfileExShim"


# instance fields
.field public audioBitRate:I

.field public audioChannels:I

.field public audioCodec:I

.field public audioSampleRate:I

.field public duration:I

.field public fileFormat:I

.field public quality:I

.field public videoBitRate:I

.field public videoCodec:I

.field public videoFrameHeight:I

.field public videoFrameRate:I

.field public videoFrameWidth:I


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 5
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static get(II)Lcom/lge/media/CamcorderProfileEx;
    .registers 5

    .line 22
    const/4 v0, 0x0

    :try_start_1
    invoke-static {p1}, Lcom/lge/media/CamcorderProfileEx;->map(I)I

    move-result v1

    invoke-static {p0, v1}, Landroid/media/CamcorderProfile;->get(II)Landroid/media/CamcorderProfile;

    move-result-object p0
    :try_end_9
    .catchall {:try_start_1 .. :try_end_9} :catchall_41

    .line 26
    nop

    .line 27
    if-nez p0, :cond_d

    return-object v0

    .line 28
    :cond_d
    new-instance v0, Lcom/lge/media/CamcorderProfileEx;

    invoke-direct {v0}, Lcom/lge/media/CamcorderProfileEx;-><init>()V

    .line 29
    iget v1, p0, Landroid/media/CamcorderProfile;->duration:I

    iput v1, v0, Lcom/lge/media/CamcorderProfileEx;->duration:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->quality:I

    iget p1, p0, Landroid/media/CamcorderProfile;->fileFormat:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->fileFormat:I

    .line 30
    iget p1, p0, Landroid/media/CamcorderProfile;->videoCodec:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->videoCodec:I

    iget p1, p0, Landroid/media/CamcorderProfile;->videoBitRate:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->videoBitRate:I

    iget p1, p0, Landroid/media/CamcorderProfile;->videoFrameRate:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->videoFrameRate:I

    .line 31
    iget p1, p0, Landroid/media/CamcorderProfile;->videoFrameWidth:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->videoFrameWidth:I

    iget p1, p0, Landroid/media/CamcorderProfile;->videoFrameHeight:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->videoFrameHeight:I

    .line 32
    iget p1, p0, Landroid/media/CamcorderProfile;->audioCodec:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->audioCodec:I

    iget p1, p0, Landroid/media/CamcorderProfile;->audioBitRate:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->audioBitRate:I

    .line 33
    iget p1, p0, Landroid/media/CamcorderProfile;->audioSampleRate:I

    iput p1, v0, Lcom/lge/media/CamcorderProfileEx;->audioSampleRate:I

    iget p0, p0, Landroid/media/CamcorderProfile;->audioChannels:I

    iput p0, v0, Lcom/lge/media/CamcorderProfileEx;->audioChannels:I

    .line 34
    return-object v0

    .line 23
    :catchall_41
    move-exception v1

    .line 24
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "no profile "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p0

    const-string v1, "/"

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "CamcorderProfileExShim"

    invoke-static {p1, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 25
    return-object v0
.end method

.method public static getManualSupportedList(IIF)Ljava/lang/String;
    .registers 3

    .line 43
    const-string p0, "NotSupport"

    return-object p0
.end method

.method public static hasProfile(II)Z
    .registers 2

    .line 38
    :try_start_0
    invoke-static {p1}, Lcom/lge/media/CamcorderProfileEx;->map(I)I

    move-result p1

    invoke-static {p0, p1}, Landroid/media/CamcorderProfile;->hasProfile(II)Z

    move-result p0
    :try_end_8
    .catchall {:try_start_0 .. :try_end_8} :catchall_9

    return p0

    .line 39
    :catchall_9
    move-exception p0

    .line 40
    const/4 p0, 0x0

    return p0
.end method

.method static map(I)I
    .registers 2

    .line 12
    const/4 v0, 0x6

    sparse-switch p0, :sswitch_data_a

    .line 16
    return p0

    .line 15
    :sswitch_5
    const/4 p0, 0x5

    return p0

    .line 14
    :sswitch_7
    return v0

    .line 13
    :sswitch_8
    return v0

    nop

    :sswitch_data_a
    .sparse-switch
        0x271c -> :sswitch_8
        0x271d -> :sswitch_7
        0x2721 -> :sswitch_5
    .end sparse-switch
.end method
