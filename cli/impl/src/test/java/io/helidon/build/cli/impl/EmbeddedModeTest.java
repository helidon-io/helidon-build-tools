/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.SubstitutionVariables;
import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.test.utils.LogRecordExtension;
import io.helidon.build.common.test.utils.WorkDirExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.RestoreSystemProperties;

import static io.helidon.build.common.ansi.AnsiTextStyle.strip;
import static io.helidon.build.common.FileUtils.cwd;
import static io.helidon.build.common.FileUtils.directory;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.userHome;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.test.utils.LogMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for embedded mode.
 */
@ExtendWith(LogRecordExtension.class)
@ExtendWith(WorkDirExtension.class)
@RestoreSystemProperties
class EmbeddedModeTest {

    private final LogRecorder recorder;
    private final Path workDir;
    private String metadataUrl;
    private Path projectDir;

    EmbeddedModeTest(LogRecorder recorder, Path workDir) {
        this.recorder = recorder;
        this.workDir = workDir;
    }

    @BeforeEach
    void setUp() {
        System.setProperty("user.home", workDir.toString());
        projectDir = ensureDirectory(workDir.resolve("project"));
        System.setProperty("user.dir", projectDir.toString());
        metadataUrl = directory(workDir.resolve("cli-data"), Map.of(
                "versions.xml", """
                        <data>
                            <archetypes>
                                <version default="true">4.0.0</version>
                            </archetypes>
                        </data>
                        """,
                "4.0.0/cli-data.zip", zip(Map.of(
                        "metadata.properties", "cli.version=0.0.1\n"))))
                .toUri()
                .toString();
    }

    @Test
    void testRedirectedPaths() {
        Path configDir = workDir.resolve(".helidon");

        assertThat(userHome(), is(workDir));
        assertThat(cwd(), is(projectDir));
        assertThat(UserConfig.current().path(), is(configDir.resolve("config")));
        assertThat(UserConfig.current().cacheDir(), is(configDir.resolve("cache")));
        assertThat(Metadata.builder().build().rootDir(), is(configDir.resolve("cache")));
        assertThat(new CommonOptions(null, null, null, false, null).project(), is(projectDir));
    }

    @Test
    void testValidCommand() {
        Helidon.execute("version", "--url", metadataUrl);

        var variables = SubstitutionVariables.of(Map.of(
                "buildVersion", BuildProperties.buildVersion(),
                "buildRevision", BuildProperties.buildRevision()));

        var output = strip(recorder.output());
        assertThat(output, matches(variables.resolve("""
                ~...
                build.version            ${buildVersion}
                build.revision           ${buildRevision}
                ~...
                """)));
    }

    @Test
    void testUnknownCommand() {
        var e = assertThrows(Error.class, () -> Helidon.execute("foo"));

        assertThat(e.getMessage(), matches("""
                'foo' is not a valid command.
                See 'helidon --help' for more information
                """.stripTrailing()));

        var output = strip(recorder.output());
        assertThat(output, matches("""

                error: 'foo' is not a valid command.
                See 'helidon --help' for more information

                """));
    }

    @Test
    void testInvalidCommand() {
        var e = assertThrows(Error.class, () -> Helidon.execute("*"));

        assertThat(e.getMessage(), is("Invalid command name: *"));

        var output = strip(recorder.output());
        assertThat(output, matches("""

                error: Invalid command name: *

                """));
    }

    @Test
    void testFormatStringInProperties() {
        System.setProperty("format", "%s");
        Helidon.execute("info", "--verbose", "--url", metadataUrl);

        var output = strip(recorder.output());
        Path configFile = workDir.resolve(".helidon/config");
        Path cacheDir = workDir.resolve(".helidon/cache");
        var variables = SubstitutionVariables.of(Map.of(
                "configFile", configFile.toString(),
                "cacheDir", cacheDir.toString(),
                "projectDir", projectDir.toString(),
                "workDir", workDir.toString()));
        assertThat(output, matches(variables.resolve("""
                ~...
                config.file ~.* ${configFile}
                ~...
                cache.dir ~.* ${cacheDir}
                ~...
                format ~.* %s
                ~...
                user.dir ~.* ${projectDir}
                user.home ~.* ${workDir}
                ~...
                formatEnv ~.* %s
                ~...
                """)));
        assertThat(values(output, "config.file"), everyItem(is(configFile.toString())));
        assertThat(values(output, "cache.dir"), everyItem(is(cacheDir.toString())));
        assertThat(values(output, "user.dir"), everyItem(is(projectDir.toString())));
        assertThat(values(output, "user.home"), everyItem(is(workDir.toString())));
    }

    @Test
    void testSystemPropertiesAreScopedToExecution() throws IOException {
        Path configFile = ensureDirectory(workDir.resolve(".helidon")).resolve("config");
        Files.writeString(configFile, "system_embedded.scope=configured\n");
        System.setProperty("embedded.scope", "original");

        Helidon.execute("info", "--verbose", "--url", metadataUrl);

        var output = strip(recorder.output());
        assertThat(values(output, "embedded.scope"), contains("configured"));
        assertThat(System.getProperty("embedded.scope"), is("original"));

        assertThrows(Error.class, () -> Helidon.execute("*"));
        assertThat(System.getProperty("embedded.scope"), is("original"));
    }

    private static List<String> values(String output, String key) {
        return output.lines()
                .filter(line -> line.startsWith(key + " "))
                .map(line -> line.substring(key.length()).trim())
                .toList();
    }
}
