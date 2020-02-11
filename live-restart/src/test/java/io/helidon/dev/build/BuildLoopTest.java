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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;
import org.junit.jupiter.api.Test;

import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.build.test.TestFiles.helidonSeProjectCopy;
import static io.helidon.build.test.TestFiles.touch;
import static io.helidon.dev.build.TestUtils.newLoop;
import static io.helidon.dev.build.TestUtils.run;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link BuildLoop}.
 */
class BuildLoopTest {

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
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
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
        assertThat(monitor.buildType(0), is(BuildType.CleanComplete));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
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
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(true));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.binariesOnly(2), is(false));
        assertThat(monitor.buildStart(2), is(true));
        assertThat(monitor.buildType(2), is(BuildType.Incremental));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(true));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.binariesOnly(3), is(false));
        assertThat(monitor.buildStart(3), is(false));
        assertThat(monitor.buildType(3), is(nullValue()));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(true));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Compiling " + sourceFilesTouched.get() + " source files"));
    }

    @Test
    void testQuickstartSeResourceChangeWhileRunning() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final AtomicInteger resourceFilesTouched = new AtomicInteger();
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    Log.info("sleeping 1.25 second before touching resource files");
                    try {
                        Thread.sleep(1250);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Log.info("touching resource files");
                    FileUtils.list(rootDir.resolve("src/main/resources"), 16).forEach(file -> {
                        if (Files.isRegularFile(file)) {
                            touch(file);
                            resourceFilesTouched.incrementAndGet();
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
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(true));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.binariesOnly(2), is(false));
        assertThat(monitor.buildStart(2), is(true));
        assertThat(monitor.buildType(2), is(BuildType.Incremental));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(true));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.binariesOnly(3), is(false));
        assertThat(monitor.buildStart(3), is(false));
        assertThat(monitor.buildType(3), is(nullValue()));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(true));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Copying " + resourceFilesTouched.get() + " resource files"));
    }

    @Test
    void testQuickstartSePomFileChangeWhileRunning() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    Log.info("sleeping 1.25 second before touching pom file");
                    try {
                        Thread.sleep(1250);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Log.info("touching pom file");
                    touch(rootDir.resolve("pom.xml"));
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
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.binariesOnly(1), is(false));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(true));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.binariesOnly(2), is(false));
        assertThat(monitor.buildStart(2), is(false));
        assertThat(monitor.buildType(2), is(nullValue()));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(false));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.binariesOnly(3), is(false));
        assertThat(monitor.buildStart(3), is(true));
        assertThat(monitor.buildType(3), is(BuildType.Complete));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(true));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("BUILD SUCCESS"));
    }
}
