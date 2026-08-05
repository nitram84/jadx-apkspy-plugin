package jadx.plugins.apkspy.rename.jar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.commons.Remapper;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;

public class JadxASMRenamer extends Remapper {
	private final JadxDecompiler decompiler;
	private final Map<String, JavaClass> classMap = new HashMap<>();

	private final Set<String> classIgnoreList = new HashSet<>();

	public JadxASMRenamer(final int api, final JadxDecompiler decompiler) {
		super(api);
		this.decompiler = decompiler;
	}

	/**
	 * Prepopulate class map (faster than searching class), but not all classes are available by class
	 * traversal (jadx bug?)
	 */
	public void prepopulateNameCache() {
		for (final JavaClass javaClass : decompiler.getClasses()) {
			addInnerClassesToNameCache(javaClass);
			classMap.put(javaClass.getRawName().replace('.', '/'), javaClass);
		}
	}

	private void addInnerClassesToNameCache(final JavaClass javaClass) {
		if (!javaClass.getInnerClasses().isEmpty()) {
			for (final JavaClass inner : javaClass.getInnerClasses()) {
				final int length = inner.getTopParentClass().getRawName().length();
				classMap.put(inner.getRawName().substring(0, length).replace('.', '/')
						+ inner.getRawName().substring(length).replace('.', '$'), inner);
				addInnerClassesToNameCache(inner);
			}
		}
	}

	@Override
	public String map(final String internalName) {
		if (classIgnoreList.contains(internalName)) {
			return internalName;
		}

		final String name = internalName.replace('/', '.').replace('$', '.');

		JavaClass jadxClass = classMap.get(internalName);
		if (jadxClass == null) {
			jadxClass = decompiler.searchJavaClassByOrigFullName(name);
			if (jadxClass != null) {
				classMap.put(internalName, jadxClass);
			} else {
				classIgnoreList.add(internalName);
				return internalName;
			}
		}

		if (name.equals(jadxClass.getFullName())) {
			return internalName;
		} else {
			if (jadxClass.isInner()) {
				final int length = jadxClass.getTopParentClass().getFullName().length();
				return jadxClass.getFullName().substring(0, length).replace('.', '/')
						+ jadxClass.getFullName().substring(length).replace('.', '$');
			} else {
				return jadxClass.getFullName().replace('.', '/');
			}
		}
	}

	@Override
	public String mapMethodName(final String owner, final String name, final String descriptor) {
		if (classIgnoreList.contains(owner)) {
			return name;
		}

		JavaClass jadxClass = classMap.get(owner);
		if (jadxClass == null) {
			jadxClass =
					decompiler.searchJavaClassByOrigFullName(owner.replace('/', '.').replace('$', '.'));
			if (jadxClass != null) {
				classMap.put(owner, jadxClass);
			} else {
				classIgnoreList.add(owner);
				return name;
			}
		}

		final String methodRawId = owner.replace('/', '.') + "." + name + descriptor;
		for (final JavaMethod method : jadxClass.getMethods()) {
			if (method.getMethodNode().getMethodInfo().getRawFullId().equals(methodRawId)) {
				return method.getName();
			}
		}
		return name;
	}

	@Override
	public String mapFieldName(final String owner, final String name, final String descriptor) {
		if (classIgnoreList.contains(owner)) {
			return name;
		}

		JavaClass jadxClass = classMap.get(owner);
		if (jadxClass == null) {
			jadxClass =
					decompiler.searchJavaClassByOrigFullName(owner.replace('/', '.').replace('$', '.'));
			if (jadxClass != null) {
				classMap.put(owner, jadxClass);
			} else {
				classIgnoreList.add(owner);
				return name;
			}
		}

		final String rawId = owner.replace('/', '.') + "." + name + ":" + descriptor;
		for (final JavaField field : jadxClass.getFields()) {
			if (rawId.equals(field.getFieldNode().getFieldInfo().getRawFullId())) {
				return field.getName();
			}
		}
		return name;
	}
}
