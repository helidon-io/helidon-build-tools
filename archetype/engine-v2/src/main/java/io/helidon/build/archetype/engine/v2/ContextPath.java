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

import java.util.Arrays;
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
 *     <li>Two reference operators are available: current scope {@code "."} ; parent scope {@code ".."}</li>
 *     <li>A path that starts with {@code "."} is relative to the associated {@link ContextScope}</li>
 *     <li>A path that starts with a segment is absolute. I.e. relative to the root scope</li>
 * </ul>
 *
 * @see io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput
 * @see ContextScope
 */
public final class ContextPath {

    private final String rawPath;
    private final String[] segments;
    private final boolean absolute;

    private ContextPath(String path) {
        this.rawPath = path;
        this.segments = parse(path);
        this.absolute = segments.length == 0 || !".".equals(segments[0]);
    }

    /**
     * Get the segments.
     *
     * @return segments
     */
    public String[] segments() {
        return segments;
    }

    /**
     * Test if this path is absolute.
     *
     * @return {@code true} if absolute, {@code false} otherwise
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * Test if this path is natural.
     * A path is natural if it does not include parent references {@code ".."}
     *
     * @return {@code true} if natural, {@code false} otherwise
     */
    public boolean isNatural() {
        for (String segment : segments) {
            if ("..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get this path as a string.
     *
     * @param endIndex last index of the segments to include
     * @return String
     */
    public String asString(int endIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= endIndex; i++) {
            sb.append(segments[i]);
        }
        return sb.toString();
    }

    /**
     * Get this path as a string.
     *
     * @return String
     */
    public String asString() {
        return asString(0);
    }

    @Override
    public String toString() {
        return "ContextPath{"
                + "rawPath='" + rawPath + '\''
                + ", segments=" + Arrays.toString(segments)
                + ", absolute=" + absolute
                + '}';
    }

    /**
     * Create a context path.
     *
     * @param path raw path
     * @return context path
     * @throws IllegalArgumentException if the path is not valid
     * @throws NullPointerException     if path is {@code null}
     */
    public static ContextPath create(String path) {
        return new ContextPath(path);
    }

    private static String[] parse(String path) {
        LinkedList<String> segments = new LinkedList<>();
        StringBuilder buf = new StringBuilder();
        char[] chars = requireNonNull(path, "path is null").toCharArray();
        for (int i = 0; i < path.length(); i++) {
            char c = chars[i];
            boolean last = i + 1 == chars.length;
            if (c == '.') {
                if (last) {
                    if (segments.isEmpty()) {
                        buf.append(c);
                    } else {
                        // ignore trailing dot
                        break;
                    }
                } else {
                    if (chars[i + 1] == '.') {
                        // double dot
                        i++; // skip next character
                        if (i > 1 && chars[i - 2] != '.') {
                            // delete previous segment
                            segments.removeLast();
                            continue;
                        }
                        buf.append(c);
                    }
                    buf.append(c);
                }
                segments.add(buf.toString());
                buf.setLength(0);
                continue;
            }
            boolean dash = c == '-';
            if (!(dash || (isLetterOrDigit(c) && isLowerCase(c)))) {
                throw new InvalidPathException(c, path, i);
            }
            int buf_len = buf.length();
            if (i > 0 && chars[i - 1] == '.' && buf_len > 0) {
                segments.add(buf.toString());
                buf.setLength(0);
            }
            if (dash && (buf_len == 0 || buf.charAt(buf_len - 1) == '-')) {
                throw new InvalidPathException(c, path, i);
            }
            buf.append(c);
            if (last || chars[i + 1] == '.') {
                if (c == '-') {
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
