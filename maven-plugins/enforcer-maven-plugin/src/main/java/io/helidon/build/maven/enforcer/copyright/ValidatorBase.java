/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.copyright;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.helidon.build.common.Log;
import io.helidon.build.maven.enforcer.EnforcerException;
import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.RuleFailureException;

import static io.helidon.build.maven.enforcer.copyright.Copyright.logBad;
import static io.helidon.build.maven.enforcer.copyright.Copyright.logGood;

/**
 * Base class for validators.
 */
public abstract class ValidatorBase implements Validator {
    private final List<TemplateLine> templateLines;
    private final String currentYear;

    /**
     * Create a new instance.
     *
     * @param validatorConfig validator configuration
     * @param templateLines lines of the template
     */
    protected ValidatorBase(ValidatorConfig validatorConfig,
                            List<TemplateLine> templateLines) {
        this.templateLines = templateLines;
        this.currentYear = validatorConfig.currentYear();
    }

    @Override
    public boolean supports(Path path) {
        return false;
    }

    @Override
    public void validate(FileRequest file, Path path) {
        Log.verbose("Processing file: " + path);

        List<FileLine> copyrightComment;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            copyrightComment = readComment(file, br);
        } catch (IOException e) {
            throw new EnforcerException("Failed to read file " + path, e);
        }

        if (copyrightComment.size() != templateLines.size()) {
            if (copyrightComment.isEmpty()) {
                throw new RuleFailureException(file,
                                               0,
                                               "Expecting copyright with " + logGood(String.valueOf(templateLines.size()))
                                                       + " lines, but it was " + logBad("empty"));
            } else {
                throw new RuleFailureException(file,
                                               copyrightComment.iterator().next().lineNumber(),
                                               "Expecting copyright with "
                                                       + logGood(String.valueOf(templateLines.size()))
                                                       + " lines, but it contained "
                                                       + logBad(String.valueOf(copyrightComment.size()))
                                                       + " lines");
            }
        }

