/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.common.ConfigProperties;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.logging.LogWriter;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.impl.Metadata.DEFAULT_UPDATE_FREQUENCY;
import static io.helidon.build.cli.impl.TestMetadata.CLI_DATA_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_MP;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_SE;
import static io.helidon.build.cli.impl.TestMetadata.LAST_UPDATE_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.MAVEN_VERSION_RC1;
import static io.helidon.build.cli.impl.TestMetadata.MAVEN_VERSION_RC2;
import static io.helidon.build.cli.impl.TestMetadata.RC1_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.RC2_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.TEST_CLI_DATA_URL;
import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC1;
import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC2;
import static io.helidon.build.cli.impl.TestMetadata.VERSIONS_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC1;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC2;
import static io.helidon.build.common.FileUtils.*;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.Unchecked.unchecked;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link Metadata}.
 */
public class MetadataTest {

    private static final String RC1_LAST_UPDATE = VERSION_RC1 + File.separator + LAST_UPDATE_FILE_NAME;
    private static final String RC2_LAST_UPDATE = VERSION_RC2 + File.separator + LAST_UPDATE_FILE_NAME;
    private static final LogRecorder LOG_RECORDER = LogRecorder.create();

    private String baseUrl;
    private Path cacheDir;
    private Path versionsFile;
    private Metadata meta;
    private MavenVersion defaultVersion;
    private MetadataTestServer testServer;

    @BeforeAll
    public static void beforeAll() {
        LogWriter.addRecorder(LOG_RECORDER);
    }

    @AfterAll
    public static void afterAll() {
        LogWriter.removeRecorder(LOG_RECORDER);
    }

    @BeforeEach
    public void beforeEach(TestInfo info) {
        String testClassName = info.getTestClass().orElseThrow().getSimpleName();
        String testName = info.getTestMethod().orElseThrow().getName();
        Log.info("%n--- %s $(bold %s) -------------------------------------------%n", testClassName, testName);
        Path userHome = ensureDirectory(unique(TestFiles.targetDir(MetadataTest.class), "alice"));
        Config.setUserHome(userHome);
        UserConfig userConfig = UserConfig.create(userHome);
        Config.setUserConfig(userConfig);
        Plugins.reset(false);
        baseUrl = TEST_CLI_DATA_URL.toExternalForm();
        cacheDir = userConfig.cacheDir();
        versionsFile = cacheDir.resolve(VERSIONS_FILE_NAME);
        LOG_RECORDER.clear();
        LogLevel.set(LogLevel.DEBUG);
    }

    @AfterEach
    protected void afterEach() {
        LogLevel.set(LogLevel.INFO);
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    void smokeTest() throws Exception {
        // Setup with RC1 as default
        startMetadataTestServer(RC1);
        meta = newDefaultInstance();

        assertInitialDefaultVersionRequestPerformsUpdate(DEFAULT_UPDATE_FREQUENCY, HOURS, VERSION_RC1, RC1_ETAG, false);

        // Check properties. Should not perform update.
        LOG_RECORDER.clear();
        ConfigProperties props = meta.propertiesOf(defaultVersion);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("build-tools.version"), is(VERSION_RC1));
        assertThat(props.property("cli.version"), is(VERSION_RC1));
        assertThat(props.contains("cli.2.0.0-M2.message"), is(true));
        assertThat(props.contains("cli.2.0.0-M3.message"), is(false));
        assertThat(props.contains("cli.2.0.0-M4.message"), is(true));
        assertThat(props.contains("cli.2.0.0-RC1.message"), is(true));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC1_LAST_UPDATE);

