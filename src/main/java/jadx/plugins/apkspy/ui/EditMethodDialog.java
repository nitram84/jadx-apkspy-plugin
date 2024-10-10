package jadx.plugins.apkspy.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.panel.ContentPanel;
import jadx.plugins.apkspy.ApkSpy;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.model.ChangeCache;
import jadx.plugins.apkspy.model.ClassBreakdown;
import jadx.plugins.apkspy.utils.Util;

public class EditMethodDialog extends JDialog {

	private static final Logger LOG = LoggerFactory.getLogger(EditMethodDialog.class);

	private final transient ApkSpyCodeArea codeArea;

	private final transient JTextArea output;

	private final JadxDecompiler decompiler;

	private final MethodNode methodNode;

	private final ApkSpyOptions options;

	private final JFrame mainWindow;

	public EditMethodDialog(final JFrame mainWindow, final ApkSpyOptions options, final JadxDecompiler decompiler,
			final MethodNode methodNode,
			final String title) {
		super(SwingUtilities.windowForComponent(mainWindow));

		this.options = options;
		this.mainWindow = mainWindow;
		this.decompiler = decompiler;
		this.methodNode = methodNode;

		JPanel content = new JPanel();

		this.codeArea = new ApkSpyCodeArea();
		RTextScrollPane codeScrollPane = new RTextScrollPane(codeArea);

		codeScrollPane.setPreferredSize(new Dimension(800, 600));
		content.add(codeScrollPane);
		this.output = new JTextArea();
		this.output.setFont(new Font("Courier New", Font.PLAIN, this.output.getFont().getSize()));
		this.output.setEditable(false);

		JScrollPane scroll2 = new JScrollPane(output);
		Dimension size = codeScrollPane.getPreferredSize();

		scroll2.setPreferredSize(size);
		content.add(scroll2);

		JButton save = new JButton("Save");
		save.addActionListener(e -> onSave());
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		JButton compile = new JButton("Compile");
		compile.addActionListener(e -> {
			output.setText("");

			Thread thread = new Thread(this::onCompile);
			thread.start();
		});

		JPanel buttons = new JPanel();
		buttons.add(compile);
		buttons.add(save);
		buttons.add(cancel);

		add(buttons, BorderLayout.PAGE_START);
		add(content, BorderLayout.PAGE_END);

		setTitle(title);
		pack();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModal(true);
		setLocationRelativeTo(null);

		this.codeArea.setEditable(true);
		this.codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);

		this.codeArea.requestFocus();
	}

	private ClassBreakdown merge(ClassBreakdown changed, ClassBreakdown original) {
		return changed.mergeMemberVariables(original.getMemberVariables())
				.mergeMethodStubs(original.getMethods()).mergeInnerClassStubs(original);
	}

	private void onSave() {
		final ClassNode clsNode = methodNode.getParentClass();
		final String originalCode = decompiler.getRoot().getCodeCache().get(clsNode.getFullName()).getCodeStr();
		ClassBreakdown original = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(), originalCode);
		ClassBreakdown changed = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(),
				Util.formatSources(this.codeArea.getText()));

		ClassBreakdown completed = original.mergeImports(changed.getImports())
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
		dispose();
	}

	private void onCompile() {
		final ClassNode clsNode = methodNode.getParentClass();
		try {
			final String originalCode = decompiler.getRoot().getCodeCache().get(clsNode.getFullName()).getCodeStr();
			ClassBreakdown original = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(), originalCode);
			ClassBreakdown changed = ClassBreakdown.breakdown(clsNode.getFullName(), clsNode.getName(),
					Util.formatSources(codeArea.getText()));

			if (ApkSpy.lint(this.decompiler.getArgs().getInputFiles().get(0).toString(), clsNode.getFullName(),
					this.merge(changed, original), options.getAndroidSdkPath(), options.getJdkLocation(), new OutputStream() {
						@Override
						public void write(int b) throws IOException {
							System.out.print((char) b);
							output.append(Character.toString((char) b));
						}
					})) {
				output.append("Successfully compiled!\n");
			} else {
				output.append("Encountered errors while compiling!\n");
			}
		} catch (IOException | InterruptedException ex) {
			LOG.error("Compiling failed: ", ex);
		}
	}

	public void setCodeAreaContent(String determinedContent) {
		codeArea.setText(determinedContent);
	}
}
