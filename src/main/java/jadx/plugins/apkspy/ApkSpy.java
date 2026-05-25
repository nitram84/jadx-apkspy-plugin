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
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brut.androlib.exceptions.AndrolibException;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.plugins.apkspy.model.ChangeCache;
import jadx.plugins.apkspy.model.ClassBreakdown;
import jadx.plugins.apkspy.model.SmaliBreakdown;
import jadx.plugins.apkspy.model.SmaliMethod;
import jadx.plugins.apkspy.utils.Util;

public class ApkSpy {

	private static final Logger LOG = LoggerFactory.getLogger(ApkSpy.class);

	private static String getClasspath(String... libs) {
		return Arrays.stream(libs)
				.map(lib -> new File(lib).isAbsolute() ? lib : String.join(File.separator, "..", "libs", lib))
				.collect(Collectors.joining(File.pathSeparator));
	}

	private static String findLatestAndroidJars(final String sdkPath) {
		File platformDir = new File(sdkPath, "platforms");
		if (platformDir.exists() && platformDir.listFiles() != null) {
			int lastVersion = -1;
			for (File androidVersionDir : Objects.requireNonNull(platformDir.listFiles())) {
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

	public static boolean lint(JadxDecompiler decompiler, Path baseTempDir, String className, ClassBreakdown content, String sdkPath,
			String jdkLocation,
			OutputStream out)
			throws IOException, InterruptedException {
		String apk = decompiler.getArgs().getInputFiles().get(0).toString();
		LOG.info("Linting: {}", apk);
		File modifyingApk = new File(apk);
		Path root = baseTempDir.resolve("lint_" + System.currentTimeMillis());
		Files.createDirectories(root);

		// create current source file
		String pkg = className.substring(0, className.lastIndexOf('.'));
		Path folder = root.resolve(Paths.get("src", pkg.replace('.', File.separatorChar)));
		if (!Files.isDirectory(folder)) {
			Files.createDirectories(folder);
		}
		Files.writeString(root.resolve(Paths.get("src", className.replace('.', File.separatorChar) + ".java")),
				content.toString());

		// add R file if generated
		Path rPath = null;
		ClassNode generatedR = findGeneratedRFile(decompiler.getRoot());
		if (generatedR != null) {
			Path rFolder = root.resolve(Paths.get("src", generatedR.getPackage().replace('.', File.separatorChar)));
			if (!Files.isDirectory(rFolder)) {
				Files.createDirectories(rFolder);
			}
			rPath = root.resolve(Paths.get("src", generatedR.getFullName().replace('.', File.separatorChar) + ".java"));
			Files.writeString(rPath,
					generatedR.getCode().getCodeStr());
		}

		// create stub jar
		Path stubPath = root.resolve(Paths.get("libs", "stub.jar"));
		Files.createDirectories(root.resolve("libs"));
		Map<String, ClassBreakdown> classes = Collections.singletonMap(className, content);

		try {
			JarGenerator.generateStubJar(modifyingApk, stubPath.toFile(), out, classes, root);
		} catch (IOException ex) {
			out.write(("Failed to generate stub jar: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
			LOG.error("Failed to generate stub jar: ", ex);
		}

		Files.createDirectories(root.resolve("bin"));

		// compile
		Path javac = null;
		if (jdkLocation != null && !jdkLocation.isEmpty()) {
			javac = Paths.get(jdkLocation, "bin", "javac");
			if (!Files.isExecutable(javac)) {
				javac = javac.getParent().resolve("javac.exe");
				if (!Files.isExecutable(javac)) {
					javac = null;
				}
			}
		}

		out.write("Started compile...\n".getBytes(StandardCharsets.UTF_8));
		String targetVersionDir = findLatestAndroidJars(sdkPath);
		int code = Util.system(root.resolve("src").toFile(), jdkLocation, out, javac == null ? "javac" : javac.toAbsolutePath().toString(),
				"-cp",
				getClasspath(targetVersionDir + File.separator + "android.jar", "stub.jar",
						targetVersionDir + File.separator + "optional" + File.separator + "org.apache.http.legacy.jar"),
				"-d",
				".." + File.separator + "bin", className.replace('.', File.separatorChar) + ".java",
				rPath == null ? "" : rPath.toAbsolutePath().toString());

		Util.attemptDelete(root.toFile());

		return code == 0;
	}

	private static ClassNode findGeneratedRFile(RootNode root) {
		String appPackage = root.getAppPackage();
		String fullName = appPackage != null ? appPackage + ".R" : "R";
		ClassInfo clsInfo = ClassInfo.fromName(root, fullName);
		ClassNode resCls = root.resolveClass(clsInfo);
		if (resCls != null && resCls.getAccessFlags().containsFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
				&& resCls.getCode().getCodeStr().contains("This class is generated by JADX")) {
			return resCls;
		}
		return null;
	}

	public static boolean merge(JadxDecompiler decompiler, String outputLocation, Path baseTempDir, String sdkPath, String jdkLocation,
			String applicationId,
			OutputStream out, boolean keepOnError, boolean cleanOnSuccess)
			throws IOException, InterruptedException {

		String apk = decompiler.getArgs().getInputFiles().get(0).toString();
		LOG.info("Merging: {}", apk);
		Path root = baseTempDir.resolve("merge_" + System.currentTimeMillis());
		Files.createDirectories(root);

		File modifyingApk = new File(apk);

		Path projectRoot = root.resolve("project-tmp");
		Files.createDirectories(projectRoot);

		copyProjectTemplate(projectRoot.toFile());

		Files.writeString(projectRoot.resolve("local.properties"),
				"sdk.dir=" + sdkPath.replace("\\", "\\\\"));

		Path gradleBuildPath = projectRoot.resolve(projectRoot.resolve(Paths.get("app", "build.gradle")));
		String buildGradle = Files.readString(gradleBuildPath);
		buildGradle = buildGradle.replace("$APPLICATION_ID", applicationId);

		Files.writeString(gradleBuildPath, buildGradle);

		Map<String, ClassBreakdown> classes = ChangeCache.getInstance().getChanges();
		for (Map.Entry<String, ClassBreakdown> entry : classes.entrySet()) {
			String className = entry.getKey();
			ClassBreakdown content = entry.getValue();

			Path completePath = projectRoot.resolve(Paths.get("app", "src", "main", "java",
					className.substring(0, className.lastIndexOf('.')).replace(".", File.separator)));
			Files.createDirectories(completePath);

			Path newFile = completePath.resolve("ApkSpy_" + className.substring(className.lastIndexOf('.') + 1) + ".java");// toCompile.getName());
			Files.writeString(newFile, content.toString());

			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			String newFileContent = Files.readString(newFile);
			newFileContent = newFileContent.replaceAll("(class|interface|enum|@interface) +" + simpleName + "(.*)\\{",
					"$1 ApkSpy_" + simpleName + "$2{");
			newFileContent = newFileContent.replaceAll(simpleName + " *\\((.*)\\) *\\{",
					"ApkSpy_" + simpleName + "($1) {");
			Files.writeString(newFile, newFileContent);
		}

		// add R file if generated
		ClassNode generatedR = findGeneratedRFile(decompiler.getRoot());
		if (generatedR != null) {
			Path rFolder =
					projectRoot.resolve(Paths.get("app", "src", "main", "java", generatedR.getPackage().replace('.', File.separatorChar)));
			if (!Files.isDirectory(rFolder)) {
				Files.createDirectories(rFolder);
			}
			Files.writeString(
					projectRoot.resolve(
							Paths.get("app", "src", "main", "java", generatedR.getFullName().replace('.', File.separatorChar) + ".java")),
					generatedR.getCode().getCodeStr());
		}

		Path stubPath = projectRoot.resolve(Paths.get("app", "libs", "stub.jar"));
		try {
			JarGenerator.generateStubJar(modifyingApk, stubPath.toFile(), out, classes, root);
		} catch (IOException e) {
			return false;
		}
		if (!Util.isWindows()) {
			Runtime.getRuntime().exec("chmod +x " + projectRoot.toFile().getAbsolutePath() + "/gradlew").waitFor();
		}

		if (Util.system(projectRoot.toFile().getAbsoluteFile(), jdkLocation, out, projectRoot.toFile().getAbsoluteFile() + File.separator
				+ (Util.isWindows() ? "gradlew.bat" : "gradlew"), "build") != 0) {
			if (!keepOnError) {
				Util.attemptDelete(projectRoot.toFile());
			}
			return false;
		}

		Path target = root.resolve("generated.apk");
		Files.move(projectRoot.resolve(Paths.get("app", "build", "outputs", "apk", "debug", "app-debug.apk")),
				target, StandardCopyOption.REPLACE_EXISTING);
		if (cleanOnSuccess) {
			Util.attemptDelete(projectRoot.toFile());
		}

		Path smaliDir = root.resolve("smali");
		Files.createDirectories(smaliDir);

		out.write("Apktool: Decode generated apk\n".getBytes(StandardCharsets.UTF_8));
		try {
			ApktoolWrapper.decode(target, new File(smaliDir.toFile(), "generated"), false);
		} catch (AndrolibException e) {
			LOG.error("Decoding intermediate apk failed: ", e);
			out.write(("Decoding intermediate apk failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
			if (!keepOnError) {
				Files.delete(target);
			}
			return false;
		}
		if (cleanOnSuccess) {
			Files.delete(target);
		}

		out.write("Apktool: Decode original apk\n".getBytes(StandardCharsets.UTF_8));
		try {
			ApktoolWrapper.decode(modifyingApk.toPath(), new File(smaliDir.toFile(), "original"), true);
		} catch (AndrolibException e) {
			LOG.error("Decoding original apk failed: ", e);
		}

		List<Path> smaliFolders = Files.list(smaliDir.resolve("generated"))
				.filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("smali")).collect(Collectors.toList());
		List<Path> destinationFolders = smaliFolders.stream()
				.map(path -> Paths.get(path.toString().replace("generated", "original"))).collect(Collectors.toList());

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

							String modifiedContent = Files.readString(path);
							modifiedContent = modifiedContent.replace("ApkSpy_", "");

							if (equivalent != null) {
								String originalContent = Files.readString(equivalent);

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

									Files.writeString(equivalent, builder.toString());
								}
							} else {
								equivalent = Paths.get(smaliFolder.toString().replace("generated", "original"),
										path.toAbsolutePath().toString()
												.substring(smaliFolder.toAbsolutePath().toString().length())
												.replace("ApkSpy_", ""));
								Files.createDirectories(equivalent.getParent());
								Files.writeString(equivalent, modifiedContent, StandardOpenOption.CREATE,
										StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
							}
						} catch (IOException e) {
							LOG.error(
									"ApkSpy Plugin: Abort merge. Please report an issue at https://github.com/nitram84/jadx-apkspy-plugin/issues",
									e);
							throw new JadxRuntimeException(
									"ApkSpy Plugin: Abort merge. Please report an issue at https://github.com/nitram84/jadx-apkspy-plugin/issues",
									e);
						}
					});
		}

		Set<String> deletions = ChangeCache.getInstance().getClassDeletions();
		for (String deletion : deletions) {
			// file might not exist, as we could delete temporary classes that we made in
			// between compilations in the editor

			for (Path path : destinationFolders) {
				if (Files.deleteIfExists(Paths.get(path.toAbsolutePath().toString(),
						deletion))) {
					break;
				}
			}
		}

		out.write("Apktool: Build modified apk\n".getBytes(StandardCharsets.UTF_8));
		try {
			ApktoolWrapper.build(smaliDir.resolve("original"), outputLocation);
		} catch (AndrolibException e) {
			if (!keepOnError) {
				Util.attemptDelete(root.toFile());
			}
			LOG.error("Could not generate apk: ", e);
			out.write(("Could not generate apk: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
			return false;
		}

		if (new File(outputLocation).exists()) {
			out.write("Finished creating APK!".getBytes(StandardCharsets.UTF_8));
			if (cleanOnSuccess) {
				Util.attemptDelete(root.toFile());
			}
			return true;
		} else {
			out.write("Error: Could not generate apk.".getBytes(StandardCharsets.UTF_8));
			LOG.error("Could not generate apk.");
			if (!keepOnError) {
				Util.attemptDelete(root.toFile());
			}
			return false;
		}
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
