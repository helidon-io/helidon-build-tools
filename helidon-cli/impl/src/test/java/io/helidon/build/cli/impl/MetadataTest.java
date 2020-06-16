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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.archetype.engine.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.test.CapturingLogWriter;
import io.helidon.build.test.TestFiles;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.UserConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static java.util.Objects.requireNonNull;
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

    private String baseUrl;
    private Path cacheDir;
    private Path latestFile;
    private CapturingLogWriter logged;
    private Metadata meta;
    private MavenVersion latestVersion;
    private MetadataTestServer mockServer;

    @BeforeEach
    public void beforeEach(TestInfo info) throws IOException {
        final String testName = info.getTestMethod().orElseThrow().getName();
        Log.info("%n--- MetadataTest $(bold %s) -------------------------------------------%n", testName);

        Config.setUserHome(TestFiles.targetDir().resolve("alice"));
        final UserConfig userConfig = Config.userConfig();
        userConfig.clearCache();
        userConfig.clearPlugins();
        Plugins.clearPluginJar();
        useTestCliDataBaseUrl();
        cacheDir = userConfig.cacheDir();
        latestFile = cacheDir.resolve(TestMetadata.LATEST_FILE_NAME);
        logged = CapturingLogWriter.install();
    }

    @AfterEach
    public void afterEach() {
        logged.uninstall();
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    /**
     * Use the test cli-data directory as the base url.
     */
    protected void useTestCliDataBaseUrl() {
        useBaseUrl(TestMetadata.TEST_CLI_DATA_URL.toExternalForm());
    }

    /**
     * Use the given url as the base url.
     *
     * @param baseUrl The base url.
     */
    protected void useBaseUrl(String baseUrl) {
        this.baseUrl = requireNonNull(baseUrl);
    }

    /**
     * Starts the mock server and client and sets the base url pointing to it.
     *
     * @param latestVersion The version to return from "/latest"
     */
    protected void startMockServer(TestVersion latestVersion) {
        mockServer = new MetadataTestServer(latestVersion, false).start();
        useBaseUrl(mockServer.url());
    }

    /**
     * Returns a new {@link Metadata} instance with the given frequency.
     *
     * @param updateFrequency The update frequency.
     * @param updateFrequencyUnits The update frequency units.
     * @return The instance.
     */
    protected Metadata newInstance(long updateFrequency, TimeUnit updateFrequencyUnits) {
        return Metadata.newInstance(cacheDir, baseUrl, updateFrequency, updateFrequencyUnits, true);
    }

    private Metadata newDefaultInstance() {
        return newInstance(24, TimeUnit.HOURS);
    }

    @Test
    void smokeTest() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(24, TimeUnit.HOURS, TestMetadata.RC1, TestMetadata.NO_ETAG);

        // Check properties. Should not perform update.

        logged.clear();
        ConfigProperties props = meta.propertiesOf(latestVersion);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("helidon.version"), is(TestMetadata.RC1));
        assertThat(props.property("build-tools.version"), is(TestMetadata.RC1));
        assertThat(props.property("cli.version"), is(TestMetadata.RC1));
        assertThat(props.contains("cli.2.0.0-M2.message"), is(true));
        assertThat(props.contains("cli.2.0.0-M3.message"), is(false));
        assertThat(props.contains("cli.2.0.0-M4.message"), is(true));
        assertThat(props.contains("cli.2.0.0-RC1.message"), is(true));
        assertThat(logged.size(), is(1));
        logged.assertLinesContainingAll(1, "stale check", "is false", TestMetadata.RC1_LAST_UPDATE);

        // Check catalog. Should not perform update.

        logged.clear();
        ArchetypeCatalog catalog = meta.catalogOf(latestVersion);
        assertThat(catalog, is(not(nullValue())));
        assertThat(catalog.entries().size(), is(2));
        Map<String, ArchetypeEntry> entriesById = catalog.entries()
                                                         .stream()
                                                         .collect(Collectors.toMap(ArchetypeEntry::artifactId, entry -> entry));
        assertThat(entriesById.size(), is(2));
        assertThat(entriesById.get(TestMetadata.HELIDON_BARE_SE), is(notNullValue()));
        assertThat(entriesById.get(TestMetadata.HELIDON_BARE_SE).name(), is("bare"));
        assertThat(entriesById.get(TestMetadata.HELIDON_BARE_MP), is(notNullValue()));
        assertThat(entriesById.get(TestMetadata.HELIDON_BARE_MP).name(), is("bare"));
        assertThat(logged.size(), is(1));
        logged.assertLinesContainingAll(1, "stale check", "is false", TestMetadata.RC1_LAST_UPDATE);

        // Check archetype. Should not perform update.

        logged.clear();
        Path archetypeJar = meta.archetypeOf(entriesById.get("helidon-bare-se"));
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC1.jar"));
        assertThat(logged.size(), is(1));
        logged.assertLinesContainingAll(1, "stale check", "is false", TestMetadata.RC1_LAST_UPDATE);

        // Check that more calls do not update

        logged.clear();
        assertThat(meta.latestVersion(), is(latestVersion));
        assertThat(meta.propertiesOf(latestVersion), is(props));
        assertThat(meta.catalogOf(latestVersion), is(catalog));

        assertThat(logged.size(), is(3));
        logged.assertLinesContainingAll(1, "stale check", "is false", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(2, "stale check", "is false", TestMetadata.RC1_LAST_UPDATE);

        // Check the build tools and CLI versions

        logged.clear();
        MavenVersion expected = toMavenVersion(TestMetadata.RC1);
        assertThat(meta.buildToolsVersionOf(latestVersion), is(expected));
        assertThat(meta.cliVersionOf(latestVersion), is(expected));
    }

    @Test
    void testReleaseNotes() throws Exception {
        meta = newDefaultInstance();
        MavenVersion helidonVersion = toMavenVersion(TestMetadata.RC2);

        // Check from M1

        Map<MavenVersion, String> notes = meta.cliReleaseNotesOf(helidonVersion, toMavenVersion("2.0.0-M1"));
        assertThat(notes, is(not(nullValue())));
        List<MavenVersion> keys = new ArrayList<>(notes.keySet());
        assertThat(keys.size(), is(4));
        assertThat(keys.get(0), is(toMavenVersion("2.0.0-M2")));
        assertThat(keys.get(1), is(toMavenVersion("2.0.0-M4")));
        assertThat(keys.get(2), is(toMavenVersion("2.0.0-RC1")));
        assertThat(keys.get(3), is(toMavenVersion("2.0.0-RC2")));
        assertThat(notes.get(keys.get(0)), containsString("dev command"));
        assertThat(notes.get(keys.get(1)), containsString("archetype support"));
        assertThat(notes.get(keys.get(2)), containsString("Performance"));
        assertThat(notes.get(keys.get(3)), containsString("DB archetype"));

        // Check from latest version (RC1)

        notes = meta.cliReleaseNotesOf(helidonVersion, meta.latestVersion());
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
    void testLatestVersionUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, TimeUnit.SECONDS, TestMetadata.RC1, TestMetadata.NO_ETAG);

        // Wait 1.25 seconds and check version. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.latestVersion(), is(latestVersion));

        logged.assertLinesContainingAll(1, "stale check", "is true", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "updated", TestMetadata.RC1_LAST_UPDATE, "etag " + TestMetadata.NO_ETAG);
        logged.assertLinesContainingAll("Looking up latest Helidon version");
        logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + TestMetadata.RC1);
        logged.assertLinesContainingAll(1, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", TestMetadata.LATEST_FILE_NAME);
    }

    @Test
    void testPropertiesUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, TimeUnit.SECONDS, TestMetadata.RC1, TestMetadata.NO_ETAG);

        // Wait 1.25 seconds and check properties. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));

        logged.assertLinesContainingAll(1, "stale check", "is true", TestMetadata.RC1_LAST_UPDATE);
        logged.assertLinesContainingAll(1, "updated", TestMetadata.RC1_LAST_UPDATE, "etag " + TestMetadata.NO_ETAG);
        logged.assertNoLinesContainingAll("Looking up latest Helidon version");
        logged.assertLinesContainingAll("Updating metadata for Helidon version " + TestMetadata.RC1);
        logged.assertLinesContainingAll(1, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", TestMetadata.LATEST_FILE_NAME);
    }

    @Test
    void testCatalogUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, TimeUnit.SECONDS, TestMetadata.RC1, TestMetadata.NO_ETAG);

        // Wait 1.25 seconds and check catalog. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.catalogOf(latestVersion), is(not(nullValue())));

        logged.assertLinesContainingAll(1, "stale check", "is true", TestMetadata.RC1_LAST_UPDATE);
        logged.assertLinesContainingAll(1, "updated", TestMetadata.RC1_LAST_UPDATE, "etag " + TestMetadata.NO_ETAG);
        logged.assertNoLinesContainingAll("Looking up latest Helidon version");
        logged.assertLinesContainingAll("Updating metadata for Helidon version " + TestMetadata.RC1);
        logged.assertLinesContainingAll(1, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", TestMetadata.LATEST_FILE_NAME);
    }

    @Test
    void testZipIsNotDownloadedWhenEtagMatches() throws Exception {
        startMockServer(TestVersion.RC1);
        assertZipIsNotDownloadedFromServerWhenEtagMatches();
    }

    protected void assertZipIsNotDownloadedFromServerWhenEtagMatches() throws Exception {

        // Make the initial latestVersion call and validate the result

        assertInitialLatestVersionRequestPerformsUpdate(0, TimeUnit.NANOSECONDS, TestMetadata.RC1, TestMetadata.INITIAL_ETAG);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the latest version

        logged.clear();
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));
        logged.assertLinesContainingAll(1, "not modified", TestMetadata.RC1_CLI_DATA_ZIP);
        logged.assertLinesContainingAll(1, "updated", TestMetadata.RC1_LAST_UPDATE, "etag " + TestMetadata.INITIAL_ETAG);
        logged.assertLinesContainingAll(1, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", TestMetadata.LATEST_FILE_NAME);
    }

    @Test
        // NOTE: This test case can only work against mock server
    void testUpdateWhenLatestChanges() throws Exception {

        // Setup mock server with latest set to RC2

        startMockServer(TestVersion.RC2);

        // Make the initial latestVersion call and validate the result

        assertInitialLatestVersionRequestPerformsUpdate(0, TimeUnit.NANOSECONDS, TestMetadata.RC2, TestMetadata.INITIAL_ETAG);

        // Now get the properties again and make sure we skip the zip download but still updated the latest version.
        // Note that the update is forced here because we used a zero frequency.

        logged.clear();
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));
        logged.assertLinesContainingAll(1, "not modified", TestMetadata.RC2_CLI_DATA_ZIP);
        logged.assertLinesContainingAll(1, "updated", TestMetadata.RC2_LAST_UPDATE, "etag " + TestMetadata.INITIAL_ETAG);
        logged.assertLinesContainingAll(1, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", TestMetadata.LATEST_FILE_NAME);
    }

    @Test
    void testCatalogUpdatesWhenUnseenVersionRequested() throws Exception {
        startMockServer(TestVersion.RC2);
        assertServerUpdatesWhenUnseenRC2VersionRequested();
    }

    protected void assertServerUpdatesWhenUnseenRC2VersionRequested() throws Exception {

        // Make the initial catalog request and validate the result

        final Runnable request = () -> catalogRequest(TestMetadata.RC2, true);
        assertInitialRequestPerformsUpdate(request, 24, TimeUnit.HOURS, TestMetadata.RC2, TestMetadata.INITIAL_ETAG);

        // Now request the catalog again and make sure we do no updates

        logged.clear();
        catalogRequest(TestMetadata.RC2, false);
        assertThat(meta.propertiesOf(TestMetadata.RC2), is(not(nullValue())));
        logged.assertLinesContainingAll(0, "not modified", TestMetadata.RC2_CLI_DATA_ZIP);
        logged.assertLinesContainingAll(0, "updated", TestMetadata.RC2_LAST_UPDATE, "etag " + TestMetadata.INITIAL_ETAG);
        logged.assertLinesContainingAll(0, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(0, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(0, "wrote", TestMetadata.LATEST_FILE_NAME);
    }

    protected void assertInitialLatestVersionRequestPerformsUpdate(long updateFrequency,
                                                                   TimeUnit updateFrequencyUnits,
                                                                   String expectedVersion,
                                                                   String expectedEtag) {
        final Runnable request = () -> firstLatestVersionRequest(expectedVersion);
        assertInitialRequestPerformsUpdate(request, updateFrequency, updateFrequencyUnits, expectedVersion, expectedEtag);
    }

    protected void assertInitialRequestPerformsUpdate(Runnable request,
                                                      long updateFrequency,
                                                      TimeUnit updateFrequencyUnits,
                                                      String expectedVersion,
                                                      String expectedEtag) {
        Path versionDir = cacheDir.resolve(expectedVersion);
        Path propertiesFile = versionDir.resolve(TestMetadata.PROPERTIES_FILE_NAME);
        Path catalogFile = versionDir.resolve(TestMetadata.CATALOG_FILE_NAME);
        Path seJarFile = versionDir.resolve(TestMetadata.HELIDON_BARE_SE + "-" + expectedVersion + ".jar");
        Path mpJarFile = versionDir.resolve(TestMetadata.HELIDON_BARE_MP + "-" + expectedVersion + ".jar");
        String zipPath = expectedVersion + TestMetadata.CLI_DATA;
        String lastUpdatePath = expectedVersion + TestMetadata.LAST_UPDATE;

        // Ensure latest file and version directory not present

        assertThat(Files.exists(latestFile), is(false));
        assertThat(Files.exists(versionDir), is(false));

        // Make request. Should update both latest file and latest archetype.

        logged.clear();
        meta = newInstance(updateFrequency, updateFrequencyUnits);
        request.run();

        assertFile(latestFile);
        assertDir(versionDir);
        assertFile(propertiesFile);
        assertFile(catalogFile);
        assertFile(seJarFile);
        assertFile(mpJarFile);

        logged.assertLinesContainingAll(1, "unpacked", "cli-plugins-", ".jar");
        logged.assertLinesContainingAll(1, "executing", "cli-plugins-", "UpdateMetadata");
        logged.assertLinesContainingAll(1, "downloading", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connecting", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", TestMetadata.LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "downloading", zipPath);
        logged.assertLinesContainingAll(1, "connecting", zipPath);
        logged.assertLinesContainingAll(1, "connected", zipPath);

        logged.assertLinesContainingAll(1, "unzipping", zipPath);
        logged.assertLinesContainingAll(1, "deleting", zipPath);
        logged.assertLinesContainingAll(1, "updated", lastUpdatePath, "etag " + expectedEtag);
    }

    private void firstLatestVersionRequest(String expectedVersion) {
        try {
            latestVersion = meta.latestVersion();
            assertThat(latestVersion, is(not(nullValue())));
            assertThat(latestVersion, is(toMavenVersion(expectedVersion)));
            logged.assertLinesContainingAll("Looking up latest Helidon version");
            logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + expectedVersion);
            logged.assertLinesContainingAll(1, "stale check", "(not found)", TestMetadata.LATEST_FILE_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void catalogRequest(String version, boolean expectUpdate) {
        try {
            String staleType = expectUpdate ? "(not found)" : "is false";
            String staleFilePath = version + "/" + TestMetadata.LAST_UPDATE_FILE_NAME;
            meta.catalogOf(version);
            logged.assertNoLinesContainingAll("Looking up latest Helidon version");
            if (expectUpdate) {
                logged.assertLinesContainingAll("Updating metadata for Helidon version " + version);
            } else {
                logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + version);
            }
            logged.assertLinesContainingAll(1, "stale check", staleType, staleFilePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
