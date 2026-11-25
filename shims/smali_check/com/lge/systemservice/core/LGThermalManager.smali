.class public Lcom/lge/systemservice/core/LGThermalManager;
.super Ljava/lang/Object;
.source "LGThermalManager.java"


# instance fields
.field private final mContext:Landroid/content/Context;


# direct methods
.method constructor <init>(Landroid/content/Context;)V
    .registers 2

    .line 7
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/lge/systemservice/core/LGThermalManager;->mContext:Landroid/content/Context;

    return-void
.end method


# virtual methods
.method public isThermalActionTriggered(Ljava/lang/String;)Z
    .registers 4

    .line 10
    const/4 p1, 0x0

    :try_start_1
    iget-object v0, p0, Lcom/lge/systemservice/core/LGThermalManager;->mContext:Landroid/content/Context;

    const-class v1, Landroid/os/PowerManager;

    invoke-virtual {v0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/Class;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/os/PowerManager;

    .line 11
    if-eqz v0, :cond_15

    invoke-virtual {v0}, Landroid/os/PowerManager;->getCurrentThermalStatus()I

    move-result v0
    :try_end_11
    .catchall {:try_start_1 .. :try_end_11} :catchall_16

    const/4 v1, 0x3

    if-lt v0, v1, :cond_15

    const/4 p1, 0x1

    :cond_15
    return p1

    .line 12
    :catchall_16
    move-exception v0

    .line 13
    return p1
.end method
