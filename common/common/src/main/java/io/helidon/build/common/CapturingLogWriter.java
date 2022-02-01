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

package io.helidon.build.common;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.common.Log.Level;

import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;

/**
 * A {@link Log#writer} that captures all output.
 */
public class CapturingLogWriter extends DefaultLogWriter {

    private static final String EOL = System.getProperty("line.separator");
    private final LogWriter delegate;
    private final StringBuilder output;
    private final PrintStream stdOut;
    private final PrintStream stdErr;

    private CapturingLogWriter() {
        super(Log.Level.DEBUG);
        this.delegate = Log.writer();
        if (delegate instanceof SystemLogWriter) {
            ((SystemLogWriter) delegate).level(Level.DEBUG);
        }
        this.output = new StringBuilder();
        this.stdOut = PrintStreams.delegate(STDOUT, (p, s) -> {
            p.print(s);
            output.append(s);
        });
        this.stdErr = PrintStreams.delegate(STDERR, (p, s) -> {
            p.print(s);
            output.append(s);
        });
    }

    @Override
    public PrintStream stdOut() {
        return stdOut;
    }

    @Override
    public PrintStream stdErr() {
        return stdErr;
    }

    @Override
    public boolean isSystem() {
        return true;
    }

    /**
     * A log entry.
     */
    public static class LogEntry {
        private final Level level;
        private final Throwable thrown;
        private final String message;
        private final Object[] args;

        private LogEntry(Level level, Throwable thrown, String message, Object[] args) {
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
        public Level level() {
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
         * Returns the message arguments.
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
     * Create a new instance.
     *
     * @return The instance.
     */
    public static CapturingLogWriter create() {
        return new CapturingLogWriter();
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
     * Uninstall this writer.
     */
    public void uninstall() {
        Log.writer(delegate);
    }

    /**
     * Install this writer.
     */
    public void install() {
        Log.writer(this);
    }

    /**
     * Clear the captured output.
     */
    public void clear() {
        output.setLength(0);
    }

    /**
     * Returns the captured lines.
     *
     * @return The lines.
     */
    public List<String> lines() {
        return Arrays.stream(output.toString().split("\\R"))
                     .collect(Collectors.toList());
    }

    /**
     * Get the captured raw output.
     *
     * @return output
     */
    public String output() {
        return output.toString();
    }

    /**
     * Indent the captured output.
     *
     * @return indented output
     */
    public String indented() {
        StringBuilder sb = new StringBuilder();
        lines().forEach(msg -> sb.append("    ").append(msg).append(EOL));
        return sb.toString();
    }

    /**
     * Returns the number of messages logged.
     *
     * @return The size.
     */
    public int size() {
        return output.toString().split("\\R").length;
    }

    /**
     * Assert that there is at least one log line that contains all given fragments.
     *
     * @param fragments The fragments.
     */
    public void assertLinesContainingAll(String... fragments) {
        if (!atLeastOneLineContainingAll(fragments)) {
            throw new AssertionError(String.format(
                    "log should contain at least one line with all of the following: %s%n%s",
                    Arrays.toString(fragments), this));
        }
    }

    /**
     * Assert that there are no log lines that contain all given fragments.
     *
     * @param fragments The fragments.
     */
    public void assertNoLinesContainingAll(String... fragments) {
        if (atLeastOneLineContainingAll(fragments)) {
            throw new AssertionError(String.format(
                    "log should not contain any lines with all of the following: %s%n%s",
                    Arrays.toString(fragments), this));
        }
    }

    /**
     * Test whether there is at least one log line that contain all given fragments.
     *
     * @param fragments The fragments.
     * @return {@code true} if at least one line matches all.
     */
    public boolean atLeastOneLineContainingAll(String... fragments) {
        return countLinesContainingAll(fragments) > 0;
    }

    /**
     * Asserts the expected count of log lines contains all given fragments.
     *
     * @param expectedCount The expected count.
     * @param fragments The fragments.
     */
    public void assertLinesContainingAll(int expectedCount, String... fragments) {
        final int count = countLinesContainingAll(fragments);
        if (count != expectedCount) {
            throw new AssertionError(String.format(
                    "log should contain %d lines with all of the following, found %d: %s%n%s",
                    expectedCount, count, Arrays.toString(fragments), this));
        }
    }

    /**
     * Returns the count of log lines contains all given fragments.
     *
     * @param fragments The fragments.
     * @return The count.
     */
    public int countLinesContainingAll(String... fragments) {
        return (int) lines().stream().filter(msg -> containsAll(msg, fragments)).count();
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
