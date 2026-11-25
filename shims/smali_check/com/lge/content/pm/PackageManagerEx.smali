.class public Lcom/lge/content/pm/PackageManagerEx;
.super Ljava/lang/Object;
.source "PackageManagerEx.java"


# static fields
.field private static final INSTANCE:Lcom/lge/content/pm/PackageManagerEx;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 5
    new-instance v0, Lcom/lge/content/pm/PackageManagerEx;

    invoke-direct {v0}, Lcom/lge/content/pm/PackageManagerEx;-><init>()V

    sput-object v0, Lcom/lge/content/pm/PackageManagerEx;->INSTANCE:Lcom/lge/content/pm/PackageManagerEx;

    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .line 4
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static getDefault()Lcom/lge/content/pm/PackageManagerEx;
    .registers 1

    .line 6
    sget-object v0, Lcom/lge/content/pm/PackageManagerEx;->INSTANCE:Lcom/lge/content/pm/PackageManagerEx;

    return-object v0
.end method


# virtual methods
.method public getIconDrawableAsIconFrameTheme(Landroid/content/Context;Landroid/graphics/drawable/Drawable;Ljava/lang/String;I)Landroid/graphics/drawable/Drawable;
    .registers 5

    .line 7
    return-object p2
.end method
