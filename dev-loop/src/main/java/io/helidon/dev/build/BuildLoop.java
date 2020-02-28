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

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.helidon.build.util.FileUtils.assertDir;
import static java.util.Objects.requireNonNull;

/**
 * A continuous incremental build loop.
 */
public class BuildLoop {
    private static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Path projectDirectory;
    private final Consumer<String> stdOut;
    private final Consumer<String> stdErr;
    private final ProjectSupplier projectSupplier;
    private final BuildMonitor monitor;
    private final boolean watchBinariesOnly;
    private final AtomicBoolean clean;
    private final AtomicBoolean run;
    private final AtomicInteger cycleNumber;
    private final AtomicReference<Future<?>> task;
    private final AtomicReference<CountDownLatch> running;
    private final AtomicReference<CountDownLatch> stopped;
    private final AtomicReference<Project> project;
    private final AtomicBoolean ready;
    private final AtomicLong delay;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private BuildLoop(Builder builder) {
        this.projectDirectory = builder.projectDirectory;
        this.stdOut = builder.stdOut;
        this.stdErr = builder.stdErr;
        this.projectSupplier = builder.projectSupplier;
        this.monitor = builder.monitor;
        this.watchBinariesOnly = builder.watchBinariesOnly;
        this.clean = new AtomicBoolean(builder.clean);
        this.run = new AtomicBoolean();
        this.cycleNumber = new AtomicInteger(0);
        this.task = new AtomicReference<>();
        this.running = new AtomicReference<>(new CountDownLatch(1));
        this.stopped = new AtomicReference<>(new CountDownLatch(1));
        this.project = new AtomicReference<>();
        this.ready = new AtomicBoolean();
        this.delay = new AtomicLong();
    }

    /**
     * Starts the build loop.
     *
     * @return This instance.
     */
    public BuildLoop start() {
        if (!run.getAndSet(true)) {
            stopped.get().countDown(); // In case any previous waiters.
            running.set(new CountDownLatch(1));
            stopped.set(new CountDownLatch(1));
            task.set(EXECUTOR.submit(this::loop));
        }
        return this;
    }

    /**
     * Returns the project, if present.
     *
     * @return The project or {@code null} if the loop has not started or not progressed to the
     * point at which the project has been built.
     */
    public Project project() {
        return project.get();
    }

    /**
     * Returns the monitor.
     *
     * @return The monitor.
     */
    public BuildMonitor monitor() {
        return monitor;
    }

    /**
     * Stops the build loop.
     *
     * @param maxWaitMillis The e maximum milliseconds to wait.
     * @return This instance.
     * @throws InterruptedException If interrupted.
     */
    public BuildLoop stop(long maxWaitMillis) throws InterruptedException {
        if (run.getAndSet(false)) {
            if (!stopped.get().await(maxWaitMillis, TimeUnit.MILLISECONDS)) {
                task.get().cancel(true);
            }
        }
        return this;
    }

