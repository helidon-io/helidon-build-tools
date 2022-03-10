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
package io.helidon.build.cli.harness;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.util.Style;

/**
 * Utility class to help with output.
 */
public final class OutputHelper {

    private static final String BEGIN_SPACING = "  ";
    private static final String COL_SPACING = "    ";

    private OutputHelper() {
    }

    /**
     * Render map as a table with the second column aligned.
     *
     * @param map map to render
     * @return rendered table
     */
    public static String table(Map<String, String> map) {
        int maxKeyWidth = maxKeyWidth(map);
        return map.entrySet().stream()
                  .map(e -> BEGIN_SPACING + e.getKey() + padding(maxKeyWidth, e.getKey()) + COL_SPACING + e.getValue())
                  .collect(Collectors.joining("\n"));
    }

    /**
     * Render map as a table with the second column aligned.
     *
     * @param map map to render
     * @param maxKeyWidth the maximum key width
     * @return rendered table
     */
    public static String table(Map<String, String> map, int maxKeyWidth) {
        return map.entrySet().stream()
                  .map(e -> BEGIN_SPACING + e.getKey() + padding(maxKeyWidth, e.getKey()) + COL_SPACING + e.getValue())
                  .collect(Collectors.joining("\n"));
    }

    /**
     * Returns the maximum key width from a list of maps.
     * @param maps maps
     * @return maximum key width
     */
    public static int maxKeyWidth(Map<?, ?>... maps) {
        return Arrays.stream(maps)
                     .map(m -> m.keySet().stream().map(k -> Style.strip(k.toString())).mapToInt(String::length).max().orElse(0))
                     .max(Comparator.naturalOrder())
                     .orElse(0);
    }

    private static String padding(int maxKeyWidth, String key) {
        final int keyLen = Style.strip(key).length();
        if (maxKeyWidth > keyLen) {
            return " ".repeat(maxKeyWidth - keyLen);
        } else {
            return "";
        }
    }
}
