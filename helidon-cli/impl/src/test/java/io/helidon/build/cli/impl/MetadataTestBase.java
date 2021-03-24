/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.test.CapturingLogWriter;
import io.helidon.build.test.TestFiles;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;

import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.impl.TestMetadata.LAST_UPDATE_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.LATEST_FILE_NAME;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static io.helidon.build.util.TestUtils.uniqueDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Base class for {@link Metadata} tests.
 */
public class MetadataTestBase {

    private static final URI CWD_URI = Path.of("").toUri();

    protected String baseUrl;
    protected Path cacheDir;
    protected Path latestFile;
    protected CapturingLogWriter logged;
    protected Metadata meta;
    protected MavenVersion latestVersion;
    protected MetadataTestServer testServer;

    /**
     * Prepare for each test.
     *
     * @param info The test info.
     * @param baseUrl The base url to use.
     */
    protected void prepareEach(TestInfo info, String baseUrl) {
        String testClassName = info.getTestClass().orElseThrow().getSimpleName();
        String testName = info.getTestMethod().orElseThrow().getName();
        Log.info("%n--- %s $(bold %s) -------------------------------------------%n", testClassName, testName);
        Path userHome = uniqueDir(TestFiles.targetDir(), "alice");
        Config.setUserHome(userHome);
        UserConfig userConfig = UserConfig.create(userHome);
        Config.setUserConfig(userConfig);
        Plugins.reset(false);
        useBaseUrl(baseUrl);
        cacheDir = userConfig.cacheDir();
        latestFile = cacheDir.resolve(LATEST_FILE_NAME);
        logged = CapturingLogWriter.install();
    }

    /**
     * Cleanup after each test.
     */
    protected void cleanupEach() {
        logged.uninstall();
        if (testServer != null) {
            testServer.stop();
        }
    }

    /**
     * Use the given url as the base url.
     *
     * @param baseUrl The base url.
     */
    protected void useBaseUrl(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    /**
     * Starts the metadata test server and client and sets the base url pointing to it.
     *
     * @param latestVersion The version to return from "/latest"
     */
    protected void startMetadataTestServer(TestVersion latestVersion) {
        startMetadataTestServer(latestVersion, false);
    }

    /**
     * Starts the metadata test server and client and sets the base url pointing to it.
     *
     * @param verbose       {@code true} if the server should be verbose.
     * @param latestVersion The version to return from "/latest"
     */
    protected void startMetadataTestServer(TestVersion latestVersion, boolean verbose) {
        testServer = new MetadataTestServer(latestVersion, verbose).start();
        useBaseUrl(testServer.url());
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

    /**
     * Returns a new {@link Metadata} instance with the default frequency.
     *
     * @return The instance.
     */
    protected Metadata newDefaultInstance() {
        return newInstance(24, TimeUnit.HOURS);
    }

    /**
     * Asserts that the very first latest metadata request does a metadata update and make assertions on the logs.
     *
     * @param updateFrequency      update frequency of the metadata created
     * @param updateFrequencyUnits update frequency unit of the metadata created
     * @param expectedVersion      expected Helidon version
     * @param expectedEtag         expected ETAG
     * @param latestFileExists     {@code true} if the latest file is expected to exist, {@code false} otherwise
     */
    protected void assertInitialLatestVersionRequestPerformsUpdate(long updateFrequency,
                                                                   TimeUnit updateFrequencyUnits,
                                                                   String expectedVersion,
                                                                   String expectedEtag,
                                                                   boolean latestFileExists) {
        final Runnable request = () -> firstLatestVersionRequest(expectedVersion, !latestFileExists);
        assertInitialRequestPerformsUpdate(request, updateFrequency, updateFrequencyUnits,
                expectedVersion, expectedEtag, latestFileExists);
    }

    /**
     * Assert that the very first metadata request does a metadata update and make assertions on the logs.
     *
     * @param request              runnable to runs the request
     * @param updateFrequency      update frequency of the metadata created
     * @param updateFrequencyUnits update frequency unit of the metadata created
     * @param expectedVersion      expected Helidon version
     * @param expectedEtag         expected ETAG
     * @param latestFileExists     {@code true} if the latest file is expected to exist, {@code false} otherwise
     */
    protected void assertInitialRequestPerformsUpdate(Runnable request,
                                                      long updateFrequency,
                                                      TimeUnit updateFrequencyUnits,
                                                      String expectedVersion,
                                                      String expectedEtag,
                                                      boolean latestFileExists) {
        Path versionDir = cacheDir.resolve(expectedVersion);
        Path propertiesFile = versionDir.resolve(TestMetadata.PROPERTIES_FILE_NAME);
        Path catalogFile = versionDir.resolve(TestMetadata.CATALOG_FILE_NAME);
        Path seJarFile = versionDir.resolve(TestMetadata.HELIDON_BARE_SE + "-" + expectedVersion + ".jar");
        Path mpJarFile = versionDir.resolve(TestMetadata.HELIDON_BARE_MP + "-" + expectedVersion + ".jar");
        String zipPath = expectedVersion + File.separator + TestMetadata.CLI_DATA_FILE_NAME;
        String zipUriPath = expectedVersion + "/" + TestMetadata.CLI_DATA_FILE_NAME;
        String lastUpdatePath = expectedVersion + File.separator + LAST_UPDATE_FILE_NAME;

        // Check expected latest file and version directory existence

        assertThat(Files.exists(latestFile), is(latestFileExists));
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
        logged.assertLinesContainingAll(1, "downloading", zipUriPath);
        logged.assertLinesContainingAll(1, "connecting", zipUriPath);
        logged.assertLinesContainingAll(1, "connected", zipUriPath);

        logged.assertLinesContainingAll(1, "unzipping", zipPath);
        logged.assertLinesContainingAll(1, "deleting", zipPath);
        logged.assertLinesContainingAll(1, "updated", lastUpdatePath, "etag " + expectedEtag);
    }

    /**
     * Request the latest metadata version and perform assertions on the logs.
     *
     * @param expectedVersion expected Helidon version
     * @param expectUpdate    {@code true} if a metadata update is expected, {@code false otherwise}
     */
    protected void firstLatestVersionRequest(String expectedVersion, boolean expectUpdate) {
        try {
            String staleType = expectUpdate ? "(not found)" : "(zero delay)";
            latestVersion = meta.latestVersion();
            assertThat(latestVersion, is(not(nullValue())));
            if (expectedVersion != null) {
                assertThat(latestVersion, is(toMavenVersion(expectedVersion)));
            }
            logged.assertLinesContainingAll(1, "stale check", staleType, LATEST_FILE_NAME);
            logged.assertLinesContainingAll("Looking up latest Helidon version");
            logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + expectedVersion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform a catalog request and perform assertions on the logs.
     *
     * @param version      Helidon version
     * @param expectUpdate {@code true} if a metadata update is expected, {@code false otherwise}
     */
    protected void catalogRequest(String version, boolean expectUpdate) {
        try {
            String staleType = expectUpdate ? "(not found)" : "is false";
            String staleFilePath = version + File.separator + LAST_UPDATE_FILE_NAME;
            meta.catalogOf(version);
            logged.assertLinesContainingAll(1, "stale check", staleType, staleFilePath);
            logged.assertNoLinesContainingAll("Looking up latest Helidon version");
            if (expectUpdate) {
                logged.assertLinesContainingAll("Updating metadata for Helidon version " + version);
            } else {
                logged.assertNoLinesContainingAll("Updating metadata for Helidon version " + version);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
