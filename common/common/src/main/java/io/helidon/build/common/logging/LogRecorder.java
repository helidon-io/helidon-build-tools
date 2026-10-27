/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;

/**
 * Log recorder.
 */
public class LogRecorder implements AutoCloseable {

    private static final String EOL = System.lineSeparator();
    private static final ThreadLocal<Deque<RecorderInfo>> THREAD_LOCAL = ThreadLocal.withInitial(ArrayDeque::new);

    private final List<String> entries = new LinkedList<>();
    private final LogLevel level;

    /**
     * Create a new instance.
     *
     * @param level level
     */
    public LogRecorder(LogLevel level) {
        this.level = level;
    }

    /**
     * Start recording.
     *
     * @return this instance
     */
    public LogRecorder start() {
        THREAD_LOCAL.get().push(new RecorderInfo(this));
        LogLevel.set(level);
        return this;
    }

    /**
     * Get the level.
     *
     * @return level
     */
    public LogLevel level() {
        return level;
    }

    /**
     * Add a new log entry to record.
     *
     * @param entry log entry
     */
    public void addEntry(String entry) {
        entries.add(entry);
    }

    /**
     * Returns the captured entries.
     *
     * @return entries.
     */
    public List<String> entries() {
        return Lists.drain(entries);
    }

    /**
     * Get the captured lines.
     *
     * @return lines
     */
    public List<String> lines() {
        return Lists.flatMap(entries(), Strings::lines);
    }

    /**
     * Get the captured raw output.
     *
     * @return output
     */
    public String output() {
        String output = String.join(EOL, entries());
        if (!output.isEmpty()) {
            return output + EOL;
        }
        return output;
    }

    @Override
    public void close() {
        RecorderInfo info = THREAD_LOCAL.get().pop();
        LogLevel.set(info.initialLevel);
    }

    static void recordEntry(LogLevel level, String entry) {
        for (RecorderInfo info : THREAD_LOCAL.get()) {
            if (level.ordinal() >= info.recorder.level().ordinal()) {
                info.recorder.entries.add(entry);
            }
        }
    }

    static LogLevel recordLevel() {
        LogLevel level = LogLevel.INFO;
        for (RecorderInfo info : THREAD_LOCAL.get()) {
            if (level.ordinal() > info.recorder.level().ordinal()) {
                level = info.recorder.level();
            }
        }
        return level;
    }

    private static final class RecorderInfo {
        private final LogLevel initialLevel;
        private final LogRecorder recorder;

        RecorderInfo(LogRecorder recorder) {
            this.initialLevel = LogLevel.get();
            this.recorder = recorder;
        }
    }
}
