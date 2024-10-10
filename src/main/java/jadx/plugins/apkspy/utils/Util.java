package jadx.plugins.apkspy.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;

public class Util {

	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	public static void attemptDelete(File file) {
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			if (e.getMessage().startsWith("Unable to delete file:")) {
				LOG.warn("Warning: could not delete write-protected file: {}",
						e.getMessage().split(":")[1].trim());
			}
		}
	}

	public static int system(File dir, String jdkLocation, OutputStream out, String... args) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(args).directory(dir).redirectErrorStream(true);
		if (System.getenv("PATH") != null) {
			builder.environment().put("PATH", System.getenv("PATH"));
		}
		if (System.getenv("Path") != null) {
			builder.environment().put("Path", System.getenv("Path"));
		}
		builder.environment().put("JAVA_HOME", jdkLocation);

		Process proc = builder.start();

		out.write(String.join(" ", args).getBytes(StandardCharsets.UTF_8));
		out.write('\n');

		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			out.write(line.getBytes(StandardCharsets.UTF_8));
			out.write('\n');
		}

		return proc.waitFor();
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	public static boolean isValidSdkPath(Path path) throws IOException {
		return Files.isDirectory(path) && Files.isDirectory(path.resolve("platform-tools"))
				&& Files.list(path.resolve("platform-tools")).filter(file -> file.getFileName().toString().equals("adb")
						|| file.getFileName().toString().equals("adb.exe")).count() > 0;
	}

	public static int findClosingBracket(String expression, int index) {
		if (expression.charAt(index) != '{') {
			return -1;
		}

		int count = 0;
		for (int i = index; i < expression.length(); i++) {
			if (expression.charAt(i) == '{') {
				count++;
			} else if (expression.charAt(i) == '}') {
				if (--count == 0) {
					return i;
				}
			}
		}

		return -1;
	}

	public static String formatSources(final String content) {
		CompilationUnit cu = StaticJavaParser.parse(content);
		DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();
		DefaultPrinterConfiguration prettyPrinterConfig = new DefaultPrinterConfiguration();
		defaultPrettyPrinter.setConfiguration(prettyPrinterConfig);
		return defaultPrettyPrinter.print(cu);
	}
}
