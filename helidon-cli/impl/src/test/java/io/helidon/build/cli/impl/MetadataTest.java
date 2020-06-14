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
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.archetype.engine.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.test.CapturingLogWriter;
import io.helidon.build.test.TestFiles;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.UserConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

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
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Unit test for class {@link Metadata}.
 */
public class MetadataTest {
    static final URL TEST_CLI_DATA_URL = requireNonNull(MetadataTest.class.getClassLoader().getResource("cli-data"));
    static final Path TEST_CLI_DATA_PATH = assertDir(Path.of(TEST_CLI_DATA_URL.getFile()));
    static final int MOCK_SERVER_PORT = 8087;
    static final String MOCK_SERVER_BASE_URL = "http://localhost:" + MOCK_SERVER_PORT;
    static final String LATEST_FILE_NAME = "latest";
    static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    static final String CLI_DATA_FILE_NAME = "cli-data.zip";
    static final String PROPERTIES_FILE_NAME = "metadata.properties";
    static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    static final String LAST_UPDATE = "/" + LAST_UPDATE_FILE_NAME;
    static final String CLI_DATA = "/" + CLI_DATA_FILE_NAME;
    static final String HELIDON_BARE_SE = "helidon-bare-se";
    static final String HELIDON_BARE_MP = "helidon-bare-mp";
    static final String RC1 = "2.0.0-RC1";
    static final String RC2 = "2.0.0-RC2";
    static final String RC1_LAST_UPDATE = RC1 + LAST_UPDATE;
    static final String RC2_LAST_UPDATE = RC2 + LAST_UPDATE;
    static final String RC1_CLI_DATA_ZIP = RC1 + CLI_DATA;
    static final String RC2_CLI_DATA_ZIP = RC2 + CLI_DATA;
    static final byte[] RC1_ZIP = readCliDataFile(RC1_CLI_DATA_ZIP);
    static final byte[] RC2_ZIP = readCliDataFile(RC2_CLI_DATA_ZIP);
    static final String ETAG_HEADER = "Etag";
    static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    static final String NO_ETAG = "<no-etag>";
    static final String NO_FILE_ETAG = "<no-file>";
    static final String INITIAL_ETAG = "<initial>";

    private static byte[] readCliDataFile(String file) {
        try {
            return Files.readAllBytes(TEST_CLI_DATA_PATH.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String baseUrl;
    private Path cacheDir;
    private Path latestFile;
    private CapturingLogWriter logged;
    private Metadata meta;
    private MavenVersion latestVersion;
    private ClientAndServer mockServer;

    @BeforeEach
    public void beforeEach(TestInfo info) throws IOException {
        Log.info("%n--- %s ---------------%n", info.getTestMethod().orElseThrow().getName());
        Config.setUserHome(TestFiles.targetDir().resolve("alice"));
        final UserConfig userConfig = Config.userConfig();
        userConfig.clearCache();
        userConfig.clearPlugins();
        Plugins.clearPluginJar();
        useTestCliDataBaseUrl();
        cacheDir = userConfig.cacheDir();
        latestFile = cacheDir.resolve(LATEST_FILE_NAME);
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
        useBaseUrl(TEST_CLI_DATA_URL.toExternalForm());
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
     */
    @SuppressWarnings("ConstantConditions")
    protected void startMockServer() {
        if (Thread.currentThread().getContextClassLoader() != ClassLoader.getSystemClassLoader()) {
            final String reason = "don't yet know how to run MockServer when not in system class loader";
            Log.info("$(italic,yellow Skipping: %s)", reason);
            Assumptions.assumeTrue(false, reason);
        }
        ConfigurationProperties.logLevel("INFO");
        Log.info("Using mock server at %s", MOCK_SERVER_BASE_URL);
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
        useBaseUrl(MOCK_SERVER_BASE_URL);
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

    @Test
    void smokeTest() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(24, TimeUnit.HOURS, RC1, NO_ETAG);

        // Check properties. Should not perform update.

        logged.clear();
        ConfigProperties props = meta.propertiesOf(latestVersion);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("helidon.version"), is(RC1));
        assertThat(props.property("build-tools.version"), is(RC1));
        assertThat(props.property("cli.version"), is(RC1));
        assertThat(props.contains("cli.2.0.0-M2.message"), is(true));
        assertThat(props.contains("cli.2.0.0-M3.message"), is(false));
        assertThat(props.contains("cli.2.0.0-M4.message"), is(true));
        assertThat(props.contains("cli.2.0.0-RC1.message"), is(true));
        assertThat(logged.size(), is(1));
        logged.assertLinesContainingAll(1, "stale check", "is false", RC1_LAST_UPDATE);

        // Check catalog. Should not perform update.

        logged.clear();
        ArchetypeCatalog catalog = meta.catalogOf(latestVersion);
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
        assertThat(logged.size(), is(1));
        logged.assertLinesContainingAll(1, "stale check", "is false", RC1_LAST_UPDATE);

        // Check archetype. Should not perform update.

        logged.clear();
        Path archetypeJar = meta.archetypeOf(entriesById.get("helidon-bare-se"));
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC1.jar"));
        assertThat(logged.size(), is(1));
        logged.assertLinesContainingAll(1, "stale check", "is false", RC1_LAST_UPDATE);

        // Check that more calls do not update

        logged.clear();
        assertThat(meta.latestVersion(), is(latestVersion));
        assertThat(meta.propertiesOf(latestVersion), is(props));
        assertThat(meta.catalogOf(latestVersion), is(catalog));

        assertThat(logged.size(), is(3));
        logged.assertLinesContainingAll(1, "stale check", "is false", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(2, "stale check", "is false", RC1_LAST_UPDATE);

        // Check the build tools and CLI versions

        logged.clear();
        MavenVersion expected = toMavenVersion(RC1);
        assertThat(meta.buildToolsVersionOf(latestVersion), is(expected));
        assertThat(meta.cliVersionOf(latestVersion), is(expected));
    }

    @Test
    void testReleaseNotes() throws Exception {
        meta = newInstance(24, TimeUnit.HOURS);
        MavenVersion helidonVersion = toMavenVersion(RC2);

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
        assertInitialLatestVersionRequestPerformsUpdate(1, TimeUnit.SECONDS, RC1, NO_ETAG);

        // Wait 1.25 seconds and check version. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.latestVersion(), is(latestVersion));

        logged.assertLinesContainingAll(1, "stale check", "is true", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + NO_ETAG);
        logged.assertLinesContainingAll("Looking up latest Helidon version");
        logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + RC1);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testPropertiesUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, TimeUnit.SECONDS, RC1, NO_ETAG);

        // Wait 1.25 seconds and check properties. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));

        logged.assertLinesContainingAll(1, "stale check", "is true", RC1_LAST_UPDATE);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + NO_ETAG);
        logged.assertNoLinesContainingAll("Looking up latest Helidon version");
        logged.assertLinesContainingAll("Updating metadata for Helidon version " + RC1);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testCatalogUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, TimeUnit.SECONDS, RC1, NO_ETAG);

        // Wait 1.25 seconds and check catalog. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.catalogOf(latestVersion), is(not(nullValue())));

        logged.assertLinesContainingAll(1, "stale check", "is true", RC1_LAST_UPDATE);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + NO_ETAG);
        logged.assertNoLinesContainingAll("Looking up latest Helidon version");
        logged.assertLinesContainingAll("Updating metadata for Helidon version " + RC1);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testZipIsNotDownloadedWhenEtagMatches() throws Exception {
        setupMockServer(RC1);
        assertZipIsNotDownloadedFromServerWhenEtagMatches();
    }

