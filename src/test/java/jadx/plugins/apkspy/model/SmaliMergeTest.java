package jadx.plugins.apkspy.model;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmaliMergeTest {

	@Test
	void SmaliBreakdownInnerClassMergeTest() {
		String modifiedContent = ".class Lb/p/y$a;\n" +
				".super Landroid/animation/LayoutTransition;\n" +
				".source \"c.java\"\n" +
				"\n" +
				"\n" +
				"# annotations\n" +
				".annotation system Ldalvik/annotation/EnclosingClass;\n" +
				"    value = La/b/c;\n" +
				".end annotation\n" +
				"\n" +
				".annotation system Ldalvik/annotation/InnerClass;\n" +
				"    accessFlags = 0x8\n" +
				"    name = \"d\"\n" +
				".end annotation\n" +
				"\n" +
				"\n" +
				"# direct methods\n" +
				".method constructor <init>()V\n" +
				"    .locals 0\n" +
				"\n" +
				"    .line 18\n" +
				"    invoke-direct {p0}, Landroid/animation/LayoutTransition;-><init>()V\n" +
				"\n" +
				"    return-void\n" +
				".end method\n" +
				"\n" +
				"\n" +
				"# virtual methods\n" +
				".method public isChangingLayout()Z\n" +
				"    .locals 1\n" +
				"\n" +
				"    .line 21\n" +
				"    const/4 v0, 0x0\n" +
				"\n" +
				"    return v0\n" +
				".end method\n" +
				"\n";

		String originalContent = ".class final La/b/c$d;\n" +
				".super Landroid/animation/LayoutTransition;\n" +
				".source \"\"\n" +
				"\n" +
				"\n" +
				"# annotations\n" +
				".annotation system Ldalvik/annotation/EnclosingMethod;\n" +
				"    value = La/b/c;->b(Landroid/view/ViewGroup;Z)V\n" +
				".end annotation\n" +
				"\n" +
				".annotation system Ldalvik/annotation/InnerClass;\n" +
				"    accessFlags = 0x8\n" +
				"    name = null\n" +
				".end annotation\n" +
				"\n" +
				"\n" +
				"# direct methods\n" +
				".method constructor <init>()V\n" +
				"    .locals 0\n" +
				"\n" +
				"    invoke-direct {p0}, Landroid/animation/LayoutTransition;-><init>()V\n" +
				"\n" +
				"    return-void\n" +
				".end method\n" +
				"\n" +
				"\n" +
				"# virtual methods\n" +
				".method public isChangingLayout()Z\n" +
				"    .locals 1\n" +
				"\n" +
				"    const/4 v0, 0x1\n" +
				"\n" +
				"    return v0\n" +
				".end method\n" +
				"\n";

		String relativeClass = "package a.b;\n" +
				"\n" +
				"import android.animation.LayoutTransition;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"class c {\n" +
				"    static class d extends LayoutTransition {\n" +
				"\n" +
				"        @Override // android.animation.LayoutTransition\n" +
				"        public boolean isChangingLayout() {\n" +
				"            return false;\n" +
				"        }\n" +
				"    }\n" +
				"}";

		SmaliBreakdown modifiedSmali = SmaliBreakdown.breakdown(modifiedContent);

		ClassBreakdown relative = ClassBreakdown.breakdown("a.b.c.d", relativeClass).getInnerClasses().get(0);
		Assertions.assertNotNull(relative);
		Assertions.assertEquals("b.p.y$a", modifiedSmali.getClassName());

		List<SmaliMethod> methods = modifiedSmali.getChangedMethods(relative);

		Assertions.assertEquals(1, relative.getChangedMethods().size());
		Assertions.assertEquals(1, methods.size());

		StringBuilder builder = new StringBuilder(originalContent);
		for (SmaliMethod method : methods) {
			SmaliBreakdown originalSmali = SmaliBreakdown.breakdown(builder.toString());
			SmaliMethod equivalentMethod = originalSmali.getEquivalentMethod(method);

			if (equivalentMethod != null) {
				builder.delete(equivalentMethod.getStart(), equivalentMethod.getEnd());
				builder.insert(equivalentMethod.getStart(), method.getContent());
			}
		}

		String smaliMerged = ".class final La/b/c$d;\n" +
				".super Landroid/animation/LayoutTransition;\n" +
				".source \"\"\n" +
				"\n" +
				"\n" +
				"# annotations\n" +
				".annotation system Ldalvik/annotation/EnclosingMethod;\n" +
				"    value = La/b/c;->b(Landroid/view/ViewGroup;Z)V\n" +
				".end annotation\n" +
				"\n" +
				".annotation system Ldalvik/annotation/InnerClass;\n" +
				"    accessFlags = 0x8\n" +
				"    name = null\n" +
				".end annotation\n" +
				"\n" +
				"\n" +
				"# direct methods\n" +
				".method constructor <init>()V\n" +
				"    .locals 0\n" +
				"\n" +
				"    invoke-direct {p0}, Landroid/animation/LayoutTransition;-><init>()V\n" +
				"\n" +
				"    return-void\n" +
				".end method\n" +
				"\n" +
				"\n" +
				"# virtual methods\n" +
				".method public isChangingLayout()Z\n" +
				"    .locals 1\n" +
				"\n" +
				"    .line 21\n" +
				"    const/4 v0, 0x0\n" +
				"\n" +
				"    return v0\n" +
				".end method\n" +
				"\n" +
				"\n";
		Assertions.assertEquals(smaliMerged, builder.toString());
	}
}
