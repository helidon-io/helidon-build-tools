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

import java.io.PrintStream;

import io.helidon.build.common.PrintStreams;

import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;

/**
 * Default log writer implementation.
 */
public final class SystemLogWriter extends LogWriter {

    /**
     * Singleton.
     */
    public static final SystemLogWriter INSTANCE = new SystemLogWriter();

    private final PrintStream stdErr;
    private final PrintStream stdOut;

    private SystemLogWriter() {
        super();
        stdOut = PrintStreams.autoFlush(STDOUT);
        stdErr = PrintStreams.autoFlush(STDERR);
    }

    @Override
    public void writeEntry(LogLevel level, Throwable thrown, String message, Object... args) {
        if (level.ordinal() >= LogLevel.get().ordinal()) {
            final String entry = LogFormatter.format(level, thrown, message, args);
            recordEntry(entry);
            switch (level) {
                case DEBUG:
                case VERBOSE:
                case INFO:
                    stdOut.println(entry);
                    break;
                case WARN:
                case ERROR:
                    stdErr.println(entry);
                    break;
                default:
                    throw new Error();
            }
        }
    }
}
