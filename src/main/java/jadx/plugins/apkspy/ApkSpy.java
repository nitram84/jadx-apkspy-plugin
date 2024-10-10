package jadx.plugins.apkspy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.plugins.apkspy.model.ClassBreakdown;
import jadx.plugins.apkspy.model.SmaliBreakdown;
import jadx.plugins.apkspy.model.SmaliMethod;
import jadx.plugins.apkspy.utils.Util;

public class ApkSpy {

	private static final Logger LOG = LoggerFactory.getLogger(ApkSpy.class);

	private static String getClasspath(String... libs) {
		return Arrays.stream(libs)
				.map(lib -> lib.startsWith("/") ? lib : String.join(File.separator, "..", "libs", lib))
				.collect(Collectors.joining(File.pathSeparator));
	}

	private static String findLatestAndroidJars(final String sdkPath) {
		File platformDir = new File(sdkPath, "platforms");
		if (platformDir.exists() && platformDir.listFiles() != null) {
			int lastVersion = -1;
			for (File androidVersionDir : platformDir.listFiles()) {
				if (androidVersionDir.isDirectory() && androidVersionDir.getName().startsWith("android-")) {
					try {
						int version = Integer.parseInt(androidVersionDir.getName().substring(8));
						if (version > lastVersion) {
							lastVersion = version;
						}
					} catch (NumberFormatException ignore) {
						// ignore
					}
				}
			}
			if (lastVersion > -1) {
				return new File(platformDir, "android-" + lastVersion).getAbsolutePath();
			}
		}
		return null;
	}

