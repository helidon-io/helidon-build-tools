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
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.archetype.engine.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.impl.Metadata.DEFAULT_UPDATE_FREQUENCY;
import static io.helidon.build.cli.impl.TestMetadata.CLI_DATA_PATH;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_MP;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_SE;
import static io.helidon.build.cli.impl.TestMetadata.LATEST_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.MAVEN_VERSION_RC1;
import static io.helidon.build.cli.impl.TestMetadata.MAVEN_VERSION_RC2;
import static io.helidon.build.cli.impl.TestMetadata.NO_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.RC1_CLI_DATA_ZIP_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.RC1_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.RC1_LAST_UPDATE;
import static io.helidon.build.cli.impl.TestMetadata.RC2_CLI_DATA_ZIP_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.RC2_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.RC2_LAST_UPDATE;
import static io.helidon.build.cli.impl.TestMetadata.TEST_CLI_DATA_URL;
import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC2;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC1;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC2;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
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
public class MetadataTest extends BaseMetadataTest {

    @BeforeEach
    public void beforeEach(TestInfo info) throws IOException {
        prepareEach(info, TEST_CLI_DATA_URL.toExternalForm());
    }

    @AfterEach
    public void afterEach() {
        cleanupEach();
    }

    @Test
    void smokeTest() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(DEFAULT_UPDATE_FREQUENCY, HOURS, VERSION_RC1, NO_ETAG, false);

        // Check properties. Should not perform update.

