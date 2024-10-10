package jadx.plugins.apkspy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
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

	public static void generateStubJar(File apk, File output, OutputStream out, Map<String, ClassBreakdown> classes)
			throws IOException, InterruptedException {
		Util.attemptDelete(new File("decompiled-apk"));

		Dex2jarCmd.main("-nc", "-o",
				output.getAbsolutePath(), apk.getAbsolutePath());

		Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "apkSpy", "dex2jar-classes");
		Util.attemptDelete(tmpDir.toFile());
		Files.createDirectories(tmpDir);

		JarFile jarFile = new JarFile(output);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			String entryName = entry.getName();
			if (entryName.startsWith("android")) {
				continue;
			}
			if (entryName.endsWith(".class")) {
				visitClass(jarFile, entry, tmpDir, classes);
			} else if (!entry.isDirectory()) {
				InputStream is = jarFile.getInputStream(entry);
				Path path = tmpDir.resolve(entryName);
				path.toFile().getParentFile().mkdirs();
				FileUtils.copyInputStreamToFile(is, path.toFile());
			}
		}
		jarFile.close();

		output.delete();
		pack(tmpDir, output.toPath());

		Util.attemptDelete(tmpDir.toFile());
	}

	private static void visitClass(JarFile jarFile, JarEntry entry, Path tmpDir, Map<String, ClassBreakdown> classes)
			throws IOException {
		ClassNode classNode = new ClassNode();

		InputStream classFileInputStream = jarFile.getInputStream(entry);
		try {
			ClassReader classReader = new ClassReader(classFileInputStream);
			classReader.accept(classNode, 0);
		} finally {
			classFileInputStream.close();
		}

		ClassWriter writer = new ClassWriter(0);
		writer.visit(classNode.version, classNode.access, classNode.name, classNode.signature, classNode.superName,
				classNode.interfaces.toArray(new String[0]));

		List<FieldNode> fieldNodes = classNode.fields;
		for (FieldNode fieldNode : fieldNodes) {
			for (String className : classes.keySet()) {
				String internal = className.replace('.', '/');
				String pkg = internal.substring(0, internal.lastIndexOf('/'));
				String simple = "ApkSpy$" + internal.substring(internal.lastIndexOf('/') + 1);
				if (fieldNode.desc.equals("L" + internal + ";")) {
					fieldNode.desc = "L" + pkg + "/" + simple + ";";
				}
			}
			writer.visitField(fieldNode.access, fieldNode.name, fieldNode.desc, fieldNode.signature, fieldNode.value);
		}

		List<MethodNode> methodNodes = classNode.methods;
		for (MethodNode methodNode : methodNodes) {
			for (String className : classes.keySet()) {
				String internal = className.replace('.', '/');
				if (methodNode.desc.contains(internal)) {
					String pkg = internal.substring(0, internal.lastIndexOf('/'));
					String simple = "ApkSpy$" + internal.substring(internal.lastIndexOf('/') + 1);
					methodNode.desc = methodNode.desc.replace(internal, pkg + "/" + simple);
				}
			}
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
