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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.Map;

import io.helidon.build.common.Log.Level;
import io.helidon.build.common.LogWriter;
import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.SystemLogWriter;

import static io.helidon.build.common.ansi.AnsiTextStyles.BoldYellow;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;
import static io.helidon.build.common.ansi.AnsiTextStyles.ItalicRed;
import static io.helidon.build.common.ansi.AnsiTextStyles.Plain;
import static io.helidon.build.common.ansi.AnsiTextStyles.Red;

/**
 * {@link LogWriter} that writes to {@link System#out} and {@link System#err}. Supports use of
 * {@link RichTextRenderer} substitutions in log messages.
 */
public final class AnsiLogWriter extends SystemLogWriter {

    private static final String EOL = System.getProperty("line.separator");
    private static final boolean STYLES_ENABLED = AnsiConsoleInstaller.areAnsiEscapesEnabled();
    private static final String WARN_PREFIX = STYLES_ENABLED ? BoldYellow.apply("warning: ") : "WARNING: ";
    private static final String ERROR_PREFIX = STYLES_ENABLED ? Red.apply("error: ") : "ERROR: ";
    private static final String DEFAULT_LEVEL = "info";
    private static final String LEVEL_PROPERTY = "log.level";
    private static final Map<Level, AnsiTextStyles> DEFAULT_STYLES = defaultStyles();
    private final Map<Level, AnsiTextStyles> styles;

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
    public AnsiLogWriter() {
        this(Level.valueOf(System.getProperty(LEVEL_PROPERTY, DEFAULT_LEVEL).toUpperCase()));
    }

    /**
     * Returns a new instance with the given level.
     *
     * @param level The level at or above which messages should be logged.
     */
    public AnsiLogWriter(Level level) {
        this(level, DEFAULT_STYLES);
    }

    /**
     * Create a new instance with the given level.
     *
     * @param level  The level at or above which messages should be logged.
     * @param styles The style to apply to messages at a given level.
     */
    public AnsiLogWriter(Level level, Map<Level, AnsiTextStyles> styles) {
        super(level);
        this.styles = styles;
    }

    @Override
    public void write(Level level, Throwable thrown, String message, Object[] args) {
        if (level.ordinal() >= level().ordinal()) {
            final String msg = render(level, thrown, message, args);
            switch (level) {
                case DEBUG:
                case VERBOSE:
                case INFO:
                    System.out.println(msg);
                    break;
                case WARN:
                    System.err.println(WARN_PREFIX + msg);
                    break;
                case ERROR:
                    System.err.println(ERROR_PREFIX + msg);
                    break;
                default:
                    throw new Error();
            }
        }
    }

    private String render(Level level, Throwable thrown, String message, Object... args) {
        final String rendered = RichTextRenderer.render(message, args);
        final String styled = toStyled(level, rendered);
        final String trace = toStackTrace(thrown);
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

    private String toStackTrace(Throwable thrown) {
        if (thrown != null) {
            if (isDebug()) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return style(Level.DEBUG, sw.toString());
                } catch (Exception ignored) {
                }
            } else if (isVerbose()) {
                return style(Level.DEBUG, thrown.toString());
            }
        }
        return null;
    }

    private String style(Level level, String message) {
        final AnsiTextStyles style = styles.get(level);
        return style == Plain ? message : style.apply(message);
    }
}
