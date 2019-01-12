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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;

/**
 * Describes content that is a {@code [source]} block that might contain
 * includes.
 * <p>
 * <h1>Overview</h1>
 * For our purposes a source block contains
 * <ul>
 * <li>its declaration ({@code [source]} with, potentially, additional
 * attributes),
 * <li>any includes that contribute to the body of the source block, and
 * <li>the body itself.
 * </ul>
 * <h1>Text Formatting</h1>
 * At different times we represent a source block as text in three ways.
 * <h2>Brief Bracketed Form (internal)</h2>
 * In this form we bracket each AsciiDoc {@code include::} directive in the body
 * with a preceding {@code // _include-start::} and a following
 * {@code // _include-end::} comment. Our preprocessing submits this form to
 * AsciiDoc in a way that creates the full bracketed form.
 * <h2>Full Bracketed Form (internal)</h2>
 * In this format the bracketing comments surround the included text itself
 * rather than the {@code include::} AsciiDoc directive.
 * <h2>Numbered Form</h2>
 * In this format:
 * <ul>
 * <li>The preamble contains an AsciiDoc comment for each AsciiDoc
 * {@code include::} that originally appeared in the body. Each such comment
 * indicates the origin of the include and the line numbers within the source
 * block's body where the corresponding included content resides.
 * <li>The body contains the included content in the correct positions.
 * </ul>
 * For example, given an original AsciiDoc source block like this:
 * <pre>
 * {@code
 * [source]
 * .Title for the source
 * ----
 * // This is un-included source
 * include::Include1.adoc
 * More un-included source
 * include::Include2.adoc
 * Further content
 * ----
 * }
 * </pre> here is the corresponding numbered form:
 * <pre>
 * {@code
 * [source]
 * .Title for the source
 * // _include::1-4:Include1.adoc
 * // _include::6-9:Include2.adoc
 * ----
 * // This is un-included source
 * src inc 1.1
 * src inc 1.2
 * src inc 1.3
 * src inc 1.4
 * More un-included source
 * src inc 2.1
 * src inc 2.2
 * src inc 2.3
 * src inc 2.4
 * Further content
 * ----
 * }
 * </pre>
 * <h2>Creating {@code SourceBlock} Instances by Parsing AsciiDoc</h2>
 * The {@link #consumeSourceBlock} method reads content and builds an instance
 * representing the corresponding source block, advancing the current line
 * indicator for the content as it does so.
 * <p>
 * Note that this method handles the original AsciiDoc form (with
 * {@code include::} directives in the body), pure numbered format, and a hybrid
 * in which the user has added AsciiDoc {@code include::} directives in the body
 * of the numbered form.
 */
public class SourceBlock {

    private static final String BLOCK_DELIMITER = "----";

    /**
     * Creates a SourceBlock by consuming input text from the AsciiDoc
     * preprocessor reader.
     *
     * @param content lines containing AsciiDoc
     * @param lineNumber line number at which to begin processing the source
     * block
     * @return a new SourceBlock describing the source block
     */
    static SourceBlock consumeSourceBlock(List<String> content, AtomicInteger lineNumber) {
        SourceBlock sb = new SourceBlock();
        sb.prepare(content, lineNumber);
        return sb;
    }

    /**
     * Returns whether the specified line represents the start of a source
     * block.
     *
     * @param line the line to check
     * @return true if the line starts a source block; false otherwise
     */
    static boolean isSourceStart(String line) {
        return line.startsWith("[source");
    }

    private static boolean isBlock(String line) {
        return line.equals(BLOCK_DELIMITER);
    }

    private String sourceBlockDecl = null;
    private final List<Include> includes = new ArrayList<>();
    private final List<String> body = new ArrayList<>();

    private List<String> preamble = new ArrayList<>();

    /**
     * Formats the source block using numbered include comments in the preamble
     * and the actual inserted text in the body.
     *
     * @return source block with (if needed) numbered include comments in the
     * preamble
     */
    List<String> asNumberedSourceBlock() {
        return asSourceBlock(() -> {
            List<String> result = new ArrayList<>();
            includes.forEach((ia) -> {
                result.add(ia.asNumberedAsciiDocInclude());
            });
            return result;
        },
                () -> {
                    return body();
                });

    }

    /**
     * Formats the source block with no special include-related comments and no
     * pre-included content, using instead what were (or could have been)
     * original AsciiDoc {@code include::} directives.
     *
     * @return source block formatted as normal AsciiDoc
     */
    List<String> asOriginalSourceBlock() {
        return asSourceBlock(
                () -> {
                    return originalBody();
                });
    }

