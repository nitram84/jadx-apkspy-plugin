package jadx.plugins.apkspy.model;

import java.util.ArrayList;
import java.util.List;

public class JavaMethod {
	private String comments;
	private String method;

	private final List<String> annotations = new ArrayList<>();

	public JavaMethod(final String content) {
		final StringBuilder comments = new StringBuilder();
		final StringBuilder method = new StringBuilder();

		boolean isCommentBlock = false;
		for (final String line : content.split("\n")) {
			if (isCommentBlock || (!line.startsWith(" ") && line.startsWith("/*"))) {
				comments.append(line).append('\n');
				isCommentBlock = !line.contains("*/");
			} else {
				if (line.trim().startsWith("@")) {
					annotations.add(line.trim());
				} else {
					method.append(line).append('\n');
				}
			}
		}

		this.comments = comments.toString().trim();
		this.method = method.toString().trim();
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getHeader() {
		return this.method.substring(0, this.method.indexOf('{') + 1);
	}

	@Override
	public String toString() {
		if (this.comments.isEmpty()) {
			return this.method;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(this.comments).append('\n');
		for (String annotation : annotations) {
			sb.append(annotation).append('\n');
		}
		sb.append(this.method);
		return sb.toString();
	}

	public List<String> getAnnotations() {
		return annotations;
	}
}
