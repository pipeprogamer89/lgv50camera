.class public interface abstract Lcom/lge/appbox/updateproxy/manager/Delegate;
.super Ljava/lang/Object;
.source "Delegate.java"


# virtual methods
.method public abstract downloadAndInstallAppAsync(Lcom/lge/appbox/updateproxy/AppInfo;)V
.end method

.method public abstract registeSelfUpdateCallback(Lcom/lge/appbox/updateproxy/manager/ISelfUpdateServiceCallback;)V
.end method

.method public abstract retrieveAppListAsync(Lcom/lge/appbox/updateproxy/RequestForm;)V
.end method

.method public abstract unregisteSelfUpdateCallback(Lcom/lge/appbox/updateproxy/manager/ISelfUpdateServiceCallback;)V
.end method
