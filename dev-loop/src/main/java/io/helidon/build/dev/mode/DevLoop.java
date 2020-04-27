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

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.BuildLoop;
import io.helidon.build.dev.BuildMonitor;
import io.helidon.build.dev.BuildType;
import io.helidon.build.dev.ChangeType;
import io.helidon.build.dev.Project;
import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.dev.maven.EmbeddedMavenExecutor;
import io.helidon.build.dev.maven.ForkedMavenExecutor;
import io.helidon.build.util.Log;

import static io.helidon.build.util.AnsiConsoleInstaller.clearScreen;
import static io.helidon.build.util.Constants.DEV_LOOP_START_MESSAGE;
import static io.helidon.build.util.Style.Bold;
import static io.helidon.build.util.Style.BoldBlue;
import static io.helidon.build.util.Style.BoldBrightGreen;
import static io.helidon.build.util.Style.BoldRed;
import static io.helidon.build.util.Style.BoldYellow;

/**
 * A development loop that manages application lifecycle based on events from a {@link BuildLoop}.
 */
public class DevLoop {
    private static final int MAX_BUILD_WAIT_SECONDS = 5 * 60;

    private final BuildMonitor monitor;
    private final BuildExecutor buildExecutor;
    private final ProjectSupplier projectSupplier;
    private final boolean initialClean;

    /**
     * Create a dev loop.
     *
     * @param rootDir Project's root.
     * @param projectSupplier Project supplier.
     * @param initialClean Clean flag.
     * @param forkBuilds {@code true} if builds should be forked.
     */
    public DevLoop(Path rootDir, ProjectSupplier projectSupplier, boolean initialClean, boolean forkBuilds) {
        this.monitor = new DevModeMonitor(rootDir, projectSupplier.buildFileName());
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
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::onStopped));
        BuildLoop loop = newLoop(buildExecutor, initialClean, false);
        run(loop, maxWaitInSeconds);
    }

    static class DevModeMonitor implements BuildMonitor {
        private static final int ON_READY_DELAY = 1000;
        private static final int BUILD_FAIL_DELAY = 1000;
        private static final String HEADER = Bold.apply("dev loop");
        private static final String LOG_PREFIX = Bold.apply("|");

        private final String projectName;
        private final String buildFileName;
        private ProjectExecutor projectExecutor;
        private ChangeType lastChangeType;
        private long buildStartTime;

        private DevModeMonitor(Path projectDir, String buildFileName) {
            this.projectName = Bold.apply(projectDir.getFileName().toString());
            this.buildFileName = buildFileName;
        }

        private void clear() {
            if (clearScreen()) {
                Log.info();
                Log.info(HEADER);
                Log.info(LOG_PREFIX);
            } else {
                Log.info();
            }
        }

        @Override
        public void onStarted() {
            clear();
        }

        @Override
        public void onCycleStart(int cycleNumber) {
        }

        @Override
        public void onChanged(int cycleNumber, ChangeType type) {
            clear();
            Log.info("%s %s", LOG_PREFIX, BoldBlue.apply(type + " changed"));
            lastChangeType = type;
            ensureStop();
        }

        @Override
        public void onBuildStart(int cycleNumber, BuildType type) {
            if (type == BuildType.Skipped) {
                Log.info("%s %s", LOG_PREFIX, BoldBrightGreen.apply("up to date"));
            } else {
                String operation = cycleNumber == 0 ? "building" : "rebuilding";
                Log.info("%s %s (%s)", LOG_PREFIX, BoldBlue.apply(operation), type);
                buildStartTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onBuildSuccess(int cycleNumber, BuildType type) {
            if (type != BuildType.Skipped) {
                long elapsedTime = System.currentTimeMillis() - buildStartTime;
                float elapsedSeconds = elapsedTime / 1000F;
                String operation = cycleNumber == 0 ? "build" : "rebuild";
                Log.info("%s %s (%.1f seconds)", LOG_PREFIX, BoldBlue.apply(operation + " completed"), elapsedSeconds);
            }
        }

        @Override
        public long onBuildFail(int cycleNumber, BuildType type, Throwable error) {
            Log.info("%s %s", LOG_PREFIX, BoldRed.apply("build failed"));
            ensureStop();
            String message;
            if (lastChangeType == ChangeType.BuildFile) {
                message = String.format("waiting for %s changes before retrying build", buildFileName);
            } else if (lastChangeType == ChangeType.SourceFile) {
                message = "waiting for source file changes before retrying build";
            } else {
                message = "waiting for changes before retrying build";
            }
            Log.info("%s %s", LOG_PREFIX, BoldYellow.apply(message));
            return BUILD_FAIL_DELAY;
        }

        @Override
        public long onReady(int cycleNumber, Project project) {
            if (projectExecutor == null) {
                projectExecutor = new ProjectExecutor(project, LOG_PREFIX);
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
        Log.info(DEV_LOOP_START_MESSAGE);
        loop.start();
        Log.debug("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            throw new TimeoutException("While waiting for loop completion");
        }
        return (T) loop.monitor();
    }
}
