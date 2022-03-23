/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.logging;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.common.RichTextStyle;

import static io.helidon.build.common.Strings.padding;

/**
 * Simple, centralized logging.
 */
public class Log {

    private static final String PAD = " ";
    private static final AtomicInteger MESSAGES = new AtomicInteger();
    private static final AtomicInteger WARNINGS = new AtomicInteger();
    private static final AtomicInteger ERRORS = new AtomicInteger();

    static {
        LogWriter.init();
        LogFormatter.init();
    }

    private Log() {
    }

    /**
     * Returns the number of messages logged.
     *
     * @return The count.
     */
    public static int messages() {
        return MESSAGES.get();
    }

    /**
     * Returns the number of WARN messages logged.
     *
     * @return The count.
     */
    @SuppressWarnings("unused")
    public static int warnings() {
        return WARNINGS.get();
    }

    /**
     * Returns the number of ERROR messages logged.
     *
     * @return The count.
     */
    public static int errors() {
        return ERRORS.get();
    }

    /**
     * Log a message at DEBUG level.
     *
     * @param message The message.
     * @param args    The message args.
     */
    public static void debug(String message, Object... args) {
        log(LogLevel.DEBUG, message, args);
    }

    /**
     * Log a message at VERBOSE level.
     *
     * @param message The message.
     * @param args    The message args.
     */
    public static void verbose(String message, Object... args) {
        log(LogLevel.VERBOSE, message, args);
    }

    /**
     * Log an empty message at INFO level.
     */
    public static void info() {
        log(LogLevel.INFO, "");
    }

    /**
     * Log a message at INFO level.
     *
     * @param message The message.
     * @param args    The message args.
     */
    public static void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    /**
     * Log a message at WARNING level.
     *
     * @param message The message.
     * @param args    The message args.
     */
    public static void warn(String message, Object... args) {
        log(LogLevel.WARN, message, args);
    }

    /**
     * Log a warning message with associated throwable.
     *
     * @param thrown The throwable.
     * @param msg    Message to be logged.
     * @param args   Format string arguments.
     */
    public static void warn(Throwable thrown, String msg, Object... args) {
        log(LogLevel.WARN, thrown, msg, args);
    }

    /**
     * Log a message at ERROR level.
     *
     * @param message The message.
     * @param args    The message args.
     */
    public static void error(String message, Object... args) {
        log(LogLevel.ERROR, message, args);
    }

    /**
     * Log a message at ERROR level with associated throwable.
     *
     * @param thrown  The throwable.
     * @param message The message.
     * @param args    The message args.
     */
    public static void error(Throwable thrown, String message, Object... args) {
        log(LogLevel.ERROR, thrown, message, args);
    }

    /**
     * Log the message if at or above the given level.
     *
     * @param level   The level.
     * @param message The message.
     * @param args    The message args.
     */
    public static void log(LogLevel level, String message, Object... args) {
        log(level, null, message, args);
    }

    /**
     * Log the entries using the given styles.
     *
     * @param level      The level.
     * @param map        The entries.
     * @param keyStyle   The style to apply to all keys.
     * @param valueStyle The style to apply to all values.
     */
    public static void log(LogLevel level, Map<Object, Object> map, RichTextStyle keyStyle, RichTextStyle valueStyle) {
        log(level, map, maxKeyWidth(map), keyStyle, valueStyle);
    }

    /**
     * Log the entries using the given styles.
     *
     * @param level      The level.
     * @param map         The entries.
     * @param maxKeyWidth The maximum key width.
     * @param keyStyle    The style to apply to all keys.
     * @param valueStyle  The style to apply to all values.
     */
    public static void log(LogLevel level,
                           Map<Object, Object> map,
                           int maxKeyWidth,
                           RichTextStyle keyStyle,
                           RichTextStyle valueStyle) {

        if (!map.isEmpty()) {
            map.forEach((key, value) -> {
                final String padding = padding(PAD, maxKeyWidth, key.toString());
                Log.log(level, "%s %s %s", keyStyle.apply(key), padding, valueStyle.apply(value));
            });
        }
    }

    /**
     * Log the message and throwable if at or above the given level.
     *
     * @param level   The level.
     * @param thrown  The throwable. May be {@code null}.
     * @param message The message.
     * @param args    The message args.
     */
    public static void log(LogLevel level, Throwable thrown, String message, Object... args) {
        MESSAGES.incrementAndGet();
        if (level == LogLevel.WARN) {
            WARNINGS.incrementAndGet();
        } else if (level == LogLevel.ERROR) {
            ERRORS.incrementAndGet();
        }
        if (message == null) {
            message = "<null>";
        } else {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    args[i] = "<null>";
                }
            }
        }
        LogWriter.write(level, thrown, message, args);
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
