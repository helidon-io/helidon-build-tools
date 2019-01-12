/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.helidon.sitegen.asciidoctor;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;

import io.helidon.sitegen.Site;
import io.helidon.sitegen.SourcePathFilter;

import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.sitegen.TestHelper.SOURCE_DIR_PREFIX;
import static io.helidon.sitegen.TestHelper.assertRendering;
import static io.helidon.sitegen.TestHelper.getFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class IncludePreprocessorTest {

    public IncludePreprocessorTest() {
    }

    private static final File OUTPUT_DIR = getFile("target/basic-backend-test");

    @Test
    public void testBasic2() throws Exception {
        File sourcedir = getFile(SOURCE_DIR_PREFIX + "testbasic2");
        Site.builder()
                .pages(listOf(SourcePathFilter.builder()
                        .includes(listOf("**/*.adoc"))
                        .excludes(listOf("**/_*"))
                        .build()))
                .build()
                .generate(sourcedir, OUTPUT_DIR);
        assertRendering(
                OUTPUT_DIR,
                new File(sourcedir, "_expected.ftl"),
                new File(OUTPUT_DIR, "example-manual.html"));
    }

    @Test
    public void testSourceBlockHandling() {
        String includedContentText =
                "public class A {\n" +
                "  public void hi() {\n" +
                "    System.out.println(\"Hi!\")\n" +
                "  }\n" +
                "}\n";

        String contentText = "[source]\n.Title for the block\nOther preamble\n" +
                "----\n" +
                "not-included\n" +
                "// _include-start::A.java\n" +
                includedContentText +
                "// _include-end::A.java\n" +
                "public class B {\n" +
                "  public void bye() {\n" +
                "    System.out.println(\"Bye!\");\n" +
                "  }\n" +
                "}\n" +
                "----";

        List<Include> expectedIncludes = new ArrayList<>();

        List<String> content = asList(contentText);
        expectedIncludes.add(new Include(2, 1, 5, asList(includedContentText), "A.java"));

        AtomicInteger lineNumber = new AtomicInteger(0);
        SourceBlock sba = SourceBlock.consumeSourceBlock(content, lineNumber);
        List<Include> includes = sba.sourceIncludes();

        assertEquals(content.size(), lineNumber.get(), "returned line number did not match");
        assertEquals(expectedIncludes, includes);

    }

    @Test
    public void testIncludeAnalysisFromNumberedInclude() {
        String expectedIncludeTarget = "randomStuff.adoc";
        List<String> includedContent = asList(
                "inc line 1\n" +
                "inc line 2\n" +
                "inc line 3\n");
        List<String> content = new ArrayList<>();
        content.add("before start of block");
        content.add("line 1");
        content.addAll(includedContent);
        content.addAll(asList(
                "line 2\n" +
                "line 3\n"));
        int expectedStartWithinBlock = 0;
        int expectedEndWithinBlock = 2;

        Include ia = Include.fromNumberedInclude(content, 2,
                String.format("// _include::%d-%d:%s", expectedStartWithinBlock, expectedEndWithinBlock, expectedIncludeTarget));
        assertEquals(expectedStartWithinBlock, ia.startWithinBlock(), "unexpected starting line number");
        assertEquals(expectedEndWithinBlock, ia.endWithinBlock(), "unexpected ending line number");
        assertEquals(includedContent, ia.body(), "unexpected body");
        assertEquals(expectedIncludeTarget, ia.includeTarget(), "unexpected include target");
    }

    @Test
    public void testIncludeAnalysisFromBracketedInclude() {
        String expectedIncludeTarget = "randomStuff.adoc";
        List<String> includedContent = asList(
                "inc 1\n" +
                "inc 2\n" +
                "inc 3\n");
        List<String> content = new ArrayList<>();
        content.add("before start of block");
        content.add("line 1");
        content.add("// _include-start::" + expectedIncludeTarget);
        content.addAll(includedContent);
        content.add("// _include-end::" + expectedIncludeTarget);
        content.addAll(asList(
                "line 2\n" +
                "line 3\n"));
        int expectedStartWithinBlock = 0;
        int expectedEndWithinBlock = 2;
        int expectedFinalLineAfterConsuming = 7;

        AtomicInteger lineNumber = new AtomicInteger(2);
        List<String> result = new ArrayList<>();
        Include ia = Include.consumeBracketedInclude(content, lineNumber, result, result.size());

        assertEquals(expectedStartWithinBlock, ia.startWithinBlock(), "unexpected start within block");
        assertEquals(expectedEndWithinBlock, ia.endWithinBlock(), "unexpected end within block");
        assertEquals(includedContent, ia.body(), "unexpected body");
        assertEquals(expectedIncludeTarget, ia.includeTarget(), "unexpected include target");
        assertEquals(expectedFinalLineAfterConsuming, lineNumber.get(), "unexpected final line number");
    }

    @Test
    public void testOverallConversion() throws DiffException {
        /*
         * Create content containing begin-end bracketed includes, both in and
         * outside [source] blocks.
         */
        String include1Target = "aFewLines.adoc";
        List<String> include1Body = asList(
            "inc 1\n" +
            "inc 2\n" +
            "inc 3\n");

        String srcInclude1Target = "Include1.adoc";
        List<String> srcInclude1Body = asList(
            "src inc 1.1\n" +
            "src inc 1.2\n" +
            "src inc 1.3\n" +
            "src inc 1.4\n");

        String srcInclude2Target = "Include2.adoc";
        List<String> srcInclude2Body = asList(
            "src inc 2.1\n" +
            "src inc 2.2\n" +
            "src inc 2.3\n" +
            "src inc 2.4\n");

        String srcInclude3Target = "Include3.adoc";
        List<String> srcInclude3Body = asList(
            "src inc 3.1\n" +
            "src inc 3.2\n");

        List<String> src1Body = new ArrayList<>();
        src1Body.add("// This is un-included source");
        src1Body.add("// _include-start::" + srcInclude1Target);
        src1Body.addAll(srcInclude1Body);
        src1Body.add("// _include-end::" + srcInclude1Target);

        src1Body.add("More un-included source");

        src1Body.add("// _include-start::" + srcInclude2Target);
        src1Body.addAll(srcInclude2Body);
        src1Body.add("// _include-end::" + srcInclude2Target);

        // Note - no intervening non-includec source here. Test two adjacent includes.

        src1Body.add("// _include-start::" + srcInclude3Target);
        src1Body.addAll(srcInclude3Body);
        src1Body.add("// _include-end::" + srcInclude3Target);

        // Build what we expect to see after includes have been converted to
        // numbered include comments.
        List<String> src1BodyNumbered = new ArrayList<>();
        List<String> src1NumberedIncludes = new ArrayList<>();

        src1BodyNumbered.add("// This is un-included source");
        src1NumberedIncludes.add("// _include::1-4:" + srcInclude1Target);
        src1BodyNumbered.addAll(srcInclude1Body);

        src1BodyNumbered.add("More un-included source");

        src1NumberedIncludes.add("// _include::6-9:" + srcInclude2Target);
        src1BodyNumbered.addAll(srcInclude2Body);

        src1NumberedIncludes.add("// _include::10-11:" + srcInclude3Target);
        src1BodyNumbered.addAll(srcInclude3Body);

        List<String> src1 = new ArrayList<>();
        src1.addAll(asList(
            "[source]\n" +
            ".Title for the source\n" +
            "----\n" +
            ""));
        src1.addAll(src1Body);
        src1.add("----");

        List<String> src1Numbered = new ArrayList<>();
        src1Numbered.addAll(asList(
            "[source]\n" +
            ".Title for the source\n"));
        src1Numbered.addAll(src1NumberedIncludes);
        src1Numbered.add("----");
        src1Numbered.addAll(src1BodyNumbered);
        src1Numbered.add("----");

        // Now build the entire content.

        List<String> contentBeginning = asList(
            "Beginning of document\n" +
            "This is some useful information before any includes.");

        List<String> content = new ArrayList<>();
        content.addAll(contentBeginning);
        content.add("// _include-start::" + include1Target);
        content.addAll(include1Body);
        content.add("// _include-end::" + include1Target);

        content.add("");

        content.addAll(src1);

        List<String> expectedNumberedContent = new ArrayList<>();
        expectedNumberedContent.addAll(contentBeginning);
        expectedNumberedContent.add("// _include::0-2:" + include1Target);
        expectedNumberedContent.addAll(include1Body);

        expectedNumberedContent.add("");

        expectedNumberedContent.addAll(src1Numbered);


        List<String> numberedContent = IncludePreprocessor.convertBracketedToNumberedIncludes(content);

        assertEquals(expectedNumberedContent, numberedContent, "overall resulting content did not match; " +
                DiffUtils.diff(expectedNumberedContent, numberedContent));
    }

    @Test
    public void testInitialProcessing() throws IOException, DiffException, URISyntaxException {
        String testFileDir = "preprocess-adoc";
        Path originalPath = Paths.get(testFileDir, "variousIncludes.adoc");
        List<String> originalLines = loadFromPath(originalPath);

        Path afterInitialPreprocessingPath = Paths.get(testFileDir,
                "variousIncludes-afterInitialPreprocessing.adoc");
        List<String> expectedAfterInitialPreprocessingLines =
                loadFromPath(afterInitialPreprocessingPath);

        List<String> actualAfterInitialPreprocessingLines =
                IncludePreprocessor.addBeginAndEndIncludeComments(originalLines);

        assertEquals(expectedAfterInitialPreprocessingLines,
                actualAfterInitialPreprocessingLines,
                DiffUtils.diff(expectedAfterInitialPreprocessingLines,
                        actualAfterInitialPreprocessingLines).toString());
    }

    private List<String> loadFromPath(Path path) throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource(path.toString());
        List<String> result = new ArrayList<>();
        boolean inCommentBlock = false;
        for (String line : Files.readAllLines(Paths.get(url.toURI()))) {
            if (line.startsWith("////")) {
                inCommentBlock = !inCommentBlock;
            } else if (!inCommentBlock) {
                result.add(line);
            }
        }
        return result;
    }

    @Test
    public void testSourceBlockIncludeBracketing() throws DiffException {
        List<String> orig = asList(
                  "[source]\n"
                + ".Title of the source block\n"
                + "// _include::1-3:a.adoc\n"
                + "// _include::5-7:b.adoc\n"
                + "----\n"
                + "Not included\n"
                + "inc 1.1\n"
                + "inc 1.2\n"
                + "inc 1.3\n"
                + "Also not included\n"
                + "inc 2.1\n"
                + "inc 2.2\n"
                + "inc 2.3\n"
                + "Other not included\n"
                + "----"
        );

        List<String> expectedBracketed = asList(
                  "[source]\n"
                + ".Title of the source block\n"
                + "----\n"
                + "Not included\n"
                + "// _include-start::a.adoc\n"
                + "include::a.adoc\n"
                + "// _include-end::a.adoc\n"
                + "Also not included\n"
                + "// _include-start::b.adoc\n"
                + "include::b.adoc\n"
                + "// _include-end::b.adoc\n"
                + "Other not included\n"
                + "----"
        );

        AtomicInteger lineNumber = new AtomicInteger(0);
        SourceBlock sba = SourceBlock.consumeSourceBlock(orig, lineNumber);

        List<String> bracketed = sba.asBracketedSourceBlock();
        assertEquals(expectedBracketed, bracketed,
                DiffUtils.diff(orig, bracketed).toString());
    }

    private static List<String> asList(String text) {
        return Arrays.asList(text.split("\n"));
    }
}
