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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;
import io.helidon.build.devloop.BuildExecutor;
import io.helidon.build.devloop.TestMonitor;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.cli.common.CliProperties.ENABLE_HELIDON_CLI;
import static io.helidon.build.cli.common.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_LAST_BUILD_SUCCESS_TIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link MavenProjectConfigCollector}.
 */
@Order(2)
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
@SuppressWarnings("SpellCheckingInspection")
class MavenProjectConfigCollectorTestIT {
    private static final String DEBUG_ARG = "-Dproject.config.collector.debug=true";

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testDisabledDoesNothing(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);

        executor.execute(DEBUG_ARG, "validate");
        final String output = monitor.outputAsString();
        assertThat(output, not(containsString(DOT_HELIDON + " file is missing")));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testMissingDotHelidonFileDoesNotCauseFailure(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);

        executor.execute(DEBUG_ARG, ENABLE_HELIDON_CLI, "validate");
        final String output = monitor.outputAsString();
        assertThat(output, containsString(DOT_HELIDON + " file is missing"));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testWithoutCompile(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);
        FileUtils.touch(dotHelidonFile);
        ProjectConfig config = ProjectConfig.projectConfig(projectDir);
        assertThat(config.keySet(), is(Set.of("schema.version")));

        executor.execute(DEBUG_ARG, ENABLE_HELIDON_CLI, "validate");
        final String output = monitor.outputAsString();
        assertThat(output, containsString("Helidon project is supported"));
        assertThat(output, not(containsString("Updating config")));

        config = ProjectConfig.projectConfig(projectDir);
        assertThat(config.keySet(), is(Set.of("schema.version")));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testWithCompile(String basedir) throws Exception {
        final Path projectDir = Path.of(basedir);
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);
        FileUtils.touch(dotHelidonFile);
        ProjectConfig config = ProjectConfig.projectConfig(projectDir);
        assertThat(config.keySet(), is(Set.of("schema.version")));

        final long startTime = System.currentTimeMillis();
        executor.execute(DEBUG_ARG, ENABLE_HELIDON_CLI, "compile");
        final String output = monitor.outputAsString();
        assertThat(output, containsString("Helidon project is supported"));
        assertThat(output, containsString("Updating config"));

        config = ProjectConfig.projectConfig(projectDir);
        assertThat(config.keySet().size(), is(greaterThan(1)));
        assertThat(config.property(PROJECT_LAST_BUILD_SUCCESS_TIME), is(notNullValue()));
        assertThat(config.lastSuccessfulBuildTime(), is(greaterThan(startTime)));
    }
}
