package jadx.plugins.apkspy.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

public class SmaliBreakdown {
	private String className;
	private List<SmaliMethod> methods;

	public static SmaliBreakdown breakdown(String content) {
		String className = "";
		SmaliMethod method = null;
		String methodContent = "";
		boolean isReadingMethod = false;
		List<SmaliMethod> methods = new ArrayList<>();

		int i = 0;
		for (String line : content.split("\n")) {
			if (line.startsWith(".class")) {
				line = line.trim();
				className = line.substring(line.indexOf('L') + 1, line.length() - 1).replace('/', '.');
			} else if (line.startsWith(".method")) {
				isReadingMethod = true;
				method = new SmaliMethod(i, -1, null);
				methodContent += StringUtils.stripEnd(line, " \r\n") + "\n";
			} else if (isReadingMethod) {
				methodContent += StringUtils.stripEnd(line, " \r\n") + "\n";

				if (line.startsWith(".end method")) {
					method.setContent(methodContent);
					method.setEnd(i + line.length());
					methods.add(method);

					isReadingMethod = false;
					methodContent = "";
					method = null;
				}
			}
			i += line.length() + 1;
		}

		return new SmaliBreakdown(className, methods);
	}

	public SmaliBreakdown(String className, List<SmaliMethod> methods) {
		this.className = className;
		this.methods = methods;
	}

	private String getSimpleName(Type type) {
		String name = type.getClassName();
		if (name.contains(".")) {
			return name.substring(name.lastIndexOf('.') + 1);
		}
		return name;
	}

	public List<SmaliMethod> getChangedMethods(ClassBreakdown fromClass) {
		List<SmaliMethod> smalis = new ArrayList<>();

		for (JavaMethod javaMethod : fromClass.getChangedMethods()) {
			smali: for (SmaliMethod smaliMethod : this.methods) {
				String methodDeclaration = smaliMethod.getContent().split("\n")[0];
				methodDeclaration = methodDeclaration.substring(methodDeclaration.lastIndexOf(' '));

				String name = methodDeclaration.substring(0, methodDeclaration.indexOf('(')).trim();
				String descriptor = methodDeclaration.substring(methodDeclaration.indexOf('(')).trim();

				Type[] types = Type.getArgumentTypes(descriptor);
				Type returnType = Type.getReturnType(descriptor);

				String javaDeclaration = javaMethod.getHeader();
				String beforeArguments = javaDeclaration.substring(0, javaDeclaration.indexOf('(')).trim();
				String javaName = beforeArguments.substring(beforeArguments.lastIndexOf(' ') + 1);
				int x = beforeArguments.lastIndexOf(' ', beforeArguments.lastIndexOf(' ') - 1);
				String javaReturnType = null;
				if (x == -1) {
					javaReturnType = beforeArguments.substring(0, beforeArguments.lastIndexOf(' '));
				} else {
					javaReturnType = beforeArguments.substring(x, beforeArguments.lastIndexOf(' '));
				}
				if (javaName.equals(fromClass.getSimpleName())) {
					javaName = "<init>";
					javaReturnType = null;
				} else {
					javaReturnType = javaReturnType.trim();
				}

				List<String> javaArgumentsTypes = Arrays.stream(javaDeclaration
						.substring(javaDeclaration.indexOf('(') + 1, javaDeclaration.lastIndexOf(')')).split(","))
						.map(param -> param.trim().split(" ", 2)[0]).collect(Collectors.toList());
				if (javaArgumentsTypes.size() == 1 && javaArgumentsTypes.get(0).isEmpty()) {
					// empty arguments
					javaArgumentsTypes = new ArrayList<String>();
				}

				String simpleReturnType = getSimpleName(returnType);

				if (javaName.equals(name) && (javaReturnType == null || javaReturnType.equals(simpleReturnType))
						&& types.length == javaArgumentsTypes.size()) {
					for (int i = 0; i < types.length; i++) {
						if (!javaArgumentsTypes.get(i).equals(getSimpleName(types[i]))) {
							continue smali;
						}
					}
				} else {
					continue;
				}

				smalis.add(smaliMethod);
				break;
			}
		}

		return smalis;
	}

	public List<SmaliMethod> getMethods() {
		return methods;
	}

	public void setMethods(List<SmaliMethod> methods) {
		this.methods = methods;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public SmaliMethod getEquivalentMethod(SmaliMethod method) {
		for (SmaliMethod subMethod : this.methods) {
			if (subMethod.getContent().split("\n")[0].trim().equals(method.getContent().split("\n")[0].trim())) {
				return subMethod;
			}
		}
		return null;
	}
}
