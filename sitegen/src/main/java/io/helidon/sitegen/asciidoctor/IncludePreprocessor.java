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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.regex.Matcher;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

/**
 * AsciiDoc preprocessor which pulls in and describes included text.
 *
 * The preprocessor uses the preprocessor reader to bring in the text indicated
 * by the normal include directive as in
 * <pre>
 * {@code
 * include::pathToFile.adoc-plus-attributes
 * }
 * </pre>
 * and describes it by inserting a comment of the form
 * <pre>
 * {@code
 * // _include::x-y:pathToFile.adoc-plus-attributes
 * }
 * </pre>
 * where x and y are line numbers within a block of the included text and the
 * rest of the line is as it was from the original include directive.
 * <p>
 * These "numbered include" comments appear immediately before included text
 * that falls outside a source block, and they appear in the preamble (between
 * the [source] and the "----") of the source block. In this way we can track
 * where the included data came from and where it is in the file while preventing
 * the comment itself from being rendered.
 */
public class IncludePreprocessor extends Preprocessor {

    /**
     * Converts an external AsciiDoc file to our initial intermediate form,
     * with include:: and // _include:: turned into bracketed includes.
     *
     * @param lines
     * @return
     */
    static List<String> addBeginAndEndIncludeComments(List<String> lines) {
        List<String> augmentedLines = new ArrayList<>();

        for (AtomicInteger lineNumber = new AtomicInteger(0); lineNumber.get() < lines.size();) {
            String line = lines.get(lineNumber.get());
            if (line.startsWith("include::")) {
                augmentedLines.addAll(handleADocInclude(lines, lineNumber));
            } else if (line.startsWith("[source")) {
                augmentedLines.addAll(handleSourceBlock(lines, lineNumber));
            } else if (Include.isIncludeNumbered(line)) {
                augmentedLines.addAll(handleNumberedInclude(lines, lineNumber));
            } else {
                augmentedLines.add(line);
                lineNumber.getAndIncrement();
            }
        }
        return augmentedLines;
    }


    static List<String> convertBracketedToNumberedIncludes(List<String> content) {
        List<String> result = new ArrayList<>();

        AtomicInteger lineNumber = new AtomicInteger(0);
        while (lineNumber.get() < content.size()) {
            String line = content.get(lineNumber.get());
            if (Include.isIncludeStart(line)) {
                Include ia = Include.consumeBracketedInclude(
                        content, lineNumber, result, result.size());
                result.add(ia.asNumberedAsciiDocInclude());
                result.addAll(ia.body());
            } else if (SourceBlock.isSourceStart(line)) {
                SourceBlock sba = SourceBlock.consumeSourceBlock(content, lineNumber);
                result.addAll(sba.asNumberedSourceBlock());
            } else {
                result.add(line);
                lineNumber.getAndIncrement();
            }
        }
        return result;
    }

    @Override
    public void process(Document doc, PreprocessorReader reader) {
        markIncludes(reader.lines(), doc, reader);
    }

    private void markIncludes(List<String> origLines, Document doc, PreprocessorReader reader) {

        /*
         * Read from the raw input; temporarily we need to suppress include processing
         * because we need to see the include directives ourselves.
         */
        List<String> updatedLines = addBeginAndEndIncludeComments(origLines);

        /*
         * Force the reader to consume the original input, then erase it by
         * restoring with an empty list. Add our augmented content as an
         * include which causes AsciiDoctorJ to process it (although ADJ will
         * not invoke this preprocessor again when it processes that
         * pseudo-included content).
         */
        reader.readLines();
        reader.restoreLines(Collections.emptyList());
        String updatedContent = updatedLines.stream()
                .collect(Collectors.joining(System.lineSeparator()));
        reader.push_include(updatedContent, null, null, 1, Collections.emptyMap());

        /*
         * Convert the bracketed include comments and included text to numbered
         * include comments and included text.
         */

        List<String> convertedLines = convertBracketedToNumberedIncludes(reader.readLines());
        reader.restoreLines(Collections.emptyList());
        String convertedContent = convertedLines.stream()
                .collect(Collectors.joining(System.lineSeparator()));
        reader.push_include(convertedContent, null, null, 1, Collections.emptyMap());
    }

    private static List<String> handleADocInclude(List<String> lines, AtomicInteger lineNumber) {
        List<String> result = new ArrayList<>();
        String line = lines.get(lineNumber.getAndIncrement());
        String includeTarget = line.substring("include::".length());
        result.add(Include.includeStart(includeTarget));
        result.add(line);
        result.add(Include.includeEnd(includeTarget));
        return result;
    }

    private static List<String> handleSourceBlock(List<String> lines, AtomicInteger lineNumber) {
        SourceBlock sba = SourceBlock.consumeSourceBlock(lines, lineNumber);
        return sba.asBracketedSourceBlock();
    }

    private static List<String> handleNumberedInclude(List<String> lines, AtomicInteger lineNumber) {
        Include ia = Include.fromNumberedInclude(lines, 0, lines.get(lineNumber.getAndIncrement()));

        // Skip over the previously-included text.
        lineNumber.addAndGet(ia.endWithinBlock() - ia.startWithinBlock() + 1);

        return ia.asBracketedAsciiDocInclude();
    }
}
