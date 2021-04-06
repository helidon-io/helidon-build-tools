/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.ansi;

import java.util.Map;

import io.helidon.build.common.Log;
import io.helidon.build.common.Log.Level;
import io.helidon.build.common.RequirementFailure;

import static io.helidon.build.common.Strings.padding;

/**
 * Style log.
 */
public class AnsiLog {

    private static final String PAD = " ";

    private AnsiLog() {
    }

    /**
     * Handle requirement failures.
     *
     * @param runnable runnable that may throw requirement failures to be handled
     */
    public static void handleRequirementFailure(Runnable runnable) {
        try {
            runnable.run();
        } catch (RequirementFailure failure) {
            throw new RequirementFailure(StyleRenderer.render(failure.getMessage()));
        }
    }

    /**
     * Log the entries using {@link StyleFunction#Italic} for all keys and {@link StyleFunction#BoldBlue} for all keys.
     *
     * @param map The entries.
     */
    public static void logEntries(Map<Object, Object> map) {
        logEntries(map, maxKeyWidth(map));
    }

    /**
     * Log the entries using {@link StyleFunction#Italic} for all keys and {@link StyleFunction#BoldBlue} for all keys.
     *
     * @param map         The entries.
     * @param maxKeyWidth The maximum key width.
     */
    public static void logEntries(Map<Object, Object> map, int maxKeyWidth) {
        logEntries(map, maxKeyWidth, StyleFunction.Italic, StyleFunction.BoldBlue);
    }

    /**
     * Log the entries using the given styles.
     *
     * @param map        The entries.
     * @param keyStyle   The style to apply to all keys.
     * @param valueStyle The style to apply to all values.
     */
    public static void logEntries(Map<Object, Object> map, StyleFunction keyStyle, StyleFunction valueStyle) {
        logEntries(map, maxKeyWidth(map), keyStyle, valueStyle);
    }

    /**
     * Log the entries using the given styles.
     *
     * @param map         The entries.
     * @param maxKeyWidth The maximum key width.
     * @param keyStyle    The style to apply to all keys.
     * @param valueStyle  The style to apply to all values.
     */
    public static void logEntries(Map<Object, Object> map, int maxKeyWidth, StyleFunction keyStyle, StyleFunction valueStyle) {
        if (!map.isEmpty()) {
            map.forEach((key, value) -> {
                final String padding = padding(PAD, maxKeyWidth, key.toString());
                Log.log(Level.INFO, "%s %s %s", keyStyle.apply(key), padding, valueStyle.apply(value));
            });
        }
    }

    /**
     * Returns the maximum key width.
     *
     * @param maps The maps.
     * @return The max key width.
     */
    @SafeVarargs
    public static int maxKeyWidth(Map<Object, Object>... maps) {
        int maxLen = 0;
        for (Map<Object, Object> map : maps) {
            for (Object key : map.keySet()) {
                final int len = key.toString().length();
                if (len > maxLen) {
                    maxLen = len;
                }
            }
        }
        return maxLen;
    }
}
