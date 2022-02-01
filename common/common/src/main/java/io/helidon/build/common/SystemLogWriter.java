/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

import io.helidon.build.common.Log.Level;

import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;

/**
 * {@link LogWriter} that writes to {@link PrintStreams#STDOUT} and {@link  PrintStreams#STDERR}.
 */
public final class SystemLogWriter extends DefaultLogWriter {

    /**
     * Installs an instance of this type as the writer in {@link Log} at the given {@link Level}.
     *
     * @param level The level.
     * @return The instance.
     */
    public static SystemLogWriter install(Level level) {
        final SystemLogWriter writer = create(level);
        Log.writer(writer);
        return writer;
    }

    /**
     * Returns a new instance.
     *
     * @return The instance.
     */
    public static SystemLogWriter create() {
        return new SystemLogWriter();
    }

    /**
     * Returns a new instance with the given level.
     *
     * @param level The level at or above which messages should be logged.
     * @return The instance.
     */
    public static SystemLogWriter create(Level level) {
        return new SystemLogWriter(level);
    }

    private final PrintStream stdErr;
    private final PrintStream stdOut;

    private SystemLogWriter() {
        super();
        this.stdOut = PrintStreams.autoFlush(STDOUT);
        this.stdErr = PrintStreams.autoFlush(STDERR);
    }

    private SystemLogWriter(Level level) {
        super(level);
        this.stdOut = PrintStreams.autoFlush(STDOUT);
        this.stdErr = PrintStreams.autoFlush(STDERR);
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
}
