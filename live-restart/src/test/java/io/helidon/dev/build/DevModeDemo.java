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

package io.helidon.dev.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.test.TestFiles;
import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;

import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.dev.build.BuildLoopTest.newLoop;
import static io.helidon.dev.build.BuildLoopTest.run;

/**
 * DevModeDemo class.
 */
class DevModeDemo {

    public static void main(String[] args) throws Exception {
        TestFiles.targetDirFromClass(DevModeDemo.class);
        Path rootDir = helidonSeProject();
        DevModeMonitor monitor = new DevModeMonitor();
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::killProcessMaybe));
        BuildLoop loop = newLoop(rootDir, false, false, monitor);
        run(loop, 60 * 60);
    }

    static class DevModeMonitor implements BuildMonitor {
        private static final int DELAY = 1;
        private static final String MAVEN_EXEC = Constants.OS.mavenExec();
        private static final List<String> EXEC_COMMAND = List.of(MAVEN_EXEC, "exec:java");
        private static final String JAVA_EXEC = Constants.OS.javaExecutable();

        private boolean start;
        private boolean restart;
        private ProcessHandle processHandle;

        @Override
        public void onStarted() {
            start = true;
        }

        @Override
        public void onCycleStart(int cycleNumber) {
        }

        @Override
        public void onChanged(int cycleNumber, boolean binariesOnly) {
        }

        @Override
        public void onBuildStart(int cycleNumber, BuildType type) {
            restart = true;
        }

        @Override
        public long onBuildFail(int cycleNumber, Throwable error) {
            restart = false;
            return 0;
        }

        @Override
        public long onReady(int cycleNumber, Project project) {
            if (start || restart) {
                killProcessMaybe();
                startProcessJava(project);
                start = restart = false;
            }
            return DELAY;
        }

        @Override
        public boolean onCycleEnd(int cycleNumber) {
            return true;
        }

        @Override
        public void onStopped() {
            killProcessMaybe();
        }

        private void killProcessMaybe() {
            if (processHandle != null) {
                CompletableFuture<ProcessHandle> future = processHandle.onExit();
                processHandle.destroy();
                try {
                    future.get(5L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.error("Error stopping process " + e.getMessage());
                    throw new RuntimeException(e);
                }
                Log.info("Process with PID " + processHandle.pid() + " stopped");
                processHandle = null;
            }
        }

        private void startProcessMvn(Project project) {
            String javaHome = System.getProperty("java.home");
            String javaHomeBin = javaHome + File.separator + "bin";
            ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory(project.root().path().toFile())
                    .command(EXEC_COMMAND);
            Map<String, String> env = processBuilder.environment();
            String path = javaHomeBin + File.pathSeparatorChar + env.get("PATH");
            env.put("PATH", path);
            env.put("JAVA_HOME", javaHome);

            try {
                ProcessMonitor processMonitor = ProcessMonitor.builder()
                        .processBuilder(processBuilder)
                        .stdOut(System.out::println)
                        .stdErr(System.err::println)
                        .capture(true)
                        .build()
                        .start();

                processHandle = processMonitor.toHandle();
                Log.info("Process with PID " + processHandle.pid() + " is starting");
            } catch (Exception e) {
                Log.error("Error starting process " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        private void startProcessJava(Project project) {
            String javaHome = System.getProperty("java.home");
            String javaHomeBin = javaHome + File.separator + "bin";

            List<String> command = new ArrayList<>();
            command.add(JAVA_EXEC);
            command.add("-cp");
            command.add(classPathString(project));
            command.add(project.mainClassName());
            ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory(project.root().path().toFile())
                    .command(command);

            Map<String, String> env = processBuilder.environment();
            String path = javaHomeBin + File.pathSeparatorChar + env.get("PATH");
            env.put("PATH", path);
            env.put("JAVA_HOME", javaHome);

            try {
                ProcessMonitor processMonitor = ProcessMonitor.builder()
                        .processBuilder(processBuilder)
                        .stdOut(System.out::println)
                        .stdErr(System.err::println)
                        .capture(true)
                        .build()
                        .start();

                processHandle = processMonitor.toHandle();
                Log.info("Process with PID " + processHandle.pid() + " is starting");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String classPathString(Project project) {
            List<String> paths = project.classpath().stream()
                    .map(File::getAbsolutePath).collect(Collectors.toList());
            return paths.stream().reduce("", (s1, s2) -> s1 + ":" + s2);
        }
    }
}
