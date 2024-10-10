package jadx.plugins.apkspy;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.plugins.apkspy.ui.ApkSpyUI;

public class ApkSpyPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "apkspy";

	private final ApkSpyOptions options = new ApkSpyOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("ApkSpy")
				.description("Support for editing and recompiling Java source")
				.provides("jadx-apkspy-plugin")
				.build();
	}

	@Override
	public void init(final JadxPluginContext context) {
		context.registerOptions(options);

		final JadxGuiContext guiContext = context.getGuiContext();
		if (guiContext != null) {
			ApkSpyUI.setup(context, guiContext, options);
		}
	}
}
