/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.helidon.build.cli.impl.Metadata.UpdateFailed;
import io.helidon.build.cli.impl.TestMetadata.TestVersion;

import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogWriter;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.impl.TestMetadata.LAST_UPDATE_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.LATEST_FILE_NAME;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.Unchecked.unchecked;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Base class for {@link Metadata} tests.
 */
public class MetadataTestBase {

    protected static final LogRecorder LOG_RECORDER = LogRecorder.create();

    protected String baseUrl;
    protected Path cacheDir;
    protected Path latestFile;
    protected Metadata meta;
    protected MavenVersion latestVersion;
    protected MetadataTestServer testServer;

    @BeforeAll
    public static void beforeAll() {
        LogWriter.addRecorder(LOG_RECORDER);
    }

    @AfterAll
    public static void afterAll() {
        LogWriter.removeRecorder(LOG_RECORDER);
    }

    /**
     * Prepare for each test.
     *
     * @param info    The test info.
     * @param baseUrl The base url to use.
     */
    protected void beforeEach(TestInfo info, String baseUrl) {
        String testClassName = info.getTestClass().orElseThrow().getSimpleName();
        String testName = info.getTestMethod().orElseThrow().getName();
        Log.info("%n--- %s $(bold %s) -------------------------------------------%n", testClassName, testName);
        Path userHome = ensureDirectory(unique(TestFiles.targetDir(MetadataTestBase.class), "alice"));
        Config.setUserHome(userHome);
        UserConfig userConfig = UserConfig.create(userHome);
        Config.setUserConfig(userConfig);
        Plugins.reset(false);
        useBaseUrl(baseUrl);
        cacheDir = userConfig.cacheDir();
        latestFile = cacheDir.resolve(LATEST_FILE_NAME);
        LOG_RECORDER.clear();
        LogLevel.set(LogLevel.DEBUG);
    }

