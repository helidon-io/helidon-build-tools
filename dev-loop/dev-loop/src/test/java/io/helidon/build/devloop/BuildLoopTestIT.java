/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.devloop;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.common.FileChanges.DetectionType;
import io.helidon.build.common.Log;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.devloop.maven.MavenProjectSupplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.lastModifiedTime;
import static io.helidon.build.common.FileUtils.touch;
import static io.helidon.build.common.test.utils.TestFiles.pathOf;
import static io.helidon.build.devloop.TestUtils.newLoop;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link BuildLoop}.
 */
@Order(4)
class BuildLoopTestIT {

    private static final String VALID_JAVA_PUBLIC_CLASS_PREFIX = "public class ";
    private static final String INVALID_JAVA_PUBLIC_CLASS_PREFIX = "not so much a class ";

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

    private static FileTime fixJava(Path javaFile) {
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

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSeUpToDate(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final BuildLoop loop = TestUtils.newLoop(projectDir, false, false, 1);
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
        assertThat(pathOf(components.get(0).outputRoot().path()), endsWith("target/classes"));
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainClass.hasChanged(), is(false));
        assertThat(mainClass.hasChanged(), is(false));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSeCleanInitialBuild(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final BuildLoop loop = TestUtils.newLoop(projectDir, true, false, 1);
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
        assertThat(pathOf(components.get(0).outputRoot().path()), endsWith("target/classes"));
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainClass.hasChanged(), is(false));
        assertThat(mainClass.hasChanged(), is(false));

        final String allOutput = String.join(" ", monitor.outputAsString());
        assertThat(allOutput, containsString("Changes detected - recompiling the module!"));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSeSourceChangeWhileRunning(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final AtomicInteger sourceFilesTouched = new AtomicInteger();
        Path javaFile = projectDir.resolve("src/main/java/io/helidon/build/devloop/tests/GreetService.java");
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

        final BuildLoop loop = newLoop(projectDir, false, false, monitor);
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

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSeResourceChangeWhileRunning(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final AtomicInteger resourceFilesTouched = new AtomicInteger();
        Path resourceFile = projectDir.resolve("src/main/resources/application.yaml");
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

        final BuildLoop loop = newLoop(projectDir, false, false, monitor);
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

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSePomFileChangeWhileRunning(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(3) {
            @Override
            public void onCycleStart(int cycleNumber) {
                super.onCycleStart(cycleNumber);
                if (cycleNumber == 2) {
                    touchFile(projectDir.resolve("pom.xml"));
                }
            }
        };

        final BuildLoop loop = newLoop(projectDir, false, false, monitor);
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

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSeSourceChangeWhileBuildingAfterFailure(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final AtomicReference<FileTime> breakJavaTime = new AtomicReference<>();
        final AtomicReference<FileTime> fixJavaTime = new AtomicReference<>();
        Path javaFile = projectDir.resolve("src/main/java/io/helidon/build/devloop/tests/GreetService.java");
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

        final BuildLoop loop = newLoop(projectDir, false, false, monitor);
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

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    @Disabled("used only for occasional sanity checks; timing related, so could cause intermittent failures")
    void testChangeDetectionMethodsRelativePerformance(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(0);
        final BuildLoop loop = newLoop(projectDir, false, false, monitor);
        final int iterations = 1000;
        final FileTime timeZero = FileTime.fromMillis(0);
        TestUtils.run(loop);
        final Project project = loop.project();

        Log.info("%d iterations of MavenProjectSupplier.changedSince()", iterations);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            MavenProjectSupplier.changedSince(projectDir, timeZero, DetectionType.LATEST);
        }
        final long changedSinceTotal = System.currentTimeMillis() - startTime;
        Log.info("changedSince: %d ms", changedSinceTotal);


        Log.info("%d iterations of project.sourceChangesSince()", iterations);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            project.sourceChangesSince(timeZero);
        }
        final long sourceChangesSinceTotal = System.currentTimeMillis() - startTime;
        Log.info("sourceChangesSince: %d ms", sourceChangesSinceTotal);

        assertThat(sourceChangesSinceTotal, is(lessThan(changedSinceTotal)));
    }
}
