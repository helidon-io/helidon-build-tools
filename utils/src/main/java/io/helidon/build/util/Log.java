/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Simple, centralized logging.
 */
public abstract class Log {
    private static final AtomicReference<Writer> WRITER = new AtomicReference<>();

    /**
     * Levels.
     */
    public enum Level {
        /**
         * Debug level.
         */
        DEBUG,
        /**
         * Info level.
         */
        INFO,
        /**
         * Warn level.
         */
        WARN,
        /**
         * Error level.
         */
        ERROR
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
         * Returns whether or not debug messages will be written.
         *
         * @return {@code true} if enabled.
         */
        boolean isDebugEnabled();
    }

    /**
     * Sets the writer.
     *
     * @param writer The writer.
     */
    public static void setWriter(Writer writer) {
        WRITER.set(requireNonNull(writer));
    }

    /**
     * Returns whether or not debug messages will be written.
     *
     * @return {@code true} if enabled.
     */
    public static boolean isDebugEnabled() {
        return WRITER.get().isDebugEnabled();
    }

    /**
     * Log a message at FINE level.
     *
     * @param message The message.
     * @param args The message args.
     */
    public static void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
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
        writer().write(level, thrown, message, args);
    }

    private static Writer writer() {
        Writer writer = WRITER.get();
        if (writer == null) {
            writer = SystemLogWriter.create(Level.INFO);
            setWriter(writer);
        }
        return writer;
    }
}
