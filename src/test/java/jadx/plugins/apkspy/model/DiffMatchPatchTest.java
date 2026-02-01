/*
 * Diff Match and Patch -- Test harness
 * Copyright 2018 The diff-match-patch Authors.
 * https://github.com/google/diff-match-patch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jadx.plugins.apkspy.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.plugins.apkspy.model.DiffMatchPatch.Diff;
import jadx.plugins.apkspy.model.DiffMatchPatch.LinesToCharsResult;
import jadx.plugins.apkspy.model.DiffMatchPatch.Patch;

public class DiffMatchPatchTest {

	private static DiffMatchPatch dmp = new DiffMatchPatch();
	private static DiffMatchPatch.Operation DELETE = DiffMatchPatch.Operation.DELETE;
	private static DiffMatchPatch.Operation EQUAL = DiffMatchPatch.Operation.EQUAL;
	private static DiffMatchPatch.Operation INSERT = DiffMatchPatch.Operation.INSERT;

	// DIFF TEST FUNCTIONS

	@Test
	void testDiffCommonPrefix() {
		// Detect any common prefix.
		Assertions.assertEquals(0, dmp.diffCommonPrefix("abc", "xyz"), "diffCommonPrefix: Null case.");

		Assertions.assertEquals(4, dmp.diffCommonPrefix("1234abcdef", "1234xyz"), "diffCommonPrefix: Non-null case.");

		Assertions.assertEquals(4, dmp.diffCommonPrefix("1234", "1234xyz"), "diffCommonPrefix: Whole case.");
	}

	@Test
	void testDiffCommonSuffix() {
		// Detect any common suffix.
		Assertions.assertEquals(0, dmp.diffCommonSuffix("abc", "xyz"), "diffCommonSuffix: Null case.");

		Assertions.assertEquals(4, dmp.diffCommonSuffix("abcdef1234", "xyz1234"), "diffCommonSuffix: Non-null case.");

		Assertions.assertEquals(4, dmp.diffCommonSuffix("1234", "xyz1234"), "diffCommonSuffix: Whole case.");
	}

	@Test
	void testDiffCommonOverlap() {
		// Detect any suffix/prefix overlap.
		Assertions.assertEquals(0, dmp.diffCommonOverlap("", "abcd"), "diffCommonOverlap: Null case.");

		Assertions.assertEquals(3, dmp.diffCommonOverlap("abc", "abcd"), "diffCommonOverlap: Whole case.");

		Assertions.assertEquals(0, dmp.diffCommonOverlap("123456", "abcd"), "diffCommonOverlap: No overlap.");

		Assertions.assertEquals(3, dmp.diffCommonOverlap("123456xxx", "xxxabcd"), "diffCommonOverlap: Overlap.");

		// Some overly clever languages (C#) may treat ligatures as equal to their
		// component letters. E.g. U+FB01 == 'fi'
		Assertions.assertEquals(0, dmp.diffCommonOverlap("fi", "\ufb01i"), "diffCommonOverlap: Unicode.");
	}

	@Test
	void testDiffHalfmatch() {
		// Detect a halfmatch.
		dmp.diffTimeout = 1;
		Assertions.assertNull(dmp.diffHalfMatch("1234567890", "abcdef"), "diffHalfMatch: No match #1.");

		Assertions.assertNull(dmp.diffHalfMatch("12345", "23"), "diffHalfMatch: No match #2.");

		Assertions.assertArrayEquals(new String[] { "12", "90", "a", "z", "345678" }, dmp.diffHalfMatch("1234567890", "a345678z"),
				"diffHalfMatch: Single Match #1.");

		Assertions.assertArrayEquals(new String[] { "a", "z", "12", "90", "345678" }, dmp.diffHalfMatch("a345678z", "1234567890"),
				"diffHalfMatch: Single Match #2.");

		Assertions.assertArrayEquals(new String[] { "abc", "z", "1234", "0", "56789" }, dmp.diffHalfMatch("abc56789z", "1234567890"),
				"diffHalfMatch: Single Match #3.");

		Assertions.assertArrayEquals(new String[] { "a", "xyz", "1", "7890", "23456" }, dmp.diffHalfMatch("a23456xyz", "1234567890"),
				"diffHalfMatch: Single Match #4.");

		Assertions.assertArrayEquals(new String[] { "12123", "123121", "a", "z", "1234123451234" },
				dmp.diffHalfMatch("121231234123451234123121", "a1234123451234z"), "diffHalfMatch: Multiple Matches #1.");

		Assertions.assertArrayEquals(new String[] { "", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-=" },
				dmp.diffHalfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="), "diffHalfMatch: Multiple Matches #2.");

		Assertions.assertArrayEquals(new String[] { "-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y" },
				dmp.diffHalfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"), "diffHalfMatch: Multiple Matches #3.");

		// Optimal diff would be -q+x=H-i+e=lloHe+Hu=llo-Hew+y not -qHillo+x=HelloHe-w+Hulloy
		Assertions.assertArrayEquals(new String[] { "qHillo", "w", "x", "Hulloy", "HelloHe" },
				dmp.diffHalfMatch("qHilloHelloHew", "xHelloHeHulloy"), "diffHalfMatch: Non-optimal halfmatch.");

		dmp.diffTimeout = 0;
		Assertions.assertNull(dmp.diffHalfMatch("qHilloHelloHew", "xHelloHeHulloy"), "diffHalfMatch: Optimal no halfmatch.");
	}

	@Test
	void testDiffLinesToChars() {
		// Convert lines down to characters.
		ArrayList<String> tmpVector = new ArrayList<String>();
		tmpVector.add("");
		tmpVector.add("alpha\n");
		tmpVector.add("beta\n");
		assertLinesToCharsResultEquals("diffLinesToChars: Shared lines.",
				new LinesToCharsResult("\u0001\u0002\u0001", "\u0002\u0001\u0002", tmpVector),
				dmp.diffLinesToChars("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n"));

		tmpVector.clear();
		tmpVector.add("");
		tmpVector.add("alpha\r\n");
		tmpVector.add("beta\r\n");
		tmpVector.add("\r\n");
		assertLinesToCharsResultEquals("diffLinesToChars: Empty string and blank lines.",
				new LinesToCharsResult("", "\u0001\u0002\u0003\u0003", tmpVector), dmp.diffLinesToChars("", "alpha\r\nbeta\r\n\r\n\r\n"));

		tmpVector.clear();
		tmpVector.add("");
		tmpVector.add("a");
		tmpVector.add("b");
		assertLinesToCharsResultEquals("diffLinesToChars: No linebreaks.", new LinesToCharsResult("\u0001", "\u0002", tmpVector),
				dmp.diffLinesToChars("a", "b"));

		// More than 256 to reveal any 8-bit limitations.
		int n = 300;
		tmpVector.clear();
		StringBuilder lineList = new StringBuilder();
		StringBuilder charList = new StringBuilder();
		for (int i = 1; i < n + 1; i++) {
			tmpVector.add(i + "\n");
			lineList.append(i + "\n");
			charList.append(String.valueOf((char) i));
		}
		Assertions.assertEquals(n, tmpVector.size(), "Test initialization fail #1.");
		String lines = lineList.toString();
		String chars = charList.toString();
		Assertions.assertEquals(n, chars.length(), "Test initialization fail #2.");
		tmpVector.add(0, "");
		assertLinesToCharsResultEquals("diffLinesToChars: More than 256.", new LinesToCharsResult(chars, "", tmpVector),
				dmp.diffLinesToChars(lines, ""));
	}

	@Test
	void testDiffCharsToLines() {
		// First check that Diff equality works.
		Assertions.assertTrue(new Diff(EQUAL, "a").equals(new Diff(EQUAL, "a")), "diffCharsToLines: Equality #1.");

		Assertions.assertEquals(new Diff(EQUAL, "a"), new Diff(EQUAL, "a"), "diffCharsToLines: Equality #2.");

		// Convert chars up to lines.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "\u0001\u0002\u0001"), new Diff(INSERT, "\u0002\u0001\u0002"));
		ArrayList<String> tmpVector = new ArrayList<String>();
		tmpVector.add("");
		tmpVector.add("alpha\n");
		tmpVector.add("beta\n");
		dmp.diffCharsToLines(diffs, tmpVector);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "alpha\nbeta\nalpha\n"), new Diff(INSERT, "beta\nalpha\nbeta\n")), diffs,
				"diffCharsToLines: Shared lines.");

		// More than 256 to reveal any 8-bit limitations.
		int n = 300;
		tmpVector.clear();
		StringBuilder lineList = new StringBuilder();
		StringBuilder charList = new StringBuilder();
		for (int i = 1; i < n + 1; i++) {
			tmpVector.add(i + "\n");
			lineList.append(i + "\n");
			charList.append(String.valueOf((char) i));
		}
		Assertions.assertEquals(n, tmpVector.size(), "Test initialization fail #3.");
		String lines = lineList.toString();
		String chars = charList.toString();
		Assertions.assertEquals(n, chars.length(), "Test initialization fail #4.");
		tmpVector.add(0, "");
		diffs = diffList(new Diff(DELETE, chars));
		dmp.diffCharsToLines(diffs, tmpVector);
		Assertions.assertEquals(diffList(new Diff(DELETE, lines)), diffs, "diffCharsToLines: More than 256.");

		// More than 65536 to verify any 16-bit limitation.
		lineList = new StringBuilder();
		for (int i = 0; i < 66000; i++) {
			lineList.append(i + "\n");
		}
		chars = lineList.toString();
		LinesToCharsResult results = dmp.diffLinesToChars(chars, "");
		diffs = diffList(new Diff(INSERT, results.chars1));
		dmp.diffCharsToLines(diffs, results.lineArray);
		Assertions.assertEquals(chars, diffs.getFirst().text, "diffCharsToLines: More than 65536.");
	}

	@Test
	void testDiffCleanupMerge() {
		// Cleanup a messy diff.
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(), diffs, "diffCleanupMerge: Null case.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(INSERT, "c"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(INSERT, "c")), diffs,
				"diffCleanupMerge: No change case.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(EQUAL, "b"), new Diff(EQUAL, "c"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "abc")), diffs, "diffCleanupMerge: Merge equalities.");

		diffs = diffList(new Diff(DELETE, "a"), new Diff(DELETE, "b"), new Diff(DELETE, "c"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abc")), diffs, "diffCleanupMerge: Merge deletions.");

		diffs = diffList(new Diff(INSERT, "a"), new Diff(INSERT, "b"), new Diff(INSERT, "c"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(INSERT, "abc")), diffs, "diffCleanupMerge: Merge insertions.");

		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"), new Diff(DELETE, "c"), new Diff(INSERT, "d"), new Diff(EQUAL, "e"),
				new Diff(EQUAL, "f"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "ac"), new Diff(INSERT, "bd"), new Diff(EQUAL, "ef")), diffs,
				"diffCleanupMerge: Merge interweave.");

		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "d"), new Diff(INSERT, "b"), new Diff(EQUAL, "c")), diffs,
				"diffCleanupMerge: Prefix and suffix detection.");

		diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"),
				new Diff(EQUAL, "y"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "xa"), new Diff(DELETE, "d"), new Diff(INSERT, "b"), new Diff(EQUAL, "cy")), diffs,
				"diffCleanupMerge: Prefix and suffix detection with equalities.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "ba"), new Diff(EQUAL, "c"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(INSERT, "ab"), new Diff(EQUAL, "ac")), diffs, "diffCleanupMerge: Slide edit left.");

		diffs = diffList(new Diff(EQUAL, "c"), new Diff(INSERT, "ab"), new Diff(EQUAL, "a"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "ca"), new Diff(INSERT, "ba")), diffs, "diffCleanupMerge: Slide edit right.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(EQUAL, "c"), new Diff(DELETE, "ac"), new Diff(EQUAL, "x"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "acx")), diffs,
				"diffCleanupMerge: Slide edit left recursive.");

		diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "ca"), new Diff(EQUAL, "c"), new Diff(DELETE, "b"), new Diff(EQUAL, "a"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "xca"), new Diff(DELETE, "cba")), diffs,
				"diffCleanupMerge: Slide edit right recursive.");

		diffs = diffList(new Diff(DELETE, "b"), new Diff(INSERT, "ab"), new Diff(EQUAL, "c"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(INSERT, "a"), new Diff(EQUAL, "bc")), diffs, "diffCleanupMerge: Empty merge.");

		diffs = diffList(new Diff(EQUAL, ""), new Diff(INSERT, "a"), new Diff(EQUAL, "b"));
		dmp.diffCleanupMerge(diffs);
		Assertions.assertEquals(diffList(new Diff(INSERT, "a"), new Diff(EQUAL, "b")), diffs, "diffCleanupMerge: Empty equality.");
	}

	@Test
	void testDiffCleanupSemanticLossless() {
		// Slide diffs to match logical boundaries.
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(), diffs, "diffCleanupSemanticLossless: Null case.");

		diffs = diffList(new Diff(EQUAL, "AAA\r\n\r\nBBB"), new Diff(INSERT, "\r\nDDD\r\n\r\nBBB"), new Diff(EQUAL, "\r\nEEE"));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(
				diffList(new Diff(EQUAL, "AAA\r\n\r\n"), new Diff(INSERT, "BBB\r\nDDD\r\n\r\n"), new Diff(EQUAL, "BBB\r\nEEE")), diffs,
				"diffCleanupSemanticLossless: Blank lines.");

		diffs = diffList(new Diff(EQUAL, "AAA\r\nBBB"), new Diff(INSERT, " DDD\r\nBBB"), new Diff(EQUAL, " EEE"));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "AAA\r\n"), new Diff(INSERT, "BBB DDD\r\n"), new Diff(EQUAL, "BBB EEE")), diffs,
				"diffCleanupSemanticLossless: Line boundaries.");

		diffs = diffList(new Diff(EQUAL, "The c"), new Diff(INSERT, "ow and the c"), new Diff(EQUAL, "at."));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "The "), new Diff(INSERT, "cow and the "), new Diff(EQUAL, "cat.")), diffs,
				"diffCleanupSemanticLossless: Word boundaries.");

		diffs = diffList(new Diff(EQUAL, "The-c"), new Diff(INSERT, "ow-and-the-c"), new Diff(EQUAL, "at."));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "The-"), new Diff(INSERT, "cow-and-the-"), new Diff(EQUAL, "cat.")), diffs,
				"diffCleanupSemanticLossless: Alphanumeric boundaries.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "a"), new Diff(EQUAL, "ax"));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "a"), new Diff(EQUAL, "aax")), diffs,
				"diffCleanupSemanticLossless: Hitting the start.");

		diffs = diffList(new Diff(EQUAL, "xa"), new Diff(DELETE, "a"), new Diff(EQUAL, "a"));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "xaa"), new Diff(DELETE, "a")), diffs,
				"diffCleanupSemanticLossless: Hitting the end.");

		diffs = diffList(new Diff(EQUAL, "The xxx. The "), new Diff(INSERT, "zzz. The "), new Diff(EQUAL, "yyy."));
		dmp.diffCleanupSemanticLossless(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "The xxx."), new Diff(INSERT, " The zzz."), new Diff(EQUAL, " The yyy.")), diffs,
				"diffCleanupSemanticLossless: Sentence boundaries.");
	}

	@Test
	void testDiffCleanupSemantic() {
		// Cleanup semantically trivial equalities.
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(), diffs, "diffCleanupSemantic: Null case.");

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "cd"), new Diff(EQUAL, "12"), new Diff(DELETE, "e"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "cd"), new Diff(EQUAL, "12"), new Diff(DELETE, "e")),
				diffs, "diffCleanupSemantic: No elimination #1.");

		diffs = diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "ABC"), new Diff(EQUAL, "1234"), new Diff(DELETE, "wxyz"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(
				diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "ABC"), new Diff(EQUAL, "1234"), new Diff(DELETE, "wxyz")), diffs,
				"diffCleanupSemantic: No elimination #2.");

		diffs = diffList(new Diff(DELETE, "a"), new Diff(EQUAL, "b"), new Diff(DELETE, "c"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "b")), diffs,
				"diffCleanupSemantic: Simple elimination.");

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(EQUAL, "cd"), new Diff(DELETE, "e"), new Diff(EQUAL, "f"), new Diff(INSERT, "g"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abcdef"), new Diff(INSERT, "cdfg")), diffs,
				"diffCleanupSemantic: Backpass elimination.");

		diffs = diffList(new Diff(INSERT, "1"), new Diff(EQUAL, "A"), new Diff(DELETE, "B"), new Diff(INSERT, "2"), new Diff(EQUAL, "_"),
				new Diff(INSERT, "1"), new Diff(EQUAL, "A"), new Diff(DELETE, "B"), new Diff(INSERT, "2"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "AB_AB"), new Diff(INSERT, "1A2_1A2")), diffs,
				"diffCleanupSemantic: Multiple elimination.");

		diffs = diffList(new Diff(EQUAL, "The c"), new Diff(DELETE, "ow and the c"), new Diff(EQUAL, "at."));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(EQUAL, "The "), new Diff(DELETE, "cow and the "), new Diff(EQUAL, "cat.")), diffs,
				"diffCleanupSemantic: Word boundaries.");

		diffs = diffList(new Diff(DELETE, "abcxx"), new Diff(INSERT, "xxdef"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abcxx"), new Diff(INSERT, "xxdef")), diffs,
				"diffCleanupSemantic: No overlap elimination.");

		diffs = diffList(new Diff(DELETE, "abcxxx"), new Diff(INSERT, "xxxdef"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "xxx"), new Diff(INSERT, "def")), diffs,
				"diffCleanupSemantic: Overlap elimination.");

		diffs = diffList(new Diff(DELETE, "xxxabc"), new Diff(INSERT, "defxxx"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(diffList(new Diff(INSERT, "def"), new Diff(EQUAL, "xxx"), new Diff(DELETE, "abc")), diffs,
				"diffCleanupSemantic: Reverse overlap elimination.");

		diffs = diffList(new Diff(DELETE, "abcd1212"), new Diff(INSERT, "1212efghi"), new Diff(EQUAL, "----"), new Diff(DELETE, "A3"),
				new Diff(INSERT, "3BC"));
		dmp.diffCleanupSemantic(diffs);
		Assertions.assertEquals(
				diffList(new Diff(DELETE, "abcd"), new Diff(EQUAL, "1212"), new Diff(INSERT, "efghi"), new Diff(EQUAL, "----"),
						new Diff(DELETE, "A"), new Diff(EQUAL, "3"), new Diff(INSERT, "BC")),
				diffs, "diffCleanupSemantic: Two overlap eliminations.");
	}

	@Test
	void testDiffCleanupEfficiency() {
		// Cleanup operationally trivial equalities.
		dmp.diffEditCost = 4;
		LinkedList<Diff> diffs = diffList();
		dmp.diffCleanupEfficiency(diffs);
		Assertions.assertEquals(diffList(), diffs, "diffCleanupEfficiency: Null case.");

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"),
				new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"),
				new Diff(INSERT, "34")), diffs, "diffCleanupEfficiency: No elimination.");

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "xyz"), new Diff(DELETE, "cd"),
				new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abxyzcd"), new Diff(INSERT, "12xyz34")), diffs,
				"diffCleanupEfficiency: Four-edit elimination.");

		diffs = diffList(new Diff(INSERT, "12"), new Diff(EQUAL, "x"), new Diff(DELETE, "cd"), new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "xcd"), new Diff(INSERT, "12x34")), diffs,
				"diffCleanupEfficiency: Three-edit elimination.");

		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "xy"), new Diff(INSERT, "34"),
				new Diff(EQUAL, "z"), new Diff(DELETE, "cd"), new Diff(INSERT, "56"));
		dmp.diffCleanupEfficiency(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abxyzcd"), new Diff(INSERT, "12xy34z56")), diffs,
				"diffCleanupEfficiency: Backpass elimination.");

		dmp.diffEditCost = 5;
		diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"),
				new Diff(INSERT, "34"));
		dmp.diffCleanupEfficiency(diffs);
		Assertions.assertEquals(diffList(new Diff(DELETE, "abwxyzcd"), new Diff(INSERT, "12wxyz34")), diffs,
				"diffCleanupEfficiency: High cost elimination.");
		dmp.diffEditCost = 4;
	}

	@Test
	void testDiffPrettyHtml() {
		// Pretty print.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "a\n"), new Diff(DELETE, "<B>b</B>"), new Diff(INSERT, "c&d"));
		Assertions.assertEquals(
				"<span>a&para;<br></span><del style=\"background:#ffe6e6;\">&lt;B&gt;b&lt;/B&gt;</del><ins style=\"background:#e6ffe6;\">c&amp;d</ins>",
				dmp.diffPrettyHtml(diffs), "diffPrettyHtml:");
	}

	@Test
	void testDiffText() {
		// Compute the source and destination texts.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "),
				new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, " lazy"));
		Assertions.assertEquals("jumps over the lazy", dmp.diffText1(diffs), "diffText1:");
		Assertions.assertEquals("jumped over a lazy", dmp.diffText2(diffs), "diffText2:");
	}

	@Test
	void testDiffDelta() {
		// Convert a diff into delta string.
		LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "),
				new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, " lazy"), new Diff(INSERT, "old dog"));
		String text1 = dmp.diffText1(diffs);
		Assertions.assertEquals("jumps over the lazy", text1, "diffText1: Base text.");

		String delta = dmp.diffToDelta(diffs);
		Assertions.assertEquals("=4\t-1\t+ed\t=6\t-3\t+a\t=5\t+old dog", delta, "diffToDelta:");

		// Convert delta string into a diff.
		Assertions.assertEquals(diffs, dmp.diffFromDelta(text1, delta), "diffFromDelta: Normal.");

		// Generates error (19 < 20).
		try {
			dmp.diffFromDelta(text1 + "x", delta);
			Assertions.fail("diffFromDelta: Too long.");
		} catch (IllegalArgumentException ex) {
			// Exception expected.
		}

		// Generates error (19 > 18).
		try {
			dmp.diffFromDelta(text1.substring(1), delta);
			Assertions.fail("diffFromDelta: Too short.");
		} catch (IllegalArgumentException ex) {
			// Exception expected.
		}

		// Generates error (%c3%xy invalid Unicode).
		try {
			dmp.diffFromDelta("", "+%c3%xy");
			Assertions.fail("diffFromDelta: Invalid character.");
		} catch (IllegalArgumentException ex) {
			// Exception expected.
		}

		// Test deltas with special characters.
		diffs = diffList(new Diff(EQUAL, "\u0680 \000 \t %"), new Diff(DELETE, "\u0681 \001 \n ^"), new Diff(INSERT, "\u0682 \002 \\ |"));
		text1 = dmp.diffText1(diffs);
		Assertions.assertEquals("\u0680 \000 \t %\u0681 \001 \n ^", text1, "diffText1: Unicode text.");

		delta = dmp.diffToDelta(diffs);
		Assertions.assertEquals("=7\t-7\t+%DA%82 %02 %5C %7C", delta, "diffToDelta: Unicode.");

		Assertions.assertEquals(diffs, dmp.diffFromDelta(text1, delta), "diffFromDelta: Unicode.");

		// Verify pool of unchanged characters.
		diffs = diffList(new Diff(INSERT, "A-Z a-z 0-9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , # "));
		String text2 = dmp.diffText2(diffs);
		Assertions.assertEquals("A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", text2, "diffText2: Unchanged characters.");

		delta = dmp.diffToDelta(diffs);
		Assertions.assertEquals("+A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", delta, "diffToDelta: Unchanged characters.");

		// Convert delta string into a diff.
		Assertions.assertEquals(diffs, dmp.diffFromDelta("", delta), "diffFromDelta: Unchanged characters.");

		// 160 kb string.
		String a = "abcdefghij";
		for (int i = 0; i < 14; i++) {
			a += a;
		}
		diffs = diffList(new Diff(INSERT, a));
		delta = dmp.diffToDelta(diffs);
		Assertions.assertEquals("+" + a, delta, "diffToDelta: 160kb string.");

		// Convert delta string into a diff.
		Assertions.assertEquals(diffs, dmp.diffFromDelta("", delta), "diffFromDelta: 160kb string.");
	}

	@Test
	void testDiffXIndex() {
		// Translate a location in text1 to text2.
		LinkedList<Diff> diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "1234"), new Diff(EQUAL, "xyz"));
		Assertions.assertEquals(5, dmp.diffXIndex(diffs, 2), "diffXIndex: Translation on equality.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "1234"), new Diff(EQUAL, "xyz"));
		Assertions.assertEquals(1, dmp.diffXIndex(diffs, 3), "diffXIndex: Translation on deletion.");
	}

	@Test
	void testDiffLevenshtein() {
		LinkedList<Diff> diffs = diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "1234"), new Diff(EQUAL, "xyz"));
		Assertions.assertEquals(4, dmp.diffLevenshtein(diffs), "diffLevenshtein: Levenshtein with trailing equality.");

		diffs = diffList(new Diff(EQUAL, "xyz"), new Diff(DELETE, "abc"), new Diff(INSERT, "1234"));
		Assertions.assertEquals(4, dmp.diffLevenshtein(diffs), "diffLevenshtein: Levenshtein with leading equality.");

		diffs = diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "xyz"), new Diff(INSERT, "1234"));
		Assertions.assertEquals(7, dmp.diffLevenshtein(diffs), "diffLevenshtein: Levenshtein with middle equality.");
	}

	@Test
	void testDiffBisect() {
		// Normal.
		String a = "cat";
		String b = "map";
		// Since the resulting diff hasn't been normalized, it would be ok if
		// the insertion and deletion pairs are swapped.
		// If the order changes, tweak this test as required.
		LinkedList<Diff> diffs =
				diffList(new Diff(DELETE, "c"), new Diff(INSERT, "m"), new Diff(EQUAL, "a"), new Diff(DELETE, "t"), new Diff(INSERT, "p"));
		Assertions.assertEquals(diffs, dmp.diffBisect(a, b, Long.MAX_VALUE), "diffBisect: Normal.");

		// Timeout.
		diffs = diffList(new Diff(DELETE, "cat"), new Diff(INSERT, "map"));
		Assertions.assertEquals(diffs, dmp.diffBisect(a, b, 0), "diffBisect: Timeout.");
	}

	@Test
	void testDiffMain() {
		// Perform a trivial diff.
		LinkedList<Diff> diffs = diffList();
		Assertions.assertEquals(diffs, dmp.diffMain("", "", false), "diffMain: Null case.");

		diffs = diffList(new Diff(EQUAL, "abc"));
		Assertions.assertEquals(diffs, dmp.diffMain("abc", "abc", false), "diffMain: Equality.");

		diffs = diffList(new Diff(EQUAL, "ab"), new Diff(INSERT, "123"), new Diff(EQUAL, "c"));
		Assertions.assertEquals(diffs, dmp.diffMain("abc", "ab123c", false), "diffMain: Simple insertion.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "123"), new Diff(EQUAL, "bc"));
		Assertions.assertEquals(diffs, dmp.diffMain("a123bc", "abc", false), "diffMain: Simple deletion.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "123"), new Diff(EQUAL, "b"), new Diff(INSERT, "456"),
				new Diff(EQUAL, "c"));
		Assertions.assertEquals(diffs, dmp.diffMain("abc", "a123b456c", false), "diffMain: Two insertions.");

		diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "123"), new Diff(EQUAL, "b"), new Diff(DELETE, "456"),
				new Diff(EQUAL, "c"));
		Assertions.assertEquals(diffs, dmp.diffMain("a123b456c", "abc", false), "diffMain: Two deletions.");

		// Perform a real diff.
		// Switch off the timeout.
		dmp.diffTimeout = 0;
		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"));
		Assertions.assertEquals(diffs, dmp.diffMain("a", "b", false), "diffMain: Simple case #1.");

		diffs = diffList(new Diff(DELETE, "Apple"), new Diff(INSERT, "Banana"), new Diff(EQUAL, "s are a"), new Diff(INSERT, "lso"),
				new Diff(EQUAL, " fruit."));
		Assertions.assertEquals(diffs, dmp.diffMain("Apples are a fruit.", "Bananas are also fruit.", false), "diffMain: Simple case #2.");

		diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "\u0680"), new Diff(EQUAL, "x"), new Diff(DELETE, "\t"),
				new Diff(INSERT, "\000"));
		Assertions.assertEquals(diffs, dmp.diffMain("ax\t", "\u0680x\000", false), "diffMain: Simple case #3.");

		diffs = diffList(new Diff(DELETE, "1"), new Diff(EQUAL, "a"), new Diff(DELETE, "y"), new Diff(EQUAL, "b"), new Diff(DELETE, "2"),
				new Diff(INSERT, "xab"));
		Assertions.assertEquals(diffs, dmp.diffMain("1ayb2", "abxab", false), "diffMain: Overlap #1.");

		diffs = diffList(new Diff(INSERT, "xaxcx"), new Diff(EQUAL, "abc"), new Diff(DELETE, "y"));
		Assertions.assertEquals(diffs, dmp.diffMain("abcy", "xaxcxabc", false), "diffMain: Overlap #2.");

		diffs = diffList(new Diff(DELETE, "ABCD"), new Diff(EQUAL, "a"), new Diff(DELETE, "="), new Diff(INSERT, "-"),
				new Diff(EQUAL, "bcd"), new Diff(DELETE, "="), new Diff(INSERT, "-"), new Diff(EQUAL, "efghijklmnopqrs"),
				new Diff(DELETE, "EFGHIJKLMNOefg"));
		Assertions.assertEquals(diffs, dmp.diffMain("ABCDa=bcd=efghijklmnopqrsEFGHIJKLMNOefg", "a-bcd-efghijklmnopqrs", false),
				"diffMain: Overlap #3.");

		diffs = diffList(new Diff(INSERT, " "), new Diff(EQUAL, "a"), new Diff(INSERT, "nd"), new Diff(EQUAL, " [[Pennsylvania]]"),
				new Diff(DELETE, " and [[New"));
		Assertions.assertEquals(diffs, dmp.diffMain("a [[Pennsylvania]] and [[New", " and [[Pennsylvania]]", false),
				"diffMain: Large equality.");

		dmp.diffTimeout = 0.1f; // 100ms
		String a =
				"`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n";
		String b =
				"I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n";
		// Increase the text lengths by 1024 times to ensure a timeout.
		for (int i = 0; i < 10; i++) {
			a += a;
			b += b;
		}
		long startTime = System.currentTimeMillis();
		dmp.diffMain(a, b);
		long endTime = System.currentTimeMillis();
		// Test that we took at least the timeout period.
		Assertions.assertTrue(dmp.diffTimeout * 1000 <= endTime - startTime, "diffMain: Timeout min.");
		// Test that we didn't take forever (be forgiving).
		// Theoretically this test could fail very occasionally if the
		// OS task swaps or locks up for a second at the wrong moment.
		Assertions.assertTrue(dmp.diffTimeout * 1000 * 2 > endTime - startTime, "diffMain: Timeout max.");
		dmp.diffTimeout = 0;

		// Test the linemode speedup.
		// Must be long to pass the 100 char cutoff.
		a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
		b = "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n";
		Assertions.assertEquals(dmp.diffMain(a, b, true), dmp.diffMain(a, b, false), "diffMain: Simple line-mode.");

		a = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		b = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
		Assertions.assertEquals(dmp.diffMain(a, b, true), dmp.diffMain(a, b, false), "diffMain: Single line-mode.");

		a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
		b = "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n";
		String[] texts_linemode = diff_rebuildtexts(dmp.diffMain(a, b, true));
		String[] texts_textmode = diff_rebuildtexts(dmp.diffMain(a, b, false));
		Assertions.assertArrayEquals(texts_textmode, texts_linemode, "diffMain: Overlap line-mode.");

		// Test null inputs.
		try {
			dmp.diffMain(null, null);
			Assertions.fail("diffMain: Null inputs.");
		} catch (IllegalArgumentException ex) {
			// Error expected.
		}
	}

	// MATCH TEST FUNCTIONS

	@Test
	void testMatchAlphabet() {
		// Initialise the bitmasks for Bitap.
		Map<Character, Integer> bitmask;
		bitmask = new HashMap<Character, Integer>();
		bitmask.put('a', 4);
		bitmask.put('b', 2);
		bitmask.put('c', 1);
		Assertions.assertEquals(bitmask, dmp.matchAlphabet("abc"), "matchAlphabet: Unique.");

		bitmask = new HashMap<Character, Integer>();
		bitmask.put('a', 37);
		bitmask.put('b', 18);
		bitmask.put('c', 8);
		Assertions.assertEquals(bitmask, dmp.matchAlphabet("abcaba"), "matchAlphabet: Duplicates.");
	}

	@Test
	void testMatchBitap() {
		// Bitmap algorithm.
		dmp.matchDistance = 100;
		dmp.matchThreshold = 0.5f;
		Assertions.assertEquals(5, dmp.matchBitap("abcdefghijk", "fgh", 5), "matchBitap: Exact match #1.");

		Assertions.assertEquals(5, dmp.matchBitap("abcdefghijk", "fgh", 0), "matchBitap: Exact match #2.");

		Assertions.assertEquals(4, dmp.matchBitap("abcdefghijk", "efxhi", 0), "matchBitap: Fuzzy match #1.");

		Assertions.assertEquals(2, dmp.matchBitap("abcdefghijk", "cdefxyhijk", 5), "matchBitap: Fuzzy match #2.");

		Assertions.assertEquals(-1, dmp.matchBitap("abcdefghijk", "bxy", 1), "matchBitap: Fuzzy match #3.");

		Assertions.assertEquals(2, dmp.matchBitap("123456789xx0", "3456789x0", 2), "matchBitap: Overflow.");

		Assertions.assertEquals(0, dmp.matchBitap("abcdef", "xxabc", 4), "matchBitap: Before start match.");

		Assertions.assertEquals(3, dmp.matchBitap("abcdef", "defyy", 4), "matchBitap: Beyond end match.");

		Assertions.assertEquals(0, dmp.matchBitap("abcdef", "xabcdefy", 0), "matchBitap: Oversized pattern.");

		dmp.matchThreshold = 0.4f;
		Assertions.assertEquals(4, dmp.matchBitap("abcdefghijk", "efxyhi", 1), "matchBitap: Threshold #1.");

		dmp.matchThreshold = 0.3f;
		Assertions.assertEquals(-1, dmp.matchBitap("abcdefghijk", "efxyhi", 1), "matchBitap: Threshold #2.");

		dmp.matchThreshold = 0.0f;
		Assertions.assertEquals(1, dmp.matchBitap("abcdefghijk", "bcdef", 1), "matchBitap: Threshold #3.");

		dmp.matchThreshold = 0.5f;
		Assertions.assertEquals(0, dmp.matchBitap("abcdexyzabcde", "abccde", 3), "matchBitap: Multiple select #1.");

		Assertions.assertEquals(8, dmp.matchBitap("abcdexyzabcde", "abccde", 5), "matchBitap: Multiple select #2.");

		dmp.matchDistance = 10; // Strict location.
		Assertions.assertEquals(-1, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24), "matchBitap: Distance test #1.");

		Assertions.assertEquals(0, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdxxefg", 1), "matchBitap: Distance test #2.");

		dmp.matchDistance = 1000; // Loose location.
		Assertions.assertEquals(0, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24), "matchBitap: Distance test #3.");
	}

	@Test
	void testMatchMain() {
		// Full match.
		Assertions.assertEquals(0, dmp.matchBitmap("abcdef", "abcdef", 1000), "matchBitmap: Equality.");

		Assertions.assertEquals(-1, dmp.matchBitmap("", "abcdef", 1), "matchBitmap: Null text.");

		Assertions.assertEquals(3, dmp.matchBitmap("abcdef", "", 3), "matchBitmap: Null pattern.");

		Assertions.assertEquals(3, dmp.matchBitmap("abcdef", "de", 3), "matchBitmap: Exact match.");

		Assertions.assertEquals(3, dmp.matchBitmap("abcdef", "defy", 4), "matchBitmap: Beyond end match.");

		Assertions.assertEquals(0, dmp.matchBitmap("abcdef", "abcdefy", 0), "matchBitmap: Oversized pattern.");

		dmp.matchThreshold = 0.7f;
		Assertions.assertEquals(4, dmp.matchBitmap("I am the very model of a modern major general.", " that berry ", 5),
				"matchBitmap: Complex match.");
		dmp.matchThreshold = 0.5f;

		// Test null inputs.
		try {
			dmp.matchBitmap(null, null, 0);
			Assertions.fail("matchBitmap: Null inputs.");
		} catch (IllegalArgumentException ex) {
			// Error expected.
		}
	}

	// PATCH TEST FUNCTIONS

	@Test
	void testPatchObj() {
		// Patch Object.
		Patch p = new Patch();
		p.start1 = 20;
		p.start2 = 21;
		p.length1 = 18;
		p.length2 = 17;
		p.diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "),
				new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, "\nlaz"));
		String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
		Assertions.assertEquals(strp, p.toString(), "Patch: toString.");
	}

	@Test
	void testPatchFromText() {
		Assertions.assertTrue(dmp.patchFromText("").isEmpty(), "patchFromText: #0.");

		String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
		Assertions.assertEquals(strp, dmp.patchFromText(strp).get(0).toString(), "patchFromText: #1.");

		Assertions.assertEquals("@@ -1 +1 @@\n-a\n+b\n", dmp.patchFromText("@@ -1 +1 @@\n-a\n+b\n").get(0).toString(),
				"patchFromText: #2.");

		Assertions.assertEquals("@@ -1,3 +0,0 @@\n-abc\n", dmp.patchFromText("@@ -1,3 +0,0 @@\n-abc\n").get(0).toString(),
				"patchFromText: #3.");

		Assertions.assertEquals("@@ -0,0 +1,3 @@\n+abc\n", dmp.patchFromText("@@ -0,0 +1,3 @@\n+abc\n").get(0).toString());

		// Generates error.
		try {
			dmp.patchFromText("Bad\nPatch\n");
			Assertions.fail("patchFromText: #5.");
		} catch (IllegalArgumentException ex) {
			// Exception expected.
		}
	}

	@Test
	void testPatchToText() {
		String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
		List<Patch> patches;
		patches = dmp.patchFromText(strp);
		Assertions.assertEquals(strp, dmp.patchToText(patches), "patchToText: Single.");

		strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n  tes\n";
		patches = dmp.patchFromText(strp);
		Assertions.assertEquals(strp, dmp.patchToText(patches), "patchToText: Dual.");
	}

	@Test
	void testPatchAddContext() {
		dmp.patchMargin = 4;
		Patch p;
		p = dmp.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps over the lazy dog.");
		Assertions.assertEquals("@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", p.toString(), "patchAddContext: Simple case.");

		p = dmp.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps.");
		Assertions.assertEquals("@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString(),
				"patchAddContext: Not enough trailing context.");

		p = dmp.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps.");
		Assertions.assertEquals("@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n", p.toString(), "patchAddContext: Not enough leading context.");

		p = dmp.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
		dmp.patchAddContext(p, "The quick brown fox jumps.  The quick brown fox crashes.");
		Assertions.assertEquals("@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n", p.toString(),
				"patchAddContext: Ambiguity.");
	}

	@SuppressWarnings("deprecation")
	@Test
	void testPatchMake() {
		LinkedList<Patch> patches;
		patches = dmp.patchMake("", "");
		Assertions.assertEquals("", dmp.patchToText(patches), "patchMake: Null case.");

		String text1 = "The quick brown fox jumps over the lazy dog.";
		String text2 = "That quick brown fox jumped over a lazy dog.";
		String expectedPatch = "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n  qui\n@@ -21,17 +21,18 @@\n jump\n-ed\n+s\n  over \n-a\n+the\n  laz\n";
		// The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to rolling context.
		patches = dmp.patchMake(text2, text1);
		Assertions.assertEquals(expectedPatch, dmp.patchToText(patches), "patchMake: Text2+Text1 inputs.");

		expectedPatch = "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n  quick b\n@@ -22,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
		patches = dmp.patchMake(text1, text2);
		Assertions.assertEquals(expectedPatch, dmp.patchToText(patches), "patchMake: Text1+Text2 inputs.");

		LinkedList<Diff> diffs = dmp.diffMain(text1, text2, false);
		patches = dmp.patchMake(diffs);
		Assertions.assertEquals(expectedPatch, dmp.patchToText(patches), "patchMake: Diff input.");

		patches = dmp.patchMake(text1, diffs);
		Assertions.assertEquals(expectedPatch, dmp.patchToText(patches), "patchMake: Text1+Diff inputs.");

		patches = dmp.patchMake(text1, text2, diffs);
		Assertions.assertEquals(expectedPatch, dmp.patchToText(patches), "patchMake: Text1+Text2+Diff inputs (deprecated).");

		patches = dmp.patchMake("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?");
		Assertions.assertEquals("@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n",
				dmp.patchToText(patches), "patchToText: Character encoding.");

		diffs = diffList(new Diff(DELETE, "`1234567890-=[]\\;',./"), new Diff(INSERT, "~!@#$%^&*()_+{}|:\"<>?"));
		Assertions.assertEquals(diffs, dmp
				.patchFromText("@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n").get(0).diffs,
				"patchFromText: Character decoding.");

		text1 = "";
		for (int x = 0; x < 100; x++) {
			text1 += "abcdef";
		}
		text2 = text1 + "123";
		expectedPatch = "@@ -573,28 +573,31 @@\n cdefabcdefabcdefabcdefabcdef\n+123\n";
		patches = dmp.patchMake(text1, text2);
		Assertions.assertEquals(expectedPatch, dmp.patchToText(patches), "patchMake: Long string with repeats.");

		// Test null inputs.
		try {
			dmp.patchMake(null);
			Assertions.fail("patchMake: Null inputs.");
		} catch (IllegalArgumentException ex) {
			// Error expected.
		}
	}

	@Test
	void testPatchSplitMax() {
		// Assumes that Match_MaxBits is 32.
		LinkedList<Patch> patches;
		patches = dmp.patchMake("abcdefghijklmnopqrstuvwxyz01234567890", "XabXcdXefXghXijXklXmnXopXqrXstXuvXwxXyzX01X23X45X67X89X0");
		dmp.patchSplitMax(patches);
		Assertions.assertEquals(
				"@@ -1,32 +1,46 @@\n+X\n ab\n+X\n cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n@@ -25,13 +39,18 @@\n zX01\n+X\n 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n",
				dmp.patchToText(patches), "patchSplitMax: #1.");

		patches = dmp.patchMake("abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz", "abcdefuvwxyz");
		String oldToText = dmp.patchToText(patches);
		dmp.patchSplitMax(patches);
		Assertions.assertEquals(oldToText, dmp.patchToText(patches), "patchSplitMax: #2.");

		patches = dmp.patchMake("1234567890123456789012345678901234567890123456789012345678901234567890", "abc");
		dmp.patchSplitMax(patches);
		Assertions.assertEquals(
				"@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n",
				dmp.patchToText(patches), "patchSplitMax: #3.");

		patches = dmp.patchMake("abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1",
				"abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1");
		dmp.patchSplitMax(patches);
		Assertions.assertEquals(
				"@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n",
				dmp.patchToText(patches), "patchSplitMax: #4.");
	}

	@Test
	void testPatchAddPadding() {
		LinkedList<Patch> patches;
		patches = dmp.patchMake("", "test");
		Assertions.assertEquals("@@ -0,0 +1,4 @@\n+test\n", dmp.patchToText(patches), "patchAddPadding: Both edges full.");
		dmp.patchAddPadding(patches);
		Assertions.assertEquals("@@ -1,8 +1,12 @@\n %01%02%03%04\n+test\n %01%02%03%04\n", dmp.patchToText(patches),
				"patchAddPadding: Both edges full.");

		patches = dmp.patchMake("XY", "XtestY");
		Assertions.assertEquals("@@ -1,2 +1,6 @@\n X\n+test\n Y\n", dmp.patchToText(patches), "patchAddPadding: Both edges partial.");
		dmp.patchAddPadding(patches);
		Assertions.assertEquals("@@ -2,8 +2,12 @@\n %02%03%04X\n+test\n Y%01%02%03\n", dmp.patchToText(patches),
				"patchAddPadding: Both edges partial.");

		patches = dmp.patchMake("XXXXYYYY", "XXXXtestYYYY");
		Assertions.assertEquals("@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n", dmp.patchToText(patches), "patchAddPadding: Both edges none.");
		dmp.patchAddPadding(patches);
		Assertions.assertEquals("@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n", dmp.patchToText(patches), "patchAddPadding: Both edges none.");
	}

	@Test
	void testPatchApply() {
		dmp.matchDistance = 1000;
		dmp.matchThreshold = 0.5f;
		dmp.patchDeleteThreshold = 0.5f;
		LinkedList<Patch> patches;
		patches = dmp.patchMake("", "");
		Object[] results = dmp.patchApply(patches, "Hello world.");
		boolean[] boolArray = (boolean[]) results[1];
		String resultStr = results[0] + "\t" + boolArray.length;
		Assertions.assertEquals("Hello world.\t0", resultStr, "patchApply: Null case.");

		patches = dmp.patchMake("The quick brown fox jumps over the lazy dog.", "That quick brown fox jumped over a lazy dog.");
		results = dmp.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("That quick brown fox jumped over a lazy dog.\ttrue\ttrue", resultStr, "patchApply: Exact match.");

		results = dmp.patchApply(patches, "The quick red rabbit jumps over the tired tiger.");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("That quick red rabbit jumped over a tired tiger.\ttrue\ttrue", resultStr, "patchApply: Partial match.");

		results = dmp.patchApply(patches, "I am the very model of a modern major general.");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("I am the very model of a modern major general.\tfalse\tfalse", resultStr, "patchApply: Failed match.");

		patches = dmp.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		results = dmp.patchApply(patches, "x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("xabcy\ttrue\ttrue", resultStr, "patchApply: Big delete, small change.");

		patches = dmp.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		results = dmp.patchApply(patches, "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("xabc12345678901234567890---------------++++++++++---------------12345678901234567890y\tfalse\ttrue",
				resultStr, "patchApply: Big delete, big change 1.");

		dmp.patchDeleteThreshold = 0.6f;
		patches = dmp.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		results = dmp.patchApply(patches, "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("xabcy\ttrue\ttrue", resultStr, "patchApply: Big delete, big change 2.");
		dmp.patchDeleteThreshold = 0.5f;

		// Compensate for failed patch.
		dmp.matchThreshold = 0.0f;
		dmp.matchDistance = 0;
		patches = dmp.patchMake("abcdefghijklmnopqrstuvwxyz--------------------1234567890",
				"abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
		results = dmp.patchApply(patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		Assertions.assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue", resultStr,
				"patchApply: Compensate for failed patch.");
		dmp.matchThreshold = 0.5f;
		dmp.matchDistance = 1000;

		patches = dmp.patchMake("", "test");
		String patchStr = dmp.patchToText(patches);
		dmp.patchApply(patches, "");
		Assertions.assertEquals(patchStr, dmp.patchToText(patches), "patchApply: No side effects.");

		patches = dmp.patchMake("The quick brown fox jumps over the lazy dog.", "Woof");
		patchStr = dmp.patchToText(patches);
		dmp.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
		Assertions.assertEquals(patchStr, dmp.patchToText(patches), "patchApply: No side effects with major delete.");

		patches = dmp.patchMake("", "test");
		results = dmp.patchApply(patches, "");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0];
		Assertions.assertEquals("test\ttrue", resultStr, "patchApply: Edge exact match.");

		patches = dmp.patchMake("XY", "XtestY");
		results = dmp.patchApply(patches, "XY");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0];
		Assertions.assertEquals("XtestY\ttrue", resultStr, "patchApply: Near edge exact match.");

		patches = dmp.patchMake("y", "y123");
		results = dmp.patchApply(patches, "x");
		boolArray = (boolean[]) results[1];
		resultStr = results[0] + "\t" + boolArray[0];

		Assertions.assertEquals("x123\ttrue", resultStr, "patchApply: Edge partial match.");
	}

	private static void assertLinesToCharsResultEquals(String error_msg,
			LinesToCharsResult a, LinesToCharsResult b) {
		Assertions.assertEquals(a.chars1, b.chars1, error_msg);
		Assertions.assertEquals(a.chars2, b.chars2, error_msg);
		Assertions.assertEquals(a.lineArray, b.lineArray, error_msg);
	}

	// Construct the two texts which made up the diff originally.
	private static String[] diff_rebuildtexts(LinkedList<Diff> diffs) {
		String[] text = { "", "" };
		for (Diff myDiff : diffs) {
			if (myDiff.operation != DiffMatchPatch.Operation.INSERT) {
				text[0] += myDiff.text;
			}
			if (myDiff.operation != DiffMatchPatch.Operation.DELETE) {
				text[1] += myDiff.text;
			}
		}
		return text;
	}

	// Private function for quickly building lists of diffs.
	private static LinkedList<Diff> diffList(Diff... diffs) {
		return new LinkedList<Diff>(Arrays.asList(diffs));
	}
}
