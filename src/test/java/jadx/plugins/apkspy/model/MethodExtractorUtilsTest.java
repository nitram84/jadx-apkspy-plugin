package jadx.plugins.apkspy.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.plugins.apkspy.utils.MethodExtractorUtils;

public class MethodExtractorUtilsTest extends SmaliSourceTest {

	@Test
	public void findMethodPositionTest() throws Exception {
		final JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleFile("methodextraction.smali"));
		try (final JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			final JavaClass cls = jadx.getClasses().get(0);

			final String code =
					"\n" +
							"import jakarta.validation.Valid;\n" +
							"import java.util.Map;\n" +
							"\n" +
							"\n" +
							"public class MethodExtraction<A, B> {\n" +
							"    private void test(@Valid Map<A, Map<Map<B, String>, String>> a, long b) {\n" +
							"        System.out.print(\"This is an modified class with different line numbers\");\n" +
							"    }\n" +
							"}";

			int pos = MethodExtractorUtils.findMethodPosition(cls.getMethods().get(0), code);
			Assertions.assertEquals(-1, pos);
			pos = MethodExtractorUtils.findMethodPosition(cls.getMethods().get(1), code);
			Assertions.assertNotEquals(-1, pos);
			Assertions.assertEquals("\nimport jakarta.validation.Valid;\n" +
					"import java.util.Map;\n" +
					"\n" +
					"\n" +
					"public class MethodExtraction<A, B> {\n" +
					"\n" +
					"    private void test(@Valid Map<A, Map<Map<B, String>, String>> a, long b) {\n" +
					"        System.out.print(\"This is an modified class with different line numbers\");\n" +
					"    }\n" +
					"}", MethodExtractorUtils.extractMethod(code, pos));
		}
	}

	@Test
	void findMethodPositionAnnotationsTest() {
		String code = "package jadx.plugins.apkspy;\n"
				+ "\n"
				+ "/* JADX INFO: loaded from: classes.dex */\n"
				+ "public class AnnotationTest {\n"
				+ "    @Deprecated\n"
				+ "    @Override\n"
				+ "    public String toString() {\n"
				+ "        return super.toString();\n"
				+ "    }\n"
				+ "}";
		String method = MethodExtractorUtils.extractMethod(code, 150);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(method.contains("    @Deprecated\n    @Override\n"));
	}

	@Test
	void findMethodPositionAnnotationsUserCommentTest() {
		String code = "package jadx.plugins.apkspy;\n"
				+ "\n"
				+ "// This is a user generated comment 1\n"
				+ "/* JADX INFO: loaded from: classes.dex */\n"
				+ "public class AnnotationTest {\n"
				+ "    // This is a user generated comment 2\n"
				+ "    @Deprecated\n"
				+ "    @Override\n"
				+ "    public String toString() {\n"
				+ "        return super.toString();\n"
				+ "    }\n"
				+ "}";

		String method = MethodExtractorUtils.extractMethod(code, 223);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(method.contains("    // This is a user generated comment 2\n    @Deprecated\n    @Override\n"));
	}

	@Test
	void findMethodPositionNestedClassTest() {
		String code = "package jadx.plugin.apkspy.test;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class OuterClass {\n" +
				"\n" +
				"    public static class InnerClass1 {\n" +
				"    }\n" +
				"\n" +
				"    public static class InnerClass2 {\n" +
				"        void doSomething() {\n" +
				"        }\n" +
				"    }\n" +
				"}";

		String method = MethodExtractorUtils.extractMethod(code, 199);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(method.contains("        void doSomething() {\n"));
		Assertions.assertFalse(method.contains("InnerClass1"));
	}

	@Test
	void findMethodPositionNestedClass2Test() {
		String code = "package jadx.plugins.apkspy.test;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class Constructor3 {\n" +
				"\n" +
				"    public class Constructor2 {\n" +
				"        final int i;\n" +
				"        final int j = 2;\n" +
				"\n" +
				"        Constructor2(int i) {\n" +
				"            this.i = i;\n" +
				"        }\n" +
				"\n" +
				"        public void doSomething() {\n" +
				"        }\n" +
				"    }\n" +
				"}";

		String method = MethodExtractorUtils.extractMethod(code, 270);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(method.contains("        public void doSomething() {\n"));
		Assertions.assertFalse(method.contains("Constructor2(int i)"));
	}
}
