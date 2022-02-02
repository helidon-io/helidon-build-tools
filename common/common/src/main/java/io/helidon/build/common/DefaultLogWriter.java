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

import java.io.PrintStream;

import io.helidon.build.common.Log.Level;

import static io.helidon.build.common.Log.Level.DEBUG;

/**
 * {@link LogWriter} that writes using two {@link PrintStream}.
 * Supports use of {@link RichTextRenderer} substitutions in log messages via {@link LogFormatter}.
 */
public abstract class DefaultLogWriter implements LogWriter {

    private static final String DEFAULT_LEVEL = "info";

    private int ordinal;
    private final LogFormatter formatter;

    private static Level defaultLevel() {
        return Level.valueOf(System.getProperty(LEVEL_PROPERTY, DEFAULT_LEVEL).toUpperCase());
    }

    /**
     * Create a new instance.
     */
    public DefaultLogWriter() {
        this(defaultLevel());
    }

    /**
     * Create a new instance.
     *
     * @param level The level at or above which messages should be logged.
     */
    public DefaultLogWriter(Level level) {
        // force early initialization of the log formatter
        formatter = LogFormatter.Holder.INSTANCE;
        level(level);
    }

    /**
     * Get the print stream used for {@link Level#DEBUG}, {@link Level#VERBOSE} and {@link Level#INFO}.
     *
     * @return print stream
     */
    public abstract PrintStream stdOut();

    /**
     * Get the print stream used for {@link Level#WARN}, {@link Level#ERROR}.
     *
     * @return print stream
     */
    public abstract PrintStream stdErr();

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
            final String msg = formatter.formatMessage(level, thrown, message, args);
            switch (level) {
                case DEBUG:
                case VERBOSE:
                case INFO: {
                    stdOut().println(msg);
                    break;
                }

                case WARN:
                case ERROR: {
                    stdErr().println(msg);
                    break;
                }

                default: {
                    throw new Error();
                }
            }
        }
    }
}
