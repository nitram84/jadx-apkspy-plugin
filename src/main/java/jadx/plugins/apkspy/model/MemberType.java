package jadx.plugins.apkspy.model;

public enum MemberType {
	BYTE("0"), SHORT("0"), INT("0"), LONG("0"), FLOAT("0.0"), DOUBLE("0.0"), CHAR("0"), BOOLEAN("false"), OBJECT("null");

	MemberType(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	private String defaultValue;

	public static MemberType fromString(String typeStr) {
		switch (typeStr) {
			case "byte":
				return BYTE;
			case "short":
				return SHORT;
			case "int":
				return INT;
			case "long":
				return LONG;
			case "float":
				return FLOAT;
			case "double":
				return DOUBLE;
			case "char":
				return CHAR;
			case "boolean":
				return BOOLEAN;
			default:
				return OBJECT;
		}
	}
}
