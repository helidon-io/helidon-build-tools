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

package io.helidon.build.archetype.engine.v2;

import java.util.LinkedList;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isLowerCase;
import static java.util.Objects.requireNonNull;

/**
 * Context path.
 * A context path is a string representation of the path of a {@code DeclaredInput} in the tree.
 *
 * <ul>
 *     <li>A path contains segments separated by {@code "."} characters</li>
 *     <li>Segments can contains only lower case letters, digits and separator {@code "-"}</li>
 *     <li>The segment separator {@code "-"} must used between valid characters ; ({@code "--"} is prohibited</li>
 *     <li>Two reference operators are available, current scope: {@code "."} ; parent scope: {@code ".."}</li>
 *     <li>A path that starts with {@code "."} is relative to the associated {@link ContextScope}</li>
 *     <li>A path that starts with a segment is absolute. I.e. relative to the root scope</li>
 * </ul>
 *
 * @see io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput
 * @see ContextScope
 */
public final class ContextPath {

    /**
     * Parent path reference.
     */
    public static final String PARENT_REF = "..";

    /**
     * Path separator character.
     */
    public static final char PATH_SEPARATOR_CHAR = '.';

    /**
     * Path separator string.
     */
    public static final String PATH_SEPARATOR = ".";

    private static final char SEGMENT_SEPARATOR = '-';

    /**
     * Test if this path is absolute.
     *
     * @param segments the path segments
     * @return {@code true} if absolute, {@code false} otherwise
     */
    public static boolean isAbsolute(String[] segments) {
        return segments.length == 0 || segments[0].indexOf(PATH_SEPARATOR_CHAR) < 0;
    }

    /**
     * Test if this path is natural.
     * A path is natural if it does not include parent references {@code ".."}
     *
     * @param segments the path segments
     * @return {@code true} if natural, {@code false} otherwise
     */
    public static boolean isNatural(String[] segments) {
        for (String segment : segments) {
            if (PARENT_REF.equals(segment)) {
                return false;
            }
        }
        return true;
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
            sb.append(segments[i]);
            if (i + 1 < segments.length
                    && segments[i].indexOf(PATH_SEPARATOR_CHAR) < 0
                    && segments[i + 1].indexOf(PATH_SEPARATOR_CHAR) < 0) {
                sb.append(PATH_SEPARATOR_CHAR);
            }
        }
        return sb.toString();
    }

    /**
     * Get this path as a string.
     *
     * @param segments the path segments
     * @return String
     */
    public static String toString(String[] segments) {
        return toString(segments, segments.length - 1);
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
        LinkedList<String> segments = new LinkedList<>();
        StringBuilder buf = new StringBuilder();
        char[] chars = requireNonNull(path, "path is null").toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            boolean last = i + 1 == chars.length;
            if (c == PATH_SEPARATOR_CHAR) {
                if (i + 1 < chars.length && chars[i + 1] == PATH_SEPARATOR_CHAR) {
                    // double dot
                    i++; // skip next character
                    if (!segments.isEmpty()) {
                        if ((i > 1 && chars[i - 2] != PATH_SEPARATOR_CHAR)) {
                            // ".foo.." to "."
                            segments.removeLast();
                            continue;
                        } else if(segments.peekLast().equals(PATH_SEPARATOR)) {
                            // "..." -> ".."
                            segments.removeLast();
                        }
                    }
                    segments.add(PARENT_REF);
                } else if (i == 0) {
                    segments.add(PATH_SEPARATOR);
                }
                continue;
            }
            boolean dash = c == SEGMENT_SEPARATOR;
            if (!(dash || (Character.isDigit(c) || (Character.isLetter(c) && isLowerCase(c))))) {
                throw new InvalidPathException(c, path, i);
            }
            int buf_len = buf.length();
            if (dash && (buf_len == 0 || buf.charAt(buf_len - 1) == SEGMENT_SEPARATOR)) {
                throw new InvalidPathException(c, path, i);
            }
            buf.append(c);
            if (last || chars[i + 1] == PATH_SEPARATOR_CHAR) {
                if (c == SEGMENT_SEPARATOR) {
                    throw new InvalidPathException(c, path, i);
                }
                segments.add(buf.toString());
                buf.setLength(0);
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
