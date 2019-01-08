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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

/**
 *
 */
public class IncludePreprocessor extends Preprocessor {

    static final String INCLUDE_START = "_include-start";
    static final String INCLUDE_END = "_include-end";

    @Override
    public void process(Document doc, PreprocessorReader reader) {
        multiPass(doc, reader);
    }

    private void multiPass(Document doc, PreprocessorReader reader) {
        List<String> origLines = reader.lines();
        markIncludes(origLines, doc, reader);
    }

    private void markIncludes(List<String> origLines, Document doc, PreprocessorReader reader) {

        doc.setAttribute("includes", "yes", true);

        // Read from the raw input; temporarily we need to suppress include processing
        // because we need to see the include directives ourselves.
        List<String> updatedLines = addBeginAndEndIncludeComments(origLines);

        // Force the reader to consume the original input, then erase it by
        // restoring using an empty list. Add our augmented content as an
        // include which causes AsciiDoctorJ to process it (although ADJ will
        // not invoke this preprocessor again when it processes that
        // pseudo-included content).
        reader.readLines();
        reader.restoreLines(Collections.emptyList());
        String updatedContent = updatedLines.stream()
                .collect(Collectors.joining(System.lineSeparator()));
        reader.push_include(updatedContent, null, null, 1, Collections.emptyMap());
    }

    private void reorganizeIncludesInSourceBlocks(List<String> origLines, Document doc, PreprocessorReader reader) {
    }

    static List<String> addBeginAndEndIncludeComments(List<String> lines) {
        List<String> augmentedLines = new ArrayList<>();

        boolean isDiscardingOldIncludedContent = false;

        for (String line : lines) {
            if (line.startsWith("include::")) {
                String includeTarget = line.substring("include::".length());
                augmentedLines.add(includeStart(includeTarget));
                augmentedLines.add(line);
                augmentedLines.add(includeEnd(includeTarget));
            } else if (isLineIncludeStart(line)) {
                augmentedLines.add(line);
                isDiscardingOldIncludedContent = true;
                augmentedLines.add(include(targetFromIncludeStart(line)));
            } else if (isLineIncludeEnd(line)) {
                augmentedLines.add(line);
                isDiscardingOldIncludedContent = false;
            } else if ( ! isDiscardingOldIncludedContent) {
                augmentedLines.add(line);
            }
        }
        return augmentedLines;
    }

    static String includeStart(String includeTarget) {
        return String.format("// %s %s", INCLUDE_START, includeTarget);
    }

    static String includeEnd(String includeTarget) {
        return String.format("// %s %s", INCLUDE_END, includeTarget);
    }

    static String include(String includeTarget) {
        return String.format("include::" + includeTarget);
    }

    private static boolean isLineIncludeStart(String line) {
        return line.startsWith("// " + INCLUDE_START);
    }

    private static boolean isLineIncludeEnd(String line) {
        return line.startsWith("// " + INCLUDE_END);
    }

    private static String targetFromIncludeStart(String line) {
        return line.substring(("// " + INCLUDE_START + " ").length());
    }
}
