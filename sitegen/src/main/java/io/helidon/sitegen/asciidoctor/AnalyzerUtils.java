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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities supporting include analysis and preprocessing of AsciiDoc files.
 */
public class AnalyzerUtils {

    /**
     * Describes included content.
     *
     * For the purposes of preprocessing the AsciiDoc, we treat all included
     * content as residing within a block, whether an actual block (such as the
     * body of a [source] block) or a virtual or synthetic block composed of the
     * lines immediately following a free-standing (i.e., not in a [source]
     * block) include.
     *
     * Each include is represented by:
     * <ul>
     * <li>the starting and ending
     * line numbers within the block of interest where the included text will reside
     * <li>the include target (path and
     * other information from the original include::)
     * <li>the included text itself.
     * </ul>
     *
     * The starting and ending line numbers exclude any bracketing comment lines
     * which might mark the included text.
     * <p>
     * The "content block of interest" is either the body of a [source] block
     * (the region between the ---- delimiters) if the include appears within
     * one, or is a fictitious (synthetic) block that starts immediately after
     * the _include-start marker.
     * <p>
     * We use an intermediate form of includes during preprocessing. This
     * <pre>
     * {@code
     * include::somePath.adoc
     * }
     * </pre>
     * appears in the intermediate form as
     * <pre>
     * {@code
     * // _include-start::somePath.adoc
     * ...
     * included text
     * ...
     * // _include-end::somePath.adoc
     * }
     * </pre>
     * and then eventually as
     * <pre>
     * {@code
     * // _include::0-n:somePath.adoc
     * ...
     * n+1 lines of included text
     * ...
     * }
     * </pre>
     * in the final rendered output.
     */
    static class IncludeAnalyzer {

        private static final String INCLUDE_PREFIX = "_include";

        /* matches a bracketing include comment (start or end) */
        static final Pattern INCLUDE_BRACKET_PATTERN =
            Pattern.compile("// " + INCLUDE_PREFIX + "-" + "(start|end)::(.*)");

        /* matches a numbered include comment */
        private static final Pattern INCLUDE_NUMBERED_PATTERN =
                Pattern.compile("// " + INCLUDE_PREFIX + "::(\\d*)-(\\d*):(.*)");

        static final String INCLUDE_NUMBERED_TEMPLATE = "// " + INCLUDE_PREFIX + "::%d-%d:%s";
        static final String INCLUDE_BRACKET_TEMPLATE = "// " + INCLUDE_PREFIX + "-%s::%s";
        static final String INCLUDE_START = INCLUDE_PREFIX + "-start";
        static final String INCLUDE_END = INCLUDE_PREFIX + "-end";
        static final String INCLUDE_START_COMMENT_PREFIX = "// " + INCLUDE_START;
        static final Pattern ASCIIDOC_INCLUDE_PATTERN = Pattern.compile("include::(.*)");

        /**
         * Returns whether the line is an _include-start line.
         *
         * @param line the line to check
         * @return whether or not the line is an _include-start line
         */
        static boolean isLineIncludeStart(String line) {
            return line.startsWith("// " + INCLUDE_START);
        }

        /**
         * Returns whether the line is an _include-end line.
         *
         * @param line the line to check
         * @return whether or not the line is an _include-end line
         */
        static boolean isLineIncludeEnd(String line) {
            return line.startsWith("// " + INCLUDE_END);
        }

        /**
         * Extracts the include-relevant part of an _include-start line
         *
         * @param line the line to be examined
         * @return the include path and any modifiers from the line
         */
        static String targetFromIncludeStart(String line) {
            return line.substring(("// " + INCLUDE_START + "::").length());
        }

        static boolean isLineIncludeNumbered(String line) {
            return INCLUDE_NUMBERED_PATTERN.matcher(line).matches();
        }

        private final int startWithinBlock;
        private final int endWithinBlock;
        private final List<String> body;
        private final String includeTarget;

