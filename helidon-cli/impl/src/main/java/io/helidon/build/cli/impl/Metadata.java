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
    private static final String BASE_URL = "https://helidon.io/cli-data";
    private static final String LATEST_VERSION_FILE_NAME = "latest";
    private static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    private static final String METADATA_FILE_NAME = "metadata.properties";
    private static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    private static final String PLUGIN_NAME = "UpdateMetadata";
    private static final int MAX_WAIT_SECONDS = 30;
    private static final int DEFAULT_HOURS_BETWEEN_UPDATES = 24;

    private final Path rootDir;
    private final String baseUrl;
    private final Path latestVersionFile;
    private final int hoursBetweenUpdates;
    private final AtomicReference<MavenVersion> latestVersion;

    /**
     * Returns a new instance with default configuration.
     *
     * @return The instance.
     */
    public static Metadata newInstance() {
        return newInstance(Config.userConfig().cacheDir(), BASE_URL, DEFAULT_HOURS_BETWEEN_UPDATES);
    }

    /**
     * Returns a new instance.
     *
     * @param rootDir The root directory.
     * @param baseUrl The base url.
     * @param hoursBetweenUpdates The number of hours between updates.
     * @return The instance.
     */
    public static Metadata newInstance(Path rootDir, String baseUrl, int hoursBetweenUpdates) {
        return new Metadata(rootDir, baseUrl, hoursBetweenUpdates);
    }

    private Metadata(Path rootDir, String baseUrl, int hoursBetweenUpdates) {
        this.rootDir = rootDir;
        this.baseUrl = baseUrl;
        this.latestVersionFile = rootDir.resolve(LATEST_VERSION_FILE_NAME);
        this.hoursBetweenUpdates = hoursBetweenUpdates;
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
    public ArchetypeCatalog catalog(MavenVersion version) throws Exception {
        return ArchetypeCatalog.read(versionedFile(version, METADATA_FILE_NAME));
    }

    @SuppressWarnings("ConstantConditions")
    private Path versionedFile(MavenVersion version, String file) throws Exception {
        final Path versionDir = rootDir.resolve(requireNonNull(version).toString());
        final Path checkFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        checkForUpdates(version, checkFile);
        return assertFile(requireHelidonVersionDir(versionDir).resolve(file));
    }

    boolean checkForUpdates(MavenVersion version, Path checkFile) throws Exception {
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

    private void update(MavenVersion version) throws Exception {
        final List<String> args = new ArrayList<>();
        args.add("--debug"); // TODO REMOVE
        args.add("--baseUrl");
        args.add(baseUrl);
        args.add("--cacheDir");
        args.add(rootDir.toAbsolutePath().toString());
        if (version == null) {
            Log.info("Looking up latest version");
        } else {
            Log.info("Updating metadata for version %s", version);
            args.add("--version");
            args.add(version.toString());
        }
        Plugins.execute(PLUGIN_NAME, args, MAX_WAIT_SECONDS);
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


    boolean isStale(Path file, long currentTimeMillis) {
        if (Files.exists(file)) {
            if (hoursBetweenUpdates > 0) {
                final FileTime lastModified = FileUtils.lastModifiedTime(file);
                final FileTime current = FileTime.fromMillis(currentTimeMillis);
                final long currentHours = current.to(TimeUnit.HOURS);
                final long lastCheckedHours = lastModified.to(TimeUnit.HOURS);
                final long delta = currentHours - lastCheckedHours;
                final boolean stale = delta > hoursBetweenUpdates;
                Log.debug("%s stale: %s", file, stale);
                return stale;
            } else {
                Log.debug("%s forced stale: zero delay", file);
                return true;
            }
        } else {
            Log.debug("%s not found", file);
            return true;
        }
    }
}
