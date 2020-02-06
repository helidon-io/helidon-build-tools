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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;
import io.helidon.dev.build.maven.DefaultHelidonProjectSupplier;

import org.junit.jupiter.api.Test;

import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.build.test.TestFiles.helidonSeProjectCopy;
import static io.helidon.build.test.TestFiles.touch;
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
class BuildLoopTest {

    private static BuildLoop newLoop(Path projectRoot,
                                     boolean initialClean,
                                     boolean watchBinariesOnly,
                                     int stopCycleNumber) {
        return newLoop(projectRoot, initialClean, watchBinariesOnly, new TestMonitor(stopCycleNumber));
    }

    private static BuildLoop newLoop(Path projectRoot,
                                     boolean initialClean,
                                     boolean watchBinariesOnly,
                                     TestMonitor monitor) {
        return BuildLoop.builder()
                        .projectDirectory(projectRoot)
                        .clean(initialClean)
                        .watchBinariesOnly(watchBinariesOnly)
                        .projectSupplier(new DefaultHelidonProjectSupplier())
                        .stdOut(monitor.stdOutConsumer())
                        .stdErr(monitor.stdErrConsumer())
                        .buildMonitor(monitor)
                        .build();
    }

    private static TestMonitor run(BuildLoop loop) throws InterruptedException {
        return run(loop, 30);
    }

    private static TestMonitor run(BuildLoop loop, int maxWaitSeconds) throws InterruptedException {
        loop.start();
        Log.info("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            fail("Timeout");
        }
        return (TestMonitor) loop.monitor();
    }

    @Test
    void testQuickstartSeUpToDate() throws Exception {
        final Path rootDir = helidonSeProject();
        final BuildLoop loop = newLoop(rootDir, false, false, 1);
        final TestMonitor monitor = run(loop);
        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(1));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.binariesOnly(0), is(false));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.incremental(0), is(false));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.incremental(1), is(false));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(true));
        assertThat(monitor.cycleEnd(1), is(true));

        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainClass.hasChanged(), is(false));
        assertThat(mainClass.hasChanged(), is(false));
    }

    @Test
    void testQuickstartSeCleanInitialBuild() throws Exception {
        final Path rootDir = helidonSeProjectCopy();

        final BuildLoop loop = newLoop(rootDir, true, false, 1);
        final TestMonitor monitor = run(loop);
        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(1));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.binariesOnly(0), is(false));
        assertThat(monitor.buildStart(0), is(true));
        assertThat(monitor.incremental(0), is(false));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.incremental(1), is(false));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(true));
        assertThat(monitor.cycleEnd(1), is(true));

        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainClass.hasChanged(), is(false));
        assertThat(mainClass.hasChanged(), is(false));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Changes detected - recompiling the module!"));
    }

    @Test
    void testQuickstartSeSourceChangeWhileRunning() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final AtomicInteger sourceFilesTouched = new AtomicInteger();
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    Log.info("sleeping 1.25 second before touching source files");
                    try {
                        Thread.sleep(1250);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Log.info("touching source files");
                    FileUtils.list(rootDir.resolve("src/main/java"), 16).forEach(file -> {
                        if (file.getFileName().toString().endsWith(".java")) {
                            touch(file);
                            sourceFilesTouched.incrementAndGet();
                        }
                    });
                }
            }
        };

        final BuildLoop loop = newLoop(rootDir, false, false, monitor);
        run(loop);

        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(3));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.binariesOnly(0), is(false));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.incremental(0), is(false));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.incremental(1), is(false));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(true));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.binariesOnly(2), is(false));
        assertThat(monitor.buildStart(2), is(true));
        assertThat(monitor.incremental(2), is(true));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(true));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.binariesOnly(3), is(false));
        assertThat(monitor.buildStart(3), is(false));
        assertThat(monitor.incremental(3), is(false));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(true));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Compiling " + sourceFilesTouched.get() + " source files"));
    }
}
