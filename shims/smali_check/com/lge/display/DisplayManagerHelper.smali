.class public Lcom/lge/display/DisplayManagerHelper;
.super Ljava/lang/Object;
.source "DisplayManagerHelper.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/lge/display/DisplayManagerHelper$SmartCoverCallback;,
        Lcom/lge/display/DisplayManagerHelper$CoverDisplayCallback;
    }
.end annotation


# static fields
.field public static final STATE_COVER_OPENED:I = 0x0

.field public static final STATE_DISABLED:I = 0x2

.field public static final STATE_ENABLED:I = 0x3

.field public static final STATE_UNMOUNT:I = 0x1


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .registers 2

    .line 17
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public getCoverDisplayState()I
    .registers 2

    .line 18
    const/4 v0, 0x1

    return v0
.end method

.method public getCoverState()I
    .registers 2

    .line 19
    const/4 v0, 0x0

    return v0
.end method
