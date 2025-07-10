package jadx.plugins.apkspy.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeCache {
	private static final Map<String, ClassBreakdown> CHANGES = new HashMap<>();
	private static final List<String> CLASS_DELETIONS = new ArrayList<>();

	public static Map<String, ClassBreakdown> getChanges() {
		return CHANGES;
	}

	public static void putChange(String className, ClassBreakdown content, JavaMethod method) {
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

	public static List<String> getClassDeletions() {
		return CLASS_DELETIONS;
	}
}