        // Check catalog. Should not perform update.
        LOG_RECORDER.clear();
        ArchetypeCatalog catalog = meta.catalogOf(defaultVersion);
        assertThat(catalog, is(not(nullValue())));
        assertThat(catalog.entries().size(), is(2));
        Map<String, ArchetypeEntry> entriesById = catalog.entries()
                .stream()
                .collect(Collectors.toMap(ArchetypeEntry::artifactId, entry -> entry));
        assertThat(entriesById.size(), is(2));
        assertThat(entriesById.get(HELIDON_BARE_SE), is(notNullValue()));
        assertThat(entriesById.get(HELIDON_BARE_SE).name(), is("bare"));
        assertThat(entriesById.get(HELIDON_BARE_MP), is(notNullValue()));
        assertThat(entriesById.get(HELIDON_BARE_MP).name(), is("bare"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC1_LAST_UPDATE);

        // Check archetype. Should not perform update.
        LOG_RECORDER.clear();
        Path archetypeJar = meta.archetypeV1Of(entriesById.get("helidon-bare-se"));
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC1.jar"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC1_LAST_UPDATE);

        // Check that more calls do not update
        LOG_RECORDER.clear();
        assertThat(meta.defaultVersion(), is(defaultVersion));
        assertThat(meta.propertiesOf(defaultVersion), is(props));
        assertThat(meta.catalogOf(defaultVersion), is(catalog));

        assertThat(LOG_RECORDER.size(), is(3));
        assertLinesContainingAll(1, "stale check", "is false", VERSIONS_FILE_NAME);
        assertLinesContainingAll(2, "stale check", "is false", RC1_LAST_UPDATE);

        // Check the CLI plugin and CLI versions; the plugin should be the current build
        // version because the helidon version is pre CLI plugin
        LOG_RECORDER.clear();
        MavenVersion expectedCliPluginVersion = toMavenVersion(Config.buildVersion());
        assertThat(meta.cliVersionOf(defaultVersion, false), is(MAVEN_VERSION_RC1));
        assertThat(meta.cliPluginVersion(defaultVersion, false), is(expectedCliPluginVersion));
    }

    @Test
    void smokeTestRc2() throws Exception {
        // Setup with RC2 as default
        startMetadataTestServer(RC2);
        meta = newDefaultInstance();

        // Do the initial catalog request for RC2
        Runnable request = unchecked(() -> catalogRequest(VERSION_RC2, true));
        assertInitialRequestPerformsUpdate(request, 24, HOURS, VERSION_RC2, "", false);

        // Check latest version. Should not perform update.
        LOG_RECORDER.clear();
        defaultVersion = meta.latestVersion();
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", VERSIONS_FILE_NAME);

        // Check properties. Should not perform update.
        LOG_RECORDER.clear();
        ConfigProperties props = meta.propertiesOf(VERSION_RC2);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("build-tools.version"), is("2.0.0-RC2"));
        assertThat(props.property("cli.version"), is("2.0.0-RC2"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC2_LAST_UPDATE);

        // Check catalog again. Should not perform update.
        LOG_RECORDER.clear();
        ArchetypeCatalog catalog = meta.catalogOf(VERSION_RC2);
        assertThat(catalog, is(not(nullValue())));
        assertThat(catalog.entries().size() >= 2, is(true));
        Map<String, ArchetypeEntry> entriesById = catalog.entries()
                .stream()
                .collect(Collectors.toMap(ArchetypeEntry::artifactId, entry -> entry));
        assertThat(entriesById.get(HELIDON_BARE_SE), is(notNullValue()));
        assertThat(entriesById.get(HELIDON_BARE_SE).name(), is("bare"));
        assertThat(entriesById.get(HELIDON_BARE_MP), is(notNullValue()));
        assertThat(entriesById.get(HELIDON_BARE_MP).name(), is("bare"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC2_LAST_UPDATE);

        // Check archetype. Should not perform update.
        LOG_RECORDER.clear();
        Path archetypeJar = meta.archetypeV1Of(entriesById.get("helidon-bare-se"));
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC2.jar"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC2_LAST_UPDATE);

        // Check that more calls do not update
        LOG_RECORDER.clear();
        assertThat(meta.propertiesOf(VERSION_RC2), is(props));
        assertThat(meta.catalogOf(VERSION_RC2), is(catalog));

        assertThat(LOG_RECORDER.size(), is(2));
        assertLinesContainingAll(2, "stale check", "is false", RC2_LAST_UPDATE);
    }

    @Test
    void testCheckForCliUpdate() throws Exception {
        // Setup with RC2 as default
        startMetadataTestServer(RC2);
        meta = newDefaultInstance();

        // Simulate different cli versions
        MavenVersion cliVersionRc1 = MAVEN_VERSION_RC1;
        MavenVersion cliVersionRc2 = MAVEN_VERSION_RC2;
        MavenVersion cliVersionRc2Updated = toMavenVersion("2.0.0");

        assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(false));

        assertThat(meta.checkForCliUpdate(cliVersionRc1, false).isPresent(), is(true));
        assertThat(meta.checkForCliUpdate(cliVersionRc1, false).orElseThrow(), is(cliVersionRc2));

        // Now change the metadata for RC2 such that the cli version returned is newer
        String updatedRc2FileName = VERSION_RC2 + "-updated" + File.separator + CLI_DATA_FILE_NAME;
        byte[] updatedRc2 = TestMetadata.readCliDataFile(updatedRc2FileName);
        testServer.setupCliData(RC2, updatedRc2);

        // Make sure it doesn't update now, since the update period has not expired
        assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(false));

