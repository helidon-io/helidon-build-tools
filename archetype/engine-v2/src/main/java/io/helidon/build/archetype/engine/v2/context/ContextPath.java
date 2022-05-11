/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.context;

import java.util.LinkedList;

import static java.lang.Character.isLetterOrDigit;
import static java.util.Objects.requireNonNull;

/**
 * Context path.
 * A context path is a notation to represent the path of a value in the context tree.
 *
 * <ul>
 *     <li>A path contains segments separated by {@code "."} characters</li>
 *     <li>Segments can contains only letters, digits and separator {@code "-"}</li>
 *     <li>The segment separator {@code "-"} must be used between valid characters ; ({@code "--"} is prohibited</li>
 *     <li>Two reference operators are available, root scope: {@code "~"} ; parent scope: {@code ".."}</li>
 *     <li>A path that starts with {@code "~"} is absolute. I.e. relative to the root scope</li>
 *     <li>A path that starts with a segment is relative, or a parent reference is relative</li>
 * </ul>
 *
 * @see io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput
 * @see ContextNode
 */
public final class ContextPath {

    /**
     * Parent path reference.
     */
    public static final String PARENT_REF = "..";

    /**
     * Root path reference.
     */
    public static final String ROOT_REF = "~";

    /**
     * Root path reference char.
     */
    public static final char ROOT_REF_CHAR = '~';

    /**
     * Path separator character.
     */
    public static final char PATH_SEPARATOR_CHAR = '.';

    /**
     * Path separator string.
     */
    public static final String PATH_SEPARATOR = ".";

    private static final char SEGMENT_SEPARATOR = '-';

    private ContextPath() {
    }

    /**
     * Get the last segment of the path.
     *
     * @param segments the path segments
     * @return id
     * @throws IllegalArgumentException if the path is empty, or if the last segment contains any {@code "."}
     */
    public static String id(String[] segments) {
        if (segments.length == 0) {
            throw new IllegalArgumentException("Path is empty");
        }
        String id = segments[segments.length - 1];
        if (id.indexOf(PATH_SEPARATOR_CHAR) > 0) {
            throw new IllegalArgumentException("Invalid scope id: " + id);
        }
        return id;
    }

    /**
     * Get this path as a string.
     *
     * @param segments the path segments
     * @param endIndex last index of the segments to include
     * @return String
     */
    public static String toString(String[] segments, int endIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= endIndex; i++) {
            if (i == 0 && segments[i].equals(ROOT_REF)) {
                continue;
            }
            sb.append(segments[i]);
            if (i + 1 <= endIndex
                    && segments[i].indexOf(PATH_SEPARATOR_CHAR) < 0
                    && segments[i + 1].indexOf(PATH_SEPARATOR_CHAR) < 0) {
                sb.append(PATH_SEPARATOR_CHAR);
            }
        }
        return sb.toString();
    }

    /**
     * Parse a context path.
     *
     * @param path raw path
     * @return context path
     * @throws IllegalArgumentException if the path is not valid
     * @throws NullPointerException     if path is {@code null}
     */
    public static String[] parse(String path) {
        requireNonNull(path, "path is null");
        LinkedList<String> segments = new LinkedList<>();
        StringBuilder buf = new StringBuilder();
        char[] chars = path.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case ROOT_REF_CHAR:
                    if (i == 0) {
                        segments.add(ROOT_REF);
                        if (i + 2 < chars.length
                                && chars[i + 1] == PATH_SEPARATOR_CHAR
                                && chars[i + 2] == PATH_SEPARATOR_CHAR) {
                            // "~.." to "~"
                            i += 2;
                        }
                        continue;
                    }
                    throw new InvalidPathException(c, path, i);
                case PATH_SEPARATOR_CHAR:
                    if (i + 1 < chars.length) {
                        if (chars[i + 1] == PATH_SEPARATOR_CHAR) {
                            // ".."
                            i++;
                            if (i > 1 && isLetterOrDigit(chars[i - 2])) {
                                // ".foo.." to "."
                                segments.removeLast();
                                continue;
                            }
                            segments.add(PARENT_REF);
                            continue;
                        }
                        if (i > 0 && isLetterOrDigit(chars[i + 1])) {
                            // valid "." as path separator
                            continue;
                        }
                    }
                    throw new InvalidPathException(c, path, i);
                default:
                    if (c == SEGMENT_SEPARATOR) {
                        if (!((i > 0 && i + 1 < chars.length)
                                && isLetterOrDigit(chars[i - 1])
                                && isLetterOrDigit(chars[i + 1]))) {
                            throw new InvalidPathException(c, path, i);
                        }
                        buf.append(c);
                        continue;
                    }
                    if (!isLetterOrDigit(c)) {
                        throw new InvalidPathException(c, path, i);
                    }
                    buf.append(c);
                    if (i + 1 == chars.length || chars[i + 1] == PATH_SEPARATOR_CHAR) {
                        segments.add(buf.toString());
                        buf.setLength(0);
                    }
            }
        }
        if (buf.length() > 0) {
            throw new IllegalStateException("parse error");
        }
        return segments.toArray(new String[0]);
    }

    private static final class InvalidPathException extends IllegalArgumentException {
        InvalidPathException(char c, String path, int index) {
            super(String.format("Invalid path character: %c, path=%s, index=%s", c, path, index));
        }
    }
}
