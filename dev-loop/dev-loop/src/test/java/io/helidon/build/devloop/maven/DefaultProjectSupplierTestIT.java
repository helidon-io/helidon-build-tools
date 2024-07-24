/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

package io.helidon.build.devloop.maven;

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;
import io.helidon.build.devloop.BuildComponent;
import io.helidon.build.devloop.BuildExecutor;
import io.helidon.build.devloop.DirectoryType;
import io.helidon.build.devloop.Project;
import io.helidon.build.devloop.ProjectSupplier;
import io.helidon.build.devloop.BuildType;
import io.helidon.build.devloop.TestMonitor;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.test.utils.TestFiles.pathOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link DefaultProjectSupplier}.
 */
@Order(1)
@TestMethodOrder(OrderAnnotation.class)
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
@SuppressWarnings("SpellCheckingInspection")
class DefaultProjectSupplierTestIT {

    @Order(2)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testUpToDate(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 30);
        final ProjectSupplier supplier = new DefaultProjectSupplier();
        final Project project = supplier.newProject(executor, false, true,0);
        assertThat(project, is(not(nullValue())));
        assertThat(project.isBuildUpToDate(), is(true));
        assertThat(monitor.buildStart(0), is(false));
        assertThat(project, is(not(nullValue())));
        assertThat(project.root().directoryType(), Matchers.is(DirectoryType.Project));
        assertThat(project.root().path(), is(projectDir));
        final List<BuildComponent> components = project.components();
        assertThat(components, is(not(nullValue())));
        assertThat(components.size(), is(2));
        assertThat(pathOf(components.get(0).sourceRoot().path()), endsWith("src/main/java"));
        assertThat(pathOf(components.get(0).outputRoot().path()), endsWith("target/classes"));
        assertThat(pathOf(components.get(1).sourceRoot().path()), endsWith("src/main/resources"));
        assertThat(pathOf(components.get(1).outputRoot().path()), endsWith("target/classes"));
        assertThat(components.get(1).outputRoot(), is(not(components.get(0).outputRoot())));

        assertThat(project.classpath().size(), is(greaterThan(2)));
        assertThat(project.mainClassName(), is("io.helidon.build.devloop.tests.Main"));
    }

    @Order(1)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testCleanBuild(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 5 * 60);
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
        assertThat(pathOf(components.get(0).sourceRoot().path()), endsWith("src/main/java"));
        assertThat(pathOf(components.get(0).outputRoot().path()), endsWith("target/classes"));
        assertThat(pathOf(components.get(1).sourceRoot().path()), endsWith("src/main/resources"));
        assertThat(pathOf(components.get(1).outputRoot().path()), endsWith("target/classes"));
        assertThat(components.get(1).outputRoot(), is(not(components.get(0).outputRoot())));

        assertThat(project.classpath().size(), is(greaterThan(2)));
        assertThat(project.mainClassName(), is("io.helidon.build.devloop.tests.Main"));
    }
}
