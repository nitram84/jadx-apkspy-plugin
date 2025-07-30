package jadx.plugins.apkspy.ui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JFrame;

import jadx.api.JadxDecompiler;
import jadx.api.gui.tree.ITreeNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.MainWindow;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.model.ChangeCache;
import jadx.plugins.apkspy.model.ClassBreakdown;
import jadx.plugins.apkspy.model.JavaSourceClassData;
import jadx.plugins.apkspy.utils.Util;

public class AddClassDialog extends ApkSpyDialog {

	private final ITreeNode node;

	public AddClassDialog(final JFrame mainWindow, final ApkSpyOptions options, final ITreeNode node, final JadxDecompiler decompiler) {
		super(mainWindow, options, decompiler, "Add class");

		this.node = node;
	}

	@Override
	protected void onSave() {
		final ClassBreakdown breakdown = ClassBreakdown.breakdown(null, null, Util.formatSources(this.codeArea.getText()));
		breakdown.setFullName(this.node.getName() + "." + breakdown.getSimpleName());

		final ClassNode classNode =
				new ClassNode(decompiler.getRoot(), new JavaSourceClassData(this.node.getName(), breakdown.getSimpleName()));
		final JClass cls = (JClass) ((MainWindow) mainWindow).getCacheObject().getNodeCache().makeFrom(classNode);

		final JPackage packageNode = (JPackage) this.node;
		if (packageNode.getClasses().isEmpty()) {
			try {
				final Field classList = packageNode.getClass().getDeclaredField("classes");
				classList.setAccessible(true);
				classList.set(packageNode, new ArrayList<JClass>());
				classList.setAccessible(false);
			} catch (final NoSuchFieldException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		packageNode.getClasses().add(cls);

		Collections.sort(packageNode.getClasses());
		packageNode.update();
		((MainWindow) mainWindow).reloadTree();

		ChangeCache.getInstance().putChange(breakdown.getFullName(), breakdown, null);
	}

	@Override
	protected ClassBreakdown onPrepareCompile() {
		final ClassBreakdown breakdown = ClassBreakdown.breakdown(null, null, Util.formatSources(codeArea.getText()));
		breakdown.setFullName(this.node.getName() + "." + breakdown.getSimpleName());
		return breakdown;
	}
}