        /**
         * Creates a new instance given full information about the included content.
         *
         *
         * @param startOfBlock where, within the overall content, the block resides
         * @param startWithinBlock where, within the block, this include begins
         * @param endWithinBlock where, within the block, this include ends
         * @param body the included text
         * @param includeTarget the include path and any additional attributes
         */
        IncludeAnalyzer(int startOfBlock,
                int startWithinBlock,
                int endWithinBlock,
                List<String> body,
                String includeTarget) {
            this.startWithinBlock = startWithinBlock;
            this.endWithinBlock = endWithinBlock;
            this.includeTarget = includeTarget;
            this.body = body;
        }

        /**
         * Creates a new instance given the content and starting position within
         * it where the include content resides.
         * @param content content within which the included text resides
         * @param startOfBlock where, within the content, the block begins
         * @param startWithinBlock where, within the block, the included text begins
         * @param endWithinBlock where, within the bloc, the included text ends
         * @param includeTarget the include path and any additional attributes
         */
        IncludeAnalyzer(List<String> content,
                int startOfBlock,
                int startWithinBlock,
                int endWithinBlock,
                String includeTarget) {

            this(startOfBlock, startWithinBlock, endWithinBlock,
                    buildBody(content, startOfBlock, startWithinBlock, endWithinBlock),
                    includeTarget);
        }

        private static List<String> buildBody(List<String> content,
                int startOfBlock,
                int startWithinBlock,
                int endWithinBlock) {
            List<String> b = new ArrayList<>();
            for (int i = startWithinBlock; i <= endWithinBlock; i++) {
                b.add(content.get(startOfBlock + i));
            }
            return b;
        }

        /**
         * Creates a new IncludeAnalyzer from the specified content, given the
         * starting point of the block within that content and the numbered
         * include descriptor. The block (as indicated by the starting point)
         * does not include any include descriptor comment.
         *
         * @param content text containing the numbered include block
         * @param startOfBlock start of the block containing included content
         * @param numberedInclude descriptor for the include (start, end, target)
         */
        static IncludeAnalyzer fromNumberedInclude(
                List<String> content,
                int startOfBlock,
                String numberedInclude) {
            Matcher m = INCLUDE_NUMBERED_PATTERN.matcher(numberedInclude);
            if (!m.matches()) {
                throw new IllegalStateException(
                        "Expected numbered include but did not match expected pattern - " +
                                numberedInclude);
            }
            int startWithinBlock = Integer.parseInt(m.group(1));
            int endWithinBlock = Integer.parseInt(m.group(2));
            IncludeAnalyzer result = new IncludeAnalyzer(
                content,
                startOfBlock,
                startWithinBlock,
                endWithinBlock,
                m.group(3));

            return result;
        }

        /**
         * Parses a bracketed include block, starting at the specified line
         * within the content.
         *
         * @param content lines containing the bracketed include to be parsed
         * @param lineNumber starting point of the bracketed include
         * @param output line within which the result of consuming and translating the include will be stored
         * @oaran startOfOutputBlock where the included content itself begins in the output
         * @return an IncludeAnalyzer describing this include
         */
        static IncludeAnalyzer consumeBracketedInclude(List<String> content,
                AtomicInteger lineNumber,
                List<String> output,
                int startOfOutputBlock) {
            String line = content.get(lineNumber.get());
            Matcher m = INCLUDE_BRACKET_PATTERN.matcher(line);
            if (!(m.matches() && m.group(1).equals("start"))) {
                return null;
            }
            lineNumber.incrementAndGet();
            int startWithinBlock = output.size() - startOfOutputBlock;
            String includeTarget = m.group(2);
            boolean endFound;
            List<String> body = new ArrayList<>();
            do {
                line = content.get(lineNumber.getAndIncrement());
                m.reset(line);
                endFound = (m.matches() && m.group(1).equals("end"));
                if (!endFound) {
                    body.add(line);
                }
            } while (!endFound);
            int endWithinBlock = startWithinBlock + body.size() - 1;
            return new IncludeAnalyzer(
                    startOfOutputBlock,
                    startWithinBlock,
                    endWithinBlock,
                    body,
                    includeTarget);
        }

