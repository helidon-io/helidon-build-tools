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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;
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
    static final String ETAG_HEADER = "Etag";
    static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    static final String NO_ETAG = "<no-etag>";
    static final String NO_FILE_ETAG = "<no-file>";
    static final String INITIAL_ETAG = "<initial>";
    static final String CHANGED_ETAG = "<changed>";

    private String baseUrl;
    private Path cacheDir;
    private CapturingLogWriter output;
    private Metadata meta;
    private MavenVersion latestVersion;
    private ClientAndServer mockServer;

    @BeforeEach
    public void beforeEach() throws IOException {
        Config.setUserHome(TestFiles.targetDir().resolve("alice"));
        final UserConfig userConfig = Config.userConfig();
        userConfig.clearCache();
        userConfig.clearPlugins();
        Plugins.clearPluginJar();
        useTestCliDataBaseUrl();
        cacheDir = userConfig.cacheDir();
        output = CapturingLogWriter.install();
    }

    @AfterEach
    public void afterEach() {
        output.uninstall();
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    void smokeTest() throws Exception {
        initAndAssertLatestVersionDoesUpdate(24, TimeUnit.HOURS, "2.0.0-RC1", NO_ETAG);

        // Check properties. Should not perform update.

        clearLog();
        ConfigProperties props = meta.properties(latestVersion);
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

        assertThat(logMessages().size(), is(1));
        assertThat(countLogged("stale check", "is false", "2.0.0-RC1/.lastUpdate"), is(1));

        // Check catalog. Should not perform update.

        clearLog();
        ArchetypeCatalog catalog = meta.catalog(latestVersion);
        assertThat(catalog, is(not(nullValue())));
        assertThat(catalog.entries().size(), is(2));
        Map<String, ArchetypeEntry> entriesById = catalog.entries()
                                                         .stream()
                                                         .collect(Collectors.toMap(ArchetypeEntry::artifactId, entry -> entry));
        assertThat(entriesById.size(), is(2));
        assertThat(entriesById.get("helidon-bare-se"), is(notNullValue()));
        assertThat(entriesById.get("helidon-bare-se").name(), is("bare"));
        assertThat(entriesById.get("helidon-bare-mp"), is(notNullValue()));
        assertThat(entriesById.get("helidon-bare-mp").name(), is("bare"));
        assertThat(logMessages().size(), is(1));
        assertThat(countLogged("stale check", "is false", "2.0.0-RC1/.lastUpdate"), is(1));

        // Check archetype. Should not perform update.

        clearLog();
        Path archetypeJar = meta.archetype(entriesById.get("helidon-bare-se"));
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC1.jar"));
        assertThat(logMessages().size(), is(1));
        assertThat(countLogged("stale check", "is false", "2.0.0-RC1/.lastUpdate"), is(1));

        // Check that more calls do not update

        clearLog();
        assertThat(meta.latestVersion(), is(latestVersion));
        assertThat(meta.properties(latestVersion), is(props));
        assertThat(meta.catalog(latestVersion), is(catalog));

        assertThat(logMessages().size(), is(3));
        assertThat(countLogged("stale check", "is false", "latest"), is(1));
        assertThat(countLogged("stale check", "is false", "2.0.0-RC1/.lastUpdate"), is(2));
    }

    @Test
    void testLatestVersionUpdatesAfterDelay() throws Exception {
        initAndAssertLatestVersionDoesUpdate(1, TimeUnit.SECONDS, "2.0.0-RC1", NO_ETAG);

        // Wait 1.25 seconds and check version. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        clearLog();
        Thread.sleep(1250);
        assertThat(meta.latestVersion(), is(latestVersion));

        assertThat(countLogged("stale check", "is true", "latest"), is(1));
        assertThat(countLogged("updated", "2.0.0-RC1/.lastUpdate", "etag " + NO_ETAG), is(1));
        assertLogged("Looking up latest Helidon version");
        assertNotLogged("Updating metadata for Helidon version 2.0.0-RC1");
        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("connected", "latest"), is(1));
        assertThat(countLogged("wrote", "latest"), is(1));
    }

    @Test
    void testPropertiesUpdatesAfterDelay() throws Exception {
        initAndAssertLatestVersionDoesUpdate(1, TimeUnit.SECONDS,"2.0.0-RC1",  NO_ETAG);

        // Wait 1.25 seconds and check properties. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        clearLog();
        Thread.sleep(1250);
        assertThat(meta.properties(latestVersion), is(not(nullValue())));

        assertThat(countLogged("stale check", "is true", "2.0.0-RC1/.lastUpdate"), is(1));
        assertThat(countLogged("updated", "2.0.0-RC1/.lastUpdate", "etag " + NO_ETAG), is(1));
        assertNotLogged("Looking up latest Helidon version");
        assertLogged("Updating metadata for Helidon version 2.0.0-RC1");
        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("connected", "latest"), is(1));
        assertThat(countLogged("wrote", "latest"), is(1));
    }

    @Test
    void testCatalogUpdatesAfterDelay() throws Exception {
        initAndAssertLatestVersionDoesUpdate(1, TimeUnit.SECONDS, "2.0.0-RC1", NO_ETAG);

        // Wait 1.25 seconds and check catalog. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        clearLog();
        Thread.sleep(1250);
        assertThat(meta.catalog(latestVersion), is(not(nullValue())));

        assertThat(countLogged("stale check", "is true", "2.0.0-RC1/.lastUpdate"), is(1));
        assertThat(countLogged("updated", "2.0.0-RC1/.lastUpdate", "etag " + NO_ETAG), is(1));
        assertNotLogged("Looking up latest Helidon version");
        assertLogged("Updating metadata for Helidon version 2.0.0-RC1");
        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("connected", "latest"), is(1));
        assertThat(countLogged("wrote", "latest"), is(1));
    }

    @Test
    void testUpdatesWhenEtagMatches() throws Exception {
        byte[] rc1Zip = Files.readAllBytes(TEST_CLI_DATA_PATH.resolve("2.0.0-RC1/cli-data.zip"));

        // Setup mock server

        startMockServer();

        // Always return latest version

        mockServer.when(request().withMethod("GET")
                                 .withPath("/latest"))
                  .respond(response().withBody("2.0.0-RC1"));

        // Always return zip data when If-None-Match header is "<no-file>" (plugin defines this)

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/2.0.0-RC1/cli-data.zip"))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(rc1Zip));

        // Always return 304 when If-None-Match is "<initial>"

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, INITIAL_ETAG)
                                 .withPath("/2.0.0-RC1/cli-data.zip"))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withStatusCode(304));

        // Make the initial latestVersion call and validate the result

        initAndAssertLatestVersionDoesUpdate(0, TimeUnit.NANOSECONDS, "2.0.0-RC1", INITIAL_ETAG);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the latest version

        clearLog();
        assertThat(meta.properties(latestVersion), is(not(nullValue())));
        assertThat(countLogged("not modified", "2.0.0-RC1/cli-data.zip"), is(1));
        assertThat(countLogged("updated", "2.0.0-RC1/.lastUpdate", "etag " + INITIAL_ETAG), is(1));
        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("connected", "latest"), is(1));
        assertThat(countLogged("wrote", "latest"), is(1));
    }

    @Test
    void testUpdateWhenLatestChanges() throws Exception {
        byte[] rc2Zip = Files.readAllBytes(TEST_CLI_DATA_PATH.resolve("2.0.0-RC2/cli-data.zip"));

        // Setup mock server

        startMockServer();

        // Always return latest version

        mockServer.when(request().withMethod("GET")
                                 .withPath("/latest"))
                  .respond(response().withBody("2.0.0-RC2"));

        // Always return zip data when If-None-Match header is "<no-file>" (plugin defines this)

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/2.0.0-RC2/cli-data.zip"))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(rc2Zip));

        // Always return 304 when If-None-Match is "<initial>"

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, INITIAL_ETAG)
                                 .withPath("/2.0.0-RC2/cli-data.zip"))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withStatusCode(304));

        // Make the initial latestVersion call and validate the result

        initAndAssertLatestVersionDoesUpdate(0, TimeUnit.NANOSECONDS, "2.0.0-RC2", INITIAL_ETAG);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the latest version

        clearLog();
        assertThat(meta.properties(latestVersion), is(not(nullValue())));
        assertThat(countLogged("not modified", "2.0.0-RC2/cli-data.zip"), is(1));
        assertThat(countLogged("updated", "2.0.0-RC2/.lastUpdate", "etag " + INITIAL_ETAG), is(1));
        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("connected", "latest"), is(1));
        assertThat(countLogged("wrote", "latest"), is(1));

    }

    protected void initAndAssertLatestVersionDoesUpdate(long updateFrequency,
                                                        TimeUnit updateFrequencyUnits,
                                                        String expectedVersion,
                                                        String expectedEtag) throws Exception {
        String zipPath = expectedVersion + "/cli-data.zip";
        String lastUpdatePath = expectedVersion + "/.lastUpdate";

        meta = newInstance(updateFrequency, updateFrequencyUnits);

        // Check latest version. Should update both latest file and latest archetype.

        clearLog();
        latestVersion = meta.latestVersion();
        assertThat(latestVersion, is(not(nullValue())));
        assertThat(latestVersion, is(toMavenVersion(expectedVersion)));
        assertLogged("Looking up latest Helidon version");
        assertNotLogged("Updating metadata for Helidon version " + expectedVersion);

        assertThat(countLogged("stale check", "(not found)", "latest"), is(1));
        assertThat(countLogged("unpacked", "cli-plugins-", ".jar"), is(1));
        assertThat(countLogged("executing", "cli-plugins-", "UpdateMetadata"), is(1));
        assertThat(countLogged("downloading", "latest"), is(1));
        assertThat(countLogged("connecting", "latest"), is(1));
        assertThat(countLogged("connected", "latest"), is(1));
        assertThat(countLogged("downloading", zipPath), is(1));
        assertThat(countLogged("connecting", zipPath), is(1));
        assertThat(countLogged("connected", zipPath), is(1));

        assertThat(countLogged("unzipping", zipPath), is(1));
        assertThat(countLogged("deleting", zipPath), is(1));
        assertThat(countLogged("updated", lastUpdatePath, "etag " + expectedEtag), is(1));
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
     *
     * @return The server and client.
     */
    protected ClientAndServer startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
        Log.info("Using mock server at %s", MOCK_SERVER_BASE_URL);
        useBaseUrl(MOCK_SERVER_BASE_URL);
        return mockServer;
    }


    protected Metadata newInstance() {
        return Metadata.newInstance(cacheDir, baseUrl, 24, TimeUnit.HOURS, true);
    }

    protected Metadata newInstance(long updateFrequency, TimeUnit updateFrequencyUnits) {
        return Metadata.newInstance(cacheDir, baseUrl, updateFrequency, updateFrequencyUnits, true);
    }

    protected void clearLog() {
        output.clear();
    }

    protected List<CapturingLogWriter.LogEntry> logEntries() {
        return output.entries();
    }

    protected List<String> logMessages() {
        return output.messages();
    }

    protected List<String> logLines(Predicate<String> filter) {
        return logMessages().stream().filter(filter).collect(Collectors.toList());
    }

    protected void assertLogged(String... messageFragments) {
        assertLogged(line -> true, messageFragments);
    }

    protected void assertNotLogged(String... messageFragments) {
        assertNotLogged(line -> true, messageFragments);
    }

    protected void assertLogged(Predicate<String> filter, String... messageFragments) {
        if (!isLogged(filter, messageFragments)) {
            fail("log does not contain one of the following: " + Arrays.toString(messageFragments) + EOL + logMessages());
        }
    }

    protected void assertNotLogged(Predicate<String> filter, String... messageFragments) {
        if (isLogged(filter, messageFragments)) {
            fail("log should not contain one of the following: " + Arrays.toString(messageFragments) + EOL + logMessages());
        }
    }

    protected boolean isLogged(Predicate<String> filter, String... messageFragments) {
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

    protected long countLogged(Predicate<String> filter, String fragment) {
        return logLines(filter).stream().filter(line -> line.contains(fragment)).count();
    }

    protected int countLogged(String fragment1) {
        return logLines(line -> line.contains(fragment1)).size();
    }

    protected int countLogged(String fragment1, String fragment2) {
        return (int) logLines(line -> line.contains(fragment1)).stream()
                                                               .filter(line -> line.contains(fragment2))
                                                               .count();
    }

    protected int countLogged(String fragment1, String fragment2, String fragment3) {
        return (int) logLines(line -> line.contains(fragment1)).stream()
                                                               .filter(line -> line.contains(fragment2))
                                                               .filter(line -> line.contains(fragment3))
                                                               .count();
    }
}
