/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.common.ConfigProperties;
import io.helidon.build.common.Maps;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.Metadata.DEFAULT_UPDATE_FREQUENCY;
import static io.helidon.build.cli.impl.Metadata.DEFAULT_UPDATE_FREQUENCY_UNITS;
import static io.helidon.build.cli.impl.TestMetadata.CLI_DATA_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_MP;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_SE;
import static io.helidon.build.cli.impl.TestMetadata.LAST_UPDATE_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.MAVEN_VERSION_RC1;
import static io.helidon.build.cli.impl.TestMetadata.MAVEN_VERSION_RC2;
import static io.helidon.build.cli.impl.TestMetadata.RC1_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.RC2_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC1;
import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC2;
import static io.helidon.build.cli.impl.TestMetadata.VERSIONS_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC1;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC2;
import static io.helidon.build.cli.impl.TestMetadata.readCliDataFile;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link Metadata}.
 */
@SuppressWarnings("unchecked")
class MetadataTest {

    static final Path CWD = TestFiles.targetDir(MetadataTest.class).resolve("metadata-ut");
    static final String RC1_LAST_UPDATE = VERSION_RC1 + File.separator + LAST_UPDATE_FILE_NAME;
    static final String RC1_ZIP = VERSION_RC1 + File.separator + CLI_DATA_FILE_NAME;
    static final String RC1_ZIP_URI = VERSION_RC1 + "/" + CLI_DATA_FILE_NAME;
    static final String RC2_LAST_UPDATE = VERSION_RC2 + File.separator + LAST_UPDATE_FILE_NAME;
    static final String RC2_ZIP = VERSION_RC2 + File.separator + CLI_DATA_FILE_NAME;
    static final String RC2_ZIP_URI = VERSION_RC2 + "/" + CLI_DATA_FILE_NAME;

    @BeforeEach
    void beforeEach() {
        Plugins.reset(false);
    }