        /**
         * Format this included content as bracketed included lines.
         *
         * @return lines with an AsciiDoc include bracketed with start and end comments
         */
        List<String> asBracketedAsciiDocInclude() {
            List<String> result = new ArrayList<>();
            result.add(String.format(INCLUDE_BRACKET_TEMPLATE, "start", includeTarget));
            result.add("include::" + includeTarget);
            result.add(String.format(INCLUDE_BRACKET_TEMPLATE, "end", includeTarget));
            return result;
        }

        List<String> asAsciiDocInclude() {
            List<String> result = new ArrayList<>();
            result.add(String.format("include::%s", includeTarget));
            return result;
        }

        /**
         * Format the numbered descriptor for the included content.
         *
         * @return AsciiDoc comment describing the include using line numbers
         */
        String numberedDescriptor() {
            return String.format(INCLUDE_NUMBERED_TEMPLATE, startWithinBlock, endWithinBlock, includeTarget);
        }

        /**
         * Returns the text associated with this include.
         * @return the included content
         */
        List<String> body() {
            return body;
        }

        /**
         *
         * @return the starting position of the included text within the containing
         * block
         */
        int startWithinBlock() {
            return startWithinBlock;
        }

        /**
         *
         * @return the ending position of the included text within the containing
         * block
         */
        int endWithinBlock() {
            return endWithinBlock;
        }

        /**
         *
         * @return the include target (path and any additional attributes)
         */
        String includeTarget() {
            return includeTarget;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.startWithinBlock;
            hash = 37 * hash + this.endWithinBlock;
            hash = 37 * hash + Objects.hashCode(this.body);
            hash = 37 * hash + Objects.hashCode(this.includeTarget);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IncludeAnalyzer other = (IncludeAnalyzer) obj;
            if (this.startWithinBlock != other.startWithinBlock) {
                return false;
            }
            if (this.endWithinBlock != other.endWithinBlock) {
                return false;
            }
            if (!Objects.equals(this.includeTarget, other.includeTarget)) {
                return false;
            }
            if (!Objects.equals(this.body, other.body)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "IncludeAnalyzer{" + "startWithinBlock=" + startWithinBlock +
                    ", endWithinBlock=" + endWithinBlock + ", body=" + body +
                    ", includeTarget=" + includeTarget + '}';
        }


    }

    /**
     * Consumes and analyzes a portion of content that is a [source] block that
     * might contain bracketed includes.
     */
    static class SourceBlockAnalyzer {

        private static final String BLOCK_DELIMITER = "----";

        private String sourceBlockDecl = null;
        private final List<IncludeAnalyzer> includes = new ArrayList<>();
        private final List<String> body = new ArrayList<>();

        private List<String> preamble = new ArrayList<>();

        /**
         * Creates a SourceBlockAnalyzer by consuming input text from the
         * AsciiDoc preprocessor reader.
         *
         * @param content lines containing AsciiDoc
         * @param lineNumber line number at which to begin processing the source block
         * @return a new SourceBlockAnalyzer describing the source block
         */
        static SourceBlockAnalyzer consumeSourceBlock(List<String> content, AtomicInteger lineNumber) {

            SourceBlockAnalyzer sba = new SourceBlockAnalyzer();

            sba.run(content, lineNumber);

            return sba;
        }

