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
package io.helidon.build.common.ansi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.common.Log.Level;
import io.helidon.build.common.LogFormatter;
import io.helidon.build.common.RichTextRenderer;

import static io.helidon.build.common.LogFormatter.isDebug;
import static io.helidon.build.common.LogFormatter.isVerbose;
import static io.helidon.build.common.ansi.AnsiTextProvider.ANSI_ENABLED;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldYellow;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;
import static io.helidon.build.common.ansi.AnsiTextStyles.ItalicRed;
import static io.helidon.build.common.ansi.AnsiTextStyles.Plain;
import static io.helidon.build.common.ansi.AnsiTextStyles.Red;

/**
 * {@link LogFormatter} that supports use of {@link RichTextRenderer} substitutions in log messages.
 */
public final class AnsiLogFormatter implements LogFormatter {

    private static final String EOL = System.getProperty("line.separator");
    private static final boolean STYLES_ENABLED = ANSI_ENABLED.instance();
    private static final String WARN_PREFIX = STYLES_ENABLED ? BoldYellow.apply("warning: ") : "WARNING: ";
    private static final String ERROR_PREFIX = STYLES_ENABLED ? Red.apply("error: ") : "ERROR: ";
    private static final Map<Level, AnsiTextStyles> STYLES = defaultStyles();

    private static Map<Level, AnsiTextStyles> defaultStyles() {
        final Map<Level, AnsiTextStyles> styles = new EnumMap<>(Level.class);
        styles.put(Level.DEBUG, Italic);
        styles.put(Level.VERBOSE, Plain);
        styles.put(Level.INFO, Plain);
        styles.put(Level.WARN, Plain);
        styles.put(Level.ERROR, ItalicRed);
        return styles;
    }

    /**
     * Create a new instance.
     */
    public AnsiLogFormatter() {
    }

    @Override
    public String formatMessage(Level level, Throwable thrown, String message, Object... args) {
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

    @Override
    public Function<String, String> formatFunction(Level level) {
        return s -> formatEntry(level, s);
    }

    private String formatEntry(Level level, String message) {
        return toStyled(level,  RichTextRenderer.render(message));
    }

    private String formatEntry(Level level, Throwable thrown, String message, Object... args) {
        final String rendered = RichTextRenderer.render(message, args);
        final String styled = toStyled(level, rendered);
        final String trace = toStackTrace(thrown, level);
        if (trace == null) {
            return styled;
        } else if (styled.isEmpty()) {
            return trace;
        } else {
            return styled + EOL + trace;
        }
    }

    private String toStyled(Level level, String message) {
        return AnsiTextStyle.isStyled(message) ? message : style(level, message);
    }

    private String toStackTrace(Throwable thrown, Level level) {
        if (thrown != null) {
            if (isDebug(level)) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return style(Level.DEBUG, sw.toString());
                } catch (Exception ignored) {
                }
            } else if (isVerbose(level)) {
                return style(Level.DEBUG, thrown.toString());
            }
        }
        return null;
    }

    private String style(Level level, String message) {
        final AnsiTextStyles style = STYLES.get(level);
        return style == Plain ? message : style.apply(message);
    }
}
