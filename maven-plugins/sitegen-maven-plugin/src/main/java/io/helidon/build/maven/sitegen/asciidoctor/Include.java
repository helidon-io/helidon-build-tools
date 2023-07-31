/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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
 */
package io.helidon.build.maven.sitegen.asciidoctor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes included content and formats it in various ways.
 * <h2>Overview</h2>
 * For the purposes of preprocessing the AsciiDoc, we treat all included content
 * as residing within a block, whether that is an actual block (such as the body
 * of a {@code [source]} block between the "----" lines) or a synthetic block
 * composed of the lines immediately following a freestanding (i.e., not in a
 * {@code [source]} block) {@code include::}.
 * <p>
 * Each {@code Include} instance records:
 * <ul>
 * <li>the starting and ending line numbers within the block of interest where
 * the included text resides,
 * <li>the include target (path and other information from the original
 * {@code include::}), and
 * <li>the included text itself.
 * </ul>
 *
 * <h2>Text Formatting of Includes</h2>
 * We express included content in two intermediate formats and one external
 * one we use during preprocessing.
 *
 * <h3>Brief Bracketed Form (internal)</h3>
 * In this form we bracket an AsciiDoc {@code include::} directive with a
 * preceding {@code // _include-start::} and a following
 * {@code // _include-end::} comment. For example, if the original AsciiDoc file
 * contains
 * <pre>
 * {@code
 * include::somePath.adoc[tag=myExcerpt]
 * }
 * </pre> then the bracketed form is
 * <pre>
 * {@code
 * // _include-start::somePath.adoc[tag=myExcerpt]
 * include::somePath.adoc[tag=myExcerpt]
 * // _include-end::somePath.adoc[tag=myExcerpt]
 * }
 * </pre> The {@link IncludePreprocessor} submits this format to AsciiDoc
 * processing in a way that produces the full bracketed form.
 * <p>
 * See {@link #asBracketedAsciiDocInclude() }.
 * <h2>Full Bracketed Form (internal)</h2>
 * In this format the bracketing AsciiDoc comments surround the included text
 * itself rather than the {@code include::} AsciiDoc directive.
 * <pre>
 * {@code
 * include::somePath.adoc[tag=myExcerpt]
 * }
 * </pre> becomes
 * <pre>
 * {@code
 * // _include-start::somePath.adoc[tag=myExcerpt]
 * This text
 * is copied from
 * the somePath.adoc file as
 * identified by the myExcerpt tag.
 * // _include-end::somePath.adoc[tag=myExcerpt]
 * }
 * </pre>
 *
 * <h2>Numbered Form (external)</h2>
 * Here each AsciiDoc {@code include::} directive is replaced by:
 * <ul>
 * <li>an AsciiDoc comment indicating the original AsciiDoc {@code include::}
 * information (path, etc.) with the line numbers within the relevant block
 * where the included text resides, and
 * <li>the actual included text in the relevant block.
 * </ul>
 * A freestanding {@code include::} becomes
 * <pre>
 * {@code
 * // _include::0-n:somePath.adoc
 * n+1 lines of included text from somePath.adoc
 * }
 * </pre>
 * See {@link Block#asBlockWithNumberedIncludes() } which also creates
 * the numbered form but organizes the comment and the corresponding included text somewhat differently.
 *
 * <h2>Creating {@code Include} Instances by Parsing AsciiDoc</h2>
 * Although we use three formats to express included content, we only ever need
 * to parse two of them -- full bracketed and numbered -- into {@code Include}
 * instances.
 * <p>
 * The numbered form already provides all the information needed: the included content
 * target (path and tags), the included text, and the text's location within the
 * enclosing block. Converting numbered form into an {@code Include} has no side
 * effects. See {@link #fromNumberedInclude }.
 * <p>
 * On the other hand, converting the full bracketed form requires consuming the part
 * of the content that contains the included text, because the end of the included
 * text is marked by the ending {@code // _include-end::} comment.
 * See {@link #consumeBracketedInclude }.
 */
@SuppressWarnings("unused")
public class Include {

    private static final String INCLUDE_PREFIX = "_include";

    /*
     * matches a bracketing _include-... comment (start or end)
     */
    static final Pattern INCLUDE_BRACKET_PATTERN = Pattern.compile("// " + INCLUDE_PREFIX + "-" + "(start|end)::(.*)");

    /*
     * matches a numbered _include:: comment
     */
    static final Pattern INCLUDE_NUMBERED_PATTERN = Pattern.compile("// " + INCLUDE_PREFIX + "::(\\d*)-(\\d*):(.*)");

    /*
     * matches an AsciiDoc include:: directive
     */
    static final Pattern ASCIIDOC_INCLUDE_PATTERN = Pattern.compile("include::(.*)");
    static final String INCLUDE_BRACKET_TEMPLATE = "// " + INCLUDE_PREFIX + "-%s::%s";

    private static final String INCLUDE_NUMBERED_TEMPLATE = "// " + INCLUDE_PREFIX + "::%d-%d:%s";
    private static final String INCLUDE_START = INCLUDE_PREFIX + "-start";
    private static final String INCLUDE_END = INCLUDE_PREFIX + "-end";

    /**
     * Returns whether the line is an _include-start line.
     *
     * @param line the line to check
     * @return whether the line is an _include-start line
     */
    static boolean isIncludeStart(String line) {
        return line.startsWith("// " + INCLUDE_START);
    }

    /**
     * Returns whether the line is an _include-end line.
     *
     * @param line the line to check
     * @return whether the line is an _include-end line
     */
    static boolean isIncludeEnd(String line) {
        return line.startsWith("// " + INCLUDE_END);
    }

    /**
     * Extracts the include-relevant part of an _include-start line.
     *
     * @param line the line to be examined
     * @return include path and any modifiers from the line
     */
    static String targetFromIncludeStart(String line) {
        return line.substring(("// " + INCLUDE_START + "::").length());
    }

    /**
     * @param line the line to check
     * @return whether the line is a numbered _include:: line
     */
    static boolean isIncludeNumbered(String line) {
        return INCLUDE_NUMBERED_PATTERN.matcher(line).matches();
    }

    /**
     * Formats an include target as a normal AsciiDoc {@code include::} directive.
     *
     * @param includeTarget the target
     * @return include directive
     */
    static String include(String includeTarget) {
        return String.format("include::" + includeTarget);
    }

    /**
     * Formats an include target as the end bracket include comment.
     *
     * @param includeTarget the target
     * @return the ending bracket comment
     */
    static String includeEnd(String includeTarget) {
        return String.format("// %s::%s", Include.INCLUDE_END, includeTarget);
    }

    /**
     * Formats an include target as the starting bracket include command.
     *
     * @param includeTarget the target
     * @return the starting bracket comment
     */
    static String includeStart(String includeTarget) {
        return String.format("// %s::%s", Include.INCLUDE_START, includeTarget);
    }

    private final int startWithinBlock;
    private final int endWithinBlock;
    private final List<String> body;
    private final String includeTarget;

    /**
     * Creates a new instance given full information about the included content.
     *
     * @param startOfBlock     where, within the overall content, the block resides
     * @param startWithinBlock where, within the block, the included content begins
     * @param endWithinBlock   where, within the block, the included content ends
     * @param body             the included text
     * @param includeTarget    include path and any additional attributes
     */
    Include(int startOfBlock, int startWithinBlock, int endWithinBlock, List<String> body, String includeTarget) {
        this.startWithinBlock = startWithinBlock;
        this.endWithinBlock = endWithinBlock;
        this.includeTarget = includeTarget;
        this.body = body;
    }

    /**
     * Creates a new IncludeAnalyzer from the specified content, given the
     * starting point of the block within that content and the numbered include
     * descriptor. The block (as indicated by the starting point) should not
     * include any include descriptor comment but rather the included content.
     *
     * @param content         lines containing the included text
     * @param startOfBlock    start of the block that containing included content
     * @param numberedInclude numbered include comment line describing the included content
     */
    static Include fromNumberedInclude(List<String> content, int startOfBlock, String numberedInclude) {
        Matcher m = INCLUDE_NUMBERED_PATTERN.matcher(numberedInclude);
        if (!m.matches()) {
            throw new IllegalStateException(
                    "Expected numbered include but did not match expected pattern - "
                            + numberedInclude);
        }
        int startWithinBlock = Integer.parseInt(m.group(1));
        int endWithinBlock = Integer.parseInt(m.group(2));

        return new Include(
                content,
                startOfBlock,
                startWithinBlock,
                endWithinBlock,
                m.group(3));
    }

    /**
     * Parses a bracketed include block, starting at the specified line within
     * the content and advancing the {@code lineNumber} to just past the ending
     * bracket comment for the included content.
     *
     * @param content            lines containing the bracketed include to be parsed
     * @param lineNumber         starting point of the bracketed include (the starting comment)
     * @param output             buffer at the end of which the result of consuming and translating
     *                           the included content will be stored by the caller (used only for computing the location
     *                           in the block where the included content resides)
     * @param startOfOutputBlock where the included content itself begins in the output
     * @return Include describing this include
     */
    static Include consumeBracketedInclude(List<String> content,
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
        return new Include(
                startOfOutputBlock,
                startWithinBlock,
                endWithinBlock,
                body,
                includeTarget);
    }

    /**
     * Creates a new instance given the content and starting position within it
     * where the included content resides.
     *
     * @param content          content within which the included text resides
     * @param startOfBlock     where, within the content, the block begins
     * @param startWithinBlock where, within the block, the included text begins
     * @param endWithinBlock   where, within the bloc, the included text ends
     * @param includeTarget    the included content path and any additional attributes
     */
    private Include(List<String> content,
                    int startOfBlock,
                    int startWithinBlock,
                    int endWithinBlock,
                    String includeTarget) {

        this(startOfBlock, startWithinBlock, endWithinBlock,
                buildBody(content, startOfBlock, startWithinBlock, endWithinBlock),
                includeTarget);
    }

    /**
     * Formats this included content as bracketed included lines.
     *
     * @return lines with an AsciiDoc include bracketed with start and end
     * comments
     */
    List<String> asBracketedAsciiDocInclude() {
        List<String> result = new ArrayList<>();
        result.add(String.format(INCLUDE_BRACKET_TEMPLATE, "start", includeTarget));
        result.add("include::" + includeTarget);
        result.add(String.format(INCLUDE_BRACKET_TEMPLATE, "end", includeTarget));
        return result;
    }

    /**
     * Formats this included content as simply the corresponding
     * AsciiDoc {@code include::} directive.
     *
     * @return a single line containing the AsciiDoc {@code include::} directive
     */
    List<String> asAsciiDocInclude() {
        List<String> result = new ArrayList<>();
        result.add(String.format("include::%s", includeTarget));
        return result;
    }

    /**
     * Format the numbered descriptor for the included content.
     *
     * @return AsciiDoc comment describing the included content using line numbers
     */
    String asNumberedAsciiDocInclude() {
        return String.format(INCLUDE_NUMBERED_TEMPLATE, startWithinBlock, endWithinBlock, includeTarget);
    }

    /**
     * Returns the text associated with this include.
     *
     * @return the included content
     */
    List<String> body() {
        return body;
    }

    /**
     * @return the starting position of the included text within the containing
     * block
     */
    int startWithinBlock() {
        return startWithinBlock;
    }

    /**
     * @return the ending position of the included text within the containing block
     */
    int endWithinBlock() {
        return endWithinBlock;
    }

    /**
     * @return the included content target (path and any additional attributes)
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
        final Include other = (Include) obj;
        if (this.startWithinBlock != other.startWithinBlock) {
            return false;
        }
        if (this.endWithinBlock != other.endWithinBlock) {
            return false;
        }
        if (!Objects.equals(this.includeTarget, other.includeTarget)) {
            return false;
        }
        return Objects.equals(this.body, other.body);
    }

    @Override
    public String toString() {
        return "IncludeAnalyzer{" + "startWithinBlock=" + startWithinBlock
                + ", endWithinBlock=" + endWithinBlock + ", body=" + body
                + ", includeTarget=" + includeTarget + '}';
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
}
