package jadx.plugins.apkspy.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPluginContext;
import jadx.plugins.apkspy.ApkSpy;
import jadx.plugins.apkspy.ApkSpyOptions;
import jadx.plugins.apkspy.model.ChangeCache;
import jadx.plugins.apkspy.utils.Util;

public class ApkSpySaver extends JDialog {

	private static final Logger LOG = LoggerFactory.getLogger(ApkSpySaver.class);

	public ApkSpySaver(JFrame mainWindow, JadxPluginContext pluginContext, final ApkSpyOptions options) {
		super(SwingUtilities.windowForComponent(mainWindow));

		setLayout(new BorderLayout());
		JPanel panel = new JPanel();

		JTextArea output = new JTextArea();
		output.setEditable(false);
		output.setFocusable(true);

		JCheckBox keepOnErrors = new JCheckBox("Keep intermediate results on errors", true);
		JCheckBox cleanOnSuccess = new JCheckBox("Clean intermediate results on success", true);

		final JTextField saveLocation = new JTextField(30);
		final String inputApkFilename = pluginContext.getDecompiler().getArgs().getInputFiles().get(0).toString();
		saveLocation.setText(inputApkFilename.replace(".apk", "_apkspy.apk"));

		final JButton browse = new JButton("Browse...");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Select Save Location");

				FileNameExtensionFilter filter = new FileNameExtensionFilter("Android Package (*.apk)", "apk");
				fileChooser.setFileFilter(filter);
				fileChooser.setAcceptAllFileFilterUsed(false);

				File currentFile = new File(saveLocation.getText());
				if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
					fileChooser.setCurrentDirectory(currentFile.getParentFile());
				}
				fileChooser.setSelectedFile(new File(currentFile.getName()));

				int userSelection = fileChooser.showSaveDialog(ApkSpySaver.this);
				if (userSelection == JFileChooser.APPROVE_OPTION) {
					File fileToSave = fileChooser.getSelectedFile();
					String filePath = fileToSave.getAbsolutePath();

					if (!filePath.toLowerCase().endsWith(".apk")) {
						filePath += ".apk";
					}

					saveLocation.setText(filePath);
				}
			}
		});

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		final JButton generate = new JButton("Save");
		generate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String targetPath = saveLocation.getText().trim();

				if (targetPath.isEmpty()) {
					JOptionPane.showMessageDialog(ApkSpySaver.this, "Please specify the location for generated apk.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				File fileToSave = new File(targetPath);
				if (fileToSave.exists()) {
					int confirm = JOptionPane.showConfirmDialog(
							ApkSpySaver.this,
							"File already exists. Do you want to overwrite existing file?",
							"Overwrite file",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE);
					if (confirm == JOptionPane.NO_OPTION) {
						return;
					}
				}

				try {
					if (!Util.isValidSdkPath(Paths.get(options.getAndroidSdkPath()))) {
						JOptionPane.showMessageDialog(mainWindow,
								"Please set a valid Android SDK path in the apkSpy -> Preferences menu.", "apkSpy",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				} catch (IOException ex) {
					LOG.error("Android SDK check failed: ", ex);
				}

				if (!ChangeCache.getInstance().hasChanges()) {
					JOptionPane.showMessageDialog(mainWindow,
							"No changes have been made!", "apkSpy", JOptionPane.ERROR_MESSAGE);
					return;
				}

				cancel.setEnabled(false);
				generate.setEnabled(false);

				output.setText("");

				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							boolean success = ApkSpy.merge(inputApkFilename,
									saveLocation.getText(), pluginContext.files().getPluginTempDir(), options.getAndroidSdkPath(),
									options.getJdkLocation(),
									"jadx", new OutputStream() {
										@Override
										public void write(int b) {
											System.out.print((char) b);
											output.append(Character.toString((char) b));
										}
									}, keepOnErrors.isSelected(), cleanOnSuccess.isSelected());
							if (success) {
								JOptionPane.showMessageDialog(mainWindow,
										"Successfully created APK!", "apkSpy", JOptionPane.INFORMATION_MESSAGE);
							}
							cancel.setEnabled(true);
							generate.setEnabled(true);
						} catch (IOException | InterruptedException e) {
							LOG.error("Saving APK failed: ", e);
						}
					}
				});
				thread.start();
			}
		});

		DefaultCaret caret = (DefaultCaret) output.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane scroll = new JScrollPane(output);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setPreferredSize(new Dimension(800, 600));

		panel.add(scroll);

		JPanel buttons = new JPanel();
		buttons.add(new JLabel("Save As: "));
		buttons.add(saveLocation);
		buttons.add(browse);
		buttons.add(generate);
		buttons.add(cancel);

		JPanel optionsPanel = new JPanel(new GridLayout(2, 1, 0, 4));
		optionsPanel.setBorder(new EmptyBorder(0, 10, 10, 10)); // Abstand zu den Rändern
		optionsPanel.add(keepOnErrors);
		optionsPanel.add(cleanOnSuccess);

		JPanel topContainer = new JPanel(new BorderLayout());
		topContainer.add(buttons, BorderLayout.NORTH);
		topContainer.add(optionsPanel, BorderLayout.SOUTH);

		add(topContainer, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		output.setFont(new Font("Courier New", Font.PLAIN, output.getFont().getSize()));

		setTitle("Save APK");
		pack();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModal(true);
		setLocationRelativeTo(null);

		generate.requestFocus();
	}

}
