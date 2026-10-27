/*
 * Copyright (c) 2021, 2026 Oracle and/or its affiliates.
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

import java.util.ServiceLoader;

import io.helidon.build.common.LazyValue;

/**
 * The {@link Log} writer.
 */
public abstract class LogWriter {

    private static final LazyValue<LogWriter> INSTANCE = new LazyValue<>(LogWriter::load);

    /**
     * Create a new instance.
     */
    protected LogWriter() {
    }

    /**
     * Write the entry.
     *
     * @param level   log level
     * @param thrown  exception thrown
     * @param message log message
     * @param args    formatting arguments
     */
    public static void write(LogLevel level, Throwable thrown, String message, Object... args) {
        INSTANCE.get().writeEntry(level, thrown, message, args);
    }

    /**
     * Writes the record.
     *
     * @param level   log level
     * @param thrown  exception thrown
     * @param message log message
     * @param args    formatting arguments
     */
    public abstract void writeEntry(LogLevel level, Throwable thrown, String message, Object... args);

    /**
     * Record a formatted log entry.
     *
     * @param level log level
     * @param entry log entry
     */
    protected final void recordEntry(LogLevel level, String entry) {
        LogRecorder.recordEntry(level, entry);
    }

    /**
     * Get the lowest recorder log level.
     *
     * @return level
     */
    protected LogLevel recordLevel() {
        return LogRecorder.recordLevel();
    }

    /**
     * Load the log writer implementation.
     *
     * @throws IllegalStateException if the loaded instance is {@code null}
     */
    static void init() throws IllegalStateException {
        if (INSTANCE.get() == null) {
            throw new IllegalStateException("Unable to load log writer");
        }
    }

    private static LogWriter load() {
        return ServiceLoader.load(LogWriter.class)
                .findFirst()
                .orElse(SystemLogWriter.INSTANCE);
    }
}
