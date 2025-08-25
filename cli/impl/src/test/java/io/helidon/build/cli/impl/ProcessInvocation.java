/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.helidon.build.common.LazyValue;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.ProcessMonitor.ProcessFailedException;
import io.helidon.build.common.ProcessMonitor.ProcessTimeoutException;
import io.helidon.build.common.ansi.AnsiTextStyle;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.requireJavaExecutable;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;

class ProcessInvocation {

    static {
        LogLevel.set(LogLevel.DEBUG);
    }

    static final LazyValue<String> CLI_JAR = new LazyValue<>(ProcessInvocation::cliJar);
    static final LazyValue<String> JAVA_BIN = new LazyValue<>(() -> requireJavaExecutable().toString());

    protected Path cwd;
    protected Path stdIn;
    protected List<String> args = List.of();

    ProcessInvocation args(String... args) {
        this.args = Arrays.asList(args);
        return this;
    }

    ProcessInvocation cwd(Path cwd) {
        this.cwd = cwd;
        return this;
    }

    ProcessInvocation stdIn(Path stdIn) {
        this.stdIn = stdIn;
        return this;
    }

    Monitor start() {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(List.of(JAVA_BIN.get(), "-Xmx128M", "-jar", CLI_JAR.get()));
        cmd.addAll(args);
        ensureDirectory(cwd);
        Recorder recorder = new Recorder();
        Path logFile = unique(cwd, "cli", ".log");
        try {
            PrintStream printStream = new PrintStream(Files.newOutputStream(logFile));
            ProcessMonitor processMonitor = ProcessMonitor.builder()
                    .processBuilder(new ProcessBuilder()
                            .command(cmd)
                            .directory(cwd.toFile()))
                    .stdIn(stdIn != null ? stdIn.toFile() : null)
                    .stdOut(PrintStreams.accept(printStream, recorder::record))
                    .stdErr(PrintStreams.accept(printStream, recorder::record))
                    .capture(true)
                    .build();
            Log.debug("Executing: " + String.join(" ", cmd));
            return new Monitor(processMonitor.start(), recorder, cwd);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (Exception ex) {
            throw new MonitorException(recorder.sb.toString(), ex);
        }
    }

    static String cliJar() {
        return targetDir(ProcessInvocation.class).resolve("helidon-cli.jar").toString();
    }

    static class Recorder {
        final StringBuilder sb = new StringBuilder();

        void record(String s) {
            synchronized (sb) {
                sb.append(s);
            }
        }
    }

    static class MonitorException extends RuntimeException {
        private final String output;

        MonitorException(String output, Exception cause) {
            super(cause);
            this.output = AnsiTextStyle.strip(output);
        }

        String output() {
            return output;
        }
    }

    static class Monitor implements AutoCloseable {
        final ProcessMonitor monitor;
        final Recorder recorder;
        final Path cwd;

        Monitor(ProcessMonitor monitor, Recorder recorder, Path cwd) {
            this.monitor = monitor;
            this.recorder = recorder;
            this.cwd = cwd;
        }

        @SuppressWarnings("unused")
        String output() {
            return AnsiTextStyle.strip(recorder.sb.toString());
        }

        Path cwd() {
            return cwd;
        }

        @Override
        public void close() {
            monitor.stop();
        }

        void await() throws MonitorException {
            try {
                monitor.waitForCompletion(10, TimeUnit.MINUTES);
            } catch (ProcessTimeoutException
                     | ProcessFailedException
                     | InterruptedException e) {
                throw new MonitorException(recorder.sb.toString(), e);
            }
        }
    }
}
