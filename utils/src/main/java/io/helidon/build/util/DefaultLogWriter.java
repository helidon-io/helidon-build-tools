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

import java.io.PrintStream;

import io.helidon.build.util.Log.Level;

import static io.helidon.build.util.Log.Level.DEBUG;

/**
 * {@link Log.Writer} that writes using two {@link PrintStream}.
 * Supports use of {@link StyleRenderer} substitutions in log messages via {@link LogFormatter}.
 */
public abstract class DefaultLogWriter implements Log.Writer {

    private static final String DEFAULT_LEVEL = "info";
    private static final String LEVEL_PROPERTY = "log.level";

    private int ordinal;

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
            final String msg = LogFormatter.format(level, thrown, message, args);
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
