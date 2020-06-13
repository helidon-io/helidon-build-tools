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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;

import static io.helidon.build.cli.impl.CommandRequirements.requireHelidonVersionDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * CLI metadata access.
 */
public class Metadata {
    private static final String DEFAULT_BASE_URL = "https://helidon.io/cli-data";
    private static final String LATEST_VERSION_FILE_NAME = "latest";
    private static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    private static final String METADATA_FILE_NAME = "metadata.properties";
    private static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    private static final String PLUGIN_NAME = "UpdateMetadata";
    private static final String JAR_SUFFIX = ".jar";
    private static final TimeUnit DEFAULT_UPDATE_DELAY_UNITS = TimeUnit.HOURS;
    private static final long DEFAULT_UPDATE_FREQUENCY = 24;
    private static final int PLUGIN_MAX_WAIT_SECONDS = 30;

    private final Path rootDir;
    private final String baseUrl;
    private final Path latestVersionFile;
    private final long updateFrequencyMillis;
    private final boolean debugPlugin;
    private final AtomicReference<MavenVersion> latestVersion;

    /**
     * Returns a new instance with default configuration.
     *
     * @return The instance.
     */
    public static Metadata newInstance() {
        final Path cacheDir = Config.userConfig().cacheDir();
        final boolean debug = Log.isDebug();
        return newInstance(cacheDir, DEFAULT_BASE_URL, DEFAULT_UPDATE_FREQUENCY, DEFAULT_UPDATE_DELAY_UNITS, debug);
    }

    /**
     * Returns a new instance.
     *
     * @param rootDir The root directory.
     * @param baseUrl The base url.
     * @param updateFrequency The update frequency.
     * @param updateFrequencyUnits The update frequency units.
     * @param debugPlugin {@code true} if should enable debug logging in plugin.
     * @return The instance.
     */
    public static Metadata newInstance(Path rootDir,
                                       String baseUrl,
                                       long updateFrequency,
                                       TimeUnit updateFrequencyUnits,
                                       boolean debugPlugin) {
        return new Metadata(rootDir, baseUrl, updateFrequency, updateFrequencyUnits, debugPlugin);
    }

    private Metadata(Path rootDir,
                     String baseUrl,
                     long updateFrequency,
                     TimeUnit updateFrequencyUnits,
                     boolean debugPlugin) {
        this.rootDir = rootDir;
        this.baseUrl = baseUrl;
        this.latestVersionFile = rootDir.resolve(LATEST_VERSION_FILE_NAME);
        this.updateFrequencyMillis = updateFrequencyUnits.toMillis(updateFrequency);
        this.debugPlugin = debugPlugin;
        this.latestVersion = new AtomicReference<>();
    }

    /**
     * Returns the latest Helidon version.
     *
     * @return The version.
     * @throws Exception If an error occurs.
     */
    public MavenVersion latestVersion() throws Exception {
        if (checkForUpdates(null, latestVersionFile)) {
            latestVersion.set(readLatestVersion());
        }
        return latestVersion.get();
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param version The version.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public ConfigProperties properties(String version) throws Exception {
        return properties(toMavenVersion(version));
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param version The version.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public ConfigProperties properties(MavenVersion version) throws Exception {
        return new ConfigProperties(versionedFile(version, METADATA_FILE_NAME));
    }

    /**
     * Returns the catalog for the given Helidon version.
     *
     * @param version The version.
     * @return The catalog.
     * @throws Exception If an error occurs.
     */
    public ArchetypeCatalog catalog(String version) throws Exception {
        return catalog(toMavenVersion(version));
    }

    /**
     * Returns the catalog for the given Helidon version.
     *
     * @param version The version.
     * @return The catalog.
     * @throws Exception If an error occurs.
     */
    public ArchetypeCatalog catalog(MavenVersion version) throws Exception {
        return ArchetypeCatalog.read(versionedFile(version, CATALOG_FILE_NAME));
    }

    /**
     * Returns the path to the archetype jar for the given catalog entry.
     *
     * @param catalogEntry The catalog entry.
     * @return The path to the archetype jar.
     * @throws Exception If an error occurs.
     */
    public Path archetype(ArchetypeCatalog.ArchetypeEntry catalogEntry) throws Exception {
        final MavenVersion version = toMavenVersion(catalogEntry.version());
        final String fileName = catalogEntry.artifactId() + "-" + version + JAR_SUFFIX;
        return versionedFile(version, fileName);
    }

    @SuppressWarnings("ConstantConditions")
    private Path versionedFile(MavenVersion version, String fileName) throws Exception {
        final Path versionDir = rootDir.resolve(requireNonNull(version).toString());
        final Path checkFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        checkForUpdates(version, checkFile);
        return assertFile(requireHelidonVersionDir(versionDir).resolve(fileName));
    }

    private boolean checkForUpdates(MavenVersion version, Path checkFile) throws Exception {
        return checkForUpdates(version, checkFile, System.currentTimeMillis());
    }

    boolean checkForUpdates(MavenVersion version, Path checkFile, long currentTimeMillis) throws Exception {
        if (isStale(checkFile, currentTimeMillis)) {
            update(version);
            return true;
        } else {
            return false;
        }
    }

    private boolean isStale(Path file, long currentTimeMillis) {
        if (Files.exists(file)) {
            if (updateFrequencyMillis > 0) {
                final FileTime lastModified = FileUtils.lastModifiedTime(file);
                final FileTime current = FileTime.fromMillis(currentTimeMillis);
                final long currentMillis = current.to(TimeUnit.MILLISECONDS);
                final long lastCheckedMillis = lastModified.to(TimeUnit.MILLISECONDS);
                final long delta = currentMillis - lastCheckedMillis;
                final boolean stale = delta > updateFrequencyMillis;
                final Duration elapsed = Duration.ofMillis(delta);
                Log.debug("stale check (%d days, %d hours, %d minutes %d seconds) is %s for %s",
                        elapsed.toDaysPart(), elapsed.toHoursPart(), elapsed.toMinutesPart(), elapsed.toSecondsPart(),
                        stale, file);
                return stale;
            } else {
                Log.debug("stale check forced (zero delay) for %s", file);
                return true;
            }
        } else {
            Log.debug("stale check forced (not found) for %s", file);
            return true;
        }
    }

    private void update(MavenVersion version) throws Exception {
        final List<String> args = new ArrayList<>();
        args.add("--baseUrl");
        args.add(baseUrl);
        args.add("--cacheDir");
        args.add(rootDir.toAbsolutePath().toString());
        if (version == null) {
            Log.info("Looking up latest Helidon version");
        } else {
            Log.info("Updating metadata for Helidon version %s", version);
            args.add("--version");
            args.add(version.toString());
        }
        if (debugPlugin) {
            args.add("--debug");
        }
        Plugins.execute(PLUGIN_NAME, args, PLUGIN_MAX_WAIT_SECONDS);
    }

    private MavenVersion readLatestVersion() throws Exception {
        final List<String> lines = Files.readAllLines(latestVersionFile, UTF_8);
        for (String line : lines) {
            if (!line.isEmpty()) {
                return toMavenVersion(line.trim());
            }
        }
        throw new IllegalStateException("No version in " + latestVersionFile);
    }

    private void info(String message, Object... args) {
        Log.info(message, args);
    }

    private void debug(String message, Object... args) {
        Log.debug(message, args);
    }
}
