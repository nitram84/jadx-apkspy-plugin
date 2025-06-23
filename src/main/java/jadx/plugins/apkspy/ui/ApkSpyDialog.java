package jadx.plugins.apkspy.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;

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

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;

import jadx.api.JadxDecompiler;
import jadx.plugins.apkspy.ApkSpy;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.model.ClassBreakdown;

public abstract class ApkSpyDialog extends JDialog {

	private static final Logger LOG = LoggerFactory.getLogger(ApkSpyDialog.class);

	protected final transient ApkSpyCodeArea codeArea;

	protected final transient JTextArea output;

	protected final JFrame mainWindow;

	protected final JadxDecompiler decompiler;

	protected final ApkSpyOptions options;

	public ApkSpyDialog(final JFrame mainWindow, final ApkSpyOptions options, final JadxDecompiler decompiler, final String title) {
		super(SwingUtilities.windowForComponent(mainWindow));

		this.options = options;
		this.mainWindow = mainWindow;
		this.decompiler = decompiler;

		final JPanel content = new JPanel();

		this.codeArea = new ApkSpyCodeArea();
		final RTextScrollPane codeScrollPane = new RTextScrollPane(codeArea);

		codeScrollPane.setPreferredSize(new Dimension(800, 600));
		content.add(codeScrollPane);
		this.output = new JTextArea();
		this.output.setFont(new Font("Courier New", Font.PLAIN, this.output.getFont().getSize()));
		this.output.setEditable(false);

		final JScrollPane scroll2 = new JScrollPane(output);
		final Dimension size = codeScrollPane.getPreferredSize();

		scroll2.setPreferredSize(size);
		content.add(scroll2);

		final JButton save = new JButton("Save");
		save.addActionListener(e -> doSave());
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		final JButton compile = new JButton("Compile");
		compile.addActionListener(e -> {
			output.setText("");

			final Thread thread = new Thread(this::onCompile);
			thread.start();
		});

		final JPanel buttons = new JPanel();
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

	private void onCompile() {
		try {
			final ClassBreakdown breakdown = ApkSpyDialog.this.onPrepareCompile();
			try {
				if (ApkSpy.lint(this.decompiler.getArgs().getInputFiles().get(0).toString(), breakdown.getFullName(),
						breakdown, options.getAndroidSdkPath(), options.getJdkLocation(), new OutputStream() {
							@Override
							public void write(final int b) {
								System.out.print((char) b);
								output.append(Character.toString((char) b));
							}
						})) {
					output.append("Successfully compiled!\n");
				} else {
					output.append("Encountered errors while compiling!\n");
				}
			} catch (final IOException | InterruptedException ex) {
				LOG.error("Compiling failed: ", ex);
			}

		} catch (final ParseProblemException ex) {
			logSyntaxError(ex);
		}
	}

	private void doSave() {
		try {
			onSave();
			dispose();
		} catch (final ParseProblemException ex) {
			logSyntaxError(ex);
		}
	}

	protected abstract ClassBreakdown onPrepareCompile();

	protected abstract void onSave();

	private void logSyntaxError(final ParseProblemException e) {
		output.append("Syntax error: ");
		for (final Problem problem : e.getProblems()) {
			output.append(problem.getVerboseMessage());
		}
		output.append("\n");
	}

	public void setCodeAreaContent(String determinedContent) {
		codeArea.setText(determinedContent);
	}
}
