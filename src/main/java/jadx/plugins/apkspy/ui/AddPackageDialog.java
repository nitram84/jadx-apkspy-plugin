package jadx.plugins.apkspy.ui;

import java.awt.*;
import java.util.Collections;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import jadx.api.JadxDecompiler;
import jadx.api.JavaPackage;
import jadx.api.PackageUtil;
import jadx.core.dex.nodes.RootNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JSources;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.CommonDialog;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.pkgs.JRenamePackageWrapper;

public class AddPackageDialog extends CommonDialog {
	private static final long serialVersionUID = -3269715644416902410L;

	private final transient JNode node;
	private transient JTextField newPackageField;

	private final RootNode rootNode;

	private final JadxDecompiler decompiler;

	public AddPackageDialog(JFrame mainWindow, JNode node, JadxDecompiler decompiler) {
		super((MainWindow) mainWindow);
		this.node = node;
		this.decompiler = decompiler;
		this.rootNode = decompiler.getRoot();
		initUI();
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());

		JButton addPackageBtn = new JButton(NLS.str("common_dialog.ok"));
		addPackageBtn.addActionListener(event -> addPackage());
		getRootPane().setDefaultButton(addPackageBtn);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(addPackageBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	private void addPackage() {
		String packageName = this.newPackageField.getText();
		if ((this.node instanceof JSources || this.node instanceof JPackage) && JRenamePackageWrapper.isValidPackageName(packageName)) {
			for (JavaPackage p : decompiler.getPackages()) {
				if (p.getFullName().equals(packageName) || p.getFullName().startsWith(packageName + ".")) {
					dispose();
					return;
				}
			}
			String fullPackgeName = packageName;
			if (this.node instanceof JPackage) {
				fullPackgeName = ((JPackage) this.node).getPkg().getFullName() + "." + packageName;
			}
			JPackage newPackage = new JPackage(PackageUtil.javaPackageBuilder(rootNode, fullPackgeName), true, Collections.emptyList(),
					Collections.emptyList(), false);
			newPackage.setName(packageName);
			if (this.node instanceof JSources) {
				this.node.add(newPackage);
				((JSources) this.node).update();
			} else {
				((JPackage) this.node).getSubPackages().add(newPackage);
				((JPackage) this.node).update();
			}
			mainWindow.reloadTree();
			dispose();
		}
	}

	private void initUI() {
		JLabel lbl = new JLabel("Add package");
		newPackageField = new JTextField(40);
		newPackageField.setFont(mainWindow.getSettings().getFont());
		new TextStandardActions(newPackageField);

		JPanel renamePane = new JPanel();
		renamePane.setLayout(new FlowLayout(FlowLayout.LEFT));
		renamePane.add(lbl);
		renamePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

		JPanel textPane = new JPanel();
		textPane.setLayout(new BoxLayout(textPane, BoxLayout.PAGE_AXIS));
		textPane.add(newPackageField);
		textPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(renamePane, BorderLayout.PAGE_START);
		contentPane.add(textPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		setTitle("App package");
		commonWindowInit();
	}
}
