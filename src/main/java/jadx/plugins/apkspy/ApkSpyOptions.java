package jadx.plugins.apkspy;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class ApkSpyOptions extends BasePluginOptionsBuilder {

	private String androidSdkPath;

	private String jdkLocation;

	private String apktoolLocation;

	@Override
	public void registerOptions() {
		strOption(ApkSpyPlugin.PLUGIN_ID + ".androidSdk.path")
				.description("Android SDK location")
				.defaultValue("")
				.setter(v -> androidSdkPath = v);

		strOption(ApkSpyPlugin.PLUGIN_ID + ".jdkLocation.path")
				.description("JDK location (Java 8)")
				.defaultValue("")
				.setter(v -> jdkLocation = v);

		strOption(ApkSpyPlugin.PLUGIN_ID + ".apktoolLocation.path")
				.description("Apktool location")
				.defaultValue("")
				.setter(v -> apktoolLocation = v);
	}

	public String getAndroidSdkPath() {
		return androidSdkPath;
	}

	public String getJdkLocation() {
		return jdkLocation;
	}

	public String getApktoolLocation() {
		return apktoolLocation;
	}
}
