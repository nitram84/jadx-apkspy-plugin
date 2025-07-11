package jadx.plugins.apkspy.ui;

import jadx.api.impl.InMemoryCodeCache;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JSources;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.utils.MethodExtractorUtils;

import static jadx.api.metadata.ICodeAnnotation.AnnType;

public class ApkSpyUI {
	public static void setup(JadxPluginContext context, JadxGuiContext guiContext, ApkSpyOptions options) {
		guiContext.addMenuAction("Save APK", () -> guiContext
				.uiRun(() -> new ApkSpySaver(guiContext.getMainFrame(), context.getDecompiler(), options).setVisible(true)));

		guiContext.addPopupMenuAction("Edit method", iCodeNodeRef -> iCodeNodeRef.getAnnType().equals(AnnType.METHOD), null,
				iCodeNodeRef -> {
					if (iCodeNodeRef.getAnnType().equals(AnnType.METHOD)) {
						if (iCodeNodeRef instanceof MethodNode) {
							MethodNode methodNode = (MethodNode) iCodeNodeRef;
							String code = context.getDecompiler().getRoot().getCodeCache()
									.getCode(methodNode.getMethodInfo().getDeclClass().getFullName());

							String determinedContent = MethodExtractorUtils.extractMethod(code, iCodeNodeRef.getDefPosition());

							if (determinedContent != null) {
								guiContext.uiRun(() -> {
									EditMethodDialog dialog =
											new EditMethodDialog(guiContext.getMainFrame(), options, context.getDecompiler(), methodNode,
													"Edit Method");
									dialog.setCodeAreaContent(determinedContent);
									dialog.setVisible(true);
								});
							}
						}
					}
				});

		guiContext.addTreePopupMenuEntry("Edit method",
				node -> node instanceof JMethod,
				node -> {
					final JMethod method = (JMethod) node;
					final JClass parent = ((JMethod) node).getJParent();

					final String code = context.getDecompiler().getRoot().getCodeCache()
							.getCode(parent.getFullName());

					try {
						int pos = MethodExtractorUtils.findMethodPosition(method.getJavaMethod(), code);
						if (pos > -1) {
							final String determinedContent = MethodExtractorUtils.extractMethod(code, pos);

							if (determinedContent != null) {
								guiContext.uiRun(() -> {
									final EditMethodDialog dialog =
											new EditMethodDialog(guiContext.getMainFrame(), options, context.getDecompiler(),
													method.getJavaMethod().getMethodNode(), "Edit Method");
									dialog.setCodeAreaContent(determinedContent);
									dialog.setVisible(true);
								});
							}
						}
					} catch (final Exception ignore) { // e.g. constructor nodes without code -> no error
					}
				});

		// Adding packages and classes is not (yet) supported by disk cache
		if (context.getArgs().getCodeCache() instanceof InMemoryCodeCache) {
			guiContext.addTreePopupMenuEntry("Add package",
					node -> node instanceof JSources || node instanceof JPackage,
					node -> {
						guiContext.uiRun(() -> {
							final AddPackageDialog dialog =
									new AddPackageDialog(guiContext.getMainFrame(), (JNode) node, context.getDecompiler());
							dialog.setVisible(true);
						});
					});

			guiContext.addTreePopupMenuEntry("Add class",
					node -> node instanceof JPackage,
					node -> {
						JPackage packageNode = (JPackage) node;
						final String uniqueClassName = generateClassName(packageNode, "MyClass");
						final String clsTemplate =
								"package " + packageNode.getPkg().getFullName() + ";\n\npublic class " + uniqueClassName + " {\n}\n";

						final AddClassDialog dialog = new AddClassDialog(guiContext.getMainFrame(), options, node, context.getDecompiler());
						dialog.setCodeAreaContent(clsTemplate);
						dialog.setVisible(true);
					});
		}
	}

	private static String generateClassName(final JPackage node, final String className) {
		boolean found = true;
		for (final JClass cls : node.getClasses()) {
			if (cls.getCls().getName().equals(className)) {
				found = false;
				break;
			}
		}
		if (found) {
			return className;
		} else {
			int i = 0;
			while (true) {
				found = true;
				i++;
				for (final JClass cls : node.getClasses()) {
					if (cls.getCls().getName().equals(className + i)) {
						found = false;
						break;
					}
				}
				if (found) {
					return className + i;
				}
			}
		}
	}
}
