package jadx.plugins.apkspy.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.plugins.apkspy.utils.Util;

public class ClassBreakdownTest {

	@Test
	void testMergeOnSave() {
		final String originalCode = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"public class TestClass {\n" +
				"    public static String getString1() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"\n" +
				"    public static String getString2() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"}";
		final String modified = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"\n" +
				"public class TestClass {\n" +
				"    public static String getString1() {\n" +
				"        System.out.println(\"\");\n" + // \t
				"        return \"\";\n" +
				"    }\n" +
				"}\n";
		final String expected = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"\n" +
				"public class TestClass {\n" +
				"    public static String getString1() {\n" +
				"        System.out.println(\"\");\n" +
				"        return \"\";\n" +
				"    }\n" +
				"\n" +
				"    public static String getString2() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"}";
		final String fullName = "jadx.plugin.apkspy.test.TestClass";
		final String name = "TestClass";
		final ClassBreakdown original = ClassBreakdown.breakdown(fullName, name, originalCode);
		final ClassBreakdown changed = ClassBreakdown.breakdown(fullName, name,
				modified);

		final ClassBreakdown completed = original.mergeImports(changed.getImports())
				.mergeMethods(changed.getChangedMethods());

		Assertions.assertEquals(expected, completed.toString());
	}

	@Test
	void testMergeOnSaveFormattingWithTabs() {
		final String originalCode = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"public class TestClass {\n" +
				"    public static String getString1() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"\n" +
				"    public static String getString2() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"}";
		final String modified = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"\n" +
				"public class TestClass {\n" +
				"    public static String getString1() {\n" +
				"    \tSystem.out.println(\"\");\n" +
				"        return \"\";\n" +
				"    }\n" +
				"}\n";
		final String expected = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"public class TestClass {\n" +
				"    public static String getString1() {\n" +
				"        System.out.println(\"\");\n" +
				"        return \"\";\n" +
				"    }\n" +
				"\n" +
				"    public static String getString2() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"}";
		final String fullName = "jadx.plugin.apkspy.test.TestClass";
		final String name = "TestClass";
		final ClassBreakdown original = ClassBreakdown.breakdown(fullName, name, originalCode);
		final ClassBreakdown changed = ClassBreakdown.breakdown(fullName, name,
				Util.formatSources(modified));

		final ClassBreakdown completed = original.mergeImports(changed.getImports())
				.mergeMethods(changed.getChangedMethods());

		Assertions.assertEquals(expected, completed.toString());
	}
}
