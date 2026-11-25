.class public abstract Lcom/lge/view/IWindowManagerEx$Stub;
.super Ljava/lang/Object;
.source "IWindowManagerEx.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/lge/view/IWindowManagerEx;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x409
    name = "Stub"
.end annotation


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 7
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static asInterface(Landroid/os/IBinder;)Lcom/lge/view/IWindowManagerEx;
    .registers 1

    .line 9
    new-instance p0, Lcom/lge/view/IWindowManagerEx$Stub$$ExternalSyntheticLambda0;

    invoke-direct {p0}, Lcom/lge/view/IWindowManagerEx$Stub$$ExternalSyntheticLambda0;-><init>()V

    return-object p0
.end method

.method static synthetic lambda$asInterface$0(IZ)Ljava/util/List;
    .registers 2

    .line 9
    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;

    move-result-object p0

    return-object p0
.end method
