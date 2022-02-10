/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * A formatter that applies styles to log message based on the log level.
 */
public abstract class LogFormatter {

    /**
     * Format a log entry.
     *
     * @param level   The level
     * @param thrown  The exception, may be {@code null}
     * @param message The message
     * @param args    The message arguments, may be {@code null}
     * @return formatted message
     */
    public abstract String formatEntry(LogLevel level, Throwable thrown, String message, Object... args);

    /**
     * Create a function that formats a message with the given level.
     *
     * @param level The level
     * @return the function
     */
    public static Function<String, String> of(LogLevel level) {
        return msg -> Holder.INSTANCE.formatEntry(level, null, msg);
    }

    /**
     * Format a log message.
     *
     * @param level   The level
     * @param thrown  The exception, may be {@code null}
     * @param message The message
     * @param args    The message arguments, may be {@code null}
     * @return formatted message
     */
    public static String format(LogLevel level, Throwable thrown, String message, Object... args) {
        return Holder.INSTANCE.formatEntry(level, thrown, message, args);
    }

    /**
     * Test if the given level implies debug.
     *
     * @param level level
     * @return {@code true} if debug, {@code false} otherwise
     */
    public static boolean isDebug(LogLevel level) {
        return LogLevel.DEBUG.ordinal() >= level.ordinal();
    }

    /**
     * Test if the given level implies verbose.
     *
     * @param level level
     * @return {@code true} if verbose, {@code false} otherwise
     */
    public static boolean isVerbose(LogLevel level) {
        return LogLevel.VERBOSE.ordinal() >= level.ordinal();
    }

    /**
     * Load the log formatter implementation.
     *
     * @throws IllegalStateException if the loaded instance is {@code null}
     */
    static void init() throws IllegalStateException {
        if (Holder.INSTANCE == null) {
            throw new IllegalStateException("Unable to load log formatter");
        }
    }

    private static final class Holder {

        private Holder() {
        }

        static final LogFormatter INSTANCE = ServiceLoader.load(LogFormatter.class, LogFormatter.class.getClassLoader())
                                                          .findFirst()
                                                          .orElse(DefaultFormatter.INSTANCE);
    }
}
