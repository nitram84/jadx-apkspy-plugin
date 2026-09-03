package jadx.plugins.apkspy.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaMethod;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.plugins.apkspy.model.ClassBreakdown;

public final class MethodExtractorUtils {

	private MethodExtractorUtils() {
	}

	private static final Logger log = LoggerFactory.getLogger(MethodExtractorUtils.class);
	private static final Pattern ANON_CLASS_PATTERN = Pattern.compile("new\\s+[A-Za-z0-9_<>]+\\s*\\([^)]*\\)\\s*\\{");

	private static class CodeBlock {
		public final int startLine;
		public int endLine;
		public final int indentLevel;
		public final String header;
		public final boolean isAnonymousClass;
		public final boolean isMethod;

		public CodeBlock(int startLine, int indentLevel, String header, boolean isAnonymousClass, boolean isMethod) {
			this.startLine = startLine;
			this.endLine = startLine;
			this.indentLevel = indentLevel;
			this.header = header;
			this.isAnonymousClass = isAnonymousClass;
			this.isMethod = isMethod;
		}
	}

	public static String extractMethod(String javaSource, int position) {
		if (position < 0 || position > javaSource.length()) {
			log.error("Position ({}) no in source code (length: {}).", position, javaSource.length());
			return javaSource;
		}

		int targetLine = calculateLineFromOffset(javaSource, position);

		String[] lines = javaSource.split("\\r?\\n", -1);

		List<CodeBlock> blocks = parseBlocks(lines);
		CodeBlock targetMethod = findTargetMethod(blocks, targetLine);

		if (targetMethod == null) {
			log.warn("No method found at position {} (line: {})", position, targetLine);
			return javaSource;
		}

		CodeBlock mainOuterStructure = findMainOuterStructure(blocks, targetMethod);

		StringBuilder output = new StringBuilder();
		boolean inBlockComment = false;
		List<String> pendingComments = new ArrayList<>();

		for (int i = 0; i < lines.length; i++) {
			int currentLineNum = i + 1;
			String line = lines[i];
			String trimmed = line.trim();

			if (mainOuterStructure == null || currentLineNum < mainOuterStructure.startLine
					|| currentLineNum > mainOuterStructure.endLine) {
				output.append(line).append("\n");
				continue;
			}

			if (trimmed.startsWith("/*"))
				inBlockComment = true;
			boolean wasInComment = inBlockComment;
			if (trimmed.endsWith("*/"))
				inBlockComment = false;

			if (wasInComment || trimmed.startsWith("//") || trimmed.startsWith("@")) {
				pendingComments.add(line);
				continue;
			}

			if (trimmed.isEmpty() && (currentLineNum < targetMethod.startLine || currentLineNum > targetMethod.endLine)) {
				pendingComments.clear();
				continue;
			}

			if (currentLineNum >= targetMethod.startLine && currentLineNum <= targetMethod.endLine) {
				if (currentLineNum == targetMethod.startLine) {
					output.append("\n");
					for (String comment : pendingComments) {
						output.append(comment).append("\n");
					}
					pendingComments.clear();
				}

				output.append(line).append("\n");
				continue;
			}

			boolean keepSkeletonLine = false;
			for (CodeBlock b : blocks) {
				if (!b.isMethod && !b.isAnonymousClass) {
					if (targetMethod.startLine > b.startLine && targetMethod.endLine < b.endLine) {
						if (currentLineNum == b.startLine || currentLineNum == b.endLine) {
							keepSkeletonLine = true;
							break;
						}
					}
				}
			}

			if (keepSkeletonLine) {
				for (String comment : pendingComments) {
					output.append(comment).append("\n");
				}
				pendingComments.clear();
				output.append(line).append("\n");
			} else {
				pendingComments.clear();
			}
		}

		if (!javaSource.endsWith("\n") && output.length() > 0) {
			output.setLength(output.length() - 1);
		}

		return output.toString();
	}

	private static int calculateLineFromOffset(String source, int offset) {
		int line = 1;
		for (int i = 0; i < offset; i++) {
			if (source.charAt(i) == '\n') {
				line++;
			}
		}
		return line;
	}

