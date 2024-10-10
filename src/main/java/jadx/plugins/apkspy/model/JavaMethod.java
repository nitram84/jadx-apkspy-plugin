package jadx.plugins.apkspy.model;

public class JavaMethod {
	private String comments;
	private String method;

	public JavaMethod(String content) {
		String comments = "";
		String method = "";

		for (String line : content.split("\n")) {
			if (!line.startsWith(" ") && line.startsWith("/*")) {
				comments += line + "\n";
			} else {
				method += line + "\n";
			}
		}

		this.comments = comments.trim();
		this.method = method.trim();
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
		return this.method.split("\n")[0].trim();
	}

	@Override
	public String toString() {
		if (this.comments.isEmpty()) {
			return this.method;
		}
		return this.comments + "\n" + this.method;
	}
}
