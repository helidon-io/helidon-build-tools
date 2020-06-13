/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.util.Log;
import io.helidon.build.util.SystemLogWriter;

import static io.helidon.build.util.Constants.EOL;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A {@link Log#writer} that captures all output.
 */
public class CapturingLogWriter implements Log.Writer {
    private final Log.Writer delegate;
    private final List<LogEntry> entries;

    private CapturingLogWriter() {
        this.delegate = Log.writer();
        this.entries = new ArrayList<>();
        if (delegate instanceof SystemLogWriter) {
            ((SystemLogWriter) delegate).level(Log.Level.DEBUG);
        }
    }

    /**
     * A log entry.
     */
    public static class LogEntry {
        private final Log.Level level;
        private final Throwable thrown;
        private final String message;
        private final Object[] args;

        private LogEntry(Log.Level level, Throwable thrown, String message, Object[] args) {
            this.level = level;
            this.thrown = thrown;
            this.message = message;
            this.args = args;
        }

        /**
         * Returns the level.
         *
         * @return The level.
         */
        public Log.Level level() {
            return level;
        }

        /**
         * Returns the thrown exception, if any.
         *
         * @return The exception, or {@code null} if none.
         */
        public Throwable thrown() {
            return thrown;
        }

        /**
         * Returns the message.
         *
         * @return The message.
         */
        public String message() {
            return message;
        }

        /**
         * Returns the message arguments
         *
         * @return The arguments.
         */
        public Object[] args() {
            return args;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("[").append(level.toString().toLowerCase()).append("] ");
            sb.append(String.format(message, args));
            if (thrown != null) {
                sb.append(" (threw ").append(thrown).append(")");
            }
            return sb.toString();
        }
    }

    /**
     * Install a new instance.
     *
     * @return The instance.
     */
    public static CapturingLogWriter install() {
        final CapturingLogWriter writer = new CapturingLogWriter();
        Log.writer(writer);
        return writer;
    }

    @Override
    public void write(Log.Level level, Throwable thrown, String message, Object... args) {
        delegate.write(level, thrown, message, args);
        entries.add(new LogEntry(level, thrown, message, args));
    }

    @Override
    public boolean isDebug() {
        return delegate.isDebug();
    }

    @Override
    public boolean isVerbose() {
        return delegate.isVerbose();
    }

    /**
     * Clear the entries.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Uninstall this writer.
     */
    public void uninstall() {
        Log.writer(delegate);
    }

    /**
     * Returns the captured log entries.
     *
     * @return The entries.
     */
    public List<LogEntry> entries() {
        return entries;
    }

    /**
     * Returns the log messages.
     *
     * @return The messages.
     */
    public List<String> messages() {
        return entries.stream().map(LogEntry::toString).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        messages().forEach(msg -> sb.append(msg).append(EOL));
        return sb.toString();
    }

    /**
     * Returns the number of messages logged.
     *
     * @return The size.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Assert that there is at least one log line that contains all given fragments.
     *
     * @param fragments The fragments.
     */
    public void assertLinesContaining(String... fragments) {
        if (!atLeastOneLineContaining(fragments)) {
            fail("log does not contain one of the following: " + Arrays.toString(fragments) + EOL + this);
        }
    }

    /**
     * Assert that there are no log line that contains all given fragments.
     *
     * @param fragments The fragments.
     */
    public void assertNoLinesContaining(String... fragments) {
        if (atLeastOneLineContaining(fragments)) {
            fail("log should not contain one of the following: " + Arrays.toString(fragments) + EOL + this);
        }
    }

    /**
     * Test whether or not there is at least one log line that contains all given fragments.
     *
     * @param fragments The fragments.
     * @return {@code true} if at least one line matches all.
     */
    public boolean atLeastOneLineContaining(String... fragments) {
        return countLinesContaining(fragments) > 0;
    }

    /**
     * Returns the count of log lines that contains all given fragments.
     *
     * @param fragments The fragments.
     * @return The count.
     */
    public int countLinesContaining(String... fragments) {
        return (int) messages().stream().filter(msg -> containsAll(msg, fragments)).count();
    }

    private static boolean containsAll(String msg, String... fragments) {
        for (String fragment : fragments) {
            if (!msg.contains(fragment)) {
                return false;
            }
        }
        return true;
    }
}