	private static List<CodeBlock> parseBlocks(String[] lines) {
		List<CodeBlock> allBlocks = new ArrayList<>();
		Stack<CodeBlock> openBlocks = new Stack<>();
		boolean inBlockComment = false;

		for (int i = 0; i < lines.length; i++) {
			int lineNum = i + 1;
			String line = lines[i];
			String trimmed = line.trim();

			if (trimmed.startsWith("/*"))
				inBlockComment = true;
			boolean wasInComment = inBlockComment;
			if (trimmed.endsWith("*/"))
				inBlockComment = false;

			if (wasInComment || trimmed.startsWith("//") || trimmed.isEmpty()) {
				continue;
			}

			int opens = countOccurrences(line, '{');
			int closes = countOccurrences(line, '}');

			if (opens > 0 && !trimmed.startsWith("@")) {
				int indent = getIndentation(line);
				boolean isAnon = ANON_CLASS_PATTERN.matcher(line).find();

				boolean isMethod = indent >= 4 && !isAnon
						&& !line.contains("class ") && !line.contains("interface ") && !line.contains("enum ");

				CodeBlock newBlock = new CodeBlock(lineNum, indent, line, isAnon, isMethod);
				allBlocks.add(newBlock);
				openBlocks.push(newBlock);
			}

			for (int c = 0; c < closes; c++) {
				if (!openBlocks.isEmpty()) {
					CodeBlock closed = openBlocks.pop();
					closed.endLine = lineNum;
				}
			}
		}
		return allBlocks;
	}

	private static CodeBlock findTargetMethod(List<CodeBlock> blocks, int targetLine) {
		CodeBlock selectedMethod = null;
		CodeBlock containingAnonymous = null;

		for (CodeBlock b : blocks) {
			if (targetLine >= b.startLine && targetLine <= b.endLine) {
				if (b.isMethod) {
					selectedMethod = b;
				} else if (b.isAnonymousClass) {
					containingAnonymous = b;
				}
			}
		}

		if (containingAnonymous != null && selectedMethod != null) {
			if (containingAnonymous.startLine > selectedMethod.startLine && containingAnonymous.endLine < selectedMethod.endLine) {
				log.info("Ignoring method selection in anonymous class, line {}. Using outer method: {}",
						containingAnonymous.startLine, selectedMethod.header.trim());
				return selectedMethod;
			}
		}

		if (selectedMethod != null) {
			return selectedMethod;
		}

		if (containingAnonymous != null) {
			log.info("Position is inside an anonymous class without method context (line {}). Searching outer Scope.",
					containingAnonymous.startLine);
			for (CodeBlock b : blocks) {
				if (b.isMethod && containingAnonymous.startLine > b.startLine && containingAnonymous.endLine < b.endLine) {
					return b;
				}
			}
		}

		return null;
	}

	private static CodeBlock findMainOuterStructure(List<CodeBlock> blocks, CodeBlock targetMethod) {
		CodeBlock outer = null;
		for (CodeBlock b : blocks) {
			if (!b.isMethod && !b.isAnonymousClass) {
				if (targetMethod.startLine > b.startLine && targetMethod.endLine < b.endLine) {
					if (outer == null || b.indentLevel < outer.indentLevel) {
						outer = b;
					}
				}
			}
		}
		return outer;
	}

	private static int getIndentation(String line) {
		int spaceCount = 0;
		for (char c : line.toCharArray()) {
			if (c == ' ')
				spaceCount++;
			else
				break;
		}
		return spaceCount;
	}

	private static int countOccurrences(String text, char target) {
		int count = 0;
		for (char c : text.toCharArray()) {
			if (c == target)
				count++;
		}
		return count;
	}