    /**
     * Formats the source block with no numbering, with each include bracketed
     * with special include-related comments.
     *
     * @return source block formatted using bracketed includes
     */
    List<String> asBracketedSourceBlock() {
        return asSourceBlock(
                () -> {
                    return bracketedBody();
                });
    }

    private List<String> asSourceBlock(
            Supplier<List<String>> bodyGenerator) {
        return asSourceBlock(
                () -> {
                    return Collections.emptyList();
                },
                bodyGenerator);
    }

    private List<String> asSourceBlock(
            Supplier<List<String>> preambleCommentsGenerator,
            Supplier<List<String>> bodyGenerator) {
        List<String> result = new ArrayList<>();
        result.add(sourceBlockDecl);
        result.addAll(preamble);
        result.addAll(preambleCommentsGenerator.get());
        result.add(BLOCK_DELIMITER);
        result.addAll(bodyGenerator.get());
        result.add(BLOCK_DELIMITER);
        return result;
    }

    private List<String> body() {
        return body;
    }

    private List<String> originalBody() {
        List<String> result = new ArrayList<>(body);

        /*
         * Replace the actual included text in the body of the source block with
         * AsciiDoc includes. (Below we add brackets to all includes, which will
         * include the ones we add now and any new ones the user might have
         * added to the original .adoc file.)
         *
         * We work on the includes from bottom to top so we don't disrupt the
         * line number references from the numbered includes into the body.
         */
        for (int i = includes.size() - 1; i >= 0; i--) {
            Include ia = includes.get(i);
            for (int j = ia.startWithinBlock(); j <= ia.endWithinBlock(); j++) {
                result.remove(ia.startWithinBlock());
            }
            if (ia.startWithinBlock() >= result.size()) {
                result.addAll(ia.asAsciiDocInclude());
            } else {
                result.addAll(ia.startWithinBlock(), ia.asAsciiDocInclude());
            }
        }
        return result;
    }

    private List<String> bracketedBody() {
        List<String> result = new ArrayList<>();
        Matcher m = Include.ASCIIDOC_INCLUDE_PATTERN.matcher("");

        for (String bodyLine : originalBody()) {
            m.reset(bodyLine);
            if (m.matches()) {
                result.add(String.format(Include.INCLUDE_BRACKET_TEMPLATE,
                        "start",
                        m.group(1)));
                result.add(bodyLine);
                result.add(String.format(Include.INCLUDE_BRACKET_TEMPLATE,
                        "end",
                        m.group(1)));
            } else {
                result.add(bodyLine);
            }
        }
        return result;
    }

    /**
     *
     * @return IncludeAnalyzers for any includes processed in the source block
     */
    List<Include> sourceIncludes() {
        return includes;
    }

    private void prepare(List<String> content, AtomicInteger aLineNumber) {
        sourceBlockDecl = content.get(aLineNumber.getAndIncrement());

        /*
         * The "preamble" is any text after the [source] line and before the
         * first "----" marking the beginning of the body.
         */
        preamble = collectPreamble(content, aLineNumber);
        int blockStartLineNumber = body.size();

        doUntilBlockDelimiter(content, aLineNumber, line -> {
            if (line.startsWith(Include.INCLUDE_START_COMMENT_PREFIX)) {
                aLineNumber.decrementAndGet();
                Include ia = Include.consumeBracketedInclude(
                        content,
                        aLineNumber,
                        body,
                        blockStartLineNumber);
                includes.add(ia);
                body.addAll(ia.body());
            } else {
                body.add(line);
            }
        });
    }

    private List<String> collectPreamble(List<String> content, AtomicInteger lineNumber) {
        final List<String> result = new ArrayList<>();
        final List<String> pendingIncludes = new ArrayList<>();
        final Matcher m = Include.INCLUDE_NUMBERED_PATTERN.matcher("");
        doUntilBlockDelimiter(content, lineNumber, line -> {
            m.reset(line);
            if (m.matches()) {
                pendingIncludes.add(line);
            } else {
                result.add(line);
            }
        });
        final int startOfBlock = lineNumber.get();
        pendingIncludes.forEach((pendingInclude) -> {
            includes.add(Include.fromNumberedInclude(
                    content, startOfBlock, pendingInclude));
        });
        return result;
    }

    private void doUntilBlockDelimiter(List<String> content, AtomicInteger lineNumber, Consumer<String> lineConsumer) {
        do {
            String line = content.get(lineNumber.getAndIncrement());
            if (isBlock(line)) {
                break;
            }
            lineConsumer.accept(line);
        } while (true);
    }

}
