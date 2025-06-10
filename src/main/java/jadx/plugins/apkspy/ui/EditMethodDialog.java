package jadx.plugins.apkspy.ui;

import java.util.List;

import javax.swing.JFrame;

import jadx.api.JadxDecompiler;
import jadx.api.impl.SimpleCodeInfo;
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

	public EditMethodDialog(final JFrame mainWindow, final ApkSpyOptions options, final JadxDecompiler decompiler,
			final MethodNode methodNode,
			final String title) {
		super(mainWindow, options, decompiler, title);

		this.methodNode = methodNode;
	}

	private ClassBreakdown merge(final ClassBreakdown changed, final ClassBreakdown original) {
		return changed.mergeMemberVariables(original.getMemberVariables())
				.mergeMethodStubs(original.getMethods()).mergeInnerClassStubs(original);
	}

	@Override
	protected void onSave() {
		final ClassNode clsNode = methodNode.getParentClass();
		final String originalCode = decompiler.getRoot().getCodeCache().get(clsNode.getFullName()).getCodeStr();
		final ClassBreakdown original = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(), originalCode);
		final ClassBreakdown changed = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(),
				Util.formatSources(this.codeArea.getText()));

		final ClassBreakdown completed = original.mergeImports(changed.getImports())
				.mergeMethods(changed.getChangedMethods());

		ChangeCache.putChange(clsNode.getFullName(), this.merge(changed, original), changed.getMethods().get(0));

		decompiler.getRoot().getCodeCache().add(clsNode.getFullName(),
				new SimpleCodeInfo(completed.toString()));
		final List<ContentPanel> contentPanes = ((MainWindow) mainWindow).getTabbedPane().getTabs();
		for (final ContentPanel contentPane : contentPanes) {
			if (contentPane instanceof AbstractCodeContentPanel) {
				final AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPane).getCodeArea();
				if (codeArea.getNode().getJavaNode().getFullName().equals(clsNode.getFullName())) {
					codeArea.refresh();
					break;
				}
			}
		}
	}

	@Override
	protected ClassBreakdown onPrepareCompile() {
		final ClassNode clsNode = methodNode.getParentClass();
		final String originalCode = decompiler.getRoot().getCodeCache().get(clsNode.getFullName()).getCodeStr();
		final ClassBreakdown original = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(), originalCode);
		final ClassBreakdown changed = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(),
				Util.formatSources(codeArea.getText()));
		return this.merge(changed, original);
	}
}
