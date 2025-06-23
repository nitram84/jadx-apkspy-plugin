package jadx.plugins.apkspy.ui;

import jadx.api.impl.InMemoryCodeCache;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.utils.Util;

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

							String determinedContent = extractMethod(code, iCodeNodeRef.getDefPosition());

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

		// Adding classes is not (yet) supported by disk cache
		if (context.getArgs().getCodeCache() instanceof InMemoryCodeCache) {
			guiContext.addTreePopupMenuEntry("Add class",
					node -> node instanceof JPackage,
					node -> {
						final String uniqueClassName = generateClassName((JPackage) node, "MyClass");
						final String clsTemplate = "package " + node.getName() + ";\n\npublic class " + uniqueClassName + " {\n}\n";

						final AddClassDialog dialog = new AddClassDialog(guiContext.getMainFrame(), options, node, context.getDecompiler());
						dialog.setCodeAreaContent(clsTemplate);
						dialog.setVisible(true);
					});
		}
	}

	private static String extractMethod(String text, int offset) {
		String[] lines = text.split(System.getProperty("line.separator"));
		StringBuilder extraction = new StringBuilder();

		int linePos = 0;
		for (String line : lines) {
			int start = linePos;

			linePos += line.length();
			linePos += System.getProperty("line.separator").length();

			String str = line.trim();
			if (str.isEmpty()) {
				continue;
			}
			if (!line.startsWith("    ")) {
				if (str.startsWith("package ")) {
					str += "\n";
				} else if (str.contains("class ")) {
					str = "\n" + str;
				}

				extraction.append(str).append('\n');
			}

			if (line.startsWith("    ") && !line.startsWith("     ") && str.endsWith("{")) {
				int closing = Util.findClosingBracket(text, start + line.lastIndexOf('{'));
				if (offset > start && offset < closing) {
					String method = text.substring(start, closing);
					extraction.append(method);
					extraction.append("}\n}\n");
					return extraction.toString();
				}
			}
		}

		return null;
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
