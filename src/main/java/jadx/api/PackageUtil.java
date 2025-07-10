package jadx.api;

import java.util.Collections;

import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class PackageUtil {
	/**
	 * Wrapper to access package private constructor
	 *
	 * @param rootNode
	 * @param fullPackageName
	 * @return
	 */
	public static JavaPackage javaPackageBuilder(RootNode rootNode, String fullPackageName) {
		PackageNode packageNode = PackageNode.getOrBuild(rootNode, fullPackageName);
		return new JavaPackage(packageNode, Collections.emptyList(), Collections.emptyList());
	}
}
