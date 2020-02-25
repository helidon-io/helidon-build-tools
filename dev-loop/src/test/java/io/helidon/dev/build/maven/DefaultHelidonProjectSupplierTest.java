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

package io.helidon.dev.build.maven;

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.test.TestFiles;
import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.BuildType;
import io.helidon.dev.build.DirectoryType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectSupplier;
import io.helidon.dev.build.TestMonitor;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link DefaultHelidonProjectSupplier}.
 */
class DefaultHelidonProjectSupplierTest {

    @Test
    void testUpToDate() throws Exception {
        final Path projectDir = TestFiles.helidonSeProject();
        final ProjectSupplier supplier = new DefaultHelidonProjectSupplier(30);
        final TestMonitor monitor = new TestMonitor(1);
        final Project project = supplier.get(projectDir, monitor, false, 0);
        assertThat(project, is(not(nullValue())));
        assertThat(project.isBuildUpToDate(), is(true));
        assertThat(monitor.buildStart(0), is(false));
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
        assertThat(project.mainClassName(), is("io.helidon.examples.quickstart.se.Main"));
    }

    @Test
    void testCleanBuild() throws Exception {
        final Path projectDir = TestFiles.helidonSeProjectCopy();
        final ProjectSupplier supplier = new DefaultHelidonProjectSupplier(30);
        final TestMonitor monitor = new TestMonitor(1);
        final Project project = supplier.get(projectDir, monitor, true, 0);
        assertThat(project, is(not(nullValue())));
        assertThat(project.isBuildUpToDate(), is(true));
        assertThat(monitor.buildStart(0), is(true));
        assertThat(monitor.buildType(0), is(BuildType.CleanComplete));
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
        assertThat(project.mainClassName(), is("io.helidon.examples.quickstart.se.Main"));
    }
}
