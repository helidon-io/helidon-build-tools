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

import io.helidon.build.util.Log.Level;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.Style.BoldRed;
import static io.helidon.build.util.Style.Red;
import static io.helidon.build.util.Style.Yellow;
import static org.fusesource.jansi.Ansi.Attribute.ITALIC;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * {@link Log.Writer} that writes to {@link System#out} and {@link System#err}. Supports use of
 * {@link StyleRenderer} substitutions in log messages.
 */
public final class SystemLogWriter implements Log.Writer {
    private static final boolean STYLES_ENABLED = AnsiConsoleInstaller.areAnsiEscapesEnabled();
    private static final String DEFAULT_LEVEL = "info";
    private static final String LEVEL_PROPERTY = "log.level";
    private int ordinal;

    /**
     * Binds an instance of this type to the {@code io.helidon.build.util.Log} at the given level.
     *
     * @param level The level.
     * @return The instance.
     */
    public static SystemLogWriter bind(Level level) {
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
        return new SystemLogWriter(level);
    }

    /**
     * Returns a new instance with the given level.
     *
     * @param level The level at or above which messages should be logged.
     * @return The instance.
     */
    public static SystemLogWriter create(Level level) {
        return new SystemLogWriter(level);
    }

    private SystemLogWriter(Level level) {
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
    public boolean isDebugEnabled() {
        return Level.DEBUG.ordinal() >= ordinal;
    }

    @Override
    public boolean isVerboseEnabled() {
        return Level.VERBOSE.ordinal() >= ordinal;
    }

    @Override
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    public void write(Level level, Throwable thrown, String message, Object... args) {
        if (level.ordinal() >= ordinal) {
            final String rendered = Style.render(message, args);
            final boolean isStyled = Style.isStyled(rendered);
            switch (level) {
                case DEBUG: {
                    System.out.println(isStyled ? rendered : ansi().a(ITALIC).a(rendered).reset());
                    break;
                }

                case VERBOSE:
                case INFO: {
                    System.out.println(rendered);
                    break;
                }

                case WARN: {
                    final String msg = toStyled(rendered, isStyled, Yellow, thrown);
                    System.err.println(STYLES_ENABLED ? msg : "WARNING: " + msg);
                    break;
                }

                case ERROR: {
                    final String msg = toStyled(rendered, isStyled, BoldRed, thrown);
                    System.err.println(STYLES_ENABLED ? msg : "ERROR: " + msg);
                    break;
                }

                default: {
                    throw new Error();
                }
            }
        }
    }

    private static String toStyled(String message, boolean isStyled, Style defaultStyle) {
        return isStyled ? message : defaultStyle.apply(message);
    }

    private String toStyled(String message, boolean isStyled, Style defaultStyle, Throwable thrown) {
        final String styled = toStyled(message, isStyled, defaultStyle);
        final String trace = toStackTrace(thrown);
        if (trace == null) {
            return styled;
        } else if (styled.isEmpty()) {
            return trace;
        } else {
            return styled + EOL + EOL + trace;
        }
    }

    private static String toStackTrace(Throwable thrown) {
        if (thrown != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                thrown.printStackTrace(pw);
                pw.close();
                return Red.apply(sw.toString());
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
