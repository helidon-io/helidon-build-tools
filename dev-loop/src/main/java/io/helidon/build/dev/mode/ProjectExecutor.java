/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.dev.mode;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.dev.Project;
import io.helidon.build.util.ConsoleUtils;
import io.helidon.build.util.Constants;
import io.helidon.build.util.JavaProcessBuilder;
import io.helidon.build.util.PrintStreams;
import io.helidon.build.util.ProcessMonitor;

import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_APPLICATION_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_APPLICATION_STOPPED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_APPLICATION_STOPPING;
import static io.helidon.build.util.PrintStreams.STDERR;
import static io.helidon.build.util.PrintStreams.STDOUT;
import static io.helidon.build.util.StyleFunction.BoldBrightCyan;
import static io.helidon.build.util.StyleFunction.BoldBrightGreen;
import static io.helidon.build.util.StyleFunction.BoldBrightRed;
import static io.helidon.build.util.StyleFunction.BoldYellow;

/**
 * Project executor.
 */
public class ProjectExecutor {

    private static final String JAVA_EXEC = Constants.OS.javaExecutable();
    private static final String JIT_LEVEL_ONE = "-XX:TieredStopAtLevel=1";
    private static final String JIT_TWO_COMPILER_THREADS = "-XX:CICompilerCount=2";
    private static final String STARTING = BoldBrightGreen.apply(DEV_LOOP_APPLICATION_STARTING);
    private static final String STOPPING = BoldYellow.apply(DEV_LOOP_APPLICATION_STOPPING);
    private static final String STOPPED = BoldBrightRed.apply(DEV_LOOP_APPLICATION_STOPPED);
    private static final long ERROR_MESSAGES_DONE_NANOS = 100 * 1000;
    private static final List<String> EXIT_MESSAGE_FRAGMENTS = List.of(
            "JDWP exit error",
            "BindException: Address already in use",
            "--enable-preview"
    );

    private final Project project;
    private final String logPrefix;
    private final String name;
    private ProcessMonitor processMonitor;
    private final List<String> appJvmArgs;
    private final List<String> appArgs;
    private final StringBuilder stdErrBuf;
    private boolean hasExitMessage;
    private long lastErrorMessageTime;

    /**
     * Create an executor from a project.
     *
     * @param project The project.
     * @param logPrefix The log prefix.
     * @param appJvmArgs The application JVM arguments.
     * @param appArgs The application arguments.
     */
    public ProjectExecutor(Project project,
                           String logPrefix,
                           List<String> appJvmArgs,
                           List<String> appArgs) {
        this.project = project;
        this.logPrefix = logPrefix;
        this.name = BoldBrightCyan.apply(project.name());
        this.appJvmArgs = appJvmArgs;
        this.appArgs = appArgs;
        this.stdErrBuf = new StringBuilder();
    }

    /**
     * Get project instance.
     *
     * @return The project.
     */
    public Project project() {
        return project;
    }

    /**
     * Start execution.
     */
    public void start() {
        List<String> command = new ArrayList<>();
        command.add(JAVA_EXEC);
        command.add(JIT_LEVEL_ONE);             // Faster startup but longer warmup to peak perf
        command.add(JIT_TWO_COMPILER_THREADS);  // Faster startup but longer warmup to peak perf
        command.add("-cp");
        command.add(classPathString());
        command.addAll(appJvmArgs);
        command.add(project.mainClassName());
        command.addAll(appArgs);
        start(command);
    }

    /**
     * Stop execution.
     *
     * @throws IllegalStateException If process does not stop before timeout.
     */
    public void stop() {
        stop(false);
    }

    /**
     * Stop execution.
     *
     * @param verbose {@code true} if should log all state changes.
     * @throws IllegalStateException If process does not stop before timeout.
     */
    public void stop(boolean verbose) {
        if (processMonitor != null) {
            if (verbose) {
                stateChanged(STOPPING);
            }
            try {
                processMonitor.stop();
            } finally {
                processMonitor = null;
            }
            if (verbose) {
                stateChanged(STOPPED);
            }
        }
    }

    /**
     * Check if project is running.
     *
     * @return {@code true} if running.
     */
    public boolean isRunning() {
        return processMonitor != null
               && processMonitor.isAlive();
    }

    /**
     * Check if project has printed a message to {@link System#err} that requires the loop to exit.
     *
     * @return {@code true} if exit is required.
     */
    public boolean shouldExit() {
        if (hasExitMessage) {
            final long elapsedNanos = System.nanoTime() - lastErrorMessageTime;
            return elapsedNanos > ERROR_MESSAGES_DONE_NANOS;
        } else {
            return false;
        }
    }

    /**
     * Check if project has printed to {@link System#err}.
     *
     * @return {@code true} if anything has been printed to {@link System#err}.
     */
    public boolean hasStdErrMessage() {
        return lastErrorMessageTime > 0;
    }

    private void stateChanged(String state) {
        if (logPrefix == null) {
            STDOUT.printf("%s %s", name, state);
        } else {
            STDOUT.printf("%s%s %s", logPrefix, name, state);
        }
    }

    private void start(List<String> command) {
        lastErrorMessageTime = 0;
        ProcessBuilder processBuilder = JavaProcessBuilder.newInstance()
                                                          .directory(project.root().path().toFile())
                                                          .command(command);
        try {
            stateChanged(STARTING);
            STDOUT.println();
            STDOUT.flush();
            this.processMonitor = ProcessMonitor.builder()
                                                .processBuilder(processBuilder)
                                                .afterShutdown(ConsoleUtils::reset)
                                                .stdOut(STDOUT)
                                                .stdErr(PrintStreams.delegate(STDERR, this::printStdErr))
                                                .capture(true)
                                                .build()
                                                .start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void printStdErr(PrintStream stdErr, String str) {
        if (!str.endsWith("\n") || str.endsWith("\r")) {
            stdErrBuf.append(str);
        }
        String line = stdErrBuf.append(str).toString();
        stdErrBuf.setLength(0);
        lastErrorMessageTime = System.nanoTime();
        for (String exitMessageFragment : EXIT_MESSAGE_FRAGMENTS) {
            if (line.contains(exitMessageFragment)) {
                hasExitMessage = true;
                break;
            }
        }
        stdErr.print(str);
    }

    private String classPathString() {
        return project
                .classpath()
                .stream()
                .map(File::getAbsolutePath)
                .reduce("", (s1, s2) -> s1 + File.pathSeparator + s2);
    }
}
