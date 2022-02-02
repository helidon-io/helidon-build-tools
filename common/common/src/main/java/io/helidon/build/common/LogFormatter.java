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
package io.helidon.build.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ServiceLoader;
import java.util.function.Function;

import io.helidon.build.common.Log.Level;

/**
 * A formatter that applies styles to log message based on the log level.
 */
public interface LogFormatter {

    /**
     * Format a log message.
     *
     * @param level   The level
     * @param thrown  The exception, may be {@code null}
     * @param message The message
     * @param args    The message arguments, may be {@code null}
     * @return formatted message
     */
    String formatMessage(Level level, Throwable thrown, String message, Object... args);

    /**
     * Create a function that formats a message with the given level.
     *
     * @param level level
     * @return function
     */
    Function<String, String> formatFunction(Level level);

    /**
     * Create a function that formats a message with the given level.
     *
     * @param level The level
     * @return the function
     */
    static Function<String, String> of(Level level) {
        return Holder.INSTANCE.formatFunction(level);
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
    static String format(Level level, Throwable thrown, String message, Object... args) {
        return Holder.INSTANCE.formatMessage(level, thrown, message, args);
    }

    /**
     * Test if the given level implies debug.
     * @param level level
     * @return {@code true} if debug, {@code false} otherwise
     */
    static boolean isDebug(Level level) {
        return Level.DEBUG.ordinal() >= level.ordinal();
    }

    /**
     * Test if the given level implies verbose.
     * @param level level
     * @return {@code true} if verbose, {@code false} otherwise
     */
    static boolean isVerbose(Level level) {
        return Level.VERBOSE.ordinal() >= level.ordinal();
    }

    /**
     * Lazy initialization for the loaded provider.
     */
    final class Holder {

        private Holder() {
        }

        /**
         * The loaded formatter.
         */
        public static final LogFormatter INSTANCE =
                ServiceLoader.load(LogFormatter.class, LogFormatter.class.getClassLoader())
                             .findFirst()
                             .orElse(LogFormatter.DefaultFormatter.INSTANCE);
    }

    /**
     * Default log formatter implementation.
     */
    final class DefaultFormatter implements LogFormatter {

        /**
         * Singleton instance.
         */
        static final DefaultFormatter INSTANCE = new DefaultFormatter();
        private static final String EOL = System.getProperty("line.separator");

        private DefaultFormatter() {
        }

        @Override
        public String formatMessage(Level level, Throwable thrown, String message, Object... args) {
            final String rendered = RichTextRenderer.render(message, args);
            final String trace = trace(level, thrown);
            if (trace == null) {
                return rendered;
            } else if (rendered.isEmpty()) {
                return trace;
            } else {
                return rendered + EOL + trace;
            }
        }

        @Override
        public Function<String, String> formatFunction(Level level) {
            return RichTextRenderer::render;
        }

        private static String trace(Level level, Throwable thrown) {
            final StringWriter sw = new StringWriter();
            if (thrown != null) {
                if (isDebug(level)) {
                    try (PrintWriter pw = new PrintWriter(sw)) {
                        thrown.printStackTrace(pw);
                        return sw.toString();
                    } catch (Exception ignored) {
                    }
                } else if (isVerbose(level)) {
                    return thrown.toString();
                }
            }
            return null;
        }
    }
}
