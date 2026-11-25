.class public Lcom/lge/os/PropertyUtils;
.super Ljava/lang/Object;
.source "PropertyUtils.java"


# static fields
.field private static final INSTANCE:Lcom/lge/os/PropertyUtils;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 3
    new-instance v0, Lcom/lge/os/PropertyUtils;

    invoke-direct {v0}, Lcom/lge/os/PropertyUtils;-><init>()V

    sput-object v0, Lcom/lge/os/PropertyUtils;->INSTANCE:Lcom/lge/os/PropertyUtils;

    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .line 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static getInstance()Lcom/lge/os/PropertyUtils;
    .registers 1

    .line 4
    sget-object v0, Lcom/lge/os/PropertyUtils;->INSTANCE:Lcom/lge/os/PropertyUtils;

    return-object v0
.end method


# virtual methods
.method public get(ILjava/lang/String;)Ljava/lang/String;
    .registers 3

    .line 5
    return-object p2
.end method
