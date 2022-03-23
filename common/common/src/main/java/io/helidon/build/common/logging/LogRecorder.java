/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.util.LinkedList;
import java.util.List;

/**
 * Log recorder.
 */
public class LogRecorder {

    private static final String EOL = System.getProperty("line.separator");

    private final List<String> entries = new LinkedList<>();

    private LogRecorder() {
    }

    /**
     * Create a new instance.
     *
     * @return The instance.
     */
    public static LogRecorder create() {
        return new LogRecorder();
    }

    /**
     * Add a new log entry to record.
     * @param entry log entry
     */
    public void addEntry(String entry) {
        entries.add(entry);
    }

    /**
     * Clear the captured output.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Returns the captured lines.
     *
     * @return The lines.
     */
    public List<String> entries() {
        return entries;
    }

    /**
     * Get the captured raw output.
     *
     * @return output
     */
    public String output() {
        String output = String.join(EOL, entries());
        if (output.length() > 0) {
            return output + EOL;
        }
        return output;
    }

    /**
     * Returns the number of messages logged.
     *
     * @return The size.
     */
    public int size() {
        return entries.size();
    }
}
