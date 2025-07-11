package jadx.plugins.apkspy.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import jadx.api.JavaMethod;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.plugins.apkspy.model.ClassBreakdown;

public final class MethodExtractorUtils {

	private MethodExtractorUtils() {
	}

	public static String extractMethod(final String text, final int offset) {
		final String[] lines = text.split(System.getProperty("line.separator"));
		final StringBuilder extraction = new StringBuilder();

		int linePos = 0;
		for (final String line : lines) {
			final int start = linePos;

			linePos += line.length();
			linePos += System.getProperty("line.separator").length();

			String str = line.trim();
			if (str.isEmpty()) {
				continue;
			}
			if (!line.startsWith("    ")) {
				if (str.startsWith("package ")) {
					str += "\n";
				} else if (str.contains("class ")) {
					str = "\n" + str;
				}

				extraction.append(str).append('\n');
			}

			if (line.startsWith("    ") && !line.startsWith("     ") && str.endsWith("{")) {
				final int closing = Util.findClosingBracket(text, start + line.lastIndexOf('{'));
				if (offset > start && offset < closing) {
					final String method = text.substring(start, closing);
					extraction.append(method);
					extraction.append("}\n}\n");
					return extraction.toString();
				}
			}
		}

		return null;
	}

	/*
	 * Locate methods by signature
	 */
	public static int findMethodPosition(final JavaMethod method, final String code) throws JadxRuntimeException {
		final ClassBreakdown breakdown =
				ClassBreakdown.breakdown(method.getDeclaringClass().getFullName(), method.getDeclaringClass().getName(), code);
		List<String> imports = extractImportedClasses(breakdown.getImports());

		final StringBuilder sb = new StringBuilder();
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
				String signatureLine = code.substring(i + searchPrefix.length(), endOfSignature);
				endOfSignature = signatureLine.indexOf(") throws");
				if (endOfSignature > -1) {
					signatureLine = signatureLine.substring(0, endOfSignature);
				}
				signatureLine = signatureLine.replace("...", "[]");
				final List<String> arguments = extractArguments(cleanAnnotations(signatureLine));
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

	private static String cleanAnnotations(String params) {
		int i = 0;
		while (i != -1) {
			i = params.indexOf("(", i);
			if (i > -1) {
				final int e = params.indexOf(") ", i);
				params = params.substring(0, i) + params.substring(e + 1);
			}
		}
		i = 0;
		while (i != -1) {
			i = params.indexOf("@", i);
			if (i > -1) {
				final int e = params.indexOf(" ", i);
				params = params.substring(0, i) + params.substring(e + 1);
			}
		}
		return params;
	}
}
