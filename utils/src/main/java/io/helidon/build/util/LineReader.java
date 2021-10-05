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
package io.helidon.build.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Non-blocking line reader.
 */
class LineReader {

    private final BufferedReader reader;
    private final Consumer<String> lineConsumer;
    private final Runnable flushAction;
    private final char[] buf = new char[1024];
    private String remaining;

    /**
     * Create new line reader.
     *
     * @param is           input stream to read from
     * @param lineConsumer a consumer invoked for each line
     * @param flushAction  an action invoked after processing a batch of lines
     */
    LineReader(InputStream is, Consumer<String> lineConsumer, Runnable flushAction) {
        reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        this.lineConsumer = lineConsumer;
        this.flushAction = flushAction;
    }

    /**
     * Process the data available in the underlying buffer.
     *
     * @return {@code true} if data was processed, {@code false} otherwise
     * @throws IOException if an IO error occurs
     */
    boolean tick() throws IOException {
        if (reader.ready()) {
            readLines(true);
            return true;
        }
        return false;
    }

    private void readLines(boolean flush) throws IOException {
        int len = reader.read(buf);
        String str = new String(buf, 0, len);
        String content = remaining != null ? remaining + str : str;
        remaining = null;
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == lines.length - 1 && !content.endsWith("\n") && !content.endsWith("\r")) {
                remaining = line;
            } else {
                lineConsumer.accept(line);
            }
        }
        if (flush) {
            flushAction.run();
        }
    }

    /**
     * Drain the underlying buffer.
     */
    void drain() {
        try {
            try {
                if (reader.ready()) {
                    readLines(false);
                    if (!remaining.isEmpty()) {
                        String line = remaining;
                        remaining = null;
                        lineConsumer.accept(line);
                    }
                }
            } finally {
                try {
                    flushAction.run();
                } finally {
                    reader.close();
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
    }
}
