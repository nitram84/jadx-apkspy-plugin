package jadx.plugins.apkspy.model;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.plugins.apkspy.utils.MethodExtractorUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodExtractorUtilsTest {

	@Test
	public void findMethodPositionTest() throws Exception {
		final JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleFile("methodextraction.smali"));
		try (final JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			final JavaClass cls = jadx.getClasses().get(0);

			final String code =
					"\n" +
							"import jakarta.validation.Valid;\n" +
							"import java.util.Map;\n" +
							"\n" +
							"\n" +
							"public class MethodExtraction<A, B> {\n" +
							"    private void test(@Valid Map<A, Map<Map<B, String>, String>> a, long b) {\n" +
							"        System.out.print(\"This is an modified class with different line numbers\");\n" +
							"    }\n" +
							"}";

			int pos = MethodExtractorUtils.findMethodPosition(cls.getMethods().get(0), code);
			Assertions.assertEquals(-1, pos);
			pos = MethodExtractorUtils.findMethodPosition(cls.getMethods().get(1), code);
			Assertions.assertNotEquals(-1, pos);
			Assertions.assertEquals("import jakarta.validation.Valid;\n" +
					"import java.util.Map;\n" +
					"\n" +
					"public class MethodExtraction<A, B> {\n" +
					"    private void test(@Valid Map<A, Map<Map<B, String>, String>> a, long b) {\n" +
					"        System.out.print(\"This is an modified class with different line numbers\");\n" +
					"    }\n" +
					"}\n", MethodExtractorUtils.extractMethod(code, pos));
		}
	}

	private File getSampleFile(final String fileName) throws URISyntaxException {
		final URL file = getClass().getClassLoader().getResource("samples/" + fileName);
		assertThat(file).isNotNull();
		return new File(file.toURI());
	}
}
