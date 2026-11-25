.class public Lcom/lge/systemservice/core/SmartCoverManager;
.super Ljava/lang/Object;
.source "SmartCoverManager.java"


# static fields
.field public static final COVER_TYPE_NONE:I = 0x5


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public getCoverSetting()Z
    .registers 2

    .line 4
    const/4 v0, 0x0

    return v0
.end method

.method public getCoverType()I
    .registers 2

    .line 5
    const/4 v0, 0x5

    return v0
.end method