    /**
     * Wait for the build loop to stop.
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of {@code timeout}.
     * @return {@code true} if the loop stopped, {@code false} if a timeout occurred.
     * @throws InterruptedException If interrupted.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean waitForStopped(long timeout, TimeUnit unit) throws InterruptedException {
        return stopped.get().await(timeout, unit);
    }

    private void loop() {
        started();
        while (run.get()) {
            final Project project = cycleStarted();
            if (project == null) {

                // Need to create/recreate the project. Note that supplier calls onBuildStart().

                try {
                    setProject(projectSupplier.get(projectDirectory, monitor, clean.getAndSet(false), cycleNumber.get()));
                    ready();
                } catch (IllegalArgumentException | InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    buildFailed(e);
                }

            } else if (watchBinariesOnly) {

                // We're only watching binary changes, assuming some external process might
                // do a build. If we see any changes, we have not idea what they might be, so
                // we must recreate the project.

                if (project.hasBinaryChanges()) {
                    changed(true, true);
                } else {
                    ready();
                }
            } else if (project.haveBuildSystemFilesChanged()) {

                // A build system file (e.g. pom.xml) has changed, so recreate the project

                changed(true, false);

            } else {

                // If we have source changes, do an incremental build

                final List<BuildRoot.Changes> sourceChanges = project.sourceChanges();
                if (sourceChanges.isEmpty()) {
                    ready();
                } else {
                    try {
                        changed(false, false);
                        buildStarting(BuildType.Incremental);
                        project.incrementalBuild(sourceChanges, stdOut, stdErr);
                        project.update(false);
                        ready();
                    } catch (InterruptedException e) {
                        break;
                    } catch (Throwable e) {
                        buildFailed(e);
                    }
                }
            }

            // Delay if needed

            if (delay.get() > 0) {
                try {
                    Thread.sleep(delay.get());
                } catch (InterruptedException e) {
                    break;
                }
            }
            run.set(cycleEnded());
        }
        stopped();
    }

    private void started() {
        running.get().countDown();
        monitor.onStarted();
    }

    private Project cycleStarted() {
        monitor.onCycleStart(cycleNumber.get());
        return project.get();
    }

    private void buildStarting(BuildType type) {
        ready.set(false);
        monitor.onBuildStart(cycleNumber.get(), type);
    }

    private void ready() {
        if (!ready.getAndSet(true)) {
            delay.set(monitor.onReady(cycleNumber.get(), project.get()));
        }
    }

    private void changed(boolean clearProject, boolean binariesOnly) {
        monitor.onChanged(cycleNumber.get(), binariesOnly);
        delay.set(0);
        if (clearProject) {
            project.set(null);
        }
    }

    private void buildFailed(Throwable e) {
        delay.set(monitor.onBuildFail(cycleNumber.get(), e));
    }

    private boolean cycleEnded() {
        return monitor.onCycleEnd(cycleNumber.getAndAdd(1));
    }

    private void stopped() {
        monitor.onStopped();
        stopped.get().countDown();
    }

    private void setProject(Project project) {
        this.project.set(project);
        ready.set(false);
    }

    /**
     * A {@code BuildLoop} builder.
     */
    public static class Builder {
        private Path projectDirectory;
        private Consumer<String> stdOut;
        private Consumer<String> stdErr;
        private ProjectSupplier projectSupplier;
        private BuildMonitor monitor;
        private boolean clean;
        private boolean watchBinariesOnly;

        private Builder() {
            this.stdOut = System.out::println;
            this.stdErr = System.err::println;
        }

        /**
         * Sets the project directory.
         *
         * @param projectDirectory The directory.
         * @return The builder, for chaining.
         */
        public Builder projectDirectory(Path projectDirectory) {
            this.projectDirectory = assertDir(projectDirectory);
            return this;
        }

        /**
         * Sets a consumer for build messages written to stdout.
         *
         * @param stdOut The consumer.
         * @return The builder, for chaining.
         */
        public Builder stdOut(Consumer<String> stdOut) {
            this.stdOut = requireNonNull(stdOut);
            return this;
        }

        /**
         * Sets a consumer for build messages written to stderr.
         *
         * @param stdErr The consumer.
         * @return The consumer.
         */
        public Builder stdErr(Consumer<String> stdErr) {
            this.stdErr = requireNonNull(stdErr);
            return this;
        }

        /**
         * Sets whether or not new {@code Project} instances should perform a clean build.
         *
         * @param clean {@code true} if new instances should perform a clean build.
         * @return The builder, for chaining.
         */
        public Builder clean(boolean clean) {
            this.clean = clean;
            return this;
        }

        /**
         * Sets whether only binaries should be watched for changes.
         *
         * @param watchBinariesOnly {@code true} if only binaries should be watched for changes.
         * @return The builder, for chaining.
         */
        public Builder watchBinariesOnly(boolean watchBinariesOnly) {
            this.watchBinariesOnly = watchBinariesOnly;
            return this;
        }

        /**
         * Sets the project supplier.
         *
         * @param projectSupplier The supplier.
         * @return The builder, for chaining.
         */
        public Builder projectSupplier(ProjectSupplier projectSupplier) {
            this.projectSupplier = projectSupplier;
            return this;
        }

        /**
         * Sets the build monitor.
         *
         * @param buildMonitor The monitor.
         * @return The builder, for chaining.
         */
        public Builder buildMonitor(BuildMonitor buildMonitor) {
            this.monitor = requireNonNull(buildMonitor);
            return this;
        }

        /**
         * Returns the new {@code BuildLoop}.
         *
         * @return The loop.
         */
        public BuildLoop build() {
            if (projectDirectory == null) {
                throw new IllegalStateException("projectDirectory is required");
            }
            if (projectSupplier == null) {
                throw new IllegalStateException("projectSupplier is required");
            }
            return new BuildLoop(this);
        }
    }
}
