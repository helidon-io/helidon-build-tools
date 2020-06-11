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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.test.CapturingLogWriter;
import io.helidon.build.test.TestFiles;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.UserConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test for class {@link Metadata}.
 */
class MetadataTestIT {
    private static final String BASE_URL = baseUrl("cli-data");

    static String baseUrl(String name) {
        return requireNonNull(MetadataTestIT.class.getClassLoader().getResource(name)).toExternalForm();
    }

    private Path cacheDir;
    private CapturingLogWriter output;

    @BeforeEach
    public void beforeEach() throws IOException {
        Config.setUserHome(TestFiles.targetDir().resolve("alice"));
        final UserConfig userConfig = Config.userConfig();
        userConfig.clearCache();
        userConfig.clearPlugins();
        cacheDir = userConfig.cacheDir();
        output = CapturingLogWriter.install();
    }

    @AfterEach
    public void afterEach() {
        output.uninstall();
    }

    private Metadata newInstance(int updateDelayHours) {
        return Metadata.newInstance(cacheDir, BASE_URL, updateDelayHours, true);
    }

    private List<CapturingLogWriter.LogEntry> logEntries() {
        return output.entries();
    }

    private List<String> logMessages() {
        return output.messages();
    }

    private List<String> logLines(Predicate<String> filter) {
        return logMessages().stream().filter(filter).collect(Collectors.toList());
    }

    private void assertLogged(String... messageFragments) {
        assertLogged(line -> true, messageFragments);
    }

    private void assertNotLogged(String... messageFragments) {
        assertNotLogged(line -> true, messageFragments);
    }

    private void assertLogged(Predicate<String> filter, String... messageFragments) {
        if (!isLogged(filter, messageFragments)) {
            fail("log does not contain one of the following: " + Arrays.toString(messageFragments) + EOL + logMessages());
        }
    }

    private void assertNotLogged(Predicate<String> filter, String... messageFragments) {
        if (isLogged(filter, messageFragments)) {
            fail("log should not contain one of the following: " + Arrays.toString(messageFragments) + EOL + logMessages());
        }
    }

    private boolean isLogged(Predicate<String> filter, String... messageFragments) {
        int count = messageFragments.length;
        for (String line : logLines(filter)) {
            for (String fragment : messageFragments) {
                if (line.contains(fragment)) {
                    count--;
                    if (count == 0) {
                        return true;
                    } else {
                        break;
                    }
                }
            }
        }
        return false;
    }

    private long countLogged(Predicate<String> filter, String fragment) {
        return logLines(filter).stream().filter(line -> line.contains(fragment)).count();
    }

    private int countLogged(String fragment1, String fragment2) {
        return (int) logLines(line -> line.contains(fragment1)).stream().filter(line -> line.contains(fragment2)).count();
    }

    @Test
    void smokeTest() throws Exception {
        Metadata meta = newInstance(2);

        MavenVersion version = meta.latestVersion();
        assertThat(version, is(not(nullValue())));
        assertThat(version, is(toMavenVersion("2.0.0-RC1")));

        ConfigProperties props = meta.properties(version);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("helidon.version"), is("2.0.0-RC1"));
        assertThat(props.property("build-tools.version"), is("2.0.0-RC1"));
        assertThat(props.property("cli.version"), is("2.0.0-RC1"));
        assertThat(props.contains("cli.2.0.0-M2.message"), is(true));
        assertThat(props.contains("cli.2.0.0-M3.message"), is(false));
        assertThat(props.contains("cli.2.0.0-M4.message"), is(true));
        assertThat(props.contains("cli.2.0.0-RC1.message"), is(true));
        assertThat(logEntries().isEmpty(), is(false));

        ArchetypeCatalog catalog = meta.catalog(version);
        assertThat(catalog, is(not(nullValue())));
        assertThat(catalog.entries().size(), is(2));
        ArchetypeCatalog.ArchetypeEntry entry0 = catalog.entries().get(0);
        assertThat(entry0.name(), is("bare"));
        ArchetypeCatalog.ArchetypeEntry entry1 = catalog.entries().get(1);
        assertThat(entry0.name(), is("bare"));

        Path archetypeJar = meta.archetype(entry0);
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString().endsWith(".jar"), is(true));

        assertLogged("Looking up latest Helidon version");
        assertNotLogged("Updating metadata for Helidon version 2.0.0-RC1");

        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("downloading", "2.0.0-RC1/cli-data.zip"), is(1));
        assertThat(countLogged("unzipping", "2.0.0-RC1/cli-data.zip"), is(1));
        assertThat(countLogged("deleting", "2.0.0-RC1/cli-data.zip"), is(1));
        assertThat(countLogged("updated", "2.0.0-RC1/.lastUpdate"), is(1));

        // Check that another call does not do any work

        output.clear();
        assertThat(meta.latestVersion(), is(version));
        assertThat(meta.properties(version), is(props));
        assertThat(meta.catalog(version), is(catalog));

        assertThat(logMessages().size(), is(3));
        assertThat(countLogged("latest", "stale: false"), is(1));
        assertThat(countLogged("2.0.0-RC1/.lastUpdate", "stale: false"), is(2));
    }
}
