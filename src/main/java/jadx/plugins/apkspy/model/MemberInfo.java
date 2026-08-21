package jadx.plugins.apkspy.model;

public class MemberInfo {
	private final MemberType type;
	private final String typeName;
	private final String name;

	public MemberInfo(MemberType type, String typeName, String name) {
		this.type = type;
		this.typeName = typeName;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public MemberType getType() {
		return type;
	}
}
