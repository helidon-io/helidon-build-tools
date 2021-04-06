/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
            return value;
        }
        return value.replaceAll("\r\n", "\n");
    }

    /**
     * Generate a padding.
     *
     * @param pad         the value of a single "pad"
     * @param maxKeyWidth the max width of the padding to generate
     * @param key         the object being padded
     * @return the padding
     */
    public static String padding(String pad, int maxKeyWidth, String key) {
        final int keyLen = key.toString().length();
        if (maxKeyWidth > keyLen) {
            return pad.repeat(maxKeyWidth - keyLen);
        } else {
            return "";
        }
    }

    private Strings() {
    }
}
