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
import java.util.List;
import java.util.function.Consumer;

import io.helidon.build.test.TestFiles;
import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.BuildFile;
import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.BuildType;
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

/**
 * Unit test for class {@link ProjectDirectory}.
 */
class ProjectTest {
    private final static Consumer<String> STD_OUT = System.out::println;
    private final static Consumer<String> STD_ERR = System.err::println;

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
        final List<String> output = project.fullBuild(STD_OUT, STD_ERR, true);
        assertThat(mainClass.hasChanged(), is(true));
        final String allOutput = String.join(" ", output);
        final Path targetDir = rootDir.resolve("target");
        assertThat(allOutput, containsString("Deleting " + targetDir));
        assertThat(allOutput, containsString("Changes detected - recompiling the module!"));
    }

    @Test
    void testQuickstartSeIncrementalBuildResources() throws Exception {
        final Path rootDir = helidonSeProjectCopy();
        final Project project = createProject(rootDir);
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.isEmpty(), is(false));
        final BuildComponent component = components.get(1);
        assertThat(component.sourceRoot().buildType(), is(BuildType.Resources));
        assertThat(component.outputRoot().path().toString(), endsWith("target/classes"));
        final BuildRoot resourceSources = component.sourceRoot();
        final BuildRoot resourceBinaries = component.outputRoot();
        final BuildFile resource = resourceSources.findFirstNamed(name -> name.endsWith("application.yaml"));
        final BuildFile binary = resourceBinaries.findFirstNamed(name -> name.endsWith("application.yaml"));
        assertThat(resource.hasChanged(), is(false));
        assertThat(binary.hasChanged(), is(false));

        TestFiles.touch(resource.path());
        assertThat(resource.hasChanged(), is(true));
        assertThat(binary.hasChanged(), is(false));

        final List<BuildRoot.Changes> changes = project.sourceChanges();
        final List<String> output = project.incrementalBuild(changes, STD_OUT, STD_ERR);
        assertThat(binary.hasChanged(), is(true));
        final String allOutput = String.join(" ", output);
        assertThat(allOutput, containsString("Copying resource " + resource.path()));
    }
}
