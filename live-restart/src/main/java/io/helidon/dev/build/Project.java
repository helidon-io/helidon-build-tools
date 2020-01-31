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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * A continuous build project.
 */
public abstract class Project {
    private static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private volatile BuildCycle buildCycle;
    private volatile Future<?> buildFuture;

    /**
     * Constructor,
     */
    protected Project() {
    }

    /**
     * Returns the root directory.
     *
     * @return The root.
     */
    public abstract ProjectDirectory root();

    /**
     * Returns the build system file (e.g. {@code pom.xml}).
     *
     * @return The file.
     */
    public abstract BuildFile buildSystemFile();

    /**
     * Returns the project classpath.
     *
     * @return The classpath.
     */
    public abstract String classpath();

    /**
     * Returns a list of paths to all external dependencies. A path may point
     * to a directory, in which case all contained jar files should be considered
     * dependencies.
     *
     * @return The paths.
     */
    public abstract List<Path> dependencies();

    /**
     * Returns all components.
     *
     * @return The components.
     */
    public abstract List<BuildComponent> components();

    /**
     * Start the build cycle.
     *
     * @param monitor The monitor.
     * @return A future to enable cancellation.
     */
    public Future<?> build(BuildMonitor monitor) {
        this.buildCycle = new BuildCycle(monitor);
        this.buildFuture = EXECUTOR.submit(buildCycle);
        return buildFuture;
    }

    /**
     * Returns a list of source changes since the last update, if any.
     *
     * @return The changes.
     */
    protected List<BuildRoot.Changes> sourceChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components()) {
            final BuildRoot.Changes changes = component.sourceRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    /**
     * Returns a list of binary changes since the last update, if any.
     *
     * @return The changes.
     */
    protected List<BuildRoot.Changes> binaryChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components()) {
            final BuildRoot.Changes changes = component.outputRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    /**
     * Returns whether or not all binaries are newer than all sources and no sources have changed.
     *
     * @return {@code true} if up to date, {@code false} if not.
     */
    protected boolean isBuildUpToDate() {
        if (buildSystemFile().hasChanged()) {
            return false;
        }
        long latestSource = buildSystemFile().lastModifiedTime();
        long oldestBinary = 0;
        for (BuildComponent component : components()) {
            for (final BuildFile file : component.sourceRoot().list()) {
                if (file.hasChanged()) {
                    return false;
                }
                final long lastModified = file.lastModifiedTime();
                if (lastModified > latestSource) {
                    latestSource = lastModified;
                }
            }
            for (final BuildFile file : component.outputRoot().list()) {
                final long lastModified = file.lastModifiedTime();
                if (lastModified > oldestBinary) {
                    oldestBinary = lastModified;
                    if (oldestBinary < latestSource) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Update the project if needed.
     *
     * @param force {@code true} if update should be done regardless of any actual changes.
     */
    protected abstract void update(boolean force);

    /**
     * Returns a list of build roots of the given type.
     *
     * @param type The type.
     * @return The roots. May be empty.
     */
    protected abstract List<BuildRoot> buildRoots(BuildType type);

    /**
     * Perform a full build.
     *
     * @param stdOut A consumer for stdout.
     * @param stdErr A consumer for stderr.
     * @param clean {@code true} if the build should be cleaned first.
     * @throws Exception on error.
     */
    protected abstract void fullBuild(Consumer<String> stdOut, Consumer<String> stdErr, boolean clean) throws Exception;

    /**
     * Perform an incremental build for the given changes.
     *
     * @param changes The changes.
     * @param stdOut A consumer for stdout.
     * @param stdErr A consumer for stderr.
     * @throws Exception on error.
     */
    protected void incrementalBuild(List<BuildRoot.Changes> changes,
                                            Consumer<String> stdOut,
                                            Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
             for (final BuildRoot.Changes changed : changes) {
                changed.root().component().incrementalBuild(changed, stdOut, stdErr);
            }
        }
    }

    private class BuildCycle implements Runnable {
        private final BuildMonitor monitor;
        private boolean initialBuildCompleted;
        private long delay;

        BuildCycle(BuildMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public void run() {
            final boolean clean = monitor.onStarted();
            while (true) {
                try {
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                    if (!initialBuildCompleted) {
                        initialBuild(clean);
                        initialBuildCompleted = true;
                    }
                    final boolean checkBinaries = monitor.onCycleStart();
                    final List<BuildRoot.Changes> sourceChanges = sourceChanges();
                    if (!sourceChanges.isEmpty()) {
                        monitor.onChanged(true);
                        monitor.onBuildStart(true);
                        incrementalBuild(sourceChanges, monitor.stdOutConsumer(), monitor.stdErrConsumer());
                        update(true);
                    } else if (checkBinaries) {
                        if (!binaryChanges().isEmpty()) {
                            monitor.onChanged(false);
                            update(true);
                        }
                    }
                    delay = monitor.onReady();
                } catch (InterruptedException e) {
                    monitor.onStopped();
                    return;
                } catch (Throwable e) {
                    delay = monitor.onBuildFail(e);
                }
                if (!monitor.onCycleEnd()) {
                    monitor.onStopped();
                    return;
                }
            }
        }

        void initialBuild(boolean clean) throws Exception {
            if (clean || !isBuildUpToDate()) {
                monitor.onBuildStart(false);
                fullBuild(monitor.stdOutConsumer(), monitor.stdErrConsumer(), clean);
                update(true);
                delay = monitor.onReady();
            }
        }
    }
}
