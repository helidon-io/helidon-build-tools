/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * String utility methods.
 */
public class Strings {

    /**
     * Tests whether the value contains any non-whitespace characters.
     *
     * @param value The value, may be {@code null}.
     * @return {@code true} if any non-whitespace characters.
     */
    public static boolean isValid(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Check that the string is valid.
     *
     * @param value   value to check
     * @param message message of the exception thrown if the string is invalid
     * @return value if value
     * @throws IllegalArgumentException if the string is empty
     */
    public static String requireValid(String value, String message) {
        if (isNotValid(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Tests whether the value is {@code null} or contains only whitespace characters.
     *
     * @param value The value, may be {@code null}.
     * @return {@code false} if any non-whitespace characters.
     */
    public static boolean isNotValid(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Normalize newlines.
     *
     * @param value The value to normalize, may be {@code null}
     * @return normalized value
     */
    public static String normalizeNewLines(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\r\n", "\n");
    }

    /**
     * Force UNIX style path on Windows.
     *
     * @param value The value to normalize, may be {@code null}
     * @return normalized value
     */
    public static String normalizePath(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().replace("\\", "/");
    }

    /**
     * Read a string from an input stream.
     *
     * @param inputStream input stream
     * @return String
     */
    public static String read(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Generate a padding string.
     *
     * @param pad the value of a single "pad"
     * @param max the max width of the padding to generate
     * @param str the object being padded
     * @return the padding
     */
    public static String padding(String pad, int max, String str) {
        int keyLen = str.length();
        if (max > keyLen) {
            return pad.repeat(max - keyLen);
        } else {
            return "";
        }
    }

    /**
     * Generate a padded string.
     *
     * @param pad the value of a single "pad"
     * @param max the max width of the padding to generate
     * @param str the str being padded
     * @return the padding
     */
    public static String padded(String pad, int max, String str) {
        String padding = padding(pad, max, str);
        return padding + str;
    }

    /**
     * Strip the repeated leading character in the given string.
     *
     * @param str string to process
     * @param c   leading character
     * @return stripped string
     */
    public static String stripLeading(String str, char c) {
        int index = 0;
        while (index < str.length() && str.charAt(index++) == c) {
        }
        if (index == str.length()) {
            return "";
        }
        return str.substring(index - 1);
    }

    /**
     * Replace all white spaces by replacement string.
     *
     * @param str         string
     * @param replacement new characters
     * @return sanitized string
     */
    public static String replaceWhitespaces(String str, String replacement) {
        Objects.requireNonNull(replacement, "Replacement must not be null");
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\s+", replacement);
    }

    /**
     * Sanitize the given string with replacement keys and values.
     *
     * @param str          string
     * @param replacements map containing old and new characters
     * @return sanitized string
     */
    public static String replaceAll(String str, String... replacements) {
        if (str == null || replacements == null) {
            return str;
        }
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("replacements should be even");
        }
        for (int i = 0; i < replacements.length; i += 2) {
            str = str.replaceAll(replacements[i], replacements[i + 1]);
        }
        return str;
    }

    /**
     * Count the symbols in the line that that match the predicate.
     *
     * @param predicate predicate for the symbols
     * @param line      line
     * @return count of symbols in the line that that match the predicate
     */
    public static int countWhile(Predicate<Character> predicate, String line) {
        int result = 0;
        for (char symbol : line.toCharArray()) {
            if (predicate.test(symbol)) {
                result++;
            } else {
                return result;
            }
        }
        return result;
    }

    /**
     * <p>Compares two strings and returns the portion where they differ.
     * More precisely, return the remainder of the second String,
     * starting from where it's different from the first. This means that
     * the difference between "abc" and "ab" is the empty String and not "c". </p>
     *
     * <p>For example,
     * <pre>
     * Strings.difference(null, null) = null
     * Strings.difference("", "") = ""
     * Strings.difference("", "abc") = "abc"
     * Strings.difference("abc", "") = ""
     * Strings.difference("abc", "abc") = ""
     * Strings.difference("abc", "ab") = ""
     * Strings.difference("ab", "abxyz") = "xyz"
     * Strings.difference("abcde", "abxyz") = "xyz"
     * Strings.difference("abcde", "xyz") = "xyz"
     * </pre>
     *
     * @param str1 the first string, may be {@code null}
     * @param str2 the second string, may be {@code null}
     * @return the portion of str2 where it differs from str1; returns the
     *         empty String if they are equal
     */
    public static String difference(final String str1, final String str2) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        final int at = indexOfDifference(str1, str2);
        if (at == -1) {
            return "";
        }
        return str2.substring(at);
    }

    /**
     * <p>Compares two CharSequences and returns the index at which the
     * CharSequences begin to differ.</p>
     *
     * <p>For example,
     * <pre>
     * Strings.indexOfDifference(null, null) = -1
     * Strings.indexOfDifference("", "") = -1
     * Strings.indexOfDifference("", "abc") = 0
     * Strings.indexOfDifference("abc", "") = 0
     * Strings.indexOfDifference("abc", "abc") = -1
     * Strings.indexOfDifference("ab", "abxyz") = 2
     * Strings.indexOfDifference("abcde", "abxyz") = 2
     * Strings.indexOfDifference("abcde", "xyz") = 0
     * </pre>
     *
     * @param cs1 the first {@link CharSequence}, may be {@code null}
     * @param cs2 the second {@link CharSequence}, may be {@code null}
     * @return the index where cs1 and cs2 begin to differ; -1 if they are equal
     */
    public static int indexOfDifference(final CharSequence cs1, final CharSequence cs2) {
        if (cs1 == cs2) {
            return -1;
        }
        if (cs1 == null || cs2 == null) {
            return 0;
        }
        int i;
        for (i = 0; i < cs1.length() && i < cs2.length(); ++i) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                break;
            }
        }
        if (i < cs2.length() || i < cs1.length()) {
            return i;
        }
        return -1;
    }

    /**
     * Get the lines in a string.
     *
     * @param str string
     * @return lines
     */
    public static Collection<String> lines(String str) {
        if (str == null) {
            return List.of();
        }
        return str.lines().collect(Collectors.toList());
    }

    private Strings() {
    }
}
