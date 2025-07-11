.class public LMethodExtraction;
.super Ljava/lang/Object;
.source "MethodExtraction.java"


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "<A:",
        "Ljava/lang/Object;",
        "B:",
        "Ljava/lang/Object;",
        ">",
        "Ljava/lang/Object;"
    }
.end annotation


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 6
    .local p0, "this":LMethodExtraction;, "LMethodExtraction<TA;TB;>;"
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private test(Ljava/util/Map;J)V
    .registers 4
    .param p1    # Ljava/util/Map;
        .annotation runtime Ljakarta/validation/Valid;
        .end annotation
    .end param
    .param p2, "b"    # J
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/Map<",
            "TA;",
            "Ljava/util/Map<",
            "Ljava/util/Map<",
            "TB;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            ">;>;J)V"
        }
    .end annotation

    .line 8
    .local p0, "this":LMethodExtraction;, "LMethodExtraction<TA;TB;>;"
    .local p1, "a":Ljava/util/Map;, "Ljava/util/Map<TA;Ljava/util/Map<Ljava/util/Map<TB;Ljava/lang/String;>;Ljava/lang/String;>;>;"
    return-void
.end method
