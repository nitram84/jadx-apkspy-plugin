package jadx.plugins.apkspy.model;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.plugins.apkspy.utils.Util;

public class SmaliBreakdownTest {
	@Test
	void smaliMergeTest() {
		String modifiedContent = ".class public Lapkspy/test/b/a;\n" +
				".super Ljava/lang/Object;\n" +
				".source \"a.java\"\n" +
				"\n" +
				"\n" +
				"# direct methods\n" +
				".method public static a()Landroid/view/animation/AnimationSet;\n" +
				"    .registers 2\n" +
				"\n" +
				"    .line 13\n" +
				"    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
				"\n" +
				"    const-string v1, \"Hello world!\"\n" +
				"\n" +
				"    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n" +
				"\n" +
				"    .line 14\n" +
				"    const/4 v0, 0x0\n" +
				"\n" +
				"    return-object v0\n" +
				".end method\n";

		String originalContent = ".class public Lapkspy/test/b/a;\n" +
				".super Ljava/lang/Object;\n" +
				".source \"\"\n" +
				"\n" +
				"\n" +
				"# direct methods\n" +
				".method public static a()Landroid/view/animation/AnimationSet;\n" +
				"    .registers 2\n" +
				"\n" +
				"    .line 13\n" +
				"    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
				"\n" +
				"    const-string v1, \"Test\"\n" +
				"\n" +
				"    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n" +
				"\n" +
				"    .line 14\n" +
				"    const/4 v0, 0x0\n" +
				"\n" +
				"    return-object v0\n" +
				".end method\n";

		final ClassBreakdown original = ClassBreakdown.breakdown("apkspy.test.b.a", "a", "package apkspy.test.b;\n" +
				"\n" +
				"import android.view.animation.AnimationSet;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class a {\n" +
				"    public static AnimationSet a() {\n" +
				"        System.out.println(\"Test\");\n" +
				"        return null;\n" +
				"    }\n" +
				"}");
		final ClassBreakdown changed = ClassBreakdown.breakdown("apkspy.test.b.a", "a",
				Util.formatSources("package apkspy.test.b;\n" +
						"\n" +
						"import android.view.animation.AnimationSet;\n" +
						"/* JADX INFO: loaded from: classes.dex */\n" +
						"\n" +
						"public class a {\n" +
						"    public static AnimationSet a() {\n" +
						"        System.out.println(\"Hello world\");\n" +
						"        return null;\n" +
						"    }\n" +
						"}\n"));

		SmaliBreakdown modifiedSmali = SmaliBreakdown.breakdown(modifiedContent);
		ClassBreakdown relative = changed.mergeMemberVariables(original.getMemberVariables())
				.mergeMethodStubs(original.getMethods()).mergeInnerClassStubs(original);

		List<SmaliMethod> methods = modifiedSmali.getChangedMethods(relative);

		StringBuilder builder = new StringBuilder(originalContent);
		for (SmaliMethod method : methods) {
			SmaliBreakdown originalSmali = SmaliBreakdown.breakdown(builder.toString());
			SmaliMethod equivalentMethod = originalSmali.getEquivalentMethod(method);

			if (equivalentMethod != null) {
				builder.delete(equivalentMethod.getStart(), equivalentMethod.getEnd());
				builder.insert(equivalentMethod.getStart(), method.getContent());
			}
		}

		Assertions.assertEquals(".class public Lapkspy/test/b/a;\n" +
				".super Ljava/lang/Object;\n" +
				".source \"\"\n" +
				"\n" +
				"\n" +
				"# direct methods\n" +
				".method public static a()Landroid/view/animation/AnimationSet;\n" +
				"    .registers 2\n" +
				"\n" +
				"    .line 13\n" +
				"    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
				"\n" +
				"    const-string v1, \"Hello world!\"\n" +
				"\n" +
				"    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n" +
				"\n" +
				"    .line 14\n" +
				"    const/4 v0, 0x0\n" +
				"\n" +
				"    return-object v0\n" +
				".end method\n" +
				"\n", builder.toString());
	}
}
