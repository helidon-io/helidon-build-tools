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

package io.helidon.dev.mode;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.helidon.build.util.Log;
import io.helidon.dev.build.BuildExecutor;
import io.helidon.dev.build.BuildLoop;
import io.helidon.dev.build.BuildMonitor;
import io.helidon.dev.build.BuildType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectSupplier;
import io.helidon.dev.build.maven.DefaultHelidonProjectSupplier;
import io.helidon.dev.build.maven.EmbeddedMavenExecutor;
import io.helidon.dev.build.maven.ForkedMavenExecutor;

/**
 * A development loop that manages application lifecycle based on events from a {@link BuildLoop}.
 */
public class DevLoop {

    private static final int MAX_BUILD_WAIT_SECONDS = 5 * 60;

    private final BuildExecutor buildExecutor;
    private final ProjectSupplier projectSupplier;
    private final boolean initialClean;

    /**
     * Create a dev loop.
     *
     * @param rootDir Project's root.
     * @param initialClean Clean flag.
     * @param forkBuilds {@code true} if builds should be forked.
     */
    public DevLoop(Path rootDir, boolean initialClean, boolean forkBuilds) {
        this(rootDir, new DefaultHelidonProjectSupplier(), initialClean, forkBuilds);
    }

    /**
     * Create a dev loop.
     *
     * @param rootDir Project's root.
     * @param projectSupplier Project supplier.
     * @param initialClean Clean flag.
     * @param forkBuilds {@code true} if builds should be forked.
     */
    public DevLoop(Path rootDir, ProjectSupplier projectSupplier, boolean initialClean, boolean forkBuilds) {
        final BuildMonitor monitor = new DevModeMonitor();
        this.buildExecutor = forkBuilds ? new ForkedMavenExecutor(rootDir, monitor, MAX_BUILD_WAIT_SECONDS)
                                        : new EmbeddedMavenExecutor(rootDir, monitor);
        this.initialClean = initialClean;
        this.projectSupplier = projectSupplier;
    }

    /**
     * Start the dev loop.
     *
     * @param maxWaitInSeconds Max seconds to wait.
     * @throws Exception If a problem is found.
     */
    public void start(int maxWaitInSeconds) throws Exception {
        DevModeMonitor monitor = new DevModeMonitor();
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::onStopped));
        BuildLoop loop = newLoop(buildExecutor, initialClean, false);
        run(loop, maxWaitInSeconds);
    }

    static class DevModeMonitor implements BuildMonitor {
        private static final int ON_READY_DELAY = 1000;
        private static final int ON_BUILD_FAIL_DELAY = 10000;

        private ProjectExecutor projectExecutor;

        @Override
        public void onStarted() {
        }

        @Override
        public void onCycleStart(int cycleNumber) {
        }

        @Override
        public void onChanged(int cycleNumber, boolean binariesOnly) {
            ensureStop();
        }

        @Override
        public void onBuildStart(int cycleNumber, BuildType type) {
        }

        @Override
        public long onBuildFail(int cycleNumber, Throwable error) {
            ensureStop();
            return ON_BUILD_FAIL_DELAY;
        }

        @Override
        public long onReady(int cycleNumber, Project project) {
            if (projectExecutor == null) {
                projectExecutor = new ProjectExecutor(project);
                projectExecutor.start();
            } else if (!projectExecutor.isRunning()) {
                projectExecutor.start();
            }
            return ON_READY_DELAY;
        }

        @Override
        public boolean onCycleEnd(int cycleNumber) {
            return true;
        }

        @Override
        public void onStopped() {
            ensureStop();
        }

        private void ensureStop() {
            if (projectExecutor != null) {
                final ProjectExecutor executor = projectExecutor;
                projectExecutor = null;
                executor.stop();
            }
        }
    }

    private BuildLoop newLoop(BuildExecutor executor, boolean initialClean, boolean watchBinariesOnly) {
        return BuildLoop.builder()
                        .buildExecutor(executor)
                        .clean(initialClean)
                        .watchBinariesOnly(watchBinariesOnly)
                        .projectSupplier(projectSupplier)
                        .build();
    }

    @SuppressWarnings("unchecked")
    private static <T extends BuildMonitor> T run(BuildLoop loop, int maxWaitSeconds)
    throws InterruptedException, TimeoutException {
        loop.start();
        Log.debug("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            throw new TimeoutException("While waiting for loop completion");
        }
        return (T) loop.monitor();
    }
}
