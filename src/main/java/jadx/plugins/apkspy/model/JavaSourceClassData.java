package jadx.plugins.apkspy.model;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.ISeqConsumer;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public class JavaSourceClassData implements IClassData {

	private final String packageName;

	private final String className;

	public JavaSourceClassData(final String packageName, final String className) {
		this.packageName = packageName;
		this.className = className;
	}

	@Override
	public IClassData copy() {
		return new JavaSourceClassData(packageName, className);
	}

	@Override
	public String getInputFileName() {
		return null;
	}

	@Override
	public String getType() {
		return "L" + packageName.replace('.', '/') + "/" + className + ";";
	}

	@Override
	public int getAccessFlags() {
		return AccessFlags.PUBLIC;
	}

	@Override
	public @Nullable String getSuperType() {
		return "Ljava/lang/Object;";
	}

	@Override
	public List<String> getInterfacesTypes() {
		return null;
	}

	@Override
	public void visitFieldsAndMethods(ISeqConsumer<IFieldData> fieldsConsumer, ISeqConsumer<IMethodData> mthConsumer) {
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getDisassembledCode() {
		return null;
	}
}
