.class public Lcom/lge/systemservice/core/LGContext;
.super Ljava/lang/Object;
.source "LGContext.java"


# instance fields
.field private final mContext:Landroid/content/Context;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .registers 2

    .line 6
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/lge/systemservice/core/LGContext;->mContext:Landroid/content/Context;

    return-void
.end method


# virtual methods
.method public getLGSystemService(Ljava/lang/String;)Ljava/lang/Object;
    .registers 5

    .line 8
    const/4 v0, 0x0

    if-nez p1, :cond_4

    return-object v0

    .line 9
    :cond_4
    invoke-virtual {p1}, Ljava/lang/String;->hashCode()I

    move-result v1

    sparse-switch v1, :sswitch_data_9e

    :cond_b
    goto :goto_52

    :sswitch_c
    const-string v1, "osservice"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x0

    goto :goto_53

    :sswitch_16
    const-string v1, "lgpowermanagerhelper"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x2

    goto :goto_53

    :sswitch_20
    const-string v1, "emotionled"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x5

    goto :goto_53

    :sswitch_2a
    const-string v1, "wfdService"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x6

    goto :goto_53

    :sswitch_34
    const-string v1, "lgthermal"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x1

    goto :goto_53

    :sswitch_3e
    const-string v1, "volumevibrator"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x4

    goto :goto_53

    :sswitch_48
    const-string v1, "smartcover"

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_b

    const/4 v1, 0x3

    goto :goto_53

    :goto_52
    const/4 v1, -0x1

    :goto_53
    packed-switch v1, :pswitch_data_bc

    .line 18
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "unbacked LG system service: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v1, "LGContextShim"

    invoke-static {v1, p1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 19
    return-object v0

    .line 16
    :pswitch_6f
    new-instance p1, Lcom/lge/systemservice/core/WfdManager;

    invoke-direct {p1}, Lcom/lge/systemservice/core/WfdManager;-><init>()V

    return-object p1

    .line 15
    :pswitch_75
    new-instance p1, Lcom/lge/systemservice/core/LEDManager;

    invoke-direct {p1}, Lcom/lge/systemservice/core/LEDManager;-><init>()V

    return-object p1

    .line 14
    :pswitch_7b
    new-instance p1, Lcom/lge/systemservice/core/VolumeVibratorManager;

    invoke-direct {p1}, Lcom/lge/systemservice/core/VolumeVibratorManager;-><init>()V

    return-object p1

    .line 13
    :pswitch_81
    new-instance p1, Lcom/lge/systemservice/core/SmartCoverManager;

    invoke-direct {p1}, Lcom/lge/systemservice/core/SmartCoverManager;-><init>()V

    return-object p1

    .line 12
    :pswitch_87
    new-instance p1, Lcom/lge/systemservice/core/LGPowerManagerHelper;

    invoke-direct {p1}, Lcom/lge/systemservice/core/LGPowerManagerHelper;-><init>()V

    return-object p1

    .line 11
    :pswitch_8d
    new-instance p1, Lcom/lge/systemservice/core/LGThermalManager;

    iget-object v0, p0, Lcom/lge/systemservice/core/LGContext;->mContext:Landroid/content/Context;

    invoke-direct {p1, v0}, Lcom/lge/systemservice/core/LGThermalManager;-><init>(Landroid/content/Context;)V

    return-object p1

    .line 10
    :pswitch_95
    new-instance p1, Lcom/lge/systemservice/core/OsManager;

    iget-object v0, p0, Lcom/lge/systemservice/core/LGContext;->mContext:Landroid/content/Context;

    invoke-direct {p1, v0}, Lcom/lge/systemservice/core/OsManager;-><init>(Landroid/content/Context;)V

    return-object p1

    nop

    :sswitch_data_9e
    .sparse-switch
        -0x6b6d0c72 -> :sswitch_48
        -0x695098cd -> :sswitch_3e
        0x80779bc -> :sswitch_34
        0xed9b580 -> :sswitch_2a
        0x3e8edfd0 -> :sswitch_20
        0x3fc2b571 -> :sswitch_16
        0x6a171051 -> :sswitch_c
    .end sparse-switch

    :pswitch_data_bc
    .packed-switch 0x0
        :pswitch_95
        :pswitch_8d
        :pswitch_87
        :pswitch_81
        :pswitch_7b
        :pswitch_75
        :pswitch_6f
    .end packed-switch
.end method
