package jadx.plugins.apkspy.ui;

import java.util.List;

import javax.swing.JFrame;

import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.JadxPluginContext;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.panel.ContentPanel;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.model.ChangeCache;
import jadx.plugins.apkspy.model.ClassBreakdown;
import jadx.plugins.apkspy.utils.Util;

public class EditMethodDialog extends ApkSpyDialog {

	private final MethodNode methodNode;

	public EditMethodDialog(final JFrame mainWindow, final ApkSpyOptions options, JadxPluginContext context,
			final MethodNode methodNode,
			final String title) {
		super(mainWindow, options, context, title);

		this.methodNode = methodNode;
	}

	private ClassBreakdown merge(final ClassBreakdown changed, final ClassBreakdown original) {
		return changed.mergeMemberVariables(original.getMemberVariables())
				.mergeMethodStubs(original.getMethods()).mergeInnerClassStubs(original);
	}

	@Override
	protected void onSave() {
		ClassNode clsNode = methodNode.getParentClass();
		final ClassNode topParentClass;
		if (clsNode.isInner()) {
			topParentClass = clsNode.getTopParentClass();
		} else {
			topParentClass = clsNode;
		}
		String topParentFullName = topParentClass.getClassInfo().getFullName();
		String originalCode = decompiler.getRoot().getCodeCache().get(topParentFullName).getCodeStr();
		final ClassBreakdown original = ClassBreakdown.breakdown(clsNode.getFullName(), originalCode);
		final ClassBreakdown changed = ClassBreakdown.breakdown(clsNode.getFullName(), Util.formatSources(this.codeArea.getText()));

		final ClassBreakdown completed = original.mergeImports(changed.getImports()).addOrReplaceMethods(changed);

		ChangeCache.getInstance().putChange(topParentFullName, this.merge(changed, original), true);

		decompiler.getRoot().getCodeCache().add(topParentFullName,
				new SimpleCodeInfo(completed.toString()));
		final List<ContentPanel> contentPanes = ((MainWindow) mainWindow).getTabbedPane().getTabs();
		for (final ContentPanel contentPane : contentPanes) {
			if (contentPane instanceof AbstractCodeContentPanel) {
				final AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPane).getCodeArea();
				if (codeArea.getNode().getJavaNode().getTopParentClass().getFullName().equals(topParentFullName)) {
					codeArea.refresh();
					break;
				}
			}
		}
	}

	@Override
	protected ClassBreakdown onPrepareCompile() {
		final ClassNode clsNode = methodNode.getParentClass();
		final ClassNode topParentClass;
		if (clsNode.isInner()) {
			topParentClass = clsNode.getTopParentClass();
		} else {
			topParentClass = clsNode;
		}
		final String originalCode = decompiler.getRoot().getCodeCache().get(topParentClass.getClassInfo().getFullName()).getCodeStr();
		final ClassBreakdown original = ClassBreakdown.breakdown(clsNode.getFullName(), originalCode);
		final ClassBreakdown changed = ClassBreakdown.breakdown(clsNode.getFullName(), Util.formatSources(codeArea.getText()));
		return this.merge(changed, original);
	}
}
