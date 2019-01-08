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

import io.helidon.sitegen.Site;
import io.helidon.sitegen.SourcePathFilter;
import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.sitegen.asciidoctor.IncludePreprocessor.addBeginAndEndIncludeComments;
import static io.helidon.sitegen.asciidoctor.IncludePreprocessor.include;
import static io.helidon.sitegen.asciidoctor.IncludePreprocessor.includeEnd;
import static io.helidon.sitegen.asciidoctor.IncludePreprocessor.includeStart;
import static io.helidon.sitegen.TestHelper.SOURCE_DIR_PREFIX;
import static io.helidon.sitegen.TestHelper.assertRendering;
import static io.helidon.sitegen.TestHelper.getFile;
import java.io.File;
import java.util.Arrays;
import java.util.List;
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
    public void testIncludeCommentDiscard() {
        String includeTarget = "MyFirstInclude.java";
        String part1 = "line 1\nline 2\n//random comment";
        String part2 = "and that's how include works.";
        String includedCode = "public class IncludedClass {\n" +
                "// this comment should remain\n" +
                "private static final String HI = \"hi\";\n" +
                "}";
        String commentedText = part1 + "\n" +
                includeStart(includeTarget) + "\n" +
                includedCode + "\n" +
                includeEnd(includeTarget) + "\n" +
                part2;

        String expectedProcessedText = part1 + "\n" +
                includeStart(includeTarget) + "\n" +
                include(includeTarget) + "\n" +
                includeEnd(includeTarget) + "\n" +
                part2;

        List<String> commentedTextAsList = asList(commentedText);
        List<String> expectedProcessedTextAsList = asList(expectedProcessedText);

        List<String> processedText = addBeginAndEndIncludeComments(commentedTextAsList);

        assertEquals(expectedProcessedTextAsList,
                processedText,
                "mismatch in pre-commented included text");
    }

    private static List<String> asList(String text) {
        return Arrays.asList(text.split("\n"));
    }
}
