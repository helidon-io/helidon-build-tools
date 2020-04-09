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

import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.TestMonitor;
import io.helidon.build.util.ProjectConfig;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static io.helidon.build.test.TestFiles.ensureDevLoopExtension;
import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.build.test.TestFiles.helidonSeProjectCopy;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.ProjectConfig.PROJECT_LAST_BUILD_SUCCESS_TIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for class {@link ProjectConfigCollector}.
 */
class ProjectConfigCollectorTest {
    private static final String DEBUG_PROPERTY = "-Dproject.config.collector.debug=true";

    @Test
    void testMissingDotHelidonFileDoesNotCauseFailure() throws Exception {
        final Path projectDir = ensureDevLoopExtension(helidonSeProject());
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);

        executor.execute(DEBUG_PROPERTY, "validate");
        final String output = monitor.outputAsString();
        assertThat(output, containsString(DOT_HELIDON + " file is missing"));
    }

    @Test
    void testWithoutCompile() throws Exception {
        final Path projectDir = ensureDevLoopExtension(helidonSeProjectCopy());
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);
        FileUtils.touch(dotHelidonFile.toFile());
        ProjectConfig config = ProjectConfig.loadHelidonCliConfig(projectDir);
        assertThat(config.keySet().isEmpty(), is(true));

        executor.execute(DEBUG_PROPERTY, "validate");
        final String output = monitor.outputAsString();
        assertThat(output, containsString("Helidon project is supported"));
        assertThat(output, not(containsString("Updating config")));

        config = ProjectConfig.loadHelidonCliConfig(projectDir);
        assertThat(config.keySet().isEmpty(), is(true));
    }

    @Test
    void testWithCompile() throws Exception {
        final Path projectDir = ensureDevLoopExtension(helidonSeProjectCopy());
        final TestMonitor monitor = new TestMonitor(1);
        final BuildExecutor executor = new ForkedMavenExecutor(projectDir, monitor, 120);
        final Path dotHelidonFile = projectDir.resolve(DOT_HELIDON);

        Files.deleteIfExists(dotHelidonFile);
        FileUtils.touch(dotHelidonFile.toFile());
        ProjectConfig config = ProjectConfig.loadHelidonCliConfig(projectDir);
        assertThat(config.keySet().isEmpty(), is(true));

        final long startTime = System.currentTimeMillis();
        executor.execute(DEBUG_PROPERTY, "compile");
        final String output = monitor.outputAsString();
        assertThat(output, containsString("Helidon project is supported"));
        assertThat(output, containsString("Updating config"));

        config = ProjectConfig.loadHelidonCliConfig(projectDir);
        assertThat(config.keySet().isEmpty(), is(false));
        assertThat(config.property(PROJECT_LAST_BUILD_SUCCESS_TIME),is(notNullValue()));
        assertThat(config.lastSuccessfulBuildTime(), is(greaterThan(startTime)));
    }
}
