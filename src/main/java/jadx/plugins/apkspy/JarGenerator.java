package jadx.plugins.apkspy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import jadx.plugins.apkspy.model.ClassBreakdown;
import jadx.plugins.apkspy.utils.Util;

public class JarGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(JarGenerator.class);

	public static void generateStubJar(File apk, File output, OutputStream out, Map<String, ClassBreakdown> classes, Path tempRoot)
			throws IOException {

		PrintStream oldErr = System.err;

		try (PrintStream captureErr = new PrintStream(out, true, StandardCharsets.UTF_8)) {
			System.setErr(captureErr);
			Dex2jarCmd.main("-nc", "-o", output.getAbsolutePath(), apk.getAbsolutePath());
		} finally {
			System.setErr(oldErr);
		}
		if (!output.exists()) {
			throw new FileNotFoundException(output.getAbsolutePath());
		}

		Path tmpDir = tempRoot.resolve("dex2jar-classes");
		Files.createDirectories(tmpDir);

		JarFile jarFile = new JarFile(output);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			String entryName = entry.getName();
			if (!isExcludedClassEntry(entryName, classes.keySet())) {
				visitClass(jarFile, entry, tmpDir, classes);
			}
		}
		jarFile.close();

		output.delete();
		pack(tmpDir, output.toPath());

		Util.attemptDelete(tmpDir.toFile());
	}

	private static boolean isExcludedClassEntry(String entryName, Set<String> excludedClasses) {
		if (entryName.endsWith(".class")) {
			String className = entryName.substring(0, entryName.length() - ".class".length());
			for (String excludedClass : excludedClasses) {
				String internalName = excludedClass.replace('.', '/');
				if (className.equals(internalName) || className.startsWith(internalName + "$")) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	private static void visitClass(JarFile jarFile, JarEntry entry, Path tmpDir, Map<String, ClassBreakdown> classes)
			throws IOException {
		ClassNode classNode = new ClassNode();

		try (InputStream classFileInputStream = jarFile.getInputStream(entry)) {
			ClassReader classReader = new ClassReader(classFileInputStream);
			classReader.accept(classNode, 0);
		}

		ClassWriter writer = new ClassWriter(0);
		writer.visit(classNode.version, classNode.access, classNode.name, classNode.signature, classNode.superName,
				classNode.interfaces.toArray(new String[0]));

		List<FieldNode> fieldNodes = classNode.fields;
		for (FieldNode fieldNode : fieldNodes) {
			writer.visitField(fieldNode.access, fieldNode.name, fieldNode.desc, fieldNode.signature, fieldNode.value);
		}

		List<MethodNode> methodNodes = classNode.methods;
		for (MethodNode methodNode : methodNodes) {
			MethodVisitor visitor = writer.visitMethod(methodNode.access, methodNode.name, methodNode.desc,
					methodNode.signature, methodNode.exceptions.toArray(new String[0]));

			Type returnType = Type.getReturnType(methodNode.desc);

			visitor.visitCode();
			switch (returnType.getDescriptor()) {
				case "Z":
				case "B":
				case "S":
				case "I":
				case "C":
					visitor.visitInsn(Opcodes.ICONST_0);
					visitor.visitInsn(Opcodes.IRETURN);
					break;
				case "J":
					visitor.visitInsn(Opcodes.LCONST_0);
					visitor.visitInsn(Opcodes.LRETURN);
					break;
				case "F":
					visitor.visitInsn(Opcodes.FCONST_0);
					visitor.visitInsn(Opcodes.FRETURN);
					break;
				case "D":
					visitor.visitInsn(Opcodes.DCONST_0);
					visitor.visitInsn(Opcodes.DRETURN);
					break;
				case "V":
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
		byte[] bytes = writer.toByteArray();
		Path path = tmpDir.resolve(entry.getName());
		path.toFile().getParentFile().mkdirs();
		Files.write(path, bytes);
	}

	public static void pack(Path pp, Path zipFilePath) throws IOException {
		Path p = Files.createFile(zipFilePath);
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
			Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
				try {
					zs.putNextEntry(zipEntry);
					Files.copy(path, zs);
					zs.closeEntry();
				} catch (IOException e) {
					LOG.error("Jar creation failed: ", e);
				}
			});
		}
	}
}
