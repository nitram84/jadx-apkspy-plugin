package jadx.plugins.apkspy.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.plugins.apkspy.model.MemberInfo;
import jadx.plugins.apkspy.model.MemberType;

public class ClassBreakdownUtils {
	private static final Logger LOG = LoggerFactory.getLogger(ClassBreakdownUtils.class);

	public static List<MemberInfo> findUninitializedFinalMembers(String classCode) {
		List<MemberInfo> uninitializedFinals = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new StringReader(classCode))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
					continue;
				}

				if (line.contains("final ") && !line.contains("static ") && !line.contains("=")) {

					int lineCommentIndex = line.indexOf("//");
					if (lineCommentIndex != -1) {
						line = line.substring(0, lineCommentIndex).trim();
					}

					int blockCommentIndex = line.indexOf("/*");
					if (blockCommentIndex != -1) {
						line = line.substring(0, blockCommentIndex).trim();
					}

					if (line.endsWith(";")) {
						String clearLine = line.substring(0, line.length() - 1).trim();

						int lastSpace = clearLine.lastIndexOf(' ');
						if (lastSpace != -1) {
							String name = clearLine.substring(lastSpace + 1).trim();

							int finalIndex = clearLine.indexOf("final ");
							String typeName = clearLine.substring(finalIndex + 6, lastSpace).trim();

							MemberType type = MemberType.fromString(typeName);
							uninitializedFinals.add(new MemberInfo(type, typeName, name));
						}
					}
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to analyze final members: ", e);
			return Collections.emptyList();
		}

		return uninitializedFinals;
	}
}
