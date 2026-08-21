package jadx.plugins.apkspy.model;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassBreakdownContstructorTest {

	@Test
	void packagePrivateConstructorTest() {
		String source = "package jadx.plugins.apkspy.test;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class Constructor1 {\n" +
				"    Constructor1() {\n" +
				"    }\n" +
				"\n" +
				"    public void doSomething() {\n" +
				"    }\n" +
				"}";
		ClassBreakdown cbd = ClassBreakdown.breakdown("jadx.plugins.apkspy.test.Constructor1", "Constructor1", source);
		String stub = cbd.asStub().toString();
		Assertions.assertEquals(2, StringUtils.countMatches(stub, "return;"));
	}

	@Test
	void FinalMembersConstructorTest() {
		String source = "package jadx.plugins.apkspy.test;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class Constructor2 {\n" +
				"    final int i;\n" +
				"    final int j = 2;\n" +
				"\n" +
				"    Constructor2(int i) {\n" +
				"        this.i = i;\n" +
				"    }\n" +
				"\n" +
				"    public void doSomething() {\n" +
				"    }\n" +
				"}";
		ClassBreakdown cbd = ClassBreakdown.breakdown("jadx.plugins.apkspy.test.Constructor2", "Constructor2", source);
		String stub = cbd.asStub().toString();
		Assertions.assertTrue(stub.contains("i = 0;"));
	}
}
