/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

import io.helidon.build.util.Log.Level;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.Log.Level.DEBUG;
import static io.helidon.build.util.Styles.BoldYellow;
import static io.helidon.build.util.Styles.Italic;
import static io.helidon.build.util.Styles.ItalicRed;
import static io.helidon.build.util.Styles.Plain;
import static io.helidon.build.util.Styles.Red;

/**
 * {@link Log.Writer} that writes to {@link System#out} and {@link System#err}. Supports use of
 * {@link StyleRenderer} substitutions in log messages.
 */
public final class SystemLogWriter implements Log.Writer {
    private static final boolean STYLES_ENABLED = AnsiConsoleInstaller.areAnsiEscapesEnabled();
    private static final String WARN_PREFIX = STYLES_ENABLED ? BoldYellow.apply("warning: ") : "WARNING: ";
    private static final String ERROR_PREFIX = STYLES_ENABLED ? Red.apply("error: ") : "ERROR: ";
    private static final String DEFAULT_LEVEL = "info";
    private static final String LEVEL_PROPERTY = "log.level";
    private static final Map<Level, Styles> DEFAULT_STYLES = defaultStyles();
    private final Map<Level, Styles> styles;
    private int ordinal;

    private static Map<Level, Styles> defaultStyles() {
        final Map<Level, Styles> styles = new EnumMap<>(Level.class);
        styles.put(DEBUG, Italic);
        styles.put(Level.VERBOSE, Plain);
        styles.put(Level.INFO, Plain);
        styles.put(Level.WARN, Plain);
        styles.put(Level.ERROR, ItalicRed);
        return styles;
    }

    /**
     * Installs an instance of this type as the writer in {@code io.helidon.build.util.Log} at the given level.
     *
     * @param level The level.
     * @return The instance.
     */
    public static SystemLogWriter install(Level level) {
        final SystemLogWriter writer = create(level);
        Log.writer(writer);
        return writer;
    }

    /**
     * Returns a new instance.
     *
     * @return The instance.
     */
    public static SystemLogWriter create() {
        final Level level = Level.valueOf(System.getProperty(LEVEL_PROPERTY, DEFAULT_LEVEL).toUpperCase());
        return create(level);
    }

    /**
     * Returns a new instance with the given level.
     *
     * @param level The level at or above which messages should be logged.
     * @return The instance.
     */
    public static SystemLogWriter create(Level level) {
        return create(level, DEFAULT_STYLES);
    }

    /**
     * Returns a new instance with the given level.
     *
     * @param level The level at or above which messages should be logged.
     * @param styles The style to apply to messages at a given level.
     * @return The instance.
     */
    public static SystemLogWriter create(Level level, Map<Level, Styles> styles) {
        return new SystemLogWriter(level, styles);
    }

    private SystemLogWriter(Level level, Map<Level, Styles> styles) {
        this.styles = styles;
        level(level);
    }

    /**
     * Sets the level.
     *
     * @param level The new level.
     */
    public void level(Level level) {
        this.ordinal = level.ordinal();
    }

    @Override
    public boolean isDebug() {
        return DEBUG.ordinal() >= ordinal;
    }

    @Override
    public boolean isVerbose() {
        return Level.VERBOSE.ordinal() >= ordinal;
    }

    @Override
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    public void write(Level level, Throwable thrown, String message, Object... args) {
        if (level.ordinal() >= ordinal) {
            final String msg = toStyled(level, thrown, message, args);
            switch (level) {
                case DEBUG:
                case VERBOSE:
                case INFO: {
                    System.out.println(msg);
                    break;
                }

                case WARN: {
                    System.err.println(WARN_PREFIX + msg);
                    break;
                }

                case ERROR: {
                    System.err.println(ERROR_PREFIX + msg);
                    break;
                }

                default: {
                    throw new Error();
                }
            }
        }
    }

    private String toStyled(Level level, Throwable thrown, String message, Object... args) {
        final String rendered = StyleRenderer.render(message, args);
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
        return Style.isStyled(message) ? message : style(level, message);
    }

    private String toStackTrace(Throwable thrown) {
        if (thrown != null) {
            if (isDebug()) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return style(DEBUG, sw.toString());
                } catch (Exception ignored) {
                }
            } else if (isVerbose()) {
                return style(DEBUG, thrown.toString());
            }
        }
        return null;
    }

    private String style(Level level, String message) {
        final Styles style = styles.get(level);
        return style == Plain ? message : style.apply(message);
    }
}
