/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
            return value;
        }
        return value.replaceAll("\r\n", "\n");
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
     * Replace all white spaces by replacement string.
     *
     * @param str           string
     * @param replacement   new characters
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
     * Sanitize given string with replacement keys and values.
     *
     * @param str           string
     * @param replacements  map containing old and new characters
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

    private Strings() {
    }
}