        private void run(List<String> content, AtomicInteger aLineNumber) {
            sourceBlockDecl = content.get(aLineNumber.getAndIncrement());

            /*
             * The "preamble" is any text after the [source] line and before
             * the first "----" marking the beginning of the body.
             */
            preamble = collectPreamble(content, aLineNumber);
            int blockStartLineNumber = body.size();

            doUntilBlockDelimiter(content, aLineNumber, line -> {
                if (line.startsWith(IncludeAnalyzer.INCLUDE_START_COMMENT_PREFIX)) {
                    aLineNumber.decrementAndGet();
                    IncludeAnalyzer ia = IncludeAnalyzer.consumeBracketedInclude(
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

        /**
         * Formats the source block using numbered include comments in the
         * preamble and the actual inserted text in the body.
         *
         * @return source block with (if needed) numbered include comments
         * in the preamble
         */
        List<String> updatedSourceBlock() {
            List<String> result = new ArrayList<>();
            result.add(sourceBlockDecl);
            result.addAll(preamble);
            for (IncludeAnalyzer ia : includes) {
                result.add(ia.numberedDescriptor());
            }
            result.add(BLOCK_DELIMITER);
            result.addAll(body);
            result.add(BLOCK_DELIMITER);
            return result;
        }

        List<String> bracketedSourceBlock() {
            List<String> result = new ArrayList<>();
            result.add(sourceBlockDecl);
            result.addAll(preamble);
            result.add(BLOCK_DELIMITER);

            List<String> updatedBody = new ArrayList<>(body);

            /*
             * Replace the actual included text in the body of the source block
             * with AsciiDoc includes. (Below we add brackets to all includes,
             * which will include the ones we add now and any new ones the user
             * might have added to the original .adoc file.)
             *
             * We work on the includes from bottom to top
             * so we don't disrupt the line number references from the
             * numbered includes into the body.
             */
            for (int i = includes.size() - 1; i >= 0; i--) {
                IncludeAnalyzer ia = includes.get(i);
                for (int j = ia.startWithinBlock; j <= ia.endWithinBlock; j++) {
                    updatedBody.remove(ia.startWithinBlock);
                }
                if (ia.startWithinBlock >= updatedBody.size()) {
                    updatedBody.addAll(ia.asAsciiDocInclude());
                } else {
                    updatedBody.addAll(ia.startWithinBlock, ia.asAsciiDocInclude());
                }
            }

            /*
             * At this point the updatedBody should resemble what the user might
             * have originally created: normal AsciiDoc include directives without
             * any bracketing or include-related comments in the preamble. Now,
             * bracket each include.
             */
            List<String> fullyUpdatedBody = new ArrayList<>();
            Matcher m = IncludeAnalyzer.ASCIIDOC_INCLUDE_PATTERN.matcher("");

            for (String bodyLine : updatedBody) {
                m.reset(bodyLine);
                if (m.matches()) {
                    fullyUpdatedBody.add(
                            String.format(
                                    IncludeAnalyzer.INCLUDE_BRACKET_TEMPLATE,
                                    "start",
                                    m.group(1)));
                    fullyUpdatedBody.add(bodyLine);
                    fullyUpdatedBody.add(
                            String.format(
                                    IncludeAnalyzer.INCLUDE_BRACKET_TEMPLATE,
                                    "end",
                                    m.group(1)));
                } else {
                    fullyUpdatedBody.add(bodyLine);
                }
            }
            result.addAll(fullyUpdatedBody);
            result.add(BLOCK_DELIMITER);

            return result;
        }

        /**
         *
         * @return IncludeAnalyzers for any includes processed in the source block
         */
        List<IncludeAnalyzer> sourceIncludes() {
            return includes;
        }

        private List<String> collectPreamble(List<String> content, AtomicInteger lineNumber) {
            final List<String> result = new ArrayList<>();
            final List<String> pendingIncludes = new ArrayList<>();
            final Matcher m = IncludeAnalyzer.INCLUDE_NUMBERED_PATTERN.matcher("");
            doUntilBlockDelimiter(content, lineNumber, line -> {
                        m.reset(line);
                        if (m.matches()) {
                            pendingIncludes.add(line);
                        } else {
                            result.add(line);
                        }
                    });
            final int startOfBlock = lineNumber.get();
            for (String pendingInclude : pendingIncludes) {
                includes.add(IncludeAnalyzer.fromNumberedInclude(
                        content, startOfBlock, pendingInclude));
            }
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

        private static boolean isBlock(String line) {
            return line.equals(BLOCK_DELIMITER);
        }
    }
}