        for (int i = 0; i < copyrightComment.size(); i++) {
            FileLine actualLine = copyrightComment.get(i);
            TemplateLine templateLine = templateLines.get(i);

            templateLine.validate(file, actualLine.line(), actualLine.lineNumber());
        }
    }

    /**
     * Read comment from a reader.
     *
     *
     * @param file file request
     * @param reader read for the file
     * @return the comment that should contain copyright
     * @throws IOException in case of I/O problems
     */
    protected List<FileLine> readComment(FileRequest file,
                                         BufferedReader reader) throws IOException {
        String line;
        int lineNumber = 0;

        // skip beginning of file
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            if (isPreamble(line.trim())) {
                if (copyrightBeforePreamble()) {
                    return List.of();
                }
                continue;
            }
            // we got a real line
            break;
        }

        if (line == null) {
            return List.of();
        }

        // now we must have comment - otherwise there is no copyright
        // also the first line must be empty otherwise
        if (!isCommentStart(line.trim())) {
            throw new RuleFailureException(file, lineNumber, "No comment found");
        }

        List<FileLine> comment = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            if (supportsBlockComments()) {
                if (line.trim().startsWith(blockCommentEnd())) {
                    // end of comment
                    break;
                }
            } else {
                if (!line.trim().startsWith(lineCommentStart().orElseThrow(() -> new EnforcerException(
                        "When block comment is not supported, line comment must be defined.")))) {
                    // end of comment
                    break;
                }
            }
            line = removeComment(file, line, lineNumber);
            comment.add(FileLine.create(lineNumber, line));
        }

        if (comment.isEmpty()) {
            throw new RuleFailureException(file, lineNumber, "No comment found");
        }

        if (allowLeadEmptyLine()) {
            // allow one line before
            if (comment.get(0).line().isBlank()) {
                comment.remove(0);
            }
        }

        if (allowTrailEmptyLine(file.lastModifiedYear())) {
            // allow one line after
            if (comment.get(comment.size() - 1).line().isBlank()) {
                containsTrailingEmptyLine(file);
                comment.remove(comment.size() - 1);
            }
        }

        if (!supportsBlockComments()) {
            // last line may be empty
            if (comment.get(comment.size() - 1).line().isBlank()) {
                comment.remove(comment.size() - 1);
            }
        }
        return comment;
    }

    /**
     * Called if a trailing empty line exists in a copyright comment.
     *
     * @param file file
     */
    protected void containsTrailingEmptyLine(FileRequest file) {
    }

    /**
     * Whether we can have an additional line before the copyright text within the comment.
     *
     * @return whether to allow leading empty line
     */
    protected boolean allowLeadEmptyLine() {
        return false;
    }

    /**
     * Whether we can have an additional line after the copyright text within the comment.
     *
     * @return whether to allow trailing empty line
     * @param modifiedYear year of last modification
     */
    protected boolean allowTrailEmptyLine(String modifiedYear) {
        return false;
    }

    /**
     * Whether copyright should be before preamble (if one is supported).
     *
     * @return whether copyright should be before preamble ({@code true} by default)
     */
    protected boolean copyrightBeforePreamble() {
        return true;
    }

    /**
     * Remove comment from a line.
     *
     *
     * @param file file request
     * @param line line text
     * @param lineNumber line number
     * @return line stripped of comment prefix
     */
    protected String removeComment(FileRequest file, String line, int lineNumber) {
        if (supportsBlockComments()) {
            if (blockCommentPrefix().isPresent()) {
                String block = blockCommentPrefix().get();
                if (block.isBlank() || block.startsWith(" ")) {
                    // prefixed by spaces or tabs
                    if (line.startsWith(block)) {
                        return line.substring(block.length());
                    }
                    if (line.isBlank()) {
                        return "";
                    }
                } else {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(block)) {
                        // we expect a space after the block
                        if (trimmed.length() == block.length()) {
                            return "";
                        }
                        if (trimmed.charAt(block.length()) != ' ') {
                            throw new RuleFailureException(file,
                                                           lineNumber,
                                                           "Expected prefix "
                                                                   + logGood(block) + ", but line is "
                                                                   + logBad(line));
                        }
                        return trimmed.substring(block.length() + 1);
                    }
                }
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Expected prefix "
                                                       + logGood(block) + ", but line is "
                                                       + logBad(line));
            }
        }

        if (lineCommentStart().isPresent()) {
            String lineComment = lineCommentStart().get();
            if (line.startsWith(lineComment)) {
                String substring = line.substring(lineComment.length());
                if (substring.startsWith(" ")) {
                    substring = substring.substring(1);
                }
                return substring;
            }
        }

        return line;
    }

    /**
     * Does this line start a multiline comment, or is it a comment.
     *
     * @param line line text
     * @return whether the line is start of multiline comment or a single line comment
     */
    protected boolean isCommentStart(String line) {
        if (supportsBlockComments()) {
            return line.startsWith(blockCommentStart());
        }
        return line.startsWith(lineCommentStart().orElseThrow(() -> new EnforcerException(
                "When block comment is not supported, line comment must be defined.")));
    }

    /**
     * Whether the file starts with the provided prefix.
     * Utility method to "peek" into the file.
     *
     * @param path path of the file
     * @param prefix prefix to look for
     * @return whether the file starts with the prefix
     */
    protected final boolean startsWith(Path path, String prefix) {
        return startsWith(path, prefix.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Whether the file starts with the provided prefix.
     * Utility method to "peek" into the file.
     *
     * @param path path of the file
     * @param prefix prefix to look for
     * @return whether the file starts with the prefix
     */
    protected final boolean startsWith(Path path, byte[] prefix) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] chunk = new byte[prefix.length];
            int read = inputStream.read(chunk);
            if (read < chunk.length) {
                return false;
            }
            return Arrays.equals(prefix, chunk);
        } catch (IOException e) {
            throw new EnforcerException("Failed to check file from prefix. Path: " + path, e);
        }
    }

    /**
     * Whether this validator supports multiline (block) comments.
     *
     * @return whether block comments are supported
     */
    protected boolean supportsBlockComments() {
        return false;
    }

    /**
     * Start of block comment (if supported).
     *
     * @return beginning of block comment
     */
    protected String blockCommentStart() {
        return null;
    }

    /**
     * End of block comment (if supported).
     *
     * @return end of block comment
     */
    protected String blockCommentEnd() {
        return null;
    }

    /**
     * Optional prefix used on lines within a block comment.
     *
     * @return prefix
     */
    protected Optional<String> blockCommentPrefix() {
        return Optional.empty();
    }

    /**
     * Beginning of single line comment, only used if {@link #supportsBlockComments()}
     * is set to false.
     *
     * @return beginning of line comment
     */
    protected Optional<String> lineCommentStart() {
        return Optional.empty();
    }

    /**
     * Whether the current line is a preamble (such as {@code package} statement in Java).
     *
     * @param line line text
     * @return whether the line is a preamble
     */
    protected boolean isPreamble(String line) {
        return false;
    }

    /**
     * Current year.
     *
     * @return current year
     */
    protected final String currentYear() {
        return currentYear;
    }
}
