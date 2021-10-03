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

package io.helidon.build.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.helidon.build.util.StyleFunction.BoldBlue;
import static io.helidon.build.util.StyleFunction.Italic;
import static java.util.Objects.requireNonNull;

/**
 * Simple, centralized logging.
 */
public class Log {
    private static final AtomicReference<Writer> WRITER = new AtomicReference<>();
    private static final AtomicInteger MESSAGES = new AtomicInteger();
    private static final AtomicInteger WARNINGS = new AtomicInteger();
    private static final AtomicInteger ERRORS = new AtomicInteger();
    private static final boolean DEBUG = "debug".equals(System.getProperty("log.level"));
    private static final String PAD = " ";

    private Log() {
    }

    /**
     * Levels.
     */
    public enum Level {
        /**
         * Debug level.
         */
        DEBUG(java.util.logging.Level.FINEST),
        /**
         * Verbose level.
         */
        VERBOSE(java.util.logging.Level.FINE),
        /**
         * Info level.
         */
        INFO(java.util.logging.Level.INFO),
        /**
         * Warn level.
         */
        WARN((java.util.logging.Level.WARNING)),
        /**
         * Error level.
         */
        ERROR(java.util.logging.Level.SEVERE);

        /**
         * Returns the corresponding java.util.logging.Level level.
         *
         * @return The level.
         */
        java.util.logging.Level toJulLevel() {
            return julLevel;
        }

        private final java.util.logging.Level julLevel;

        Level(java.util.logging.Level julLevel) {
            this.julLevel = julLevel;
        }
    }

    /**
     * The log writer.
     */
    public interface Writer {

        /**
         * Writes the message and throwable if at or above the given level.
         *
         * @param level The level.
         * @param thrown The throwable. May be {@code null}.
         * @param message The message.
         * @param args The message args.
         */
        void write(Level level, Throwable thrown, String message, Object... args);

        /**
         * Returns whether debug messages will be written.
         *
         * @return {@code true} if enabled.
         */
        boolean isDebug();

        /**
         * Returns whether verbose messages will be written.
         *
         * @return {@code true} if enabled.
         */
        boolean isVerbose();
    }

    /**
     * Sets the writer.
     *
     * @param writer The writer.
     */
    public static void writer(Writer writer) {
        WRITER.set(requireNonNull(writer));
    }

    /**
     * Returns the writer.
     *
     * @return The writer.
     */
    public static Writer writer() {
        Writer writer = WRITER.get();
        if (writer == null) {
            writer = SystemLogWriter.create();
            writer(writer);
        }
        return writer;
    }

    /**
     * Returns whether or not debug messages will be written.
     *
     * @return {@code true} if enabled.
     */
    public static boolean isDebug() {
        return writer().isDebug();
    }

