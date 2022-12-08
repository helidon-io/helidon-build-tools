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

package io.helidon.lsp.server.utils;

/**
 * Utility class to work with Strings.
 */
public class LspStringUtils {

    private LspStringUtils() {
    }

    /**
     * Count leading whitespaces in the string.
     *
     * @param line string
     * @return Count of the leading whitespaces
     */
    public static int countStartingSpace(String line) {
        int count = 0;
        if (!line.startsWith(" ")) {
            return count;
        }
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Get an indent for the line.
     *
     * @param line line
     * @return an indent for the line
     */
    public static int indentSize(String line) {
        int result = 0;
        for (char symbol : line.toCharArray()) {
            if (symbol == ' ' || symbol == '-') {
                result++;
            } else {
                return result;
            }
        }
        return result;
    }
}