	public static boolean lint(String apk, String className, ClassBreakdown content, String sdkPath, String jdkLocation, OutputStream out)
			throws IOException, InterruptedException {
		LOG.info("Linting: {}", apk);
		File modifyingApk = new File(apk);
		Path root = Paths.get("project-tmp");
		Map<String, ClassBreakdown> classes = Collections.singletonMap(className, content);

		Util.attemptDelete(root.toFile());

		String pkg = className.substring(0, className.lastIndexOf('.'));
		Path folder = root.resolve(Paths.get("src", pkg.replace('.', File.separatorChar)));
		if (!Files.isDirectory(folder)) {
			Files.createDirectories(folder);
		}
		Files.write(root.resolve(Paths.get("src", className.replace('.', File.separatorChar) + ".java")),
				content.toString().getBytes(StandardCharsets.UTF_8));

		Path stubPath = Paths.get(System.getProperty("java.io.tmpdir"), "apkSpy",
				modifyingApk.getName().replace('.', '_') + "stub.jar");
		if (!Files.exists(stubPath)) {
			JarGenerator.generateStubJar(modifyingApk, stubPath.toFile(), out, classes);
		}
		Files.createDirectories(root.resolve("libs"));
		Files.copy(stubPath, Paths.get("project-tmp", "libs", "stub.jar"));

		Files.createDirectories(root.resolve("bin"));

		Path javac = null;
		if (jdkLocation != null && !jdkLocation.isEmpty()) {
			javac = Paths.get(jdkLocation, "bin", "javac");
			// if (System.getenv("JAVA_HOME") != null) {
			// javac = Paths.get(System.getenv("JAVA_HOME"), "bin", "javac");
			if (!Files.isExecutable(javac)) {
				javac = javac.getParent().resolve("javac.exe");
				if (!Files.isExecutable(javac)) {
					javac = null;
				}
			}
		}

		out.write("Started compile...\n".getBytes(StandardCharsets.UTF_8));
		String targetVersionDir = findLatestAndroidJars(sdkPath);
		int code = Util.system(root.resolve("src").toFile(), jdkLocation, new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}
		}, javac == null ? "javac" : javac.toAbsolutePath().toString(), "-cp",
				getClasspath(targetVersionDir + File.separator + "android.jar", "stub.jar",
						targetVersionDir + File.separator + "optional" + File.separator + "org.apache.http.legacy.jar"),
				"-d",
				".." + File.separator + "bin", className.replace('.', File.separatorChar) + ".java");

		Util.attemptDelete(root.toFile());

		return code == 0;
	}

	public static boolean merge(String apk, String outputLocation, String sdkPath, String jdkLocation, String apktoolLocation,
			String applicationId,
			Map<String, ClassBreakdown> classes, List<String> deletions, OutputStream out)
			throws IOException, InterruptedException {
		sdkPath = sdkPath.replace("\\", "\\\\");

		LOG.info("Merging: {}", apk);
		File modifyingApk = new File(apk);

		Util.attemptDelete(new File("project-tmp"));
		Util.attemptDelete(new File("smali"));

		copyProjectTemplate(new File("project-tmp"));

		Files.write(Paths.get("project-tmp", "local.properties"),
				("sdk.dir=" + sdkPath).getBytes(StandardCharsets.UTF_8));

		Path gradleBuildPath = Paths.get("project-tmp", "app", "build.gradle");
		String buildGradle = new String(Files.readAllBytes(gradleBuildPath), StandardCharsets.UTF_8);
		buildGradle = buildGradle.replace("$APPLICATION_ID", applicationId);

		Files.write(gradleBuildPath, buildGradle.getBytes(StandardCharsets.UTF_8));

		Path manifestPath = Paths.get("project-tmp", "app", "src", "main", "AndroidManifest.xml");
		String manifest = new String(Files.readAllBytes(manifestPath), StandardCharsets.UTF_8);
		manifest = manifest.replace("$APPLICATION_ID", applicationId);

		Files.write(manifestPath, manifest.getBytes(StandardCharsets.UTF_8));

		for (Map.Entry<String, ClassBreakdown> entry : classes.entrySet()) {
			String className = entry.getKey();
			ClassBreakdown content = entry.getValue();

			File toCompile = new File(className.substring(className.lastIndexOf('.') + 1) + ".java");
			File completePath = Paths.get("project-tmp", "app", "src", "main", "java",
					className.substring(0, className.lastIndexOf('.')).replace(".", File.separator)).toFile();
			completePath.mkdirs();

			File newFile = new File(completePath, "ApkSpy_" + toCompile.getName());
			Files.write(newFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));

			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			String newFileContent = new String(Files.readAllBytes(newFile.toPath()), StandardCharsets.UTF_8);
			newFileContent = newFileContent.replaceAll("(class|interface|enum|@interface) +" + simpleName + "(.*)\\{",
					"$1 ApkSpy_" + simpleName + "$2{");
			newFileContent = newFileContent.replaceAll(simpleName + " *\\((.*)\\) *\\{",
					"ApkSpy_" + simpleName + "($1) {");
			Files.write(newFile.toPath(), newFileContent.getBytes(StandardCharsets.UTF_8));
		}

		Path stubPath = Paths.get(System.getProperty("java.io.tmpdir"), "apkSpy",
				modifyingApk.getName().replace('.', '_') + "stub.jar");
		if (!Files.exists(stubPath)) {
			JarGenerator.generateStubJar(modifyingApk, stubPath.toFile(), out, classes);
		}
		Files.createDirectories(Paths.get("project-tmp", "app", "libs"));

		if (!Files.exists(Paths.get("project-tmp", "app", "libs", "stub.jar"))) {
			// we check if it doesn't already exist, in case gradle has a lock on it and it
			// couldn't be deleted before
			Files.copy(stubPath, Paths.get("project-tmp", "app", "libs", "stub.jar"));
		}

		if (!Util.isWindows()) {
			Runtime.getRuntime().exec("chmod +x project-tmp/gradlew").waitFor();
		}

		if (Util.system(new File("project-tmp"), jdkLocation, out, new File("project-tmp").getAbsolutePath() + File.separator
				+ (Util.isWindows() ? "gradlew.bat" : "gradlew"), "build") != 0) {
			Util.attemptDelete(new File("project-tmp"));
			return false;
		}

		Files.copy(Paths.get("project-tmp", "app", "build", "outputs", "apk", "debug", "app-debug.apk"),
				Paths.get("generated.apk"), StandardCopyOption.REPLACE_EXISTING);
		Util.attemptDelete(new File("project-tmp"));

		ApktoolWrapper.decode(Paths.get("generated.apk"), apktoolLocation, jdkLocation, "generated", false, out);
		Files.delete(Paths.get("generated.apk"));

		ApktoolWrapper.decode(modifyingApk.toPath(), apktoolLocation, jdkLocation, "original", true, out);

		List<Path> smaliFolders = Files.list(Paths.get("smali", "generated"))
				.filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("smali"))
				.collect(Collectors.toList());
		List<Path> destinationFolders = smaliFolders.stream()
				.map(path -> Paths.get(path.toString().replace("generated", "original"))).collect(Collectors.toList());

		for (String deletion : deletions) {
			// file might not exist, as we could delete temporary classes that we made in
			// between compilations in the editor
			for (Path path : smaliFolders) {
				if (Files.deleteIfExists(Paths.get(path.toAbsolutePath().toString(),
						deletion.replace('.', File.separatorChar) + ".smali"))) {
					break;
				}
			}
		}

		for (Path smaliFolder : smaliFolders) {
			LOG.info("Searching through: {}", smaliFolder);
			Files.walk(smaliFolder)
					.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().startsWith("ApkSpy_"))
					.forEach(path -> {
						try {
							LOG.info("Merging smali file: {}", path);
							Path equivalent = null;
							for (Path otherFolder : destinationFolders) {
								Path test = Paths.get(otherFolder.toString(),
										path.toAbsolutePath().toString()
												.substring(smaliFolder.toAbsolutePath().toString().length())
												.replace("ApkSpy_", ""));
								if (Files.isRegularFile(test)) {
									equivalent = test;
									break;
								}
							}
							LOG.info("Merging into file: {}", equivalent);

							String modifiedContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
							modifiedContent = modifiedContent.replace("ApkSpy_", "");

							if (equivalent != null) {
								String originalContent = new String(Files.readAllBytes(equivalent),
										StandardCharsets.UTF_8);

								SmaliBreakdown modifiedSmali = SmaliBreakdown.breakdown(modifiedContent);

								ClassBreakdown relative = classes.get(modifiedSmali.getClassName());

								// check to make sure it's not an inner class
								if (relative != null) {
									LOG.info("Merging smali for class: {}", modifiedSmali.getClassName());

									List<SmaliMethod> methods = modifiedSmali.getChangedMethods(relative);

									LOG.info("Originally changed methods: {}", relative.getChangedMethods().size());
									LOG.info("Merging method count: {}", methods.size());

									StringBuilder builder = new StringBuilder(originalContent);
									for (SmaliMethod method : methods) {
										SmaliBreakdown originalSmali = SmaliBreakdown.breakdown(builder.toString());
										SmaliMethod equivalentMethod = originalSmali.getEquivalentMethod(method);

										builder.delete(equivalentMethod.getStart(), equivalentMethod.getEnd());
										builder.insert(equivalentMethod.getStart(), method.getContent());
									}

									Files.write(equivalent, builder.toString().getBytes(StandardCharsets.UTF_8));
								}
							} else {
								equivalent = Paths.get(smaliFolder.toString().replace("generated", "original"),
										path.toAbsolutePath().toString()
												.substring(smaliFolder.toAbsolutePath().toString().length())
												.replace("ApkSpy_", ""));
								Files.createDirectories(equivalent.getParent());
								Files.copy(path, equivalent);
							}
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					});
		}

		ApktoolWrapper.build(Paths.get("smali", "original"), apktoolLocation, jdkLocation, outputLocation, out);
		Util.attemptDelete(new File("smali"));

		out.write("Finished creating APK!".getBytes(StandardCharsets.UTF_8));
		return true;
	}

	private static void copyProjectTemplate(final File projectRoot) {
		String[] projectFiles = { "apkspy/default/app/src/main/res/values/styles.xml",
				"apkspy/default/app/src/main/AndroidManifest.xml",
				"apkspy/default/app/build.gradle",
				"apkspy/default/gradle/wrapper/gradle-wrapper.jar.zip", // rename this file later
				"apkspy/default/gradle/wrapper/gradle-wrapper.properties",
				"apkspy/default/build.gradle",
				"apkspy/default/gradle.properties",
				"apkspy/default/gradlew",
				"apkspy/default/gradlew.bat",
				"apkspy/default/settings.gradle" };
		if (!projectRoot.exists()) {
			projectRoot.mkdirs();
		}
		for (final String filename : projectFiles) {
			String targetFilename = filename.substring(15);
			if (targetFilename.lastIndexOf('/') > -1) {
				String dest = targetFilename.substring(0, targetFilename.lastIndexOf('/'));
				if (File.separatorChar != '/') {
					dest = dest.replace('/', File.separatorChar);
				}
				final File destDir = new File(projectRoot, dest);
				if (!destDir.exists()) {
					destDir.mkdirs();
				}
			}
			// keep jar as a file, protect it against gradle shadow plugin
			if (targetFilename.endsWith(".jar.zip")) {
				targetFilename = targetFilename.replace(".jar.zip", ".jar");
			}
			try (final InputStream in = ApkSpy.class.getClassLoader().getResourceAsStream(filename)) {
				final File targetFile = new File(projectRoot, targetFilename);
				FileUtils.copyInputStreamToFile(in, targetFile);
			} catch (final IOException e) {
				LOG.error("Could not copy project template: ", e);
			}
		}
	}
}
