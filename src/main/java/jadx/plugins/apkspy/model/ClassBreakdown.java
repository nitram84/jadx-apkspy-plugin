package jadx.plugins.apkspy.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import jadx.plugins.apkspy.model.DiffMatchPatch.Diff;

public class ClassBreakdown implements Cloneable {
	public static final int BLOCK_STATIC = 4;
	private String className;
	private String simpleName;
	private String imports;
	private String classDeclaration;
	private String memberVariables;
	private List<JavaMethod> changedMethods;
	private List<JavaMethod> methods;
	private List<ClassBreakdown> innerClasses;

	/**
	 * @param className  full qualified class name
	 * @param simpleName
	 * @param content    source code formatted with 4 space indentation
	 * @return
	 */
	public static ClassBreakdown breakdown(String className, String simpleName, String content) {
		String[] split = content.split("\n");

		String imports = "";
		String classDeclaration = "";
		String memberVariables = "";
		List<JavaMethod> methods = new ArrayList<>();
		List<ClassBreakdown> innerClasses = new ArrayList<>();
		String currentBlock = "";
		int blockType = 0;
		boolean allowRoot = true;
		for (String line : split) {
			if (allowRoot) {
				if (!line.startsWith(" ")) {
					if (line.contains("class ") || line.contains("interface") || line.contains("enum ")
							|| line.contains("@interface ")) {
						classDeclaration = line.substring(0, line.indexOf("{")).trim();
						if (simpleName == null) {
							Matcher m = Pattern.compile(".*(class|interface|enum|@interface) (.+?) .*").matcher(line);
							if (m.find()) {
								simpleName = m.group(2);
							}
						}
						allowRoot = false;
					} else {
						imports += line.trim() + "\n";
					}
				}
			} else {
				if (line.startsWith("    ") && !line.startsWith("     ")) {
					if (line.trim().equals("}")) {
						if (blockType == 1) {
							methods.add(new JavaMethod(currentBlock.trim() + "\n}"));
						} else if (blockType == 2) {
							innerClasses.add(ClassBreakdown.breakdown(null, null, currentBlock.trim() + "\n}"));
						}
						currentBlock = "";
						blockType = 0;
					} else if (line.trim().equals("static {")) {
						currentBlock = "";
						blockType = BLOCK_STATIC;
					} else if (line.trim().equals("};") && blockType == 3) {
						memberVariables += currentBlock + "};\n";
						currentBlock = "";
						blockType = 0;
					} else if (line.trim().endsWith(";")) {
						memberVariables += line.trim() + "\n";
					} else {
						if (line.contains("new ")) {
							blockType = 3;
						} else if (line.contains("class ")) {
							blockType = 2;
						} else {
							blockType = 1;
						}
						currentBlock += StringUtils.stripEnd(line.substring(4), "\r\n ") + "\n";
					}
				} else if (line.startsWith("     ")) {
					currentBlock += StringUtils.stripEnd(line.substring(4), "\r\n ") + "\n";
				}
			}
		}

		if (!currentBlock.isEmpty()) {
			if (blockType == 1) {
				methods.add(new JavaMethod(currentBlock));
			} else if (blockType == 2) {
				innerClasses.add(ClassBreakdown.breakdown(null, null, currentBlock.trim() + "\n}"));
			} else if (blockType == 3) {
				memberVariables += currentBlock + "};\n";
			}
		}

		return new ClassBreakdown(imports, classDeclaration, className, simpleName, memberVariables, methods,
				innerClasses);
	}

	public ClassBreakdown(String imports, String classDeclaration, String className, String simpleName,
			String memberVariables, List<JavaMethod> methods, List<ClassBreakdown> innerClasses) {
		this.imports = imports;
		this.classDeclaration = classDeclaration;
		this.className = className;
		this.simpleName = simpleName;
		this.memberVariables = memberVariables;
		this.methods = methods;
		this.changedMethods = methods;
		this.innerClasses = innerClasses;
	}

	public ClassBreakdown(ClassBreakdown old) {
		this.imports = old.imports;
		this.classDeclaration = old.classDeclaration;
		this.className = old.className;
		this.simpleName = old.simpleName;
		this.memberVariables = old.memberVariables;
		this.methods = new ArrayList<>(old.methods);
		this.changedMethods = new ArrayList<>(old.changedMethods);
		this.innerClasses = new ArrayList<>(old.innerClasses);
	}

	public String getImports() {
		return imports;
	}

	public void setImports(String imports) {
		this.imports = imports;
	}

	public String getClassDeclaration() {
		return classDeclaration;
	}

	public void setClassDeclaration(String classDeclaration) {
		this.classDeclaration = classDeclaration;
	}

	public String getMemberVariables() {
		return memberVariables;
	}

	public void setMemberVariables(String memberVariables) {
		this.memberVariables = memberVariables;
	}

	public List<JavaMethod> getMethods() {
		return methods;
	}