    /**
     * Returns whether or not verbose messages will be written.
     *
     * @return {@code true} if enabled.
     */
    public static boolean isVerbose() {
        return writer().isVerbose();
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
     * @param args The message args.
     */
    public static void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    /**
     * Log a message at VERBOSE level.
     *
     * @param message The message.
     * @param args The message args.
     */
    public static void verbose(String message, Object... args) {
        log(Level.VERBOSE, message, args);
    }

    /**
     * Log an empty message at INFO level.
     */
    public static void info() {
        log(Level.INFO, "");
    }

    /**
     * Log a message at INFO level.
     *
     * @param message The message.
     * @param args The message args.
     */
    public static void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    /**
     * Log the entries using {@link StyleFunction#Italic} for all keys and {@link StyleFunction#BoldBlue} for all keys.
     *
     * @param map The entries.
     */
    public static void info(Map<Object, Object> map) {
        info(map, maxKeyWidth(map));
    }

    /**
     * Log the entries using {@link StyleFunction#Italic} for all keys and {@link StyleFunction#BoldBlue} for all keys.
     *
     * @param map The entries.
     * @param maxKeyWidth The maximum key width.
     */
    public static void info(Map<Object, Object> map, int maxKeyWidth) {
        info(map, maxKeyWidth, Italic, BoldBlue);
    }

    /**
     * Log the entries using the given styles.
     *
     * @param map The entries.
     * @param keyStyle The style to apply to all keys.
     * @param valueStyle The style to apply to all values.
     */
    public static void info(Map<Object, Object> map, StyleFunction keyStyle, StyleFunction valueStyle) {
        info(map, maxKeyWidth(map), keyStyle, valueStyle);
    }

    /**
     * Log the entries using the given styles.
     *
     * @param map The entries.
     * @param maxKeyWidth The maximum key width.
     * @param keyStyle The style to apply to all keys.
     * @param valueStyle The style to apply to all values.
     */
    public static void info(Map<Object, Object> map, int maxKeyWidth, StyleFunction keyStyle, StyleFunction valueStyle) {
        if (!map.isEmpty()) {
            map.forEach((key, value) -> {
                final String padding = padding(maxKeyWidth, key);
                info("%s %s %s", keyStyle.apply(key), padding, valueStyle.apply(value));
            });
        }
    }

    /**
     * Log a message at WARNING level.
     *
     * @param message The message.
     * @param args The message args.
     */
    public static void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    /**
     * Log a warning message with associated throwable.
     *
     * @param thrown The throwable.
     * @param msg Message to be logged.
     * @param args Format string arguments.
     */
    public static void warn(Throwable thrown, String msg, Object... args) {
        log(Level.WARN, thrown, msg, args);
    }

    /**
     * Log a message at ERROR level.
     *
     * @param message The message.
     * @param args The message args.
     */
    public static void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    /**
     * Log a message at ERROR level with associated throwable.
     *
     * @param thrown The throwable.
     * @param message The message.
     * @param args The message args.
     */
    public static void error(Throwable thrown, String message, Object... args) {
        log(Level.ERROR, thrown, message, args);
    }

    /**
     * Log the message if at or above the given level.
     *
     * @param level The level.
     * @param message The message.
     * @param args The message args.
     */
    public static void log(Level level, String message, Object... args) {
        log(level, null, message, args);
    }

    /**
     * Log the message and throwable if at or above the given level.
     *
     * @param level The level.
     * @param thrown The throwable. May be {@code null}.
     * @param message The message.
     * @param args The message args.
     */
    public static void log(Level level, Throwable thrown, String message, Object... args) {
        MESSAGES.incrementAndGet();
        if (level == Level.WARN) {
            WARNINGS.incrementAndGet();
        } else if (level == Level.ERROR) {
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
        writer().write(level, thrown, message, args);
    }

    /**
     * Tests whether a writer has been set.
     *
     * @return {@code true} if set.
     */
    public static boolean hasWriter() {
        return WRITER.get() != null;
    }

    /**
     * Tests whether or not the writer is the {@link SystemLogWriter}.
     *
     * @return {@code true} If the writer is the {@link SystemLogWriter}.
     */
    public static boolean isSystemWriter() {
        return WRITER.get() instanceof SystemLogWriter;
    }

    /**
     * Writes a debug message that will not trigger {@link Writer} lazy initialization. If no writer, has been set, the
     * message is written directly to {@code System.out}.
     *
     * @param message The message.
     * @param args The message arguments.
     */
    public static void preInitDebug(String message, Object... args) {
        // Only use debug() if we already have a writer, otherwise we will end up in a cycle
        if (hasWriter()) {
            debug(message, args);
        } else if (DEBUG) {
            System.out.printf(message + "%n", args);
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

    private static String padding(int maxKeyWidth, Object key) {
        final int keyLen = key.toString().length();
        if (maxKeyWidth > keyLen) {
            return PAD.repeat(maxKeyWidth - keyLen);
        } else {
            return "";
        }
    }
}
