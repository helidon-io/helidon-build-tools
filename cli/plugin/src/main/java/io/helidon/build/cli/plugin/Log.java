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
package io.helidon.build.cli.plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.helidon.build.cli.plugin.Style.style;
import static java.util.Objects.requireNonNull;

/**
 * Simple logging.
 */
class Log {
    /**
     * Line separator.
     */
    public static final String EOL = System.getProperty("line.separator");
    private static final AtomicReference<Consumer<String>> OUT = new AtomicReference<>(System.out::println);
    private static final AtomicReference<Verbosity> VERBOSITY = new AtomicReference<>(Verbosity.NORMAL);
    private static final String DEBUG_STYLE = "italic";
    private static final String WARN_STYLE = "YELLOW";
    private static final String ERROR_STYLE = "red";

    /**
     * Verbosity levels.
     */
    enum Verbosity {
        /**
         * Normal level.
         */
        NORMAL(false, false),
        /**
         * Verbose level.
         */
        VERBOSE(true, false),
        /**
         * Debug level.
         */
        DEBUG(true, true);

        private final boolean verbose;
        private final boolean debug;

        Verbosity(boolean verbose, boolean debug) {
            this.verbose = verbose;
            this.debug = debug;
        }

        /**
         * Tests whether verbose logging is enabled.
         *
         * @return {@code true} if verbose.
         */
        boolean isVerbose() {
            return verbose;
        }

        /**
         * Tests whether debug logging is enabled.
         *
         * @return {@code true} if verbose.
         */
        boolean isDebug() {
            return debug;
        }
    }

    /**
     * Sets the verbosity level.
     *
     * @param verbosity The level.
     */
    static void verbosity(Verbosity verbosity) {
        VERBOSITY.set(requireNonNull(verbosity));
    }

    /**
     * Returns the verbosity level.
     *
     * @return The level.
     */
    static Verbosity verbosity() {
        return VERBOSITY.get();
    }

    /**
     * Get the output.
     *
     * @return consumer of string
     */
    static Consumer<String> output() {
        return OUT.get();
    }

    /**
     * Sets the output consumer.
     *
     * @param outputConsumer The output consumer.
     */
    static void output(Consumer<String> outputConsumer) {
        OUT.set(outputConsumer);
    }

    /**
     * Returns whether debug messages will be written.
     *
     * @return {@code true} if enabled.
     */
    static boolean isDebug() {
        return verbosity().isDebug();
    }

    /**
     * Returns whether verbose messages will be written.
     *
     * @return {@code true} if enabled.
     */
    static boolean isVerbose() {
        return verbosity().isVerbose();
    }

    /**
     * Log a message if debug is enabled.
     *
     * @param message The message.
     * @param args    The message args.
     */
    static void debug(String message, Object... args) {
        if (isDebug()) {
            log("$(italic " + message + ")", args);
        }
    }

    /**
     * Log a message if verbose is enabled.
     *
     * @param message The message.
     * @param args    The message args.
     */
    @SuppressWarnings("unused")
    static void verbose(String message, Object... args) {
        if (isVerbose()) {
            log(message, args);
        }
    }

    /**
     * Log an empty message.
     */
    @SuppressWarnings("unused")
    static void info() {
        log("");
    }

    /**
     * Log a message.
     *
     * @param message The message.
     * @param args    The message args.
     */
    static void info(String message, Object... args) {
        log(message, args);
    }

    /**
     * Log a warning message.
     *
     * @param message The message.
     * @param args    The message args.
     */
    @SuppressWarnings("unused")
    static void warn(String message, Object... args) {
        log(style(WARN_STYLE, message, args));
    }

    /**
     * Log a warning message with associated throwable.
     *
     * @param thrown The throwable.
     * @param msg    Message to be logged.
     * @param args   Format string arguments.
     */
    @SuppressWarnings("unused")
    static void warn(Throwable thrown, String msg, Object... args) {
        log(thrown, style(WARN_STYLE, msg, args));
    }

    /**
     * Log an error message.
     *
     * @param message The message.
     * @param args    The message args.
     */
    static void error(String message, Object... args) {
        log(style(ERROR_STYLE, message, args));
    }

    /**
     * Log an error message with associated throwable.
     *
     * @param thrown The throwable.
     * @param msg    Message to be logged.
     * @param args   Format string arguments.
     */
    @SuppressWarnings("unused")
    static void error(Throwable thrown, String msg, Object... args) {
        log(thrown, style(ERROR_STYLE, msg, args));
    }

    private static void log(String message, Object... args) {
        if (message != null) {
            Consumer<String> consumer = OUT.get();
            if (consumer != null) {
                consumer.accept(String.format(message, args));
            }
        }
    }

    private static void log(Throwable thrown, String message, Object... args) {
        Consumer<String> consumer = OUT.get();
        if (consumer != null) {
            final String trace = toStackTrace(thrown);
            String msg = message == null ? "" : String.format(message, args);
            if (trace != null) {
                msg += (msg + EOL + trace);
            }
            consumer.accept(msg);
        }
    }

    private static String toStackTrace(Throwable thrown) {
        if (thrown != null) {
            if (isDebug()) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return style(DEBUG_STYLE, sw.toString());
                } catch (Exception ignored) {
                }
            } else if (isVerbose()) {
                return style(DEBUG_STYLE, thrown.toString());
            }
        }
        return null;
    }

    private Log() {
    }
}

