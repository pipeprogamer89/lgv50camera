.class public Lcom/lge/appbox/updateproxy/RequestForm$RequestFormBuilder;
.super Ljava/lang/Object;
.source "RequestForm.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/lge/appbox/updateproxy/RequestForm;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "RequestFormBuilder"
.end annotation


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .registers 2

    .line 5
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public build()Lcom/lge/appbox/updateproxy/RequestForm;
    .registers 2

    .line 7
    new-instance v0, Lcom/lge/appbox/updateproxy/RequestForm;

    invoke-direct {v0}, Lcom/lge/appbox/updateproxy/RequestForm;-><init>()V

    return-object v0
.end method

.method public setRequestModel(Ljava/lang/String;)Lcom/lge/appbox/updateproxy/RequestForm$RequestFormBuilder;
    .registers 2

    .line 6
    return-object p0
.end method
