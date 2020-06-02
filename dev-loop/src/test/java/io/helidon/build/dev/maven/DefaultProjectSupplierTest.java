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

package io.helidon.build.dev.maven;

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.dev.BuildComponent;
import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.DirectoryType;
import io.helidon.build.dev.Project;
import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.test.TestFiles;
import io.helidon.build.dev.BuildType;
import io.helidon.build.dev.TestMonitor;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link DefaultProjectSupplier}.
 */
class DefaultProjectSupplierTest {

    @Test
    void testUpToDate() throws Exception {
        final Path projectDir = TestFiles.helidonSeProject();
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 30);
        final ProjectSupplier supplier = new DefaultProjectSupplier();
        final Project project = supplier.newProject(executor, false, true,0);
        assertThat(project, is(not(nullValue())));
        assertThat(project.isBuildUpToDate(), is(true));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(project, is(not(nullValue())));
        MatcherAssert.assertThat(project.root().directoryType(), Matchers.is(DirectoryType.Project));
        assertThat(project.root().path(), is(projectDir));
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.size(), is(2));
        assertThat(components.get(0).sourceRoot().path().toString(), endsWith("src/main/java"));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        assertThat(components.get(1).sourceRoot().path().toString(), endsWith("src/main/resources"));
        assertThat(components.get(1).outputRoot().path().toString(), endsWith("target/classes"));
        assertThat(components.get(1).outputRoot(), is(not(components.get(0).outputRoot())));

        assertThat(project.classpath().size(), is(greaterThan(2)));
        assertThat(project.mainClassName(), is("io.helidon.examples.se.Main"));
    }

    @Test
    void testCleanBuild() throws Exception {
        final Path projectDir = TestFiles.helidonSeProjectCopy();
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 30);
        final ProjectSupplier supplier = new DefaultProjectSupplier();
        final Project project = supplier.newProject(executor, true, true,0);
        assertThat(project, is(not(nullValue())));
        assertThat(project.isBuildUpToDate(), is(true));
        assertThat(monitor.buildStart(0), is(true));
        assertThat(monitor.buildType(0), is(BuildType.ForkedCleanComplete));
        assertThat(project, is(not(nullValue())));
        assertThat(project.root().directoryType(), is(DirectoryType.Project));
        assertThat(project.root().path(), is(projectDir));
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.size(), is(2));
        assertThat(components.get(0).sourceRoot().path().toString(), endsWith("src/main/java"));
        assertThat(components.get(0).outputRoot().path().toString(), endsWith("target/classes"));
        assertThat(components.get(1).sourceRoot().path().toString(), endsWith("src/main/resources"));
        assertThat(components.get(1).outputRoot().path().toString(), endsWith("target/classes"));
        assertThat(components.get(1).outputRoot(), is(not(components.get(0).outputRoot())));

        assertThat(project.classpath().size(), is(greaterThan(2)));
        assertThat(project.mainClassName(), is("io.helidon.examples.se.Main"));
    }
}
