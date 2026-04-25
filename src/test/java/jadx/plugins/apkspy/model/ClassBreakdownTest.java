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

	@Test
	void testSkipStaticBlock() {
		final String originalCode = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"public class TestClass {\n" +
				"\n" +
				"    static String s;\n" +
				"\n" +
				"    static {\n" +
				"        s = getString();\n" +
				"    }\n" +
				"\n" +
				"    public static String getString() {\n" +
				"        return \"\";\n" +
				"    }\n" +
				"}";
		final String fullName = "jadx.plugin.apkspy.test.TestClass";
		final String name = "TestClass";
		final ClassBreakdown original = ClassBreakdown.breakdown(fullName, name, originalCode);
		Assertions.assertEquals(1, original.getMethods().size());
	}

	@Test
	void testMethodHeader() {
		final String originalCode = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* loaded from: classes.dex */\n" +
				"public class TestClass {\n" +
				"\n" +
				"    /* JADX WARN: */\n" +
				"    @Override // java.lang.Object\n" +
				"    public String toString() {\n" +
				"        return super.toString();\n" +
				"    }\n" +
				"}";
		final String fullName = "jadx.plugin.apkspy.test.TestClass";
		final String name = "TestClass";
		final ClassBreakdown original = ClassBreakdown.breakdown(fullName, name, originalCode);
		Assertions.assertEquals(1, original.getMethods().size());
		Assertions.assertEquals("@Override // java.lang.Object\npublic String toString() {", original.getMethods().get(0).getHeader());
	}

	@Test
	void testMergeMethodStubsMatchesAnnotations() {
		final String originalCode = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"import android.os.Bundle;\n" +
				"import android.support.v7.app.AppCompatActivity;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class TestActivity extends AppCompatActivity {\n" +
				"    @Deprecated\n" +
				"    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity\n"
				+
				"    protected void onCreate(Bundle bundle) {\n" +
				"        super.onCreate(bundle);\n" +
				"    }\n" +
				"}";
		String modified = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"import android.os.Bundle;\n" +
				"import android.support.v7.app.AppCompatActivity;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class TestActivity extends AppCompatActivity {\n" +
				"    @Deprecated\n" +
				"    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity\n"
				+
				"    protected void onCreate(Bundle bundle) {\n" +
				"        super.onCreate(bundle);\n" +
				"        System.out.println(\"This is a test.\");\n" +
				"    }\n" +
				"}\n";
		final String fullName = "jadx.plugin.apkspy.test.TestActivity";
		final String name = "TestActivity";
		final ClassBreakdown original = ClassBreakdown.breakdown(fullName, name, originalCode);
		System.out.println(original.getMethods().get(0).getHeader());

		final ClassBreakdown changed = ClassBreakdown.breakdown(fullName, name,
				Util.formatSources(modified));
		ClassBreakdown merged = changed.mergeMemberVariables(original.getMemberVariables())
				.mergeMethodStubs(original.getMethods()).mergeInnerClassStubs(original);

		Assertions.assertEquals(1, merged.getMethods().size());
		Assertions.assertEquals(
				"@Deprecated\n@Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity\n"
						+
						"protected void onCreate(Bundle bundle) {",
				original.getMethods().get(0).getHeader());
	}

	@Test
	void testMergeMethodStubsMatchesAnnotationsUserComments() {
		final String originalCode = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"import android.os.Bundle;\n" +
				"import android.support.v7.app.AppCompatActivity;\n" +
				"\n" +
				"// This is a custom line comment 1\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class TestActivity extends AppCompatActivity {\n" +
				"    // This is a custom line comment 2\n" +
				"    @Deprecated\n" +
				"    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity\n"
				+
				"    protected void onCreate(Bundle bundle) {\n" +
				"        super.onCreate(bundle);\n" +
				"    }\n" +
				"}";
		String modified = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"import android.os.Bundle;\n" +
				"import android.support.v7.app.AppCompatActivity;\n" +
				"\n" +
				"// This is a custom line comment 1\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class TestActivity extends AppCompatActivity {\n" +
				"    // This is a custom line comment 2\n" +
				"    @Deprecated\n" +
				"    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity\n"
				+
				"    protected void onCreate(Bundle bundle) {\n" +
				"        super.onCreate(bundle);\n" +
				"        System.out.println(\"This is a test.\");\n" +
				"    }\n" +
				"}\n";
		final String fullName = "jadx.plugin.apkspy.test.TestActivity";
		final String name = "TestActivity";
		final ClassBreakdown original = ClassBreakdown.breakdown(fullName, name, originalCode);
		System.out.println(Util.formatSources(modified));
		final ClassBreakdown changed = ClassBreakdown.breakdown(fullName, name,
				Util.formatSources(modified));
		ClassBreakdown merged = changed.mergeMemberVariables(original.getMemberVariables())
				.mergeMethodStubs(original.getMethods()).mergeInnerClassStubs(original);

		Assertions.assertEquals(1, merged.getMethods().size());
		Assertions.assertEquals(
				"// This is a custom line comment 2\n@Deprecated\n@Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity\n"
						+
						"protected void onCreate(Bundle bundle) {",
				original.getMethods().get(0).getHeader());
	}
}
