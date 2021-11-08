/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import io.helidon.build.common.Log.Level;

/**
 * {@link LogWriter} that writes to {@link System#out} and {@link System#err}.
 */
public class SystemLogWriter implements LogWriter {

    private static final String WARN_PREFIX = "WARNING: ";
    private static final String ERROR_PREFIX = "ERROR: ";
    private static final String EOL = System.getProperty("line.separator");
    private static final String DEFAULT_LEVEL = "info";
    private int ordinal;

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
        return new SystemLogWriter(level);
    }

    /**
     * Create a new instance.
     *
     * @param level The level at or above which messages should be logged.
     */
    protected SystemLogWriter(Level level) {
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

    /**
     * Get the level.
     *
     * @return level
     */
    public Level level() {
        return Level.values()[ordinal];
    }

    @Override
    public boolean isDebug() {
        return Level.DEBUG.ordinal() >= ordinal;
    }

    @Override
    public boolean isVerbose() {
        return Level.VERBOSE.ordinal() >= ordinal;
    }

    @Override
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    public void write(Level level, Throwable thrown, String message, Object[] args) {
        if (level.ordinal() >= ordinal) {
            final String msg = render(thrown, message, args);
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

    private String render(Throwable thrown, String message, Object[] args) {
        String rendered = String.format(message, args);
        String trace = null;
        if (thrown != null) {
            if (isDebug()) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    trace = sw.toString();
                } catch (Exception ignored) {
                }
            } else if (isVerbose()) {
                trace = thrown.toString();
            }
        }
        if (trace == null) {
            return rendered;
        } else if (rendered.isEmpty()) {
            return trace;
        } else {
            return rendered + EOL + trace;
        }
    }
}
