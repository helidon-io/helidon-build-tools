/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.util.ServiceLoader;

import io.helidon.build.common.Log.Level;

/**
 * The {@link Log} writer.
 */
public interface LogWriter {
    /**
     * System property name that may be used by an implementation for setting the log level. The value must be
     * parseable by {@code j.u.l.Level.parse()}.
     */
    String LEVEL_PROPERTY = "log.level";

    /**
     * Writes the record.
     *
     * @param level log level
     * @param thrown exception thrown
     * @param message log message
     * @param args formatting arguments
     */
    void write(Level level, Throwable thrown, String message, Object[] args);

    /**
     * Returns whether debug messages will be written.
     *
     * @return {@code true} if enabled.
     */
    boolean isDebug();

    /**
     * Returns whether verbose messages will be written.
     *
     * @return {@code true} if enabled.
     */
    boolean isVerbose();

    /**
     * Return whether this writer prints to the system {@code stdout} and/or {@code stderr}.
     *
     * @return {@code true} if system.
     */
    default boolean isSystem() {
        return false;
    }

    /**
     * Lazy initialization for the loaded {@link LogWriter}.
     */
    final class Holder {

        private Holder() {
        }

        /**
         * The loaded {@link LogWriter}.
         */
        public static final LogWriter INSTANCE =
                ServiceLoader.load(LogWriter.class)
                             .findFirst()
                             .orElseGet(SystemLogWriter::create);
    }
}
