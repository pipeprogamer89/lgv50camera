.class public Lcom/lge/mdm/LGMDMManager;
.super Ljava/lang/Object;
.source "LGMDMManager.java"


# static fields
.field private static final INSTANCE:Lcom/lge/mdm/LGMDMManager;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 6
    new-instance v0, Lcom/lge/mdm/LGMDMManager;

    invoke-direct {v0}, Lcom/lge/mdm/LGMDMManager;-><init>()V

    sput-object v0, Lcom/lge/mdm/LGMDMManager;->INSTANCE:Lcom/lge/mdm/LGMDMManager;

    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .line 5
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static getInstance()Lcom/lge/mdm/LGMDMManager;
    .registers 1

    .line 7
    sget-object v0, Lcom/lge/mdm/LGMDMManager;->INSTANCE:Lcom/lge/mdm/LGMDMManager;

    return-object v0
.end method


# virtual methods
.method public getAllowCameraWithWhitelist(Landroid/content/ComponentName;)Z
    .registers 2

    .line 8
    const/4 p1, 0x1

    return p1
.end method

.method public getAllowMicrophone(Landroid/content/ComponentName;)Z
    .registers 2

    .line 9
    const/4 p1, 0x1

    return p1
.end method

.method public getApplicationState(Landroid/content/ComponentName;)Ljava/util/List;
    .registers 2

    .line 10
    const/4 p1, 0x0

    return-object p1
.end method

.method public getCameraWhitelist(Landroid/content/ComponentName;)Ljava/util/List;
    .registers 2

    .line 11
    const/4 p1, 0x0

    return-object p1
.end method

.method public getMicrophoneWhitelist(Landroid/content/ComponentName;)Ljava/util/List;
    .registers 2

    .line 12
    const/4 p1, 0x0

    return-object p1
.end method
