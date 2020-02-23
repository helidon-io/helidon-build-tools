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

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.helidon.build.util.Log;
import io.helidon.dev.build.BuildLoop;
import io.helidon.dev.build.BuildMonitor;
import io.helidon.dev.build.BuildType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.maven.DefaultHelidonProjectSupplier;

/**
 * Class DevModeLoop.
 */
public class DevModeLoop {

    private final Path rootDir;

    public DevModeLoop(Path rootDir) {
        this.rootDir = rootDir;
    }

    public void start(int maxWaitInSeconds) throws Exception {
        DevModeMonitor monitor = new DevModeMonitor();
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::onStopped));
        BuildLoop loop = newLoop(rootDir, false, false, monitor);
        run(loop, maxWaitInSeconds);
    }

    static class DevModeMonitor implements BuildMonitor {
        private static final int ON_READY_DELAY = 1000;
        private static final int ON_BUILD_FAIL_DELAY = 10000;

        private boolean start;
        private boolean restart;
        private ProjectExecutor projectExecutor;

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
            return ON_BUILD_FAIL_DELAY;
        }

        @Override
        public long onReady(int cycleNumber, Project project) {
            ensureProjectExecutor(project);
            if (start) {
                projectExecutor.start();
                start = false;
            } else if (restart) {
                projectExecutor.restart();
                restart = false;
            }
            return ON_READY_DELAY;
        }

        @Override
        public boolean onCycleEnd(int cycleNumber) {
            return true;
        }

        @Override
        public void onStopped() {
            if (projectExecutor != null) {
                projectExecutor.stop();
            }
        }

        private void ensureProjectExecutor(Project project) {
            if (projectExecutor == null) {
                projectExecutor = new ProjectExecutor(project);
            }
        }
    }

    private static BuildLoop newLoop(Path projectRoot,
                                    boolean initialClean,
                                    boolean watchBinariesOnly,
                                    BuildMonitor monitor) {
        return BuildLoop.builder()
                .projectDirectory(projectRoot)
                .clean(initialClean)
                .watchBinariesOnly(watchBinariesOnly)
                .projectSupplier(new DefaultHelidonProjectSupplier(60))
                .stdOut(monitor.stdOutConsumer())
                .stdErr(monitor.stdErrConsumer())
                .buildMonitor(monitor)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <T extends BuildMonitor> T run(BuildLoop loop, int maxWaitSeconds)
            throws InterruptedException, TimeoutException {
        loop.start();
        Log.info("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            throw new TimeoutException("While waiting for loop completion");
        }
        return (T) loop.monitor();
    }

}
