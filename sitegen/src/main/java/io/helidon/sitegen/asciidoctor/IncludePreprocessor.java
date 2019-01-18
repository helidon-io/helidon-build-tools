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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
 * </pre> and describes it by inserting a comment of the form
 * <pre>
 * {@code
 * // _include::x-y:pathToFile.adoc-plus-attributes
 * }
 * </pre> where x and y are line numbers within a block of the included text and
 * the rest of the line is as it was from the original include directive.
 * <p>
 * These "numbered include" comments appear immediately before included text
 * that falls outside a block, and they appear in the preamble (between the
 * introducer such as [source] and the first delimiter such as "----") of the
 * block. In this way we can track where the included data came from and where
 * it is in the file while preventing the comment itself from being rendered.
 */
public class IncludePreprocessor extends Preprocessor {

    /**
     * Converts an external AsciiDoc file to our initial intermediate form, with
     * include:: and // _include:: turned into bracketed includes.
     *
     * @param lines possibly hybrid format containing Asciidoc {@code include::}
     * and our numbered commented includes
     * @return lines with beginning and ending comments bracketing the includes
     */
    static List<String> convertHybridToBracketed(List<String> lines) {
        List<String> augmentedLines = new ArrayList<>();

        for (AtomicInteger lineNumber = new AtomicInteger(0); lineNumber.get() < lines.size();) {
            String line = lines.get(lineNumber.get());
            if (line.startsWith("include::")) {
                augmentedLines.addAll(handleADocInclude(lines, lineNumber));
            } else if (Block.isBlockStart(line)) {
                augmentedLines.addAll(handleBlock(lines, lineNumber));
            } else if (Include.isIncludeNumbered(line)) {
                augmentedLines.addAll(handleNumberedInclude(lines, lineNumber));
            } else {
                augmentedLines.add(line);
                lineNumber.getAndIncrement();
            }
        }
        return augmentedLines;
    }

    /**
     * Converts the bracketed form to the natural form (with normal AsciiDoc
     * {@code include::} directories.
     *
     * @param lines bracketed form content
     * @return natural form content
     */
    static List<String> convertBracketedToNatural(List<String> lines) {
        List<String> result = new ArrayList<>();

        for (AtomicInteger lineNumber = new AtomicInteger(0); lineNumber.get() < lines.size();) {
            String line = lines.get(lineNumber.get());
            if (Include.isIncludeStart(line)) {
                result.addAll(bracketedIncludeToNatural(lines, lineNumber));
            } else {
                result.add(line);
                lineNumber.getAndIncrement();
            }
        }

        return result;
    }

    /**
     * Changes the provided bracketed form into the same content in numbered
     * form.
     *
     * @param content bracketed form of the content
     * @return content converted to numbered form
     */
    static List<String> convertBracketedToNumbered(List<String> content) {
        List<String> result = new ArrayList<>();

        AtomicInteger lineNumber = new AtomicInteger(0);
        while (lineNumber.get() < content.size()) {
            String line = content.get(lineNumber.get());
            if (Include.isIncludeStart(line)) {
                Include ia = Include.consumeBracketedInclude(
                        content, lineNumber, result, result.size());
                result.add(ia.asNumberedAsciiDocInclude());
                result.addAll(ia.body());
            } else if (Block.isBlockStart(line)) {
                Block sba = Block.consumeBlock(content, lineNumber);
                result.addAll(sba.asBlockWithNumberedIncludes());
            } else {
                result.add(line);
                lineNumber.getAndIncrement();
            }
        }
        return result;
    }

    @Override
    public void process(Document doc, PreprocessorReader reader) {
        OutputType outputType = OutputType.match(doc.getOptions().get("preincludeOutputType"));
        if (outputType == null) {
            return;
        }
        List<String> processedContent = markIncludes(reader, outputType);
        savePreincludedDocIfRequested(processedContent, doc);
    }

     enum OutputType {
        PREPROCESSED,
        NATURAL;

        static OutputType match(Object outputType) {
            Optional<OutputType> match = Arrays.stream(OutputType.values())
                    .filter(ot -> ot.toString().toLowerCase().equals(outputType))
                    .findFirst();
            if (!match.isPresent()) {
                return null;
            }
            return match.get();
        }
    }

    private List<String> markIncludes(PreprocessorReader reader, OutputType outputType) {

        /*
         * Use the raw input. Do not use reader.readLines() yet because that
         * would trigger {@code include::} handling which needs to happen below.
         */
        List<String> origWithBracketedIncludes = convertHybridToBracketed(reader.lines());

        /*
         * Replace the reader's content with the bracketed format.
         */
        readAndClearReader(reader);
        String origWithBracketedIncludesContent = linesToString(origWithBracketedIncludes);
        reader.push_include(origWithBracketedIncludesContent, null, null, 1, Collections.emptyMap());

        /*
         * Have the reader consume the bracketed-include content which will
         * insert the included text between the bracketing comments.
         */
        List<String> bracketedIncludesWithIncludedText = readAndClearReader(reader);

        /*
         * With the actual included text between the bracketing comments convert
         * that to the numbered format which is ultimate what we want.
         */
        List<String> numberedIncludesWithIncludedText
                = convertBracketedToNumbered(bracketedIncludesWithIncludedText);

        /*
         * Prepare the reader to consume the numbered format just computed.
         */
        String numberedIncludesWithIncludedTextContent = linesToString(numberedIncludesWithIncludedText);
        reader.push_include(
                numberedIncludesWithIncludedTextContent,
                null,
                null,
                1,
                Collections.emptyMap());

        switch (outputType) {
            case PREPROCESSED:
                return numberedIncludesWithIncludedText;

            case NATURAL:
                return convertBracketedToNatural(bracketedIncludesWithIncludedText);

            default:
                throw new IllegalArgumentException(
                        String.format("outputType %s is not one of %s",
                            outputType == null ? "null" : outputType.toString().toLowerCase(),
                            Arrays.toString(OutputType.values())));
        }
    }

    private static List<String> readAndClearReader(PreprocessorReader reader) {
        List<String> result = reader.readLines();
        reader.restoreLines(Collections.emptyList());
        return result;
    }

    private static String linesToString(List<String> lines) {
        return lines.stream()
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private void savePreincludedDocIfRequested(List<String> content, Document doc) {
        Path outputPath = Path.class.cast(doc.getOptions().get("preincludeOutputPath"));
        if (outputPath != null) {
            try {
                Files.createDirectories(outputPath.getParent());
                Files.write(outputPath, content);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
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

    private static List<String> handleBlock(List<String> lines, AtomicInteger lineNumber) {
        Block sba = Block.consumeBlock(lines, lineNumber);
        return sba.asBracketedBlock();
    }

    private static List<String> handleNumberedInclude(List<String> lines, AtomicInteger lineNumber) {
        Include ia = Include.fromNumberedInclude(lines, 0, lines.get(lineNumber.getAndIncrement()));

        // Skip over the previously-included text.
        lineNumber.addAndGet(ia.endWithinBlock() - ia.startWithinBlock() + 1);

        return ia.asBracketedAsciiDocInclude();
    }

    private static List<String> bracketedIncludeToNatural(List<String> lines, AtomicInteger lineNumber) {
        List<String> result = new ArrayList<>();
        Include inc = Include.consumeBracketedInclude(lines, lineNumber, result, 0);
        result.addAll(inc.asAsciiDocInclude());
        return result;
    }
}
