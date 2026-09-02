package jadx.plugins.apkspy.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.plugins.apkspy.utils.ClassBreakdownUtils;

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
		ClassBreakdown cbd = ClassBreakdown.breakdown("jadx.plugins.apkspy.test.Constructor1", source);
		String stub = cbd.asStub().toString();
		Assertions.assertEquals("Constructor1", cbd.getSimpleName());
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
		ClassBreakdown cbd = ClassBreakdown.breakdown("jadx.plugins.apkspy.test.Constructor2", source);
		String stub = cbd.asStub().toString();
		Assertions.assertTrue(stub.contains("i = 0;"));
	}

	@Test // b.p.d
	// Detect constructor in inner class
	void innerClassFinalMemberConstructorTest() {
		String source = "package jadx.plugins.apkspy.test;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"public class Constructor3 {\n" +
				"\n" +
				"    class Constructor2 {\n" +
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
		ClassBreakdown cbd = ClassBreakdown.breakdown("jadx.plugins.apkspy.test.Constructor3", source);
		String stub = cbd.asStub().toString();
		Assertions.assertTrue(stub.contains("i = 0;"));
	}

	@Test
	void testConstructor() {
		String source = "package jadx.plugins.apkspy.a;\n" +
				"\n" +
				"/* JADX INFO: loaded from: classes.dex */\n" +
				"class b {\n" +
				"\n" +
				"    /* JADX INFO: renamed from a for any reason */\n" +
				"    private final Object f0001a;\n" +
				"\n" +
				"    b(Object o) {\n" +
				"        this.f0001a = o;\n" +
				"    }\n" +
				"}";
		ClassBreakdown cbd = ClassBreakdown.breakdown("jadx.plugins.apkspy.a", source);
		String stub = cbd.asStub().toString();
		Assertions.assertTrue(stub.contains(
				"    /* JADX INFO: renamed from a for any reason */\n    private final Object f0001a;\n"));
		Assertions.assertTrue(stub.contains("        f0001a = null;"));
		List<MemberInfo> uninitializedFinalMembers = ClassBreakdownUtils.findUninitializedFinalMembers(cbd.getMemberVariables());
		Assertions.assertEquals(1, uninitializedFinalMembers.size());
	}
}
