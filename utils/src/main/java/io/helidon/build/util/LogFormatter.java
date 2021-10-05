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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.util.Log.Level;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.Log.Level.DEBUG;
import static io.helidon.build.util.StyleFunction.BoldYellow;
import static io.helidon.build.util.StyleFunction.Italic;
import static io.helidon.build.util.StyleFunction.ItalicRed;
import static io.helidon.build.util.StyleFunction.Plain;
import static io.helidon.build.util.StyleFunction.Red;

/**
 * A formatter that applies styles to log message based on the log level.
 */
public final class LogFormatter {

    private static final boolean STYLES_ENABLED = AnsiConsoleInstaller.areAnsiEscapesEnabled();
    private static final String WARN_PREFIX = STYLES_ENABLED ? BoldYellow.apply("warning: ") : "WARNING: ";
    private static final String ERROR_PREFIX = STYLES_ENABLED ? Red.apply("error: ") : "ERROR: ";
    private static final Map<Level, StyleFunction> DEFAULT_STYLES = defaultStyles();

    private LogFormatter() {
        // cannot be instantiated
    }

    /**
     * Create a function that formats a message with the given level.
     *
     * @param level The level
     * @return the function
     */
    public static Function<String, String> of(Level level) {
        return s -> formatEntry(level, s);
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
    public static String format(Level level, Throwable thrown, String message, Object... args) {
        final String msg = formatEntry(level, thrown, message, args);
        switch (level) {
            case DEBUG:
            case VERBOSE:
            case INFO:
                return msg;

            case WARN:
                return WARN_PREFIX + msg;

            case ERROR:
                return ERROR_PREFIX + msg;

            default:
                throw new Error();
        }
    }

    private static Map<Level, StyleFunction> defaultStyles() {
        final Map<Level, StyleFunction> styles = new EnumMap<>(Level.class);
        styles.put(DEBUG, Italic);
        styles.put(Level.VERBOSE, Plain);
        styles.put(Level.INFO, Plain);
        styles.put(Level.WARN, Plain);
        styles.put(Level.ERROR, ItalicRed);
        return styles;
    }

    private static String formatEntry(Level level, String message) {
        return toStyled(level, StyleRenderer.render(message));
    }

    private static String formatEntry(Level level, Throwable thrown, String message, Object... args) {
        final String rendered = StyleRenderer.render(message, args);
        final String styled = toStyled(level, rendered);
        final String trace = toStyled(level, thrown);
        if (trace == null) {
            return styled;
        } else if (styled.isEmpty()) {
            return trace;
        } else {
            return styled + EOL + trace;
        }
    }

    private static String toStyled(Level level, String message) {
        return Style.isStyled(message) ? message : style(level, message);
    }

    private static String toStyled(Level level, Throwable thrown) {
        if (thrown != null) {
            if (isDebug(level)) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return style(DEBUG, sw.toString());
                } catch (Exception ignored) {
                }
            } else if (isVerbose(level)) {
                return style(DEBUG, thrown.toString());
            }
        }
        return null;
    }

    private static String style(Level level, String message) {
        final StyleFunction style = DEFAULT_STYLES.get(level);
        return style == Plain ? message : style.apply(message);
    }

    private static boolean isDebug(Level level) {
        return DEBUG.ordinal() >= level.ordinal();
    }

    private static boolean isVerbose(Level level) {
        return Level.VERBOSE.ordinal() >= level.ordinal();
    }
}
