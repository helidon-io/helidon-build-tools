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
package io.helidon.build.common.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The {@link Log} writer.
 */
public abstract class LogWriter {

    private final List<LogRecorder> recorders = Collections.synchronizedList(new ArrayList<>());

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
        Holder.INSTANCE.writeEntry(level, thrown, message, args);
    }

    /**
     * Add a new log recorder.
     *
     * @param recorder log recorder
     */
    public static void addRecorder(LogRecorder recorder) {
        Holder.INSTANCE.recorders.add(recorder);
    }

    /**
     * Remove a log recorder.
     *
     * @param recorder log recorder
     */
    public static void removeRecorder(LogRecorder recorder) {
        Holder.INSTANCE.recorders.remove(recorder);
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
     * @param entry log entry
     */
    protected final void recordEntry(String entry) {
        for (LogRecorder recorder : recorders) {
            recorder.addEntry(entry);
        }
    }

    /**
     * Load the log writer implementation.
     *
     * @throws IllegalStateException if the loaded instance is {@code null}
     */
    static void init() throws IllegalStateException {
        if (Holder.INSTANCE == null) {
            throw new IllegalStateException("Unable to load log writer");
        }
    }

    private static final class Holder {

        private Holder() {
        }

        static final LogWriter INSTANCE = ServiceLoader.load(LogWriter.class)
                                                       .findFirst()
                                                       .orElse(SystemLogWriter.INSTANCE);
    }
}
