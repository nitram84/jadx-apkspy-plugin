package jadx.plugins.apkspy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;

import jadx.plugins.apkspy.ui.ApkSaveOptions;

public class ApktoolWrapper {

	private static String apktoolVersion = "";

	private static String getApktoolVersion() {

		if (apktoolVersion.isEmpty()) {
			Properties properties = new Properties();

			try (InputStream input = ApktoolWrapper.class.getClassLoader().getResourceAsStream("versions.properties")) {
				properties.load(input);
				apktoolVersion = properties.getProperty("apktool.version");
			} catch (IOException ignored) {
			}
		}
		return apktoolVersion;
	}

	private static Config getConfig() {
		return new Config(getApktoolVersion());
	}

	public static void decode(Path apk, File outDir, boolean resources)
			throws AndrolibException {

		Config config = getConfig();
		if (resources) {
			config.setDecodeResources(Config.DecodeResources.NONE);
		}

		new ApkDecoder(apk.toFile(), config).decode(outDir);
	}

	public static void build(Path apk, String outputLocation, ApkSaveOptions saveOptions)
			throws AndrolibException {

		Config config = getConfig();
		config.setNetSecConf(saveOptions.isAddNetworkSecurityConfiguration());
		config.setDebuggable(saveOptions.isCreateDebugableApk());
		File outFile = new File(outputLocation);
		new ApkBuilder(apk.toFile(), config).build(outFile);
	}
}
