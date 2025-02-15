package jadx.plugins.apkspy.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaMethodTest {
	@Test
	void methodCommentBlockTest() {
		final String content = "/* JADX WARN: */\n"
				+ "/* JADX WARN: this is a\n"
				+ " multiline warning */\n"
				+ "@Override // java.lang.Object\n"
				+ "/*\n"
				+ "    Code decompiled incorrectly, please refer to instructions dump.\n"
				+ "    To view partially-correct code enable 'Show inconsistent code' option in preferences\n"
				+ "*/\n"
				+ "public String toString() {\n"
				+ "    return \"\";\n"
				+ "}\n";
		final JavaMethod method = new JavaMethod(content);
		Assertions.assertEquals("@Override // java.lang.Object\n"
				+ "public String toString() {\n"
				+ "    return \"\";\n"
				+ "}", method.getMethod());
		Assertions.assertEquals("/* JADX WARN: */\n"
				+ "/* JADX WARN: this is a\n"
				+ " multiline warning */\n"
				+ "/*\n"
				+ "    Code decompiled incorrectly, please refer to instructions dump.\n"
				+ "    To view partially-correct code enable 'Show inconsistent code' option in preferences\n"
				+ "*/", method.getComments());
	}
}
