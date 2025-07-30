package jadx.plugins.apkspy.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

class ChangeCacheTest extends SmaliSourceTest {

	@Test
	void deleteClass() throws Exception {
		final JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleFile("deleteclass.smali"));
		try (final JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			final JavaClass cls = jadx.getClasses().get(0);
			ChangeCache.getInstance().deleteClass(cls);
			Assertions.assertEquals(1, ChangeCache.getInstance().getClassDeletions().size());
			Assertions.assertTrue(ChangeCache.getInstance().getClassDeletions().contains("jadx/apkspy/OuterClass.smali"));
		}
	}
}
