.class public Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;
.super Ljava/lang/Object;
.source "SelfUpdateBinder.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder$ISelfupdateConnection;
    }
.end annotation


# static fields
.field private static final INSTANCE:Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 9
    new-instance v0, Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;

    invoke-direct {v0}, Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;-><init>()V

    sput-object v0, Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;->INSTANCE:Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;

    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .line 4
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static getInstance(Landroid/content/Context;)Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;
    .registers 1

    .line 10
    sget-object p0, Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;->INSTANCE:Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder;

    return-object p0
.end method


# virtual methods
.method public bind(Lcom/lge/appbox/updateproxy/manager/SelfUpdateBinder$ISelfupdateConnection;)Z
    .registers 2

    .line 11
    const/4 p1, 0x0

    return p1
.end method
