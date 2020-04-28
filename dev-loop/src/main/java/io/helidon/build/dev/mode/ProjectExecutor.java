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

package io.helidon.build.dev.mode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.dev.Project;
import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;

import static io.helidon.build.util.Style.BoldBlue;
import static io.helidon.build.util.Style.BoldBrightGreen;
import static io.helidon.build.util.Style.BoldBrightRed;
import static io.helidon.build.util.Style.BoldYellow;

/**
 * Class ProjectStarter.
 */
public class ProjectExecutor {
    private static final long WAIT_TERMINATION = 5L;
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final List<String> EXEC_COMMAND = List.of(MAVEN_EXEC, "exec:java");
    private static final String JAVA_EXEC = Constants.OS.javaExecutable();
    private static final String JAVA_HOME = Constants.javaHome();
    private static final String JAVA_HOME_BIN = JAVA_HOME + File.separator + "bin";
    private static final String JIT_LEVEL_ONE = "-XX:TieredStopAtLevel=1";
    private static final String JIT_TWO_COMPILER_THREADS = "-XX:CICompilerCount=2";
    private static final String STARTING = BoldBrightGreen.apply("starting");
    private static final String STOPPING = BoldYellow.apply("stopping");
    private static final String STOPPED = BoldBrightRed.apply("stopped");

    /**
     * Execution mode.
     */
    public enum ExecutionMode {
        /**
         * Execute project via java.
         */
        JAVA,

        /**
         * Execute project via maven.
         */
        MAVEN
    }

    private final ExecutionMode mode;
    private final Project project;
    private final String logPrefix;
    private final String name;
    private ProcessMonitor processMonitor;
    private long pid;

    /**
     * Create an executor from a project.
     *
     * @param project The project.
     * @param logPrefix The log prefix.
     */
    public ProjectExecutor(Project project, String logPrefix) {
        this(project, ExecutionMode.JAVA, logPrefix);
    }

    /**
     * Create an executor from a project specifying an execution mode.
     *
     * @param project The project.
     * @param logPrefix The log prefix.
     * @param mode The execution mode.
     */
    public ProjectExecutor(Project project, ExecutionMode mode, String logPrefix) {
        this.project = project;
        this.mode = mode;
        this.logPrefix = logPrefix;
        this.name = BoldBlue.apply(project.name());
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
        switch (mode) {
            case JAVA:
                startJava();
                break;
            case MAVEN:
                startMaven();
                break;
            default:
                throw new InternalError("Unrecognized mode " + mode);
        }
    }

    /**
     * Stop execution.
     * @param quiet {@code true} if should not log.
     */
    public void stop(boolean quiet) {
        if (processMonitor != null) {
            try {
                stateChanged(STOPPING, quiet);
                processMonitor.stop(WAIT_TERMINATION, TimeUnit.SECONDS);
                stateChanged(STOPPED, quiet);
            } catch (IllegalStateException ignore) {
            } catch (ProcessMonitor.ProcessFailedException e) {
                stateChanged(STOPPED, quiet);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to stop %s: %s", project.name(), e.getMessage()));
            }
            processMonitor = null;
        }
    }

    /**
     * Check if project is running.
     *
     * @return Outcome of test.
     */
    public boolean isRunning() {
        return processMonitor != null;
    }

    private void stateChanged(String state, boolean quiet) {
        if (!quiet) {
            if (logPrefix == null) {
                Log.info("%s %s", name, state);
            } else {
                Log.info("%s%s %s", logPrefix, name, state);
            }
        }
    }

    private void startMaven() {
        start(EXEC_COMMAND);
    }

    private void startJava() {
        List<String> command = new ArrayList<>();
        command.add(JAVA_EXEC);
        command.add(JIT_LEVEL_ONE);             // Faster startup but longer warmup to peak perf
        command.add(JIT_TWO_COMPILER_THREADS);  // Faster startup but longer warmup to peak perf
        command.add("-cp");
        command.add(classPathString());
        command.add(project.mainClassName());
        start(command);
    }

    private void start(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder()
            .directory(project.root().path().toFile())
            .command(command);

        Map<String, String> env = processBuilder.environment();
        String path = JAVA_HOME_BIN + File.pathSeparatorChar + env.get("PATH");
        env.put("PATH", path);
        env.put("JAVA_HOME", JAVA_HOME);

        try {
            stateChanged(STARTING, false);
            Log.info();
            this.processMonitor = ProcessMonitor.builder()
                                                .processBuilder(processBuilder)
                                                .stdOut(System.out::println)
                                                .stdErr(System.err::println)
                                                .capture(true)
                                                .build()
                                                .start();
            this.pid = processMonitor.toHandle().pid();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String classPathString() {
        List<String> paths = project.classpath().stream()
                                    .map(File::getAbsolutePath).collect(Collectors.toList());
        return paths.stream().reduce("", (s1, s2) -> s1 + ":" + s2);
    }
}
