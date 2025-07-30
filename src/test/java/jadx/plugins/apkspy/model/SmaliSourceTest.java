package jadx.plugins.apkspy.model;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class SmaliSourceTest {
	protected File getSampleFile(final String fileName) throws URISyntaxException {
		final URL file = getClass().getClassLoader().getResource("samples/" + fileName);
		assertThat(file).isNotNull();
		return new File(file.toURI());
	}
}
