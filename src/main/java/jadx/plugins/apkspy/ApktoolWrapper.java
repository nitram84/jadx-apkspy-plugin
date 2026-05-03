package jadx.plugins.apkspy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import jadx.plugins.apkspy.utils.Util;

public class ApktoolWrapper {
	public static void decode(Path apk, String apktoolLocation, String jdkLocation, File baseDir, String dir, boolean resources,
			OutputStream out)
			throws InterruptedException, IOException {
		Util.system(baseDir, jdkLocation, out, "java", "-jar", apktoolLocation,
				"decode", "-o", new File(baseDir, dir).getAbsolutePath(),
				resources ? "" : "-r", apk.toAbsolutePath().toString());
	}

	public static void build(Path apk, String apktoolLocation, String jdkLocation, File baseDir, String outputLocation, OutputStream out)
			throws InterruptedException, IOException {
		Util.system(baseDir, jdkLocation, out, "java", "-jar", apktoolLocation,
				"build", "-o", outputLocation,
				apk.toAbsolutePath().toString());
	}
}