	/*
	 * Locate methods by signature
	 */
	public static int findMethodPosition(final JavaMethod method, final String code) throws JadxRuntimeException {
		final ClassBreakdown breakdown =
				ClassBreakdown.breakdown(method.getDeclaringClass().getFullName(), code);
		List<String> imports = extractImportedClasses(breakdown.getImports());

		final StringBuilder sb = new StringBuilder("    ");
		final AccessInfo accessFlags = method.getAccessFlags();
		if (accessFlags.isAbstract()) {
			sb.append("abstract ");
		}
		if (accessFlags.isPublic()) {
			sb.append("public ");
		}
		if (accessFlags.isPrivate()) {
			sb.append("private ");
		}
		if (accessFlags.isProtected()) {
			sb.append("protected ");
		}
		if (accessFlags.isSynchronized()) {
			sb.append("synchronized ");
		}
		if (accessFlags.isStatic()) {
			sb.append("static ");
		}
		if (accessFlags.isSynthetic()) {
			sb.append("/* synthetic */ ");
		}
		if (accessFlags.isFinal()) {
			sb.append("final ");
		}
		if (method.isConstructor()) {
			sb.append(method.getDeclaringClass().getName()).append("(");
		} else {
			final ArgType returnType = method.getReturnType();
			sb.append(getLocalArgType(returnType, imports));
			sb.append(' ').append(method.getName()).append("(");
		}
		final String searchPrefix = sb.toString();

		int i = 0;
		while (i != -1) {
			i = code.indexOf(searchPrefix, i);
			if (i != -1) {
				int endOfSignature = code.indexOf(" {", i);
				String signatureLine = code.substring(i, endOfSignature);
				endOfSignature = signatureLine.indexOf(") throws");
				if (endOfSignature > -1) {
					signatureLine = signatureLine.substring(0, endOfSignature);
				}
				signatureLine = signatureLine.replace("...", "[]");
				final List<String> arguments = extractArguments(cleanAnnotations(extractParamString(signatureLine)));
				int idx = 0;
				boolean match = true;
				for (final ArgType arg : method.getArguments()) {
					if (!getLocalArgType(arg, imports).equals(arguments.get(idx))) {
						match = false;
					}
					idx++;
				}
				if (match) {
					return i;
				}
			}
		}
		return -1;
	}

	private static List<String> extractImportedClasses(String imports) {
		List<String> importList = new ArrayList<>();
		try (final Scanner sc = new Scanner(imports)) {
			while (sc.hasNext()) {
				final String imp = sc.nextLine();
				if (imp.startsWith("import ") && imp.endsWith(";")) {
					importList.add(imp.substring(7, imp.length() - 1));
				}
			}
		}
		return importList;
	}

	private static String getLocalArgType(final ArgType type, List<String> imports) {
		String typeStr = type.toString();
		for (final String imp : imports) {
			if (typeStr.contains(imp)) {
				typeStr = typeStr.replace(imp, imp.substring(imp.lastIndexOf('.') + 1));
			}
		}

		return typeStr.replace("java.lang.", "").replace('$', '.');
	}

	private static List<String> extractArguments(final String params) throws JadxRuntimeException {
		if (params == null || params.isEmpty()) {
			return Collections.emptyList();
		}
		final List<String> args = new ArrayList<>();
		int pos = 0;
		int i;
		int hasGenerics;
		while (true) {
			hasGenerics = params.indexOf('<', pos + 1);
			i = params.indexOf(' ', pos + 1);
			if (hasGenerics > -1 && hasGenerics < i) {
				int offset = 0;
				int e = params.indexOf("> ", hasGenerics);
				if (e == -1) {
					e = params.indexOf(">[] ", hasGenerics);
					if (e == -1) {
						throw new JadxRuntimeException("Syntax error");
					} else {
						offset = 3;
					}
				} else {
					offset = 1;
				}
				args.add(params.substring(pos, e + offset));
				pos = e + offset;
			} else {
				args.add(params.substring(pos, i));
				pos = i;
			}
			pos = params.indexOf(", ", pos);
			if (pos == -1) {
				break;
			}
			pos = pos + 2;
		}
		return args;
	}

	private static String extractParamString(String signature) {
		int openBracketIndex = signature.indexOf('(');
		int closeBracketIndex = signature.lastIndexOf(')');
		return signature.substring(openBracketIndex + 1, closeBracketIndex);
	}

	private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@\\w+(?:\\([^)]*\\))?\\s+");

	private static String cleanAnnotations(String parameters) {
		return ANNOTATION_PATTERN.matcher(parameters).replaceAll("");
	}
}