    /**
     * Cleanup after each test.
     */
    @AfterEach
    protected void afterEach() {
        LogLevel.set(LogLevel.INFO);
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
    @SuppressWarnings("SameParameterValue")
    protected void startMetadataTestServer(TestVersion latestVersion) {
        startMetadataTestServer(latestVersion, false);
    }

    /**
     * Starts the metadata test server and client and sets the base url pointing to it.
     *
     * @param verbose       {@code true} if the server should be verbose.
     * @param latestVersion The version to return from "/latest"
     */
    @SuppressWarnings("SameParameterValue")
    protected void startMetadataTestServer(TestVersion latestVersion, boolean verbose) {
        testServer = new MetadataTestServer(latestVersion, verbose).start();
        useBaseUrl(testServer.url());
    }

    /**
     * Returns a new {@link Metadata} instance with the given frequency.
     *
     * @param updateFrequency      The update frequency.
     * @param updateFrequencyUnits The update frequency units.
     * @return The instance.
     */
    protected Metadata newInstance(long updateFrequency, TimeUnit updateFrequencyUnits) {
        return Metadata.builder()
                       .rootDir(cacheDir)
                       .url(baseUrl)
                       .updateFrequency(updateFrequency)
                       .updateFrequencyUnits(updateFrequencyUnits)
                       .debugPlugin(true)
                       .pluginStdOut(PrintStreams.accept(STDOUT, LOG_RECORDER::addEntry))
                       .build();
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

        final Runnable request = unchecked(() -> firstLatestVersionRequest(expectedVersion, !latestFileExists));
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

        // Check expected the latest file and version directory existence

        assertThat(Files.exists(latestFile), is(latestFileExists));
        assertThat(Files.exists(versionDir), is(false));

        // Make request. Should update both latest file and latest archetype.

        LOG_RECORDER.clear();
        meta = newInstance(updateFrequency, updateFrequencyUnits);
        request.run();

        requireFile(latestFile);
        requireDirectory(versionDir);
        requireFile(propertiesFile);
        requireFile(catalogFile);
        requireFile(seJarFile);
        requireFile(mpJarFile);

        assertLinesContainingAll(1, "downloading", LATEST_FILE_NAME);
        assertLinesContainingAll(1, "connecting", LATEST_FILE_NAME);
        assertLinesContainingAll(1, "connected", LATEST_FILE_NAME);
        assertLinesContainingAll(1, "downloading", zipUriPath);
        assertLinesContainingAll(1, "connecting", zipUriPath);
        assertLinesContainingAll(1, "connected", zipUriPath);

        assertLinesContainingAll(1, "unzipping", zipPath);
        assertLinesContainingAll(1, "deleting", zipPath);
        assertLinesContainingAll(1, "updated", lastUpdatePath, "etag " + expectedEtag);
    }

    /**
     * Request the latest metadata version and perform assertions on the logs.
     *
     * @param expectedVersion expected Helidon version
     * @param expectUpdate    {@code true} if a metadata update is expected, {@code false otherwise}
     * @throws UpdateFailed if the metadata update failed
     */
    protected void firstLatestVersionRequest(String expectedVersion, boolean expectUpdate) throws UpdateFailed {
        String staleType = expectUpdate ? "(not found)" : "(zero delay)";
        latestVersion = meta.archetypesData().latestVersion();
        assertThat(latestVersion, is(not(nullValue())));
        if (expectedVersion != null) {
            assertThat(latestVersion, is(toMavenVersion(expectedVersion)));
        }
        assertLinesContainingAll(1, "stale check", staleType, LATEST_FILE_NAME);
        assertLinesContainingAll("Looking up latest Helidon version");
        assertNoLinesContainingAll("Updating metadata for Helidon version " + expectedVersion);
    }

    /**
     * Perform a catalog request and perform assertions on the logs.
     *
     * @param version      Helidon version
     * @param expectUpdate {@code true} if a metadata update is expected, {@code false otherwise}
     * @throws UpdateFailed if the metadata update failed
     */
    @SuppressWarnings("SameParameterValue")
    protected void catalogRequest(String version, boolean expectUpdate) throws UpdateFailed {
        String staleType = expectUpdate ? "(not found)" : "is false";
        String staleFilePath = version + File.separator + LAST_UPDATE_FILE_NAME;
        meta.catalogOf(version);
        assertLinesContainingAll(1, "stale check", staleType, staleFilePath);
        assertNoLinesContainingAll("Looking up latest Helidon version");
        if (expectUpdate) {
            assertLinesContainingAll("Updating metadata for Helidon version " + version);
        } else {
            assertNoLinesContainingAll("Updating metadata for Helidon version " + version);
        }
    }

    /**
     * Assert that there is at least one log line that contains all given fragments.
     *
     * @param fragments The fragments.
     * @throws AssertionError if there is no line that contains any of the fragments
     */
    public void assertLinesContainingAll(String... fragments) throws AssertionError {
        if (!atLeastOneLineContainingAll(fragments)) {
            throw new AssertionError(String.format(
                    "log should contain at least one line with all of the following: %s%n%s",
                    Arrays.toString(fragments), this));
        }
    }

    /**
     * Assert that there are no log lines that contain all given fragments.
     *
     * @param fragments The fragments.
     * @throws AssertionError if there is at least one line that contains any of the fragments
     */
    public void assertNoLinesContainingAll(String... fragments) throws AssertionError {
        if (atLeastOneLineContainingAll(fragments)) {
            throw new AssertionError(String.format(
                    "log should not contain any lines with all of the following: %s%n%s",
                    Arrays.toString(fragments), this));
        }
    }

    /**
     * Test whether there is at least one log line that contain all given fragments.
     *
     * @param fragments The fragments.
     * @return {@code true} if at least one line matches all.
     */
    public boolean atLeastOneLineContainingAll(String... fragments) {
        return countLinesContainingAll(fragments) > 0;
    }

    /**
     * Asserts the expected count of log lines contains all given fragments.
     *
     * @param expectedCount The expected count.
     * @param fragments The fragments.
     * @throws AssertionError if the count is different than {@code expectedCount}
     */
    public void assertLinesContainingAll(int expectedCount, String... fragments) throws AssertionError {
        final int count = countLinesContainingAll(fragments);
        if (count != expectedCount) {
            throw new AssertionError(String.format(
                    "log should contain %d lines with all of the following, found %d: %s%n%s",
                    expectedCount, count, Arrays.toString(fragments), this));
        }
    }

    /**
     * Returns the count of log lines contains all given fragments.
     *
     * @param fragments The fragments.
     * @return The count.
     */
    public int countLinesContainingAll(String... fragments) {
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