        logged.clear();
        ConfigProperties props = meta.propertiesOf(latestVersion);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("build-tools.version"), is(VERSION_RC1));
        assertThat(props.property("cli.version"), is(VERSION_RC1));
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

        // Check the CLI plugin and CLI versions

        logged.clear();
        MavenVersion expected = MAVEN_VERSION_RC1;
        assertThat(meta.cliPluginVersion(latestVersion, false), is(expected));
        assertThat(meta.cliVersionOf(latestVersion, false), is(expected));
    }

    @Test
    void testCheckForCliUpdate() throws Exception {

        // Setup with RC2 as latest

        startMetadataTestServer(RC2, false);
        meta = newDefaultInstance();

        // Simulate different cli versions

        MavenVersion cliVersionRc1 = MAVEN_VERSION_RC1;
        MavenVersion cliVersionRc2 = MAVEN_VERSION_RC2;
        MavenVersion cliVersionRc2Updated = toMavenVersion("2.0.0");

        assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(false));

        assertThat(meta.checkForCliUpdate(cliVersionRc1, false).isPresent(), is(true));
        assertThat(meta.checkForCliUpdate(cliVersionRc1, false).orElseThrow(), is(cliVersionRc2));

        // Now change the metadata for RC2 such that the cli version returned is newer

        String updatedRc2FileName = VERSION_RC2 + "-updated" + CLI_DATA_PATH;
        byte[] updatedRc2 = TestMetadata.readCliDataFile(updatedRc2FileName);
        testServer.zipData(RC2, updatedRc2);

        // Make sure it doesn't update now, since the update period has not expired

        assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(false));

        // Force expiry and validate that we get expected version update

        meta = newInstance(0, HOURS);
        assertThat(meta.checkForCliUpdate(cliVersionRc2, false).isPresent(), is(true));
        assertThat(meta.checkForCliUpdate(cliVersionRc1, false).orElseThrow(), is(cliVersionRc2Updated));
    }

    @Test
    void testReleaseNotes() throws Exception {
        meta = newDefaultInstance();
        MavenVersion helidonVersion = MAVEN_VERSION_RC2;

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
        assertInitialLatestVersionRequestPerformsUpdate(1, SECONDS, VERSION_RC1, NO_ETAG, false);

        // Wait 1.25 seconds and check version. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.latestVersion(), is(latestVersion));

        logged.assertLinesContainingAll(1, "stale check", "is true", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + NO_ETAG);
        logged.assertLinesContainingAll("Looking up latest Helidon version");
        logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + VERSION_RC1);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testPropertiesUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, SECONDS, VERSION_RC1, NO_ETAG, false);

        // Wait 1.25 seconds and check properties. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));

        logged.assertLinesContainingAll(1, "stale check", "is true", RC1_LAST_UPDATE);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + NO_ETAG);
        logged.assertNoLinesContainingAll("Looking up latest Helidon version");
        logged.assertLinesContainingAll("Updating metadata for Helidon version " + VERSION_RC1);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testCatalogUpdatesAfterDelay() throws Exception {
        assertInitialLatestVersionRequestPerformsUpdate(1, SECONDS, VERSION_RC1, NO_ETAG, false);

        // Wait 1.25 seconds and check catalog. Should perform update.

        Log.info("sleeping 1.25 seconds before recheck");
        logged.clear();
        Thread.sleep(1250);
        assertThat(meta.catalogOf(latestVersion), is(not(nullValue())));

        logged.assertLinesContainingAll(1, "stale check", "is true", RC1_LAST_UPDATE);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + NO_ETAG);
        logged.assertNoLinesContainingAll("Looking up latest Helidon version");
        logged.assertLinesContainingAll("Updating metadata for Helidon version " + VERSION_RC1);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testZipIsNotDownloadedWhenEtagMatches() throws Exception {
        startMetadataTestServer(TestVersion.RC1);

        // Make the initial latestVersion call and validate the result

        assertInitialLatestVersionRequestPerformsUpdate(0, NANOSECONDS, VERSION_RC1, RC1_ETAG, false);

        // Now get the properties again and make sure we skip the zip download but still
        // updated the latest version

        logged.clear();
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));
        logged.assertLinesContainingAll(1, "not modified", RC1_CLI_DATA_ZIP_FILE_NAME);
        logged.assertLinesContainingAll(1, "updated", RC1_LAST_UPDATE, "etag " + RC1_ETAG);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);
    }

    @Test
    void testUpdateWhenLatestChanges() throws Exception {

        // Setup test server with latest set to RC2

        startMetadataTestServer(RC2);

        // Make the initial latestVersion call and validate the result

        assertInitialLatestVersionRequestPerformsUpdate(0, NANOSECONDS, VERSION_RC2, RC2_ETAG, false);

        // Now get the properties again and make sure we skip the zip download but still updated the latest version.
        // Note that the update is forced here because we used a zero frequency.

        logged.clear();
        assertThat(meta.propertiesOf(latestVersion), is(not(nullValue())));
        logged.assertLinesContainingAll(1, "not modified", RC2_CLI_DATA_ZIP_FILE_NAME);
        logged.assertLinesContainingAll(1, "updated", RC2_LAST_UPDATE, "etag " + RC2_ETAG);
        logged.assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(1, "wrote", LATEST_FILE_NAME);

        // Now change the result of /latest and validate the result

        logged.clear();
        Plugins.reset(true);
        testServer.latest(TestVersion.RC1);
        assertInitialLatestVersionRequestPerformsUpdate(0, NANOSECONDS, VERSION_RC1, RC1_ETAG, true);
    }

    @Test
    void testCatalogUpdatesWhenUnseenVersionRequested() throws Exception {
        startMetadataTestServer(RC2);

        // Make the initial catalog request and validate the result

        final Runnable request = () -> catalogRequest(VERSION_RC2, true);
        assertInitialRequestPerformsUpdate(request, DEFAULT_UPDATE_FREQUENCY, HOURS, VERSION_RC2, RC2_ETAG, false);

        // Now request the catalog again and make sure we do no updates

        logged.clear();
        catalogRequest(VERSION_RC2, false);
        assertThat(meta.propertiesOf(VERSION_RC2), is(not(nullValue())));
        logged.assertLinesContainingAll(0, "not modified", RC2_CLI_DATA_ZIP_FILE_NAME);
        logged.assertLinesContainingAll(0, "updated", RC2_LAST_UPDATE, "etag " + RC2_ETAG);
        logged.assertLinesContainingAll(0, "downloading", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(0, "connected", LATEST_FILE_NAME);
        logged.assertLinesContainingAll(0, "wrote", LATEST_FILE_NAME);
    }
}
