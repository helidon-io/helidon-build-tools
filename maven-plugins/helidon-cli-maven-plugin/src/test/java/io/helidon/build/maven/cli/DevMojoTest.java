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
package io.helidon.build.maven.cli;

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.devloop.maven.DevLoopBuildConfig;
import io.helidon.build.devloop.maven.DevLoopBuildConfig.FullBuildConfig;
import io.helidon.build.devloop.maven.DevLoopBuildConfig.IncrementalBuildConfig;
import io.helidon.build.devloop.maven.DevLoopBuildConfig.IncrementalBuildConfig.CustomDirectoryConfig;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static io.helidon.build.maven.cli.DevMojoLoader.configuredMojoFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link DevMojo}.
 */
class DevMojoTest {

    @SuppressWarnings("SameParameterValue")
    static void assertThrows(Class<?> exceptionType, Executable function, String... expectedMessageFragments) {
        try {
            function.execute();
            fail("should have failed");
        } catch (Throwable t) {
            if (!exceptionType.isAssignableFrom(t.getClass())) {
                fail("expected " + exceptionType + ", got " + t );
            }
            String message = t.getMessage();
            for (String expectedFragment : expectedMessageFragments) {
                assertThat(message, containsString(expectedFragment));
            }
        }
    }

    @Test
    void testInvalidMaxApplicationFailures() {
        assertThrows(MojoExecutionException.class,
                     () -> configuredMojoFor("invalid-max-application-failures").buildConfig(false),
                     "maxApplicationFailures cannot be negative", "devLoop", "maxApplicationFailures=-1");
    }

    @Test
    void testInvalidMaxBuildFailures() {
        assertThrows(MojoExecutionException.class,
                     () -> configuredMojoFor("invalid-max-full-build-failures").buildConfig(false),
                     "maxBuildFailures cannot be negative", "fullBuild", "maxBuildFailures=-1");
        assertThrows(MojoExecutionException.class,
                     () -> configuredMojoFor("invalid-max-incr-build-failures").buildConfig(false),
                     "maxBuildFailures cannot be negative", "incrementalBuild", "maxBuildFailures=-1");
    }

    @Test
    void testCustomDirectoryMissingPath() {
        assertThrows(MojoExecutionException.class,
                     () -> configuredMojoFor("missing-custom-path").buildConfig(false),
                     "path is required", "customDirectory");
    }

    @Test
    void testCustomDirectoryMissingGoal() {
        assertThrows(MojoExecutionException.class,
                     () -> configuredMojoFor("missing-custom-goals").buildConfig(false),
                     "one or more goals are required", "customDirectory");
    }

    @Test
    void testCustomDirectoryMatchesNone() {
        assertThrows(MojoExecutionException.class,
                     () -> configuredMojoFor("matches-none").buildConfig(false),
                     "will not match any file", "customDirectory");
    }

    @Test
    void testEmptyFullBuild() throws Exception {
        DevMojo mojo = configuredMojoFor("empty-full-build");
        assertThat(mojo, is(not(nullValue())));
        DevLoopBuildConfig config = mojo.buildConfig(false);
        assertThat(config, is(not(nullValue())));

        FullBuildConfig fullBuild = config.fullBuild();
        assertThat(fullBuild, is(not(nullValue())));
        assertThat(fullBuild.maxBuildFailures(), is(Integer.MAX_VALUE));
        assertThat(fullBuild.phase(), is("process-classes"));
    }

    @Test
    void testEmptyIncrementalBuild() throws Exception {
        DevMojo mojo = configuredMojoFor("empty-incr-build");
        assertThat(mojo, is(not(nullValue())));
        DevLoopBuildConfig config = mojo.buildConfig(false);
        assertThat(config, is(not(nullValue())));

        IncrementalBuildConfig incrementalBuild = config.incrementalBuild();
        assertThat(incrementalBuild, is(not(nullValue())));
        assertThat(incrementalBuild.maxBuildFailures(), is(Integer.MAX_VALUE));

        List<String> goals = incrementalBuild.unresolvedResourceGoals();
        assertThat(goals.size(), is(1));
        assertThat(goals.get(0), is("resources:resources"));

        goals = incrementalBuild.unresolvedJavaSourceGoals();
        assertThat(goals.size(), is(1));
        assertThat(goals.get(0), is("compiler:compile"));

        assertThat(incrementalBuild.customDirectories().size(), is(0));
    }

    @Test
    void testAllBuildConfigurationElements() throws Exception {
        DevMojo mojo = configuredMojoFor("all");
        assertThat(mojo, is(not(nullValue())));
        DevLoopBuildConfig config = mojo.buildConfig(false);
        assertThat(config, is(not(nullValue())));
        assertThat(config.maxApplicationFailures(), is(16));

        FullBuildConfig fullBuild = config.fullBuild();
        assertThat(fullBuild, is(not(nullValue())));
        assertThat(fullBuild.maxBuildFailures(), is(1));
        assertThat(fullBuild.phase(), is("process-something"));

        IncrementalBuildConfig incrementalBuild = config.incrementalBuild();
        assertThat(incrementalBuild, is(not(nullValue())));
        assertThat(incrementalBuild.maxBuildFailures(), is(8192));

        List<String> goals = incrementalBuild.unresolvedResourceGoals();
        assertThat(goals.size(), is(2));
        assertThat(goals.get(0), is("resources:resources"));
        assertThat(goals.get(1), is("resources:test"));

        goals = incrementalBuild.unresolvedJavaSourceGoals();
        assertThat(goals.size(), is(1));
        assertThat(goals.get(0), is("compiler:compile"));

        List<CustomDirectoryConfig> directories = incrementalBuild.customDirectories();
        assertThat(directories.size(), is(2));

        CustomDirectoryConfig directory = directories.get(0);
        goals = directory.unresolvedGoals();
        assertThat(directory.path().toString(), is("etc"));
        assertThat(directory.includes().test(Path.of("foo/x.hola"), null), is(true));
        assertThat(directory.includes().test(Path.of("foo/x.hole"), null), is(false));
        assertThat(goals.size(), is(2));
        assertThat(goals.get(0), is("exec:exec@say-hola"));
        assertThat(goals.get(1), is("exec:exec@say-boo"));

        directory = directories.get(1);
        goals = directory.unresolvedGoals();
        assertThat(directory.path().toString(), is("ctl"));
        assertThat(directory.includes().test(Path.of("foo/x.exit"), null), is(true));
        assertThat(directory.includes().test(Path.of("foo/x.ext"), null), is(false));
        assertThat(goals.size(), is(1));
        assertThat(goals.get(0), is("exec:java@exit"));
    }
}
