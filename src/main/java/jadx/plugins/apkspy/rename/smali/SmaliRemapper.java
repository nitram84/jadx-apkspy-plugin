package jadx.plugins.apkspy.rename.smali;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;

public class SmaliRemapper {
	private static final Pattern METHOD_DECL_PATTERN =
			Pattern.compile("^\\.method.*\\s+([\\w$<>]+)\\(([^)]*)\\)(L[^;]+;|[ZBCSIFJDV])");

	private static final Pattern METHOD_REF_PATTERN =
			Pattern.compile("(L[^;]+;)->([\\w$<>]+)\\(([^)]*)\\)(L[^;]+;|[ZBCSIFJDV])");

	private static final Pattern FIELD_DECL_PATTERN =
			Pattern.compile("\\.field\\s+(?:[a-z]+\\s+)*([a-zA-Z0-9_$]+):([L\\[a-zA-Z0-9_/$&;]+)");

	private static final Pattern FIELD_ARROW_PATTERN =
			Pattern.compile("(L[^;]+;)->([\\w$]+):([L\\[a-zA-Z0-9_/$&;]+)");

	private static final Pattern CLASS_PATTERN = Pattern.compile("L[^\\s;:<>()]+;");

	private final Map<String, JavaClass> classMap = new HashMap<>();
	private final JadxDecompiler decompiler;

	public SmaliRemapper(final JadxDecompiler decompiler) {
		this.decompiler = decompiler;
	}

	public void prepopulateNameCache() {
		// Prepopulate class map (faster than searching class), but not all classes are available by
		// class traversal (jadx bug?)
		for (final JavaClass javaClass : decompiler.getClasses()) {
			addInnerClassesToNameCache(javaClass);
			classMap.put(javaClass.getRawName().replace('.', '/'), javaClass);
		}
	}

	private void addInnerClassesToNameCache(final JavaClass javaClass) {
		if (!javaClass.getInnerClasses().isEmpty()) {
			for (final JavaClass inner : javaClass.getInnerClasses()) {
				final int length = inner.getTopParentClass().getRawName().length();
				classMap.put(inner.getRawName().substring(0, length).replace('.', '/')
						+ inner.getRawName().substring(length).replace('.', '$'), inner);
				addInnerClassesToNameCache(inner);
			}
		}
	}

