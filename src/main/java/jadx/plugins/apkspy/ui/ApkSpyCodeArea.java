package jadx.plugins.apkspy.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class ApkSpyCodeArea extends RSyntaxTextArea {
	ApkSpyCodeArea() {
		setMarkOccurrences(false);
		setFadeCurrentLineHighlight(true);
		setAntiAliasingEnabled(true);
	}
}
