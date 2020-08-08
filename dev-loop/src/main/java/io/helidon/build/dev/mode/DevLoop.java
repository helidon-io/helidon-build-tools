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
import java.util.List;
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

import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_COMPLETED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_FAILED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_FAILED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_HEADER;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_PROJECT_CHANGED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_START;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_STYLED_MESSAGE_PREFIX;
import static io.helidon.build.util.StyleFunction.Bold;
import static io.helidon.build.util.StyleFunction.BoldBlue;
import static io.helidon.build.util.StyleFunction.BoldRed;
import static io.helidon.build.util.StyleFunction.BoldYellow;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * A development loop that manages application lifecycle based on events from a {@link BuildLoop}.
 */
public class DevLoop {
    private static final int MAX_BUILD_WAIT_SECONDS = 5 * 60;
    private final boolean terminalMode;
    private final DevModeMonitor monitor;
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
     * @param terminalMode {@code true} for terminal output.
     * @param appJvmArgs The application JVM arguments.
     * @param appArgs The application arguments.
     */
    public DevLoop(Path rootDir,
                   ProjectSupplier projectSupplier,
                   boolean initialClean,
                   boolean forkBuilds,
                   boolean terminalMode,
                   List<String> appJvmArgs,
                   List<String> appArgs) {
        this.terminalMode = terminalMode;
        this.monitor = new DevModeMonitor(terminalMode, projectSupplier.buildFileName(), appJvmArgs, appArgs);
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
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::shutdown));
        BuildLoop loop = newLoop(buildExecutor, initialClean, false);
        run(loop, maxWaitInSeconds);
    }

    static class DevModeMonitor implements BuildMonitor {
        private static final int ON_READY_DELAY = 1000;
        private static final int BUILD_FAIL_DELAY = 1000;
        private static final String HEADER = Bold.apply(DEV_LOOP_HEADER);
        private static final String LOG_PREFIX = DEV_LOOP_STYLED_MESSAGE_PREFIX + " ";

        private final boolean terminalMode;
        private final String buildFileName;
        private ProjectExecutor projectExecutor;
        private ChangeType lastChangeType;
        private long buildStartTime;
        private final List<String> appJvmArgs;
        private final List<String> appArgs;


        private DevModeMonitor(boolean terminalMode, String buildFileName, List<String> appJvmArgs, List<String> appArgs) {
            this.terminalMode = terminalMode;
            this.buildFileName = buildFileName;
            this.appJvmArgs = appJvmArgs;
            this.appArgs = appArgs;
        }

        private void header() {
            if (terminalMode) {
                Log.info(HEADER);
            } else {
                Log.info();
            }
        }

        @Override
        public void onStarted() {
            header();
        }

        @Override
        public void onCycleStart(int cycleNumber) {
        }

        private void log(String message, Object... args) {
            if (terminalMode) {
                Log.info(LOG_PREFIX + message, args);
            } else {
                Log.info(message, args);
            }
        }

        @Override
        public void onChanged(int cycleNumber, ChangeType type) {
            header();
            log("%s", BoldBlue.apply(type + " " + DEV_LOOP_PROJECT_CHANGED));
            lastChangeType = type;
            ensureStop();
        }

        @Override
        public void onBuildStart(int cycleNumber, BuildType type) {
            if (type == BuildType.Skipped) {
                log("%s", BoldBlue.apply("up to date"));
            } else {
                String operation = cycleNumber == 0 ? DEV_LOOP_BUILD_STARTING : "re" + DEV_LOOP_BUILD_STARTING;
                if (type == BuildType.Incremental) {
                    log("%s (%s)", BoldBlue.apply(operation), type);
                } else {
                    log("%s", BoldBlue.apply(operation));
                }
                buildStartTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onBuildSuccess(int cycleNumber, BuildType type) {
            if (type != BuildType.Skipped) {
                long elapsedTime = System.currentTimeMillis() - buildStartTime;
                float elapsedSeconds = elapsedTime / 1000F;
                String operation = cycleNumber == 0 ? "build " : "rebuild ";
                log("%s (%.1f seconds)", BoldBlue.apply(operation + DEV_LOOP_BUILD_COMPLETED), elapsedSeconds);
            }
        }

        @Override
        public long onBuildFail(int cycleNumber, BuildType type, Throwable error) {
            Log.info();
            log("%s", BoldRed.apply(DEV_LOOP_BUILD_FAILED));
            ensureStop();
            String message;
            if (lastChangeType == ChangeType.BuildFile) {
                message = String.format("waiting for %s changes", buildFileName);
            } else if (lastChangeType == ChangeType.SourceFile) {
                message = "waiting for source file changes";
            } else {
                message = "waiting for changes";
            }
            log("%s", BoldYellow.apply(message));
            return BUILD_FAIL_DELAY;
        }

        @Override
        public long onReady(int cycleNumber, Project project) {
            if (projectExecutor == null) {
                projectExecutor = new ProjectExecutor(project, terminalMode ? LOG_PREFIX : null, appJvmArgs, appArgs);
                projectExecutor.start();
            }
            return ON_READY_DELAY;
        }

        @Override
        public boolean onCycleEnd(int cycleNumber) {
            if (projectExecutor == null) {
                return true;
            } else if (projectExecutor.isRunning()) {
                return true;
            } else if (projectExecutor.hasStdErrMessage()) {
                // Shutdown and exit loop
                projectExecutor.stop();
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void onLoopFail(int cycleNumber, Throwable error) {
            Log.info();
            log("%s %s", BoldRed.apply(DEV_LOOP_FAILED), error.getMessage());
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

        private void shutdown() {
            System.out.println(ansi().reset());
            ensureStop();
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

    private void run(BuildLoop loop, int maxWaitSeconds) throws InterruptedException, TimeoutException {
        if (terminalMode) {
            Log.info(DEV_LOOP_START);
        }
        loop.start();
        Log.debug("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            throw new TimeoutException("While waiting for loop completion");
        }
    }
}
