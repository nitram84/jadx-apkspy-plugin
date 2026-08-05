package jadx.plugins.apkspy.integration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.plugins.apkspy.JarGenerator;
import jadx.plugins.apkspy.model.ClassBreakdown;

public class JarGeneratorTest {
	private static JadxDecompiler decompile(final String apkFilename, final String target) {
		final JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.setInputFile(new File(apkFilename));
		jadxArgs.setOutDir(new File(target));
		jadxArgs.setExportAsGradleProject(true);
		jadxArgs.setInlineMethods(false);
		jadxArgs.setDeobfuscationOn(true);
		jadxArgs.setDeobfuscationMinLength(3);
		jadxArgs.setDeobfuscationForceSave(true);
		final JadxDecompiler jadx = new JadxDecompiler(jadxArgs);
		jadx.load();
		return jadx;
	}

	private static boolean checkFileNotJar(File jarFile, String className) {
		try (JarFile jar = new JarFile(jarFile)) {
			Enumeration<JarEntry> entries = jar.entries();

			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();

				if (entry.getName().equals(className)) {
					return false;
				}
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@TempDir
	File jarGenTestFolder;

	@Test
	void ApkEditTest() {

		URL apkFile = getClass().getClassLoader().getResource("beige-uml-android-2.1.11-aligned.apk");

		final JadxDecompiler decompiler = decompile(
				apkFile.getFile(), jarGenTestFolder.getAbsolutePath());
		final File outputJar = new File(jarGenTestFolder, "beige-uml-android-2.1.11-aligned.jar");

		// no excluded classes
		try {
			JarGenerator.generateStubJar(new File(apkFile.toURI()), outputJar, System.out, new HashMap<>(), decompiler,
					jarGenTestFolder.toPath());
		} catch (final URISyntaxException | IOException e) {
			Assertions.fail(e);
		}

		Assertions.assertTrue(outputJar.exists());
		Assertions.assertFalse(new File(jarGenTestFolder, "stub.jar").exists());
		Assertions.assertFalse(checkFileNotJar(outputJar, "org/beigesoft/p003ui/container/ContainerGuiSrvs.class"));

		outputJar.delete();

		// exclude "obfuscated" class
		try {
			HashMap<String, ClassBreakdown> classes = new HashMap<>();
			classes.put("org.beigesoft.ui.container.ContainerGuiSrvs", new ClassBreakdown("", "", "", "", "", null, null));
			JarGenerator.generateStubJar(new File(apkFile.toURI()), outputJar, System.out, classes, decompiler, jarGenTestFolder.toPath());
		} catch (final URISyntaxException | IOException e) {
			Assertions.fail(e);
		}

		Assertions.assertTrue(outputJar.exists());
		Assertions.assertFalse(new File(jarGenTestFolder, "stub.jar").exists());
		Assertions.assertTrue(checkFileNotJar(outputJar, "org/beigesoft/p003ui/container/ContainerGuiSrvs.class"));
	}
}