        // Force expiry and validate that we get expected version update
        meta = newInstance(0, HOURS);
        assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(true));
        assertThat(meta.checkForCliUpdate(cliVersionRc1, false).orElseThrow(), is(cliVersionRc2Updated));
    }

    @Test
    void testCliPluginVersion() throws Exception {
        // Setup with RC2 as default
        startMetadataTestServer(RC2);
        meta = newDefaultInstance();

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

    @Test
    void testReleaseNotes() throws Exception {
        // Setup with RC1 as default
        startMetadataTestServer(RC1);
        meta = newDefaultInstance();

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

    @Test
    void testDefaultVersionUpdatesAfterDelay() throws Exception {
        // Setup with RC1 as default
        startMetadataTestServer(RC1);
        meta = newDefaultInstance();

        assertInitialDefaultVersionRequestPerformsUpdate(1, SECONDS, VERSION_RC1, RC1_ETAG, false);

        // Wait 1.25 seconds and check version. Should perform update.
        Log.info("sleeping 1.25 seconds before recheck");
        LOG_RECORDER.clear();
        Thread.sleep(1250);
        assertThat(meta.defaultVersion(), is(defaultVersion));

        assertLinesContainingAll(1, "stale check", "is true", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + RC1_ETAG);
        assertLinesContainingAll("Looking up default Helidon version");
        assertNoLinesContainingAll("Updating metadata for Helidon version " + VERSION_RC1);
        assertLinesContainingAll(1, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "wrote", VERSIONS_FILE_NAME);
    }

    @Test
    void testPropertiesUpdatesAfterDelay() throws Exception {
        // Setup with RC1 as default
        startMetadataTestServer(RC1);
        meta = newDefaultInstance();

        assertInitialDefaultVersionRequestPerformsUpdate(1, SECONDS, VERSION_RC1, RC1_ETAG, false);

        // Wait 1.25 seconds and check properties. Should perform update.
        Log.info("sleeping 1.25 seconds before recheck");
        LOG_RECORDER.clear();
        Thread.sleep(1250);
        assertThat(meta.propertiesOf(defaultVersion), is(not(nullValue())));

        assertLinesContainingAll(1, "stale check", "is true", RC1_LAST_UPDATE);
        assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + RC1_ETAG);
        assertNoLinesContainingAll("Looking up default Helidon version");
        assertLinesContainingAll("Updating metadata for Helidon version " + VERSION_RC1);
        assertLinesContainingAll(1, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "wrote", VERSIONS_FILE_NAME);
    }

    @Test
    void testCatalogUpdatesAfterDelay() throws Exception {
        // Setup with RC1 as default
        startMetadataTestServer(RC1);
        meta = newDefaultInstance();

        assertInitialDefaultVersionRequestPerformsUpdate(1, SECONDS, VERSION_RC1, RC1_ETAG, false);

        // Wait 1.25 seconds and check catalog. Should perform update.
        Log.info("sleeping 1.25 seconds before recheck");
        LOG_RECORDER.clear();
        Thread.sleep(1250);
        assertThat(meta.catalogOf(defaultVersion), is(not(nullValue())));

        assertLinesContainingAll(1, "stale check", "is true", RC1_LAST_UPDATE);
        assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + RC1_ETAG);
        assertNoLinesContainingAll("Looking up default Helidon version");
        assertLinesContainingAll("Updating metadata for Helidon version " + VERSION_RC1);
        assertLinesContainingAll(1, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "wrote", VERSIONS_FILE_NAME);
    }

    @Test
    void testZipIsNotDownloadedWhenEtagMatches() throws Exception {
        // Setup test server with the default set to RC1
        startMetadataTestServer(TestVersion.RC1);
        meta = newDefaultInstance();

        // Make the initial default version call and validate the result
        assertInitialDefaultVersionRequestPerformsUpdate(0, NANOSECONDS, VERSION_RC1, RC1_ETAG, false);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the version
        LOG_RECORDER.clear();
        assertThat(meta.propertiesOf(defaultVersion), is(not(nullValue())));
        assertLinesContainingAll(1, "not modified", RC1 + "/" + CLI_DATA_FILE_NAME);
        assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + RC1_ETAG);
        assertLinesContainingAll(1, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "wrote", VERSIONS_FILE_NAME);
    }

    @Test
    void testUpdateWhenDefaultChanges() throws Exception {
        // Setup test server with the default set to RC2
        startMetadataTestServer(RC2);
        meta = newDefaultInstance();

        // Make the initial defaultVersion call and validate the result
        assertInitialDefaultVersionRequestPerformsUpdate(0, NANOSECONDS, VERSION_RC2, RC2_ETAG, false);

        // Now get the properties again and make sure we skip the zip download but still updated the default version.
        // Note that the update is forced here because we used a zero frequency.
        LOG_RECORDER.clear();
        assertThat(meta.propertiesOf(defaultVersion), is(not(nullValue())));
        assertLinesContainingAll(1, "not modified", RC2 + "/" + CLI_DATA_FILE_NAME);
        assertLinesContainingAll(1, "updated", RC2_LAST_UPDATE, "etag " + RC2_ETAG);
        assertLinesContainingAll(1, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "wrote", VERSIONS_FILE_NAME);

        // Now change versions.xml and validate the result
        LOG_RECORDER.clear();
        Plugins.reset(true);
        testServer.defaultVersion(TestVersion.RC1);
        testServer.setupVersions();
        assertInitialDefaultVersionRequestPerformsUpdate(0, NANOSECONDS, VERSION_RC1, RC1_ETAG, true);
    }

    @Test
    void testCatalogUpdatesWhenUnseenVersionRequested() throws Exception {
        startMetadataTestServer(RC2);
        meta = newDefaultInstance();

        // Make the initial catalog request and validate the result
        Runnable request = unchecked(() -> catalogRequest(VERSION_RC2, true));
        assertInitialRequestPerformsUpdate(request, DEFAULT_UPDATE_FREQUENCY, HOURS, VERSION_RC2, RC2_ETAG, false);

        // Now request the catalog again and make sure we do no updates
        LOG_RECORDER.clear();
        catalogRequest(VERSION_RC2, false);
        assertThat(meta.propertiesOf(VERSION_RC2), is(not(nullValue())));
        assertLinesContainingAll(0, "not modified", RC2 + "/" + CLI_DATA_FILE_NAME);
        assertLinesContainingAll(0, "updated", RC2_LAST_UPDATE, "etag " + RC2_ETAG);
        assertLinesContainingAll(0, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(0, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(0, "wrote", VERSIONS_FILE_NAME);
    }

    /**
     * Asserts that the very first metadata request does a metadata update and make assertions on the logs.
     *
     * @param updateFrequency      update frequency of the metadata created
     * @param updateFrequencyUnits update frequency unit of the metadata created
     * @param expectedVersion      expected Helidon version
     * @param expectedEtag         expected ETAG
     * @param versionsFileExists   {@code true} if the versions.xml is expected to exist, {@code false} otherwise
     */
    void assertInitialDefaultVersionRequestPerformsUpdate(long updateFrequency,
                                                                    TimeUnit updateFrequencyUnits,
                                                                    String expectedVersion,
                                                                    String expectedEtag,
                                                                    boolean versionsFileExists) {

        Runnable request = unchecked(() -> firstDefaultVersionRequest(expectedVersion, !versionsFileExists));
        assertInitialRequestPerformsUpdate(request, updateFrequency, updateFrequencyUnits,
                expectedVersion, expectedEtag, versionsFileExists);
    }

    /**
     * Assert that the very first metadata request does a metadata update and make assertions on the logs.
     *
     * @param request              runnable to runs the request
     * @param updateFrequency      update frequency of the metadata created
     * @param updateFrequencyUnits update frequency unit of the metadata created
     * @param expectedVersion      expected Helidon version
     * @param expectedEtag         expected ETAG
     * @param versionsFileExists   {@code true} if the versions.xml is expected to exist, {@code false} otherwise
     */
    void assertInitialRequestPerformsUpdate(Runnable request,
                                            long updateFrequency,
                                            TimeUnit updateFrequencyUnits,
                                            String expectedVersion,
                                            String expectedEtag,
                                            boolean versionsFileExists) {

        Path versionDir = cacheDir.resolve(expectedVersion);
        Path propertiesFile = versionDir.resolve(TestMetadata.PROPERTIES_FILE_NAME);
        Path catalogFile = versionDir.resolve(TestMetadata.CATALOG_FILE_NAME);
        Path seJarFile = versionDir.resolve(TestMetadata.HELIDON_BARE_SE + "-" + expectedVersion + ".jar");
        Path mpJarFile = versionDir.resolve(TestMetadata.HELIDON_BARE_MP + "-" + expectedVersion + ".jar");
        String zipPath = expectedVersion + File.separator + TestMetadata.CLI_DATA_FILE_NAME;
        String zipUriPath = expectedVersion + "/" + TestMetadata.CLI_DATA_FILE_NAME;
        String lastUpdatePath = expectedVersion + File.separator + LAST_UPDATE_FILE_NAME;

        // Check expected the versions file and version directory existence

        assertThat(Files.exists(versionsFile), is(versionsFileExists));
        assertThat(Files.exists(versionDir), is(false));

        // Make request. Should update both versions file and default archetype.

        LOG_RECORDER.clear();
        meta = newInstance(updateFrequency, updateFrequencyUnits);
        request.run();

        requireFile(versionsFile);
        requireDirectory(versionDir);
        requireFile(propertiesFile);
        requireFile(catalogFile);
        requireFile(seJarFile);
        requireFile(mpJarFile);

        assertLinesContainingAll(1, "downloading", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connecting", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "connected", VERSIONS_FILE_NAME);
        assertLinesContainingAll(1, "downloading", zipUriPath);
        assertLinesContainingAll(1, "connecting", zipUriPath);
        assertLinesContainingAll(1, "connected", zipUriPath);

        assertLinesContainingAll(1, "unzipping", zipPath);
        assertLinesContainingAll(1, "deleting", zipPath);
        assertLinesContainingAll(1, "updated", lastUpdatePath, "etag " + expectedEtag);
    }

    /**
     * Request the default version and perform assertions on the logs.
     *
     * @param expectedVersion expected Helidon version
     * @param expectUpdate    {@code true} if a metadata update is expected, {@code false otherwise}
     * @throws Metadata.UpdateFailed if the metadata update failed
     */
    void firstDefaultVersionRequest(String expectedVersion, boolean expectUpdate) throws Metadata.UpdateFailed {
        String staleType = expectUpdate ? "(not found)" : "(zero delay)";
        defaultVersion = meta.defaultVersion();
        assertThat(defaultVersion, is(not(nullValue())));
        if (expectedVersion != null) {
            assertThat(defaultVersion, is(toMavenVersion(expectedVersion)));
        }
        assertLinesContainingAll(1, "stale check", staleType, VERSIONS_FILE_NAME);
        assertLinesContainingAll("Looking up default Helidon version");
        assertNoLinesContainingAll("Updating metadata for Helidon version " + expectedVersion);
    }

    /**
     * Perform a catalog request and perform assertions on the logs.
     *
     * @param version      Helidon version
     * @param expectUpdate {@code true} if a metadata update is expected, {@code false otherwise}
     * @throws Metadata.UpdateFailed if the metadata update failed
     */
    @SuppressWarnings("SameParameterValue")
    void catalogRequest(String version, boolean expectUpdate) throws Metadata.UpdateFailed {
        String staleType = expectUpdate ? "(not found)" : "is false";
        String staleFilePath = version + File.separator + LAST_UPDATE_FILE_NAME;
        meta.catalogOf(version);
        assertLinesContainingAll(1, "stale check", staleType, staleFilePath);
        assertNoLinesContainingAll("Looking up default Helidon version");
        if (expectUpdate) {
            assertLinesContainingAll("Updating metadata for Helidon version " + version);
        } else {
            assertNoLinesContainingAll("Updating metadata for Helidon version " + version);
        }
    }

    private void startMetadataTestServer(TestVersion defaultVersion) {
        testServer = new MetadataTestServer(defaultVersion, false).start();
        baseUrl = testServer.url();
    }

    private Metadata newInstance(long updateFrequency, TimeUnit updateFrequencyUnits) {
        return Metadata.builder()
                .rootDir(cacheDir)
                .url(baseUrl)
                .updateFrequency(updateFrequency)
                .updateFrequencyUnits(updateFrequencyUnits)
                .debugPlugin(true)
                .pluginStdOut(PrintStreams.accept(STDOUT, LOG_RECORDER::addEntry))
                .build();
    }

    private Metadata newDefaultInstance() {
        return newInstance(24, TimeUnit.HOURS);
    }

    private void assertLinesContainingAll(String... fragments) throws AssertionError {
        if (!atLeastOneLineContainingAll(fragments)) {
            throw new AssertionError(String.format(
                    "log should contain at least one line with all of the following: %s%n%s",
                    Arrays.toString(fragments), this));
        }
    }

    private void assertNoLinesContainingAll(String... fragments) throws AssertionError {
        if (atLeastOneLineContainingAll(fragments)) {
            throw new AssertionError(String.format(
                    "log should not contain any lines with all of the following: %s%n%s",
                    Arrays.toString(fragments), this));
        }
    }

    private boolean atLeastOneLineContainingAll(String... fragments) {
        return countLinesContainingAll(fragments) > 0;
    }

    private void assertLinesContainingAll(int expectedCount, String... fragments) throws AssertionError {
        int count = countLinesContainingAll(fragments);
        if (count != expectedCount) {
            throw new AssertionError(String.format(
                    "log should contain %d lines with all of the following, found %d: %s%n%s",
                    expectedCount, count, Arrays.toString(fragments), this));
        }
    }

    private int countLinesContainingAll(String... fragments) {
        return (int) LOG_RECORDER.entries().stream().filter(msg -> containsAll(msg, fragments)).count();
    }

    private static boolean containsAll(String msg, String... fragments) {
        for (String fragment : fragments) {
            if (!msg.contains(fragment)) {
                return false;
            }
        }
        return true;
    }
}
