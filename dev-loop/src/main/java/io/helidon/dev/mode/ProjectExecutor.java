/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.mode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.dev.build.Project;

/**
 * Class ProjectStarter.
 */
public class ProjectExecutor {

    private static final long WAIT_TERMINATION = 5L;
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final List<String> EXEC_COMMAND = List.of(MAVEN_EXEC, "exec:java");
    private static final String JAVA_EXEC = Constants.OS.javaExecutable();
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String JAVA_HOME_BIN = JAVA_HOME + File.separator + "bin";

    public enum ExecutionMode {
        JAVA,
        MAVEN
    }

    private final ExecutionMode mode;
    private final Project project;
    private ProcessMonitor processMonitor;
    private long pid;

    public ProjectExecutor(Project project) {
        this(project, ExecutionMode.JAVA);
    }

    public ProjectExecutor(Project project, ExecutionMode mode) {
        this.project = project;
        this.mode = mode;
    }

    public Project project() {
        return project;
    }

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

    public void stop() {
        if (processMonitor != null) {
            try {
                processMonitor.stop(WAIT_TERMINATION, TimeUnit.SECONDS);
                Log.info("Process with PID %d stopped", pid);
            } catch (IllegalStateException ignore) {
            } catch (ProcessMonitor.ProcessFailedException e) {
                final int exitCode = e.exitCode();
                Log.info("Process with PID %d stopped (exit code %d)", pid, exitCode);
            } catch (Exception e) {
                Log.error("Error stopping process: %s", e.getMessage());
                throw new RuntimeException(e);
            }
            processMonitor = null;
        }
    }

    public boolean isRunning() {
        return processMonitor != null;
    }

    public void restart() {
        stop();
        start();
    }

    private void startMaven() {
        start(EXEC_COMMAND);
    }

    private void startJava() {
        List<String> command = new ArrayList<>();
        command.add(JAVA_EXEC);
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
            this.processMonitor = ProcessMonitor.builder()
                                                .processBuilder(processBuilder)
                                                .stdOut(System.out::println)
                                                .stdErr(System.err::println)
                                                .capture(true)
                                                .build()
                                                .start();
            this.pid = processMonitor.toHandle().pid();
            Log.info("Process with PID %d is starting", pid);
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
