package jadx.plugins.apkspy.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.api.JavaClass;
import jadx.api.JavaNode;

public class ChangeCache {

	private static ChangeCache instance = null;
	private final Map<String, ClassBreakdown> CHANGES = new HashMap<>();
	private final Set<String> CLASS_DELETIONS = new HashSet<>();

	public static ChangeCache getInstance() {
		if (instance == null) {
			instance = new ChangeCache();
		}
		return instance;
	}

	public Map<String, ClassBreakdown> getChanges() {
		return CHANGES;
	}

	public void putChange(String className, ClassBreakdown content, JavaMethod method) {
		if (method == null && CHANGES.containsKey(className)) {
			CHANGES.remove(className, CHANGES.get(className));
		}
		if (CHANGES.containsKey(className)) {
			ClassBreakdown original = CHANGES.get(className);

			CHANGES.put(className, original.addOrReplaceMethod(method).mergeImports(content.getImports()));
		} else {
			CHANGES.put(className, content);
		}
	}

	public Set<String> getClassDeletions() {
		return CLASS_DELETIONS;
	}

	public boolean hasChanges() {
		return !CHANGES.isEmpty() || !CLASS_DELETIONS.isEmpty();
	}

	public void deleteClass(JavaClass cls) {

		for (JavaClass nesetd : cls.getInnerClasses()) {
			deleteClass(nesetd);
		}

		// Handle nested classes
		List<String> classNameParts = new ArrayList<>();
		JavaNode c = cls;
		classNameParts.add(c.getName());
		while (c.getDeclaringClass() != null) {
			c = c.getDeclaringClass();
			if (c != null) {
				classNameParts.add(c.getName());
			}
		}

		Collections.reverse(classNameParts);
		String smaliClass = String.join("$", classNameParts);

		String fullName = cls.getFullName();
		String smaliName = fullName.substring(0, fullName.length() - smaliClass.length()) + smaliClass;
		CHANGES.remove(fullName);
		CLASS_DELETIONS.add(smaliName.replace('.', File.separatorChar) + ".smali");
	}
}
