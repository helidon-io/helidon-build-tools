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

/**
 * Unit test for class {@link ProjectDirectory}.
 */
class ProjectTest {
/*
    private static TestMonitor build(Project project,
                                     boolean initialClean,
                                     boolean watchBinaries) throws InterruptedException {
        return build(project, new TestMonitor(initialClean, watchBinaries, 1));
    }

    private static TestMonitor build(Project project, TestMonitor monitor) throws InterruptedException {
        final Future<?> future = project.build(monitor);
        System.out.println("Waiting up to 30 seconds for build completion");
        if (!monitor.waitForStopped(30)) {
            future.cancel(true);
            fail("Timeout");
        }
        return monitor;
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
    }

    @Test
    void testQuickstartSeUpToDateInitialBuild() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final Project project = createProject(rootDir);
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainClass.hasChanged(), is(false));

        final TestMonitor monitor = build(project, false, false);
        assertThat(monitor.started, is(true));
        assertThat(monitor.cycleStart, is(true));
        assertThat(monitor.changed, is(false));
        assertThat(monitor.binariesOnly, is(false));
        assertThat(monitor.buildStart, is(false));
        assertThat(monitor.incremental, is(false));
        assertThat(monitor.buildFailed, is(nullValue()));
        assertThat(monitor.ready, is(true));
        assertThat(monitor.cycleEnd, is(true));
        assertThat(monitor.stopped, is(true));

        assertThat(mainClass.hasChanged(), is(false));
        final String allOutput = String.join(" ", monitor.output);
        assertThat(allOutput, containsString("Build is up to date"));
    }

    @Test
    void testQuickstartSeOutOfDateInitialBuild() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final Project project = createProject(rootDir);
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot sources = components.get(0).sourceRoot();
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainSource = sources.findFirstNamed(name -> name.equals("Main.java"));
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainSource.hasChanged(), is(false));
        assertThat(mainClass.hasChanged(), is(false));

        TestFiles.touch(mainSource.path());
        assertThat(mainSource.hasChanged(), is(true));
        assertThat(mainClass.hasChanged(), is(false));

        final TestMonitor monitor = build(project, false, false);
        assertThat(monitor.started, is(true));
        assertThat(monitor.cycleStart, is(true));
        assertThat(monitor.changed, is(false));
        assertThat(monitor.binariesOnly, is(false));
        assertThat(monitor.buildStart, is(true));
        assertThat(monitor.incremental, is(false));
        assertThat(monitor.buildFailed, is(nullValue()));
        assertThat(monitor.ready, is(true));
        assertThat(monitor.cycleEnd, is(true));
        assertThat(monitor.stopped, is(true));

        assertThat(mainClass.hasChanged(), is(true));
        final String allOutput = String.join(" ", monitor.output);
        assertThat(allOutput, containsString("Changes detected - recompiling the module!"));
    }

    @Test
    void testQuickstartSeCleanInitialBuild() throws Exception {
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
        assertThat(monitor.buildStart, is(true));
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

    @Test
    void testQuickstartSeIncrementalBuild() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final Project project = createProject(rootDir);
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot sources = components.get(0).sourceRoot();
        final BuildRoot classes = components.get(0).outputRoot();
        final BuildFile mainSource = sources.findFirstNamed(name -> name.equals("Main.java"));
        final BuildFile mainClass = classes.findFirstNamed(name -> name.equals("Main.class"));
        assertThat(mainSource.hasChanged(), is(false));
        assertThat(mainClass.hasChanged(), is(false));

        final TestMonitor monitor = new TestMonitor(true, false, 2) {
            @Override
            public boolean onCycleStart(int cycleNumber) {
                System.out.println("begin cycle " + cycleNumber + " start");
                if (cycleNumber == 2) {
                    TestFiles.touch(mainSource.path());
                    System.out.println("touched" + mainSource.path());
                }
                System.out.println("end cycle " + cycleNumber + " start");
                return super.onCycleStart(cycleNumber);
            }
        };

        final Future<?> future = project.build(monitor);
        System.out.println("Waiting up to 30 seconds for build completion");
        if (!monitor.waitForStopped(30)) {
            future.cancel(true);
            fail("Timeout");
        }

        assertThat(monitor.started, is(true));
        assertThat(monitor.cycleStart, is(true));
        assertThat(monitor.cycleNumber, is(2));
        assertThat(monitor.changed, is(true));
        assertThat(monitor.binariesOnly, is(false));
        assertThat(monitor.started, is(true));
        assertThat(monitor.incremental, is(true));
        assertThat(monitor.buildFailed, is(nullValue()));
        assertThat(monitor.ready, is(true));
        assertThat(monitor.cycleEnd, is(true));
        assertThat(monitor.stopped, is(true));
        assertThat(mainClass.hasChanged(), is(true));
        final String allOutput = String.join(" ", monitor.output);
        assertThat(allOutput, containsString("Compiling 1 source file"));
    }
    */
}