    protected void assertZipIsNotDownloadedFromServerWhenEtagMatches() throws Exception {

        // Make the initial latestVersion call and validate the result

        assertInitialLatestVersionRequestPerformsUpdate(0, TimeUnit.NANOSECONDS, RC1, INITIAL_ETAG);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the latest version

        logged.clear();
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));
        logged.assertLinesContainingAll(1, "not modified", RC1_CLI_DATA_ZIP);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + INITIAL_ETAG);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
        // NOTE: This test case can only work against mock server
    void testUpdateWhenLatestChanges() throws Exception {

        // Setup mock server

        setupMockServer(RC2);

        // Make the initial latestVersion call and validate the result

        assertInitialLatestVersionRequestPerformsUpdate(0, TimeUnit.NANOSECONDS, RC2, INITIAL_ETAG);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the latest version

        logged.clear();
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));
        logged.assertLinesContainingAll(1, "not modified", RC2_CLI_DATA_ZIP);
        logged.assertLinesContainingAll(1, "updated", RC2_LAST_UPDATE, "etag " + INITIAL_ETAG);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testCatalogUpdatesWhenUnseenVersionRequested() throws Exception {
        setupMockServer(RC2);
        assertServerUpdatesWhenUnseenRC2VersionRequested();
    }

    protected void assertServerUpdatesWhenUnseenRC2VersionRequested() throws Exception {

        // Make the initial catalog request and validate the result

        final Runnable request = () -> catalogRequest(RC2, true);
        assertInitialRequestPerformsUpdate(request, 24, TimeUnit.HOURS, RC2, INITIAL_ETAG);

        // Now request the catalog again and make sure we do no updates

        logged.clear();
        catalogRequest(RC2, false);
        assertThat(meta.propertiesOf(RC2), is(not(nullValue())));
        logged.assertLinesContainingAll(0, "not modified", RC2_CLI_DATA_ZIP);
        logged.assertLinesContainingAll(0, "updated", RC2_LAST_UPDATE, "etag " + INITIAL_ETAG);
        logged.assertLinesContainingAll(0, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(0, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(0, "wrote", LATEST_FILE_NAME);
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
        Path propertiesFile = versionDir.resolve(PROPERTIES_FILE_NAME);
        Path catalogFile = versionDir.resolve(CATALOG_FILE_NAME);
        Path seJarFile = versionDir.resolve(HELIDON_BARE_SE + "-" + expectedVersion + ".jar");
        Path mpJarFile = versionDir.resolve(HELIDON_BARE_MP + "-" + expectedVersion + ".jar");
        String zipPath = expectedVersion + CLI_DATA;
        String lastUpdatePath = expectedVersion + LAST_UPDATE;

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
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connecting", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
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
            logged.assertLinesContainingAll(1, "stale check", "(not found)", LATEST_FILE_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void catalogRequest(String version, boolean expectUpdate) {
        try {
            String staleType = expectUpdate ? "(not found)" : "is false";
            String staleFilePath = version + "/" + LAST_UPDATE_FILE_NAME;
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

    private void setupMockServer(String latest) {

        // Start server

        startMockServer();

        // Always return latest version

        mockServer.when(request().withMethod("GET")
                                 .withPath("/latest"))
                  .respond(response().withBody(latest));

        // Always return zip data when If-None-Match header is "<no-file>" (plugin defines this)

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/" + RC1_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(RC1_ZIP));

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/" + RC2_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(RC2_ZIP));

        // Always return 304 when If-None-Match is "<initial>"

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, INITIAL_ETAG)
                                 .withPath("/" + RC1_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withStatusCode(304));

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, INITIAL_ETAG)
                                 .withPath("/" + RC2_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withStatusCode(304));
    }
}
