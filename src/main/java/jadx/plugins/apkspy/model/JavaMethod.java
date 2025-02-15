package jadx.plugins.apkspy.model;

public class JavaMethod {
	private String comments;
	private String method;

	public JavaMethod(final String content) {
		final StringBuilder comments = new StringBuilder();
		final StringBuilder method = new StringBuilder();

		boolean isCommentBlock = false;
		for (final String line : content.split("\n")) {
			if (isCommentBlock || (!line.startsWith(" ") && line.startsWith("/*"))) {
				comments.append(line).append('\n');
				isCommentBlock = !line.contains("*/");
			} else {
				method.append(line).append('\n');
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
		return this.comments + "\n" + this.method;
	}
}
