.class public Lcom/lge/mdm/config/LGMDMApplicationState;
.super Ljava/lang/Object;
.source "LGMDMApplicationState.java"


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public getEnable()I
    .registers 2

    .line 3
    const/4 v0, 0x1

    return v0
.end method

.method public getPackageName()Ljava/lang/String;
    .registers 2

    .line 4
    const-string v0, ""

    return-object v0
.end method
