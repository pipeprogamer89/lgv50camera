.class public final Lcom/lge/app/permission/RequestPermissionsHelper;
.super Ljava/lang/Object;
.source "RequestPermissionsHelper.java"


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 6
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static hasPermissions(Landroid/content/Context;[Ljava/lang/String;)Z
    .registers 7

    .line 8
    const/4 v0, 0x1

    if-nez p1, :cond_4

    return v0

    .line 9
    :cond_4
    array-length v1, p1

    const/4 v2, 0x0

    move v3, v2

    :goto_7
    if-ge v3, v1, :cond_17

    aget-object v4, p1, v3

    .line 10
    if-eqz v4, :cond_14

    invoke-virtual {p0, v4}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I

    move-result v4

    if-eqz v4, :cond_14

    return v2

    .line 9
    :cond_14
    add-int/lit8 v3, v3, 0x1

    goto :goto_7

    .line 12
    :cond_17
    return v0
.end method

.method public static requestPermissionsIfNeeded(Landroid/app/Activity;[Ljava/lang/String;Lcom/lge/app/permission/GuideUiProvider;)Z
    .registers 8

    .line 16
    invoke-static {p0, p1}, Lcom/lge/app/permission/RequestPermissionsHelper;->hasPermissions(Landroid/content/Context;[Ljava/lang/String;)Z

    move-result p2

    const/4 v0, 0x0

    if-eqz p2, :cond_8

    return v0

    .line 17
    :cond_8
    new-instance p2, Ljava/util/ArrayList;

    invoke-direct {p2}, Ljava/util/ArrayList;-><init>()V

    .line 18
    array-length v1, p1

    move v2, v0

    :goto_f
    if-ge v2, v1, :cond_21

    aget-object v3, p1, v2

    .line 19
    if-eqz v3, :cond_1e

    invoke-virtual {p0, v3}, Landroid/app/Activity;->checkSelfPermission(Ljava/lang/String;)I

    move-result v4

    if-eqz v4, :cond_1e

    invoke-virtual {p2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 18
    :cond_1e
    add-int/lit8 v2, v2, 0x1

    goto :goto_f

    .line 21
    :cond_21
    new-array p1, v0, [Ljava/lang/String;

    invoke-virtual {p2, p1}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    move-result-object p1

    check-cast p1, [Ljava/lang/String;

    invoke-virtual {p0, p1, v0}, Landroid/app/Activity;->requestPermissions([Ljava/lang/String;I)V

    .line 22
    const/4 p0, 0x1

    return p0
.end method
