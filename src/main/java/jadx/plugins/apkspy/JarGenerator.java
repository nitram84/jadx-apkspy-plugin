package jadx.plugins.apkspy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.dex2jar.tools.Dex2jarCmd;

import jadx.api.JadxDecompiler;
import jadx.plugins.apkspy.rename.jar.JadxASMRenamer;

public class JarGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(JarGenerator.class);

	public static void generateStubJar(File apk, File output, OutputStream out, Set<String> classes,
			JadxDecompiler decompiler, Path tempRoot)
			throws IOException {

		PrintStream oldErr = System.err;

		Path stubTemp = tempRoot.resolve("stub.jar");

		try (PrintStream captureErr = new PrintStream(out, true, StandardCharsets.UTF_8)) {
			System.setErr(captureErr);
			Dex2jarCmd.main("-nc", "-o", stubTemp.toAbsolutePath().toString(), apk.getAbsolutePath());
		} finally {
			System.setErr(oldErr);
		}
		if (!stubTemp.toFile().exists()) {
			throw new FileNotFoundException(stubTemp.toAbsolutePath().toString());
		}

		final JadxASMRenamer customRemapper = new JadxASMRenamer(Opcodes.ASM9, decompiler);
		customRemapper.prepopulateNameCache();

		try (JarInputStream jis = new JarInputStream(new FileInputStream(stubTemp.toFile()));
				JarOutputStream jos = new JarOutputStream(new FileOutputStream(output))) {

			JarEntry entry;
			while ((entry = jis.getNextJarEntry()) != null) {
				final String entryName = entry.getName();

				if (isExcludedClassEntry(entryName, classes, customRemapper)) {
					continue;
				}
				if (entryName.endsWith(".class")) {
					visitClass(jis, customRemapper, entryName, jos);
				}
				jos.closeEntry();
				jis.closeEntry();
			}
		}
		stubTemp.toFile().delete();
	}

	private static boolean isExcludedClassEntry(String entryName, Set<String> excludedClasses, JadxASMRenamer customRemapper) {
		if (entryName.endsWith(".class")) {
			String className = customRemapper.map(entryName.substring(0, entryName.length() - ".class".length()));
			for (String excludedClass : excludedClasses) {
				String internalName = customRemapper.map(excludedClass.replace('.', '/'));
				if (className.equals(internalName) || className.startsWith(internalName + "$")) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	private static void visitClass(JarInputStream jis, JadxASMRenamer customRemapper, String entryName, JarOutputStream jos)
			throws IOException {
		ClassNode classNode = new ClassNode();

		try (final InputStream classFileInputStream = CloseShieldInputStream.wrap(jis)) {
			ClassReader classReader = new ClassReader(classFileInputStream);
			classReader.accept(classNode, 0);
		}

		ClassWriter writer = new ClassWriter(0);
		writer.visit(classNode.version, classNode.access, customRemapper.mapType(classNode.name),
				customRemapper.mapSignature(classNode.signature, false),
				customRemapper.mapType(classNode.superName),
				classNode.interfaces.stream().map(customRemapper::mapType).toArray(String[]::new));

		List<FieldNode> fieldNodes = classNode.fields;
		for (FieldNode fieldNode : fieldNodes) {
			writer.visitField(fieldNode.access, customRemapper.mapFieldName(classNode.name, fieldNode.name, fieldNode.desc),
					customRemapper.mapDesc(fieldNode.desc),
					customRemapper.mapSignature(fieldNode.signature, true),
					customRemapper.mapValue(fieldNode.value));
		}

		List<MethodNode> methodNodes = classNode.methods;
		for (MethodNode methodNode : methodNodes) {
			MethodVisitor visitor =
					writer.visitMethod(methodNode.access, customRemapper.mapMethodName(classNode.name, methodNode.name, methodNode.desc),
							customRemapper.mapMethodDesc(methodNode.desc),
							customRemapper.mapSignature(methodNode.signature, false),
							methodNode.exceptions.stream().map(customRemapper::mapType).toArray(String[]::new));

			Type returnType = Type.getReturnType(methodNode.desc);

			visitor.visitCode();

			switch (returnType.getSort()) {
				case Type.BOOLEAN:
				case Type.CHAR:
				case Type.BYTE:
				case Type.SHORT:
				case Type.INT:
					visitor.visitInsn(Opcodes.ICONST_0);
					visitor.visitInsn(Opcodes.IRETURN);
					break;
				case Type.LONG:
					visitor.visitInsn(Opcodes.LCONST_0);
					visitor.visitInsn(Opcodes.LRETURN);
					break;
				case Type.FLOAT:
					visitor.visitInsn(Opcodes.FCONST_0);
					visitor.visitInsn(Opcodes.FRETURN);
					break;
				case Type.DOUBLE:
					visitor.visitInsn(Opcodes.DCONST_0);
					visitor.visitInsn(Opcodes.DRETURN);
					break;
				case Type.VOID:
					visitor.visitInsn(Opcodes.RETURN);
					break;
				default:
					visitor.visitInsn(Opcodes.ACONST_NULL);
					visitor.visitInsn(Opcodes.ARETURN);
					break;
			}

			visitor.visitMaxs(20, 20);
			visitor.visitEnd();
		}

		List<InnerClassNode> nodes = classNode.innerClasses;
		for (InnerClassNode node : nodes) {
			writer.visitInnerClass(node.name, node.outerName, node.innerName, node.access);
		}
		writer.visitEnd();

		final String internalClassName = entryName.substring(0, entryName.length() - 6);
		final String newClassName = customRemapper.map(internalClassName);

		jos.putNextEntry(new JarEntry(newClassName + ".class"));
		jos.write(writer.toByteArray());
	}
}