	public void setMethods(List<JavaMethod> methods) {
		this.methods = methods;
	}

	public String getFullName() {
		return className;
	}

	public void setFullName(String className) {
		this.className = className;
	}

	public List<JavaMethod> getChangedMethods() {
		return changedMethods;
	}

	public void setChangedMethods(List<JavaMethod> methods) {
		this.changedMethods = methods;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	public ClassBreakdown addOrReplaceMethod(JavaMethod newMethod) {
		ClassBreakdown clone = new ClassBreakdown(this);

		String header = newMethod.getHeader();
		for (int i = 0; i < methods.size(); i++) {
			String otherHeader = methods.get(i).getHeader();
			if (header.equals(otherHeader)) {
				clone.methods.set(i, newMethod);
				return clone;
			}
		}

		clone.methods.add(newMethod);
		return clone;
	}

	public ClassBreakdown mergeImports(String imports) {
		DiffMatchPatch dmp = new DiffMatchPatch();
		List<Diff> diffs = dmp.diffMain(this.imports, imports);

		ClassBreakdown clone = new ClassBreakdown(this);
		clone.imports = dmp.diffText2(diffs);
		return clone;
	}

	public ClassBreakdown mergeMemberVariables(String memberVariables) {
		DiffMatchPatch dmp = new DiffMatchPatch();
		List<Diff> diffs = dmp.diffMain(this.memberVariables, memberVariables);

		ClassBreakdown clone = new ClassBreakdown(this);
		clone.memberVariables = dmp.diffText2(diffs);
		return clone;
	}

	private JavaMethod toStub(JavaMethod method) {
		String header = method.getHeader();
		String containing = header.substring(0, header.indexOf('('));

		String stub = header + "\n";
		if (containing.contains("byte ") || containing.contains("short ") || containing.contains("int ")
				|| containing.contains("long ")) {
			stub += "    return 0;\n";
		} else if (containing.contains("float ")) {
			stub += "    return 0.0f;\n";
		} else if (containing.contains("double ")) {
			stub += "    return 0.0;\n";
		} else if (containing.contains("char ")) {
			stub += "    return ' ';\n";
		} else if (containing.contains("boolean ")) {
			stub += "    return false;\n";
		} else if (containing.contains("void ") || containing.contains(this.simpleName)) {
			stub += "    return;\n";
		} else {
			stub += "    return null;\n";
		}
		stub += "}";

		return new JavaMethod(stub);
	}

	public ClassBreakdown mergeMethodStubs(List<JavaMethod> methods) {
		ClassBreakdown clone = new ClassBreakdown(this);
		outer: for (JavaMethod newMethod : methods) {
			String header = newMethod.getHeader();
			for (int i = 0; i < this.methods.size(); i++) {
				String otherHeader = this.methods.get(i).getHeader();
				if (header.equals(otherHeader)) {
					continue outer;
				}
			}

			clone.methods.add(this.toStub(newMethod));
		}

		return clone;
	}

	public ClassBreakdown mergeMethods(List<JavaMethod> methods) {
		ClassBreakdown breakdown = new ClassBreakdown(this);
		for (JavaMethod method : methods) {
			breakdown = breakdown.addOrReplaceMethod(method);
		}
		return breakdown;
	}

	public ClassBreakdown mergeInnerClassStubs(ClassBreakdown original) {
		ClassBreakdown breakdown = new ClassBreakdown(this);
		breakdown.innerClasses = original.innerClasses.stream().map(cls -> cls.asStub()).collect(Collectors.toList());
		return breakdown;
	}

	public List<ClassBreakdown> getInnerClasses() {
		return innerClasses;
	}

	public void setInnerClasses(List<ClassBreakdown> innerClasses) {
		this.innerClasses = innerClasses;
	}

	public ClassBreakdown asStub() {
		ClassBreakdown breakdown = new ClassBreakdown(this);
		breakdown.methods = this.methods.stream().map(this::toStub).collect(Collectors.toList());
		return breakdown;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(this.imports);

		str.append((this.classDeclaration + " {\n").replaceAll("(.*?)(class|interface|enum|@interface) (.+?) (.+)",
				"$1$2 " + this.simpleName + " $4"));
		if (this.memberVariables.length() > 0) {
			for (String member : this.memberVariables.split("\n")) {
				str.append("    " + member + "\n");
			}
			str.append("\n");
		}
		if (this.innerClasses.size() > 0) {
			for (ClassBreakdown innerClass : this.innerClasses) {
				String toStr = innerClass.toString();
				for (String split : toStr.split("\n")) {
					str.append("    " + split + "\n");
				}
				str.append("\n");
			}
		}
		if (this.methods.size() > 0) {
			for (JavaMethod method : this.methods) {
				for (String split : method.toString().split("\n")) {
					str.append("    " + split + "\n");
				}
				str.append("\n");
			}
		}
		return str.toString().substring(0, str.length() - 1) + "}";
	}
}
