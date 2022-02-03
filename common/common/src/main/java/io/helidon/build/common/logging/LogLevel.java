/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.logging;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Levels.
 */
public enum LogLevel {

    /**
     * Debug level.
     */
    DEBUG,

    /**
     * Verbose level.
     */
    VERBOSE,

    /**
     * Info level.
     */
    INFO,

    /**
     * Warn level.
     */
    WARN,

    /**
     * Error level.
     */
    ERROR;

    /**
     * System property name that may be used by an implementation for setting the log level.
     */
    public static final String LEVEL_PROPERTY = "log.level";

    private static final String DEFAULT_LEVEL = "info";
    private static final AtomicReference<LogLevel> VALUE = new AtomicReference<>(defaultLevel());

    private static LogLevel defaultLevel() {
        return LogLevel.valueOf(System.getProperty(LEVEL_PROPERTY, DEFAULT_LEVEL).toUpperCase());
    }

    /**
     * Set the log level value.
     *
     * @param level new level
     */
    public static void set(LogLevel level) {
        VALUE.set(requireNonNull(level, "level is null"));
    }

    /**
     * Get the current log level value.
     *
     * @return current level
     */
    public static LogLevel get() {
        return VALUE.get();
    }

    /**
     * Returns whether debug messages will be written.
     *
     * @return {@code true} if enabled.
     */
    public static boolean isDebug() {
        return get() == DEBUG;
    }

    /**
     * Returns whether verbose messages will be written.
     *
     * @return {@code true} if enabled.
     */
    public static boolean isVerbose() {
        return get() == VERBOSE;
    }
}
