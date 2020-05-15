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

package io.helidon.build.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.util.Log;

import org.junit.jupiter.api.Test;

import static io.helidon.build.dev.TestUtils.newLoop;
import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.build.test.TestFiles.helidonSeProjectCopy;
import static io.helidon.build.test.TestFiles.touch;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link BuildLoop}.
 */
class BuildLoopTest {
    private static final String VALID_JAVA_PUBLIC_CLASS_PREFIX = "public class ";
    private static final String INVALID_JAVA_PUBLIC_CLASS_PREFIX = "not so much a class ";

    private Path pomFile;
    private Path javaFile;
    private Path resourceFile;

    private Path newSeProject(boolean willModify) {
        final Path rootDir = willModify ? helidonSeProjectCopy() : helidonSeProject();
        pomFile = assertFile(rootDir.resolve("pom.xml"));
        javaFile = assertFile(rootDir.resolve("src/main/java/io/helidon/examples/quickstart/se/GreetService.java"));
        resourceFile = assertFile(rootDir.resolve("src/main/resources/application.yaml"));
        return rootDir;
    }

    private static void touchFile(Path file) {
        wait(file, "touching");
        touch(file);
    }

    private static void wait(Path file, String operation) {
        Path fileName = file.getFileName();
        FileTime lastMod = lastModifiedTime(file);
        Log.info("sleeping 1.25 seconds before %s %s, mod time is %s", operation, fileName, lastMod);
        try {
            Thread.sleep(1250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static FileTime lastModified(Path file) {
        FileTime result = lastModifiedTime(file);
        Log.info("%s mod time is %s", file.getFileName(), result);
        return result;
    }

    private static FileTime breakJava(Path javaFile) {
        wait(javaFile, "BREAKING");
        return changeJava(javaFile, VALID_JAVA_PUBLIC_CLASS_PREFIX, INVALID_JAVA_PUBLIC_CLASS_PREFIX);
    }

    private static FileTime fixJava(Path javaFile)  {
        wait(javaFile, "FIXING");
        return changeJava(javaFile, INVALID_JAVA_PUBLIC_CLASS_PREFIX, VALID_JAVA_PUBLIC_CLASS_PREFIX);
    }

    private static FileTime changeJava(Path javaFile, String existing, String replacement) {
        try {
            final String source = Files.readString(javaFile);
            final String brokenSource = source.replace(existing, replacement);
            Files.writeString(javaFile, brokenSource);
            return lastModified(javaFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void testQuickstartSeUpToDate() throws Exception {
        final Path rootDir = newSeProject(false);
        final BuildLoop loop = TestUtils.newLoop(rootDir, false, false, 1);
        final TestMonitor monitor = TestUtils.run(loop);
        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(1));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.changeType(0), is(nullValue()));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.changeType(1), is(nullValue()));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(false));
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
        final Path rootDir = newSeProject(true);
        final BuildLoop loop = TestUtils.newLoop(rootDir, true, false, 1);
        final TestMonitor monitor = TestUtils.run(loop);
        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(1));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.changeType(0), is(nullValue()));
        assertThat(monitor.buildStart(0), is(true));
        assertThat(monitor.buildType(0), is(BuildType.ForkedCleanComplete));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.changeType(1), is(nullValue()));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(false));
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
        final Path rootDir = newSeProject(true);
        final AtomicInteger sourceFilesTouched = new AtomicInteger();
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    touchFile(javaFile);
                    sourceFilesTouched.incrementAndGet();
                }
            }
        };

        final BuildLoop loop = newLoop(rootDir, false, false, monitor);
        TestUtils.run(loop);

        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(3));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.changeType(0), is(nullValue()));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.changeType(1), is(nullValue()));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(false));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.changeType(2), is(ChangeType.SourceFile));
        assertThat(monitor.buildStart(2), is(true));
        assertThat(monitor.buildType(2), is(BuildType.Incremental));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(true));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.changeType(3), is(nullValue()));
        assertThat(monitor.buildStart(3), is(false));
        assertThat(monitor.buildType(3), is(nullValue()));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(false));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Compiling " + sourceFilesTouched.get() + " source file"));
    }

    @Test
    void testQuickstartSeResourceChangeWhileRunning() throws Exception {
        final Path rootDir = newSeProject(true);
        final AtomicInteger resourceFilesTouched = new AtomicInteger();
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    touch(resourceFile);
                    resourceFilesTouched.incrementAndGet();
                }
            }
        };

        final BuildLoop loop = newLoop(rootDir, false, false, monitor);
        TestUtils.run(loop);

        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(3));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.changeType(0), is(nullValue()));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.changeType(1), is(nullValue()));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(false));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.changeType(2), is(ChangeType.SourceFile));
        assertThat(monitor.buildStart(2), is(true));
        assertThat(monitor.buildType(2), is(BuildType.Incremental));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(true));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.changeType(3), is(nullValue()));
        assertThat(monitor.buildStart(3), is(false));
        assertThat(monitor.buildType(3), is(nullValue()));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(false));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Copying " + resourceFilesTouched.get() + " resource files"));
    }

    @Test
    void testQuickstartSePomFileChangeWhileRunning() throws Exception {
        final Path rootDir = newSeProject(true);
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    touchFile(pomFile);
                }
            }
        };

        final BuildLoop loop = newLoop(rootDir, false, false, monitor);
        TestUtils.run(loop);

        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(3));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.changeType(0), is(nullValue()));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.changeType(1), is(nullValue()));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(false));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.changeType(2), is(ChangeType.BuildFile));
        assertThat(monitor.buildStart(2), is(false));
        assertThat(monitor.buildType(2), is(nullValue()));
        assertThat(monitor.buildFailed(2), is(nullValue()));
        assertThat(monitor.ready(2), is(false));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(false));
        assertThat(monitor.changeType(3), is(nullValue()));
        assertThat(monitor.buildStart(3), is(true));
        assertThat(monitor.buildType(3), is(BuildType.ForkedComplete));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(true));
        assertThat(monitor.cycleEnd(3), is(true));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("BUILD SUCCESS"));
    }

    @Test
    void testQuickstartSeSourceChangeWhileBuildingAfterFailure() throws Exception {
        final Path rootDir = newSeProject(true);
        final AtomicReference<FileTime> breakJavaTime = new AtomicReference<>();
        final AtomicReference<FileTime> fixJavaTime = new AtomicReference<>();
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    breakJavaTime.set(breakJava(javaFile));
                }
            }

            @Override
            public long onBuildFail(int cycleNumber, BuildType type, Throwable error) {
                final long result = super.onBuildFail(cycleNumber, type, error);
                assertThat(cycleNumber, is(2));
                fixJavaTime.set(fixJava(javaFile));
                return result;
            }
        };

        final BuildLoop loop = newLoop(rootDir, false, false, monitor);
        TestUtils.run(loop);

        final Project project = loop.project();
        assertThat(project, is(not(nullValue())));
        assertThat(monitor.started(), is(true));
        assertThat(monitor.stopped(), is(true));
        assertThat(monitor.lastCycle(), is(3));

        assertThat(monitor.cycleStart(0), is(true));
        assertThat(monitor.changed(0), is(false));
        assertThat(monitor.changeType(0), is(nullValue()));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(monitor.buildType(0), is(nullValue()));
        assertThat(monitor.buildFailed(0), is(nullValue()));
        assertThat(monitor.ready(0), is(true));
        assertThat(monitor.cycleEnd(0), is(true));

        assertThat(monitor.cycleStart(1), is(true));
        assertThat(monitor.changed(1), is(false));
        assertThat(monitor.changeType(1), is(nullValue()));
        assertThat(monitor.buildStart(1), is(false));
        assertThat(monitor.buildType(1), is(nullValue()));
        assertThat(monitor.buildFailed(1), is(nullValue()));
        assertThat(monitor.ready(1), is(false));
        assertThat(monitor.cycleEnd(1), is(true));

        assertThat(monitor.cycleStart(2), is(true));
        assertThat(monitor.changed(2), is(true));
        assertThat(monitor.changeType(2), is(ChangeType.SourceFile));
        assertThat(monitor.buildStart(2), is(true));
        assertThat(monitor.buildType(2), is(BuildType.Incremental));
        assertThat(monitor.buildFailed(2), is(notNullValue()));
        assertThat(monitor.ready(2), is(false));
        assertThat(monitor.cycleEnd(2), is(true));

        assertThat(monitor.cycleStart(3), is(true));
        assertThat(monitor.changed(3), is(true));
        assertThat(monitor.changeType(3), is(ChangeType.SourceFile));
        assertThat(monitor.buildStart(3), is(true));
        assertThat(monitor.buildType(3), is(BuildType.Incremental));
        assertThat(monitor.buildFailed(3), is(nullValue()));
        assertThat(monitor.ready(3), is(true));
        assertThat(monitor.cycleEnd(3), is(true));
    }
}