	public void remapSmaliFolder(final Path smaliFolder, final Path targetBaseFolder)
			throws IOException {
		Files.walk(smaliFolder)
				.filter(
						path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".smali"))
				.forEach(path -> {
					final File inputSmali = path.toAbsolutePath().toFile();
					final String classname = path.toAbsolutePath().toString().substring(
							smaliFolder.toAbsolutePath().toString().length() + 1,
							path.toAbsolutePath().toString().length() - ".smali".length());

					JavaClass jadxClass = classMap.get(classname);
					if (jadxClass == null) {
						jadxClass = decompiler
								.searchJavaClassByOrigFullName(classname.replace("/", ".").replace("$", "."));
						if (jadxClass != null) {
							classMap.put(classname, jadxClass);
						} else {
							System.out.println("Could not resolve class " + classname);
							return;
						}
					}
					String deobfuscatedClassname;
					if (jadxClass.isInner()) {
						final int length = jadxClass.getTopParentClass().getFullName().length();
						deobfuscatedClassname =
								jadxClass.getFullName().substring(0, length).replace('.', File.separatorChar)
										+ jadxClass.getFullName().substring(length).replace('.', '$');
					} else {
						deobfuscatedClassname = jadxClass.getFullName().replace('.', File.separatorChar);
					}
					final String inputPath = inputSmali.getAbsolutePath();
					final String outputPath =
							targetBaseFolder.toString() + File.separator + deobfuscatedClassname + ".smali";

					final List<String> outputLines = new ArrayList<>();

					try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
						String line;
						while ((line = reader.readLine()) != null) {
							line = remapMethod(line, classname);
							line = remapField(line, classname);
							;
							line = remapClass(line);

							outputLines.add(line);
						}

						final File output = new File(outputPath.substring(0, outputPath.lastIndexOf('/')));
						output.mkdirs();
						try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
							for (final String outputLine : outputLines) {
								writer.write(outputLine);
								writer.newLine();
							}
						}

					} catch (final IOException e) {
						System.err.println("Remapping failed: " + e.getMessage());
					}
				});
	}

	private String remapMethod(final String line, final String currentFileClass) {
		if (line.trim().startsWith(".method")) {
			final Matcher matcher = METHOD_DECL_PATTERN.matcher(line);
			if (matcher.find()) {
				final String methodName = matcher.group(1);
				final String params = matcher.group(2);
				final String returnType = matcher.group(3);

				if (classMap.containsKey(currentFileClass)) {
					final JavaClass jadxClass = classMap.get(currentFileClass);
					final String methodRawId = currentFileClass.replace('/', '.') + "." + methodName + "("
							+ params + ")" + returnType;
					for (final JavaMethod method : jadxClass.getMethods()) {

						if (method.getMethodNode().getMethodInfo().getRawFullId().equals(methodRawId)) {
							// TODO: deobfuscate params, returntype here, skip remapClass for this line
							return line.substring(0, matcher.start(1))
									+ method.getMethodNode().getMethodInfo().getAlias() + "(" + params + ")"
									+ returnType + line.substring(matcher.end(3));
						}
					}
				}
			}
			return line;
		}

		// method invoke or reference
		final Matcher matcher = METHOD_REF_PATTERN.matcher(line);
		final StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			final String className = matcher.group(1);
			final String methodName = matcher.group(2);
			final String params = matcher.group(3);
			final String returnType = matcher.group(4);

			final String key = className.substring(1, className.length() - 1);
			if (classMap.containsKey(key)) {
				final JavaClass jadxClass = classMap.get(key);
				String newClassName;
				if (jadxClass.isInner()) {
					final int length = jadxClass.getTopParentClass().getFullName().length();
					newClassName = "L" + jadxClass.getFullName().substring(0, length).replace('.', '/')
							+ jadxClass.getFullName().substring(length).replace('.', '$') + ";";
				} else {
					newClassName = "L" + jadxClass.getFullName().replace('.', '/') + ";";
				}
				final String methodRawId =
						jadxClass.getRawName() + "." + methodName + "(" + params + ")" + returnType;
				for (final JavaMethod method : jadxClass.getMethods()) {
					if (method.getMethodNode().getMethodInfo().getRawFullId().equals(methodRawId)) {
						return line.substring(0, matcher.start(1)) + newClassName + "->"
								+ method.getMethodNode().getMethodInfo().getAlias() + "(" + params + ")"
								+ returnType + line.substring(matcher.end(4));
					}
				}
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private String remapClass(final String line) {
		final Matcher matcher = CLASS_PATTERN.matcher(line);
		final StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			final String classToken = matcher.group();
			final String key = classToken.substring(1, classToken.length() - 1);
			if (classMap.containsKey(key)) {
				final JavaClass jadxClass = classMap.get(key);
				String newClassName;
				if (jadxClass.isInner()) {
					final int length = jadxClass.getTopParentClass().getFullName().length();
					newClassName = "L" + jadxClass.getFullName().substring(0, length).replace('.', '/')
							+ jadxClass.getFullName().substring(length).replace('.', '$') + ";";
				} else {
					newClassName = "L" + jadxClass.getFullName().replace('.', '/') + ";";
				}

				matcher.appendReplacement(sb, Matcher.quoteReplacement(newClassName));
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private String remapField(final String line, final String currentFileClass) {
		final String trimmed = line.trim();

		// field declaration
		if (trimmed.startsWith(".field")) {
			final Matcher matcher = FIELD_DECL_PATTERN.matcher(line);
			if (matcher.find()) {

				final String fieldName = matcher.group(1);
				final String fieldType = matcher.group(2);

				if (classMap.containsKey(currentFileClass)) {
					final JavaClass jadxClass = classMap.get(currentFileClass);
					final String fieldRawId = currentFileClass.replace('/', '.') + "." + fieldName + ":" + fieldType;
					for (final JavaField field : jadxClass.getFields()) {

						if (field.getFieldNode().getFieldInfo().getFullId().equals(fieldRawId)) {
							return line.substring(0, matcher.start(1)) + field.getName() + ":" + fieldType
									+ line.substring(matcher.end(2));
						}
					}
				}
			}
		}

		// field reference
		final Matcher matcher = FIELD_ARROW_PATTERN.matcher(line);
		final StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			final String className = matcher.group(1);
			final String fieldName = matcher.group(2);
			final String fieldType = matcher.group(3);
			final String key = className.substring(1, className.length() - 1);
			if (classMap.containsKey(key)) {
				final JavaClass jadxClass = classMap.get(key);

				final String fieldRawId = key.replace('/', '.') + "." + fieldName + ":" + fieldType;
				for (final JavaField field : jadxClass.getFields()) {

					if (field.getFieldNode().getFieldInfo().getFullId().equals(fieldRawId)) {
						return line.substring(0, matcher.start(1)) + "L"
								+ field.getDeclaringClass().getFullName().replace('.', '/') + ";->"
								+ field.getName() + ":" + fieldType + line.substring(matcher.end(3));
					}
				}
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