    @Test
    void smokeTest() throws Metadata.UpdateFailed {
        // Setup with RC1 as default
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(DEFAULT_UPDATE_FREQUENCY, DEFAULT_UPDATE_FREQUENCY_UNITS);

            MavenVersion defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC1)));

            List<String> logEntries = ctx.recorder.entries();

            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC1))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC1_ZIP)),
                    allOf(containsString("deleting"), containsString(RC1_ZIP)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Check properties. Should not perform update.
            ConfigProperties props = meta.propertiesOf(defaultVersion);
            assertThat(props, is(not(nullValue())));
            assertThat(props.keySet().isEmpty(), is(false));
            assertThat(props.property("build-tools.version"), is(VERSION_RC1));
            assertThat(props.property("cli.version"), is(VERSION_RC1));
            assertThat(props.contains("cli.2.0.0-M2.message"), is(true));
            assertThat(props.contains("cli.2.0.0-M3.message"), is(false));
            assertThat(props.contains("cli.2.0.0-M4.message"), is(true));
            assertThat(props.contains("cli.2.0.0-RC1.message"), is(true));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItem(allOf(
                    containsString("stale check"), containsString("is false"), containsString(RC1_LAST_UPDATE))));

            // Check catalog. Should not perform update.
            ArchetypeCatalog catalog = meta.catalogOf(defaultVersion);
            assertThat(catalog, is(not(nullValue())));
            assertThat(catalog.entries().size(), is(2));

            Map<String, ArchetypeEntry> entriesById = Maps.from(catalog.entries(), ArchetypeEntry::artifactId);
            assertThat(entriesById.size(), is(2));
            assertThat(entriesById.get(HELIDON_BARE_SE), is(notNullValue()));
            assertThat(entriesById.get(HELIDON_BARE_SE).name(), is("bare"));
            assertThat(entriesById.get(HELIDON_BARE_MP), is(notNullValue()));
            assertThat(entriesById.get(HELIDON_BARE_MP).name(), is("bare"));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItem(allOf(
                    containsString("stale check"), containsString("is false"), containsString(RC1_LAST_UPDATE))));

            // Check archetype. Should not perform update.
            Path archetypeJar = meta.archetypeV1Of(entriesById.get("helidon-bare-se"));
            assertThat(archetypeJar, is(not(nullValue())));
            assertThat(Files.exists(archetypeJar), is(true));
            assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC1.jar"));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItem(allOf(
                    containsString("stale check"), containsString("is false"), containsString(RC1_LAST_UPDATE))));

            // Check that more calls do not update
            MavenVersion defaultVersion2 = meta.defaultVersion();
            assertThat(defaultVersion2, is(defaultVersion));

            ConfigProperties props2 = meta.propertiesOf(defaultVersion);
            assertThat(props2, is(props));

            ArchetypeCatalog catalog2 = meta.catalogOf(defaultVersion);
            assertThat(catalog2, is(catalog));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(3));
            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("is false"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("stale check"), containsString("is false"), containsString(RC1_LAST_UPDATE)),
                    allOf(containsString("stale check"), containsString("is false"), containsString(RC1_LAST_UPDATE))));

            // Check the CLI plugin and CLI versions; the plugin should be the current build
            // version because the helidon version is pre CLI plugin
            MavenVersion expectedCliPluginVersion = toMavenVersion(Config.buildVersion());

            MavenVersion cliVersion = meta.cliVersionOf(defaultVersion, false);
            assertThat(cliVersion, is(MAVEN_VERSION_RC1));

            MavenVersion cliPluginVersion = meta.cliPluginVersion(defaultVersion, false);
            assertThat(cliPluginVersion, is(expectedCliPluginVersion));
        }
    }

    @Test
    void smokeTestRc2() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC2).start()) {
            Metadata meta = ctx.metadata(24, TimeUnit.HOURS);

            // Do the initial catalog request for RC2
            meta.catalogOf(VERSION_RC2);

            List<String> logEntries = ctx.recorder.entries();

            assertThat(logEntries, not(hasItem(
                    containsString("Looking up default Helidon version"))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(RC2_LAST_UPDATE)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC2_ZIP)),
                    allOf(containsString("deleting"), containsString(RC2_ZIP)),
                    allOf(containsString("updated"), containsString(RC2_LAST_UPDATE), containsString("etag ")),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Check latest version. Should not perform update.
            meta.latestVersion();

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItem(allOf(
                    containsString("stale check"), containsString("is false"), containsString(VERSIONS_FILE_NAME))));

            // Check properties. Should not perform update.
            ConfigProperties props = meta.propertiesOf(VERSION_RC2);
            assertThat(props, is(not(nullValue())));
            assertThat(props.keySet().isEmpty(), is(false));
            assertThat(props.property("build-tools.version"), is("2.0.0-RC2"));
            assertThat(props.property("cli.version"), is("2.0.0-RC2"));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItems(allOf(
                    containsString("stale check"),
                    containsString("is false"),
                    containsString(RC2_LAST_UPDATE))));

            // Check catalog again. Should not perform update.
            ArchetypeCatalog catalog = meta.catalogOf(VERSION_RC2);
            assertThat(catalog, is(not(nullValue())));

            Map<String, ArchetypeEntry> entries = Maps.from(catalog.entries(), ArchetypeEntry::artifactId);
            assertThat(entries.size() >= 2, is(true));
            assertThat(entries.get(HELIDON_BARE_SE), is(notNullValue()));
            assertThat(entries.get(HELIDON_BARE_SE).name(), is("bare"));
            assertThat(entries.get(HELIDON_BARE_MP), is(notNullValue()));
            assertThat(entries.get(HELIDON_BARE_MP).name(), is("bare"));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItem(allOf(
                    containsString("stale check"), containsString("is false"), containsString(RC2_LAST_UPDATE))));

            // Check archetype. Should not perform update.
            Path archetypeJar = meta.archetypeV1Of(entries.get("helidon-bare-se"));
            assertThat(archetypeJar, is(not(nullValue())));
            assertThat(Files.exists(archetypeJar), is(true));
            assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC2.jar"));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(1));
            assertThat(logEntries, hasItem(allOf(
                    containsString("stale check"), containsString("is false"), containsString(RC2_LAST_UPDATE))));

            // Check that more calls do not update
            ConfigProperties props2 = meta.propertiesOf(VERSION_RC2);
            assertThat(props2, is(props));

            ArchetypeCatalog catalog2 = meta.catalogOf(VERSION_RC2);
            assertThat(catalog2, is(catalog));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries.size(), is(2));
            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("is false"), containsString(RC2_LAST_UPDATE)),
                    allOf(containsString("stale check"), containsString("is false"), containsString(RC2_LAST_UPDATE))));
        }
    }

    @Test
    void testCheckForCliUpdate() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC2).start()) {
            Metadata meta = ctx.metadata(DEFAULT_UPDATE_FREQUENCY, DEFAULT_UPDATE_FREQUENCY_UNITS);

            // Simulate different cli versions
            MavenVersion cliVersionRc1 = MAVEN_VERSION_RC1;
            MavenVersion cliVersionRc2 = MAVEN_VERSION_RC2;
            MavenVersion cliVersionRc2Updated = toMavenVersion("2.0.0");

            assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(false));
            assertThat(meta.checkForCliUpdate(cliVersionRc1, false).isPresent(), is(true));
            assertThat(meta.checkForCliUpdate(cliVersionRc1, false).orElseThrow(), is(cliVersionRc2));

            // Now change the metadata for RC2 such that the cli version returned is newer
            byte[] data = readCliDataFile(VERSION_RC2 + "-updated" + File.separator + CLI_DATA_FILE_NAME);
            ctx.server.setupCliData(RC2, data);

            // Make sure it doesn't update now, since the update period has not expired
            assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(false));

            // Force expiry and validate that we get expected version update
            meta = ctx.metadata(0, TimeUnit.SECONDS);

            assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(true));
            assertThat(meta.checkForCliUpdate(cliVersionRc1, false).orElseThrow(), is(cliVersionRc2Updated));
        }
    }

    @Test
    void testCliPluginVersion() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC2).start()) {
            Metadata meta = ctx.metadata(0, TimeUnit.SECONDS);

            // Helidon Version   metadata.properties
            // ---------------   -------------------
            // 2.0.1             cli.latest.plugin.version NOT PRESENT
            // 2.0.2             cli.latest.plugin.version=2.0.3
            // 2.0.3             cli.latest.plugin.version=2.2.0, cli.2.1.0.plugin.version=2.0.9, cli.2.0.3.plugin.version=2.0.3

            MavenVersion helidon201 = toMavenVersion("2.0.1");
            MavenVersion helidon202 = toMavenVersion("2.0.2");
            MavenVersion helidon203 = toMavenVersion("2.0.3");

            MavenVersion cli203 = toMavenVersion("2.0.3");
            MavenVersion cli204 = toMavenVersion("2.0.4");
            MavenVersion cli205 = toMavenVersion("2.0.5");
            MavenVersion cli210 = toMavenVersion("2.1.0");
            MavenVersion cli211 = toMavenVersion("2.1.1");
            MavenVersion cli212 = toMavenVersion("2.1.2");
            MavenVersion cli220 = toMavenVersion("2.2.0");
            MavenVersion cli221 = toMavenVersion("2.2.1");

            MavenVersion plugin203 = toMavenVersion("2.0.3");
            MavenVersion plugin209 = toMavenVersion("2.0.9");
            MavenVersion plugin220 = toMavenVersion("2.2.0");

            // Ensure that CLI plugin version for Helidon 2.0.1 is cliVersion with all CLI versions
            assertThat(meta.cliPluginVersion(helidon201, cli203, false), is(cli203));
            assertThat(meta.cliPluginVersion(helidon201, cli204, false), is(cli204));
            assertThat(meta.cliPluginVersion(helidon201, cli205, false), is(cli205));
            assertThat(meta.cliPluginVersion(helidon201, cli210, false), is(cli210));
            assertThat(meta.cliPluginVersion(helidon201, cli211, false), is(cli211));
            assertThat(meta.cliPluginVersion(helidon201, cli212, false), is(cli212));
            assertThat(meta.cliPluginVersion(helidon201, cli220, false), is(cli220));
            assertThat(meta.cliPluginVersion(helidon201, cli221, false), is(cli221));

            // Ensure that CLI plugin version for Helidon 2.0.2 is 2.0.3 with all CLI versions
            assertThat(meta.cliPluginVersion(helidon202, cli203, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli204, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli205, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli210, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli211, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli212, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli220, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon202, cli221, false), is(plugin203));

            // Ensure that CLI plugin version for Helidon 2.0.3 is as expected with all CLI versions
            assertThat(meta.cliPluginVersion(helidon203, cli203, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon203, cli204, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon203, cli205, false), is(plugin203));
            assertThat(meta.cliPluginVersion(helidon203, cli210, false), is(plugin209));
            assertThat(meta.cliPluginVersion(helidon203, cli211, false), is(plugin209));
            assertThat(meta.cliPluginVersion(helidon203, cli212, false), is(plugin209));
            assertThat(meta.cliPluginVersion(helidon203, cli220, false), is(plugin220));
            assertThat(meta.cliPluginVersion(helidon203, cli221, false), is(plugin220));
        }
    }

    @Test
    void testReleaseNotes() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(0, TimeUnit.SECONDS);

            MavenVersion helidonVersion = MAVEN_VERSION_RC2;

            // Check from M1
            Map<MavenVersion, String> notes = meta.cliReleaseNotesOf(helidonVersion, toMavenVersion("2.0.0-M1"));
            assertThat(notes, is(not(nullValue())));

            List<MavenVersion> keys = new ArrayList<>(notes.keySet());
            assertThat(keys.size(), is(4));
            assertThat(keys.get(0), is(toMavenVersion("2.0.0-RC2")));
            assertThat(keys.get(1), is(toMavenVersion("2.0.0-RC1")));
            assertThat(keys.get(2), is(toMavenVersion("2.0.0-M4")));
            assertThat(keys.get(3), is(toMavenVersion("2.0.0-M2")));
            assertThat(notes.get(keys.get(0)), containsString("DB archetype"));
            assertThat(notes.get(keys.get(1)), containsString("Performance"));
            assertThat(notes.get(keys.get(2)), containsString("archetype support"));
            assertThat(notes.get(keys.get(3)), containsString("dev command"));

            // Check from the default version (RC1)
            notes = meta.cliReleaseNotesOf(helidonVersion, meta.defaultVersion());
            assertThat(notes, is(not(nullValue())));
            keys = new ArrayList<>(notes.keySet());
            assertThat(keys.size(), is(1));
            assertThat(keys.get(0), is(toMavenVersion("2.0.0-RC2")));
            assertThat(notes.get(keys.get(0)), containsString("DB archetype"));

            // Simulate check from current CLI version
            notes = meta.cliReleaseNotesOf(helidonVersion, helidonVersion);
            assertThat(notes, is(not(nullValue())));
            assertThat(notes.isEmpty(), is(true));
        }
    }

    @Test
    void testDefaultVersionUpdatesAfterDelay() throws Metadata.UpdateFailed, InterruptedException {
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(1, TimeUnit.SECONDS);

            MavenVersion defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC1)));

            List<String> logEntries = ctx.recorder.entries();

            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC1))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC1_ZIP)),
                    allOf(containsString("deleting"), containsString(RC1_ZIP)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Wait 1.25 seconds and check version. Should perform update.
            Log.info("sleeping 1.25 seconds before recheck");
            Thread.sleep(1250);
            assertThat(meta.defaultVersion(), is(defaultVersion));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("is true"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));
        }
    }

    @Test
    void testPropertiesUpdatesAfterDelay() throws Metadata.UpdateFailed, InterruptedException {
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(1, TimeUnit.SECONDS);

            MavenVersion defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC1)));

            List<String> logEntries = ctx.recorder.entries();

            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC1))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC1_ZIP)),
                    allOf(containsString("deleting"), containsString(RC1_ZIP)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Wait 1.25 seconds and check properties. Should perform update.
            Log.info("sleeping 1.25 seconds before recheck");
            Thread.sleep(1250);

            ConfigProperties props = meta.propertiesOf(defaultVersion);
            assertThat(props, is(not(nullValue())));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Looking up default Helidon version"))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("is true"), containsString(RC1_LAST_UPDATE)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));
        }
    }

    @Test
    void testCatalogUpdatesAfterDelay() throws Metadata.UpdateFailed, InterruptedException {
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(1, TimeUnit.SECONDS);

            MavenVersion defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC1)));

            List<String> logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC1))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC1_ZIP)),
                    allOf(containsString("deleting"), containsString(RC1_ZIP)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Wait 1.25 seconds and check catalog. Should perform update.
            Log.info("sleeping 1.25 seconds before recheck");
            Thread.sleep(1250);

            ArchetypeCatalog catalog = meta.catalogOf(defaultVersion);
            assertThat(catalog, is(not(nullValue())));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Looking up default Helidon version"))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("is true"), containsString(RC1_LAST_UPDATE)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    containsString("Updating metadata for Helidon version " + VERSION_RC1),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));
        }
    }

    @Test
    void testZipIsNotDownloadedWhenEtagMatches() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(0, TimeUnit.NANOSECONDS);

            MavenVersion defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC1)));

            List<String> logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC1))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC1_ZIP)),
                    allOf(containsString("deleting"), containsString(RC1_ZIP)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Now get the properties again and make sure we skip the zip download but still
            // updated the version
            ConfigProperties props = meta.propertiesOf(defaultVersion);
            assertThat(props, is(not(nullValue())));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, hasItems(
                    allOf(containsString("not modified"), containsString(RC1 + "/" + CLI_DATA_FILE_NAME)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));
        }
    }

    @Test
    void testUpdateWhenDefaultChanges() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC2).start()) {
            Metadata meta = ctx.metadata(0, TimeUnit.NANOSECONDS);

            MavenVersion defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC2)));

            List<String> logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC2))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC2_ZIP)),
                    allOf(containsString("deleting"), containsString(RC2_ZIP)),
                    allOf(containsString("updated"), containsString(RC2_LAST_UPDATE), containsString("etag ")),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Now get the properties again and make sure we skip the zip download but still updated the default version.
            // Note that the update is forced here because we used a zero frequency.
            ConfigProperties props = meta.propertiesOf(defaultVersion);
            assertThat(props, is(not(nullValue())));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, hasItems(
                    allOf(containsString("not modified"), containsString(RC2 + "/" + CLI_DATA_FILE_NAME)),
                    allOf(containsString("updated"), containsString(RC2_LAST_UPDATE), containsString("etag " + RC2_ETAG)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Now change versions.xml and validate the result
            Plugins.reset(true);
            ctx.server.defaultVersion(TestVersion.RC1);
            ctx.server.setupVersions();

            meta = ctx.metadata(0, TimeUnit.NANOSECONDS);

            defaultVersion = meta.defaultVersion();
            assertThat(defaultVersion, is(toMavenVersion(VERSION_RC1)));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Updating metadata for Helidon version " + VERSION_RC1))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(zero delay)"), containsString(VERSIONS_FILE_NAME)),
                    containsString("Looking up default Helidon version"),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC1_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC1_ZIP)),
                    allOf(containsString("deleting"), containsString(RC1_ZIP)),
                    allOf(containsString("updated"), containsString(RC1_LAST_UPDATE), containsString("etag " + RC1_ETAG)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));
        }
    }

    @Test
    void testCatalogUpdatesWhenUnseenVersionRequested() throws Metadata.UpdateFailed {
        try (TestContext ctx = new TestContext(RC1).start()) {
            Metadata meta = ctx.metadata(DEFAULT_UPDATE_FREQUENCY, DEFAULT_UPDATE_FREQUENCY_UNITS);

            ArchetypeCatalog catalog = meta.catalogOf(VERSION_RC2);
            assertThat(catalog, is(not(nullValue())));

            List<String> logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItem(
                    containsString("Looking up default Helidon version"))));

            assertThat(logEntries, hasItems(
                    allOf(containsString("stale check"), containsString("(not found)"), containsString(RC2_LAST_UPDATE)),
                    containsString("Updating metadata for Helidon version " + VERSION_RC2),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connecting"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("downloading"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("connecting"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("connected"), containsString(RC2_ZIP_URI)),
                    allOf(containsString("unzipping"), containsString(RC2_ZIP)),
                    allOf(containsString("deleting"), containsString(RC2_ZIP)),
                    allOf(containsString("updated"), containsString(RC2_LAST_UPDATE), containsString("etag ")),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME))));

            // Now request the catalog again and make sure we do no updates
            catalog = meta.catalogOf(VERSION_RC2);
            assertThat(catalog, is(not(nullValue())));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItems(
                    containsString("Looking up default Helidon version"),
                    containsString("Updating metadata for Helidon version " + VERSION_RC2))));

            assertThat(logEntries, hasItem(
                    allOf(containsString("stale check"), containsString("is false"), containsString(RC2_LAST_UPDATE))));

            ConfigProperties props = meta.propertiesOf(VERSION_RC2);
            assertThat(props, is(not(nullValue())));

            logEntries = ctx.recorder.entries();
            assertThat(logEntries, not(hasItems(
                    allOf(containsString("not modified"), containsString(RC2 + "/" + CLI_DATA_FILE_NAME)),
                    allOf(containsString("updated"), containsString(RC2_LAST_UPDATE), containsString("etag " + RC2_ETAG)),
                    allOf(containsString("downloading"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("connected"), containsString(VERSIONS_FILE_NAME)),
                    allOf(containsString("wrote"), containsString(VERSIONS_FILE_NAME)))));
        }
    }

    static final class TestContext implements AutoCloseable {

        static final ThreadLocal<Deque<UserConfig>> THREAD_LOCAL = ThreadLocal.withInitial(ArrayDeque::new);

        final LogRecorder recorder;
        final MetadataServer server;
        final UserConfig userConfig;

        TestContext(TestVersion version) {
            THREAD_LOCAL.get().push(Config.userConfig());
            recorder = new LogRecorder(LogLevel.DEBUG);
            userConfig = UserConfig.create(ensureDirectory(unique(CWD, "config")));
            server = new MetadataServer(version, false);
            Config.setUserHome(userConfig.homeDir());
            Config.setUserConfig(userConfig);
        }

        TestContext start() {
            recorder.start();
            server.start();
            return this;
        }

        Metadata metadata(long frequency, TimeUnit frequencyUnit) {
            return Metadata.builder()
                    .rootDir(userConfig.cacheDir())
                    .url(server.url())
                    .updateFrequency(frequency)
                    .updateFrequencyUnits(frequencyUnit)
                    .debugPlugin(true)
                    .pluginStdOut(PrintStreams.accept(STDOUT, recorder::addEntry))
                    .build();
        }

        @Override
        public void close() {
            server.stop();
            recorder.close();
            UserConfig config = THREAD_LOCAL.get().pop();
            Config.setUserConfig(config);
            Config.setUserHome(config.homeDir());
        }
    }
}
