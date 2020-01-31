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

package io.helidon.dev;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.BuildFile;
import io.helidon.dev.build.BuildMonitor;
import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.DirectoryType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectDirectory;

import org.junit.jupiter.api.Test;

import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.build.test.TestFiles.helidonSeProjectCopy;
import static io.helidon.dev.build.ProjectFactory.createProject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link ProjectDirectory}.
 */
class ProjectTest {

    private static TestMonitor build(Project project,
                                     boolean initialClean,
                                     boolean watchBinaries) throws InterruptedException {
        final TestMonitor monitor = new TestMonitor(initialClean, watchBinaries, true);
        final Future<?> future = project.build(monitor);
        System.out.println("Waiting up to 20 seconds for build cycle completion");
        if (!monitor.waitForStopped(20)) {
            future.cancel(true);
            fail("Timeout");
        }
        return monitor;
    }

    private static class TestMonitor implements BuildMonitor {
        private final CountDownLatch stoppedLatch;
        private final List<String> output;
        private final boolean initialClean;
        private final boolean watchBinaries;
        private final boolean continueCycle;
        private boolean started;
        private boolean cycleStart;
        private boolean changed;
        private boolean binariesOnly;
        private boolean buildStart;
        private boolean incremental;
        private Throwable buildFailed;
        private boolean ready;
        private boolean cycleEnd;
        private boolean stopped;

        TestMonitor(boolean initialClean, boolean watchBinaries, boolean stopOnCycleEnd) {
            this.stoppedLatch = new CountDownLatch(1);
            this.output = new ArrayList<>();
            this.initialClean = initialClean;
            this.watchBinaries = watchBinaries;
            this.continueCycle = !stopOnCycleEnd;
        }

        @Override
        public Consumer<String> stdOutConsumer() {
            return line -> {
                output.add(line);
                System.out.println(line);
            };
        }

        @Override
        public Consumer<String> stdErrConsumer() {
            return line -> {
                output.add(line);
                System.err.println(line);
            };
        }

        @Override
        public boolean onStarted() {
            started = true;
            return initialClean;
        }

        @Override
        public boolean onCycleStart() {
            cycleStart = true;
            return watchBinaries;
        }

        @Override
        public void onChanged(boolean binariesOnly) {
            changed = true;
            this.binariesOnly = binariesOnly;
        }

        @Override
        public void onBuildStart(boolean incremental) {
            buildStart = true;
            this.incremental = incremental;
        }

        @Override
        public long onBuildFail(Throwable error) {
            buildFailed = error;
            return 0;
        }

        @Override
        public long onReady() {
            ready = true;
            return 0;
        }

        @Override
        public boolean onCycleEnd() {
            cycleEnd = true;
            return continueCycle;
        }

        @Override
        public void onStopped() {
            stopped = true;
            stoppedLatch.countDown();
        }

        boolean waitForStopped(long maxWaitSeconds) throws InterruptedException {
            return stoppedLatch.await(maxWaitSeconds, TimeUnit.SECONDS);
        }
    }

    @Test
    void testQuickstartSeParse() {
        final Path rootDir = helidonSeProject();
        final Project project = createProject(rootDir);
        assertThat(project, is(not(nullValue())));
        assertThat(project.root().directoryType(), is(DirectoryType.Project));
        assertThat(project.root().path(), is(rootDir));
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.size(), is(2));
        assertThat(components.get(0).sourceRoot().path().toString(), endsWith("src/main/java"));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        assertThat(components.get(1).sourceRoot().path().toString(), endsWith("src/main/resources"));
        assertThat(components.get(1).outputRoot().path().toString(), endsWith("target/classes"));
        assertThat(components.get(1).outputRoot(), is(not(components.get(0).outputRoot())));

        final String expectedClassPath = rootDir.resolve("target/classes") + File.pathSeparator + rootDir.resolve("target/libs");
        assertThat(project.classpath(), is(expectedClassPath));
    }

    @Test
    void testQuickstartSeCleanBuild() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final Project project = createProject(rootDir);
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainClass = classes.findFirstNamed(name -> name.endsWith("Main.class"));
        assertThat(mainClass.hasChanged(), is(false));

        final TestMonitor monitor = build(project, true, false);
        assertThat(monitor.started, is(true));
        assertThat(monitor.cycleStart, is(true));
        assertThat(monitor.changed, is(false));
        assertThat(monitor.binariesOnly, is(false));
        assertThat(monitor.started, is(true));
        assertThat(monitor.incremental, is(false));
        assertThat(monitor.buildFailed, is(nullValue()));
        assertThat(monitor.ready, is(true));
        assertThat(monitor.cycleEnd, is(true));
        assertThat(monitor.stopped, is(true));

        assertThat(mainClass.hasChanged(), is(true));
        final String allOutput = String.join(" ", monitor.output);
        final Path targetDir = rootDir.resolve("target");
        assertThat(allOutput, containsString("Deleting " + targetDir));
        assertThat(allOutput, containsString("Changes detected - recompiling the module!"));
    }

//    @Test
//    void testQuickstartSeIncrementalBuild() throws Exception {
//        final Path rootDir = helidonSeProjectCopy();
//        final Project project = createProject(rootDir);
//        final List<BuildComponent> components = project.components();
//        assertThat(components, is(not(nullValue())));
//        assertThat(components.isEmpty(), is(false));
//        final BuildComponent component = components.get(1);
//        assertThat(component.sourceRoot().buildType(), is(BuildType.Resources));
//        assertThat(component.outputRoot().path().toString(), endsWith("target/classes"));
//        final BuildRoot resourceSources = component.sourceRoot();
//        final BuildRoot resourceBinaries = component.outputRoot();
//        final BuildFile resource = resourceSources.findFirstNamed(name -> name.endsWith("application.yaml"));
//        final BuildFile binary = resourceBinaries.findFirstNamed(name -> name.endsWith("application.yaml"));
//        assertThat(resource.hasChanged(), is(false));
//        assertThat(binary.hasChanged(), is(false));
//
//        TestFiles.touch(resource.path());
//        assertThat(resource.hasChanged(), is(true));
//        assertThat(binary.hasChanged(), is(false));
//
//        final List<BuildRoot.Changes> changes = project.sourceChanges();
//        final List<String> output = project.incrementalBuild(changes, STD_OUT, STD_ERR);
//        assertThat(binary.hasChanged(), is(true));
//        final String allOutput = String.join(" ", output);
//        assertThat(allOutput, containsString("Copying resource " + resource.path()));
//    }
}
