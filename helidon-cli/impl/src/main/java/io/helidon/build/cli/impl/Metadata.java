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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.TimeUtils;

import static io.helidon.build.cli.impl.CommandRequirements.requireHelidonVersionDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * CLI metadata access.
 */
public class Metadata {
    /**
     * The default url.
     */
    public static final String DEFAULT_URL = "https://helidon.io/cli-data";

    /**
     * The default update frequency.
     */
    public static final long DEFAULT_UPDATE_FREQUENCY = 12;

    /**
     * The default update frequency units.
     */
    public static final TimeUnit DEFAULT_UPDATE_FREQUENCY_UNITS = TimeUnit.HOURS;

    private static final String LATEST_VERSION_FILE_NAME = "latest";
    private static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    private static final String METADATA_FILE_NAME = "metadata.properties";
    private static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    private static final String PLUGIN_NAME = "UpdateMetadata";
    private static final String JAR_SUFFIX = ".jar";
    private static final int PLUGIN_MAX_WAIT_SECONDS = 30;
    private static final int PLUGIN_MAX_ATTEMPTS = 3;
    private static final String CLI_MESSAGE_PREFIX = "cli.";
    private static final String CLI_MESSAGE_SUFFIX = ".message";
    private static final long STALE_RETRY_THRESHOLD = 1000;

    /**
     * The build tools version property name.
     */
    public static final String BUILD_TOOLS_VERSION_PROPERTY = "build-tools.version";

    /**
     * The CLI version property name.
     */
    public static final String CLI_VERSION_PROPERTY = "cli.version";

    private final Path rootDir;
    private final String url;
    private final Path latestVersionFile;
    private final long updateFrequencyMillis;
    private final boolean debugPlugin;
    private final Map<Path, Long> lastChecked;
    private final AtomicReference<Exception> latestVersionFailure;
    private final AtomicReference<MavenVersion> latestVersion;

    /**
     * Returns a new instance with default configuration.
     *
     * @return The instance.
     */
    public static Metadata newInstance() {
        return newInstance(DEFAULT_URL);
    }

    /**
     * Returns a new instance with the given url and default configuration.
     *
     * @param url The url.
     * @return The instance.
     */
    public static Metadata newInstance(String url) {
        return newInstance(url, DEFAULT_UPDATE_FREQUENCY);
    }

    /**
     * Returns a new instance with the given url and default configuration.
     *
     * @param url The url.
     * @param updateFrequencyHours The update frequency, in hours.
     * @return The instance.
     */
    public static Metadata newInstance(String url, long updateFrequencyHours) {
        final Path cacheDir = Config.userConfig().cacheDir();
        final boolean debug = Log.isDebug();
        return newInstance(cacheDir, url, updateFrequencyHours, DEFAULT_UPDATE_FREQUENCY_UNITS, debug);
    }

    /**
     * Returns a new instance.
     *
     * @param rootDir The root directory.
     * @param url The url.
     * @param updateFrequency The update frequency.
     * @param updateFrequencyUnits The update frequency units.
     * @param debugPlugin {@code true} if should enable debug logging in plugin.
     * @return The instance.
     */
    public static Metadata newInstance(Path rootDir,
                                       String url,
                                       long updateFrequency,
                                       TimeUnit updateFrequencyUnits,
                                       boolean debugPlugin) {
        return new Metadata(rootDir, url, updateFrequency, updateFrequencyUnits, debugPlugin);
    }

    private Metadata(Path rootDir,
                     String url,
                     long updateFrequency,
                     TimeUnit updateFrequencyUnits,
                     boolean debugPlugin) {
        this.rootDir = rootDir;
        this.url = url;
        this.latestVersionFile = rootDir.resolve(LATEST_VERSION_FILE_NAME);
        this.updateFrequencyMillis = updateFrequencyUnits.toMillis(updateFrequency);
        this.debugPlugin = debugPlugin;
        this.lastChecked = new HashMap<>();
        this.latestVersionFailure = new AtomicReference<>();
        this.latestVersion = new AtomicReference<>();
    }

    /**
     * Returns the url.
     *
     * @return The url.
     */
    public String url() {
        return url;
    }

    /**
     * Returns the url.
     *
     * @return The url.
     */
    public Path rootDir() {
        return rootDir;
    }

    /**
     * Returns the last time that an update occurred.
     *
     * @return The time.
     */
    public FileTime lastUpdateTime() {
        if (latestVersionFile == null) {
            return FileTime.fromMillis(0);
        } else {
            return lastModifiedTime(latestVersionFile);
        }
    }

    /**
     * Returns the latest Helidon version.
     *
     * @return The version.
     * @throws Exception If an error occurs.
     */
    public MavenVersion latestVersion() throws Exception {
        return latestVersion(false);
    }

    /**
     * Returns the latest Helidon version.
     *
     * @param quiet If info messages should be suppressed.
     * @return The version.
     * @throws Exception If an error occurs.
     */
    public MavenVersion latestVersion(boolean quiet) throws Exception {
        // If we fail, we only want to do so once per command, so we cache any failure
        Exception initialFailure = latestVersionFailure.get();
        if (initialFailure == null) {
            try {
                if (checkForUpdates(null, latestVersionFile, quiet)) {
                    latestVersion.set(readLatestVersion());
                } else if (latestVersion.get() == null) {
                    latestVersion.set(readLatestVersion());
                }
                return latestVersion.get();
            } catch (Exception e) {
                latestVersionFailure.set(e);
                throw e;
            }
        } else {
            throw initialFailure;
        }
    }

    /**
     * Returns the build tools version for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public MavenVersion buildToolsVersionOf(MavenVersion helidonVersion, boolean quiet) throws Exception {
        return toMavenVersion(requiredProperty(helidonVersion, BUILD_TOOLS_VERSION_PROPERTY, quiet));
    }

    /**
     * Returns the CLI version for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public MavenVersion cliVersionOf(MavenVersion helidonVersion, boolean quiet) throws Exception {
        return toMavenVersion(requiredProperty(helidonVersion, CLI_VERSION_PROPERTY, quiet));
    }

    /**
     * Checks whether or not there is a more recent CLI version available and returns the version if so.
     *
     * @param thisCliVersion The version of this CLI.
     * @param quiet If info messages should be suppressed.
     * @return A valid CLI version if a more recent CLI is available.
     * @throws Exception If an error occurs.
     */
    public Optional<MavenVersion> checkForCliUpdate(MavenVersion thisCliVersion, boolean quiet) throws Exception {
        final MavenVersion latestHelidonVersion = latestVersion(quiet);
        final MavenVersion latestCliVersion = cliVersionOf(latestHelidonVersion, quiet);
        if (latestCliVersion.isGreaterThan(thisCliVersion)) {
            return Optional.of(latestCliVersion);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the release notes for the latest Helidon version that are more recent than the given CLI version.
     *
     * @param latestHelidonVersion The latest Helidon version.
     * @param sinceCliVersion The CLI version to start with.
     * @return The notes, in sorted order.
     * @throws Exception If an error occurs.
     */
    public Map<MavenVersion, String> cliReleaseNotesOf(MavenVersion latestHelidonVersion,
                                                       MavenVersion sinceCliVersion) throws Exception {
        requireNonNull(latestHelidonVersion, "latestHelidonVersion must not be null");
        requireNonNull(sinceCliVersion, "sinceCliVersion must not be null");
        final ConfigProperties props = propertiesOf(latestHelidonVersion, true);
        final List<MavenVersion> versions = props.keySet()
                                                 .stream()
                                                 .filter(Metadata::isCliMessageKey)
                                                 .map(Metadata::versionOfCliMessageKey)
                                                 .map(MavenVersion::toMavenVersion)
                                                 .filter(v -> v.isGreaterThan(sinceCliVersion))
                                                 .sorted()
                                                 .collect(Collectors.toList());
        final Map<MavenVersion, String> result = new LinkedHashMap<>();
        versions.forEach(v -> result.put(v, props.property(toCliMessageKey(v))));
        return result;
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public ConfigProperties propertiesOf(String helidonVersion) throws Exception {
        return propertiesOf(toMavenVersion(helidonVersion));
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public ConfigProperties propertiesOf(MavenVersion helidonVersion) throws Exception {
        return propertiesOf(helidonVersion, false);
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The properties.
     * @throws Exception If an error occurs.
     */
    public ConfigProperties propertiesOf(MavenVersion helidonVersion, boolean quiet) throws Exception {
        return new ConfigProperties(versionedFile(helidonVersion, METADATA_FILE_NAME, quiet));
    }

    /**
     * Returns the catalog for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The catalog.
     * @throws Exception If an error occurs.
     */
    public ArchetypeCatalog catalogOf(String helidonVersion) throws Exception {
        return catalogOf(toMavenVersion(helidonVersion));
    }

    /**
     * Returns the catalog for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The catalog.
     * @throws Exception If an error occurs.
     */
    public ArchetypeCatalog catalogOf(MavenVersion helidonVersion) throws Exception {
        return ArchetypeCatalog.read(versionedFile(helidonVersion, CATALOG_FILE_NAME, false));
    }

    /**
     * Returns the path to the archetype jar for the given catalog entry.
     *
     * @param catalogEntry The catalog entry.
     * @return The path to the archetype jar.
     * @throws Exception If an error occurs.
     */
    public Path archetypeOf(ArchetypeCatalog.ArchetypeEntry catalogEntry) throws Exception {
        final MavenVersion helidonVersion = toMavenVersion(catalogEntry.version());
        final String fileName = catalogEntry.artifactId() + "-" + helidonVersion + JAR_SUFFIX;
        return versionedFile(helidonVersion, fileName, false);
    }

    private String requiredProperty(MavenVersion helidonVersion, String propertyName, boolean quiet) throws Exception {
        return requireNonNull(propertiesOf(helidonVersion, quiet).property(propertyName), "missing " + propertyName);
    }

    private static boolean isCliMessageKey(String key) {
        return key.startsWith(CLI_MESSAGE_PREFIX) && key.endsWith(CLI_MESSAGE_SUFFIX);
    }

    private static String versionOfCliMessageKey(String key) {
        return key.substring(CLI_MESSAGE_PREFIX.length(), key.length() - CLI_MESSAGE_SUFFIX.length());
    }

    private static String toCliMessageKey(MavenVersion version) {
        return CLI_MESSAGE_PREFIX + version.toString() + CLI_MESSAGE_SUFFIX;
    }

    @SuppressWarnings("ConstantConditions")
    private Path versionedFile(MavenVersion helidonVersion, String fileName, boolean quiet) throws Exception {
        final Path versionDir = rootDir.resolve(requireNonNull(helidonVersion).toString());
        final Path checkFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        checkForUpdates(helidonVersion, checkFile, quiet);
        return assertFile(requireHelidonVersionDir(versionDir).resolve(fileName));
    }

    private boolean checkForUpdates(MavenVersion helidonVersion, Path checkFile, boolean quiet) throws Exception {
        return checkForUpdates(helidonVersion, checkFile, System.currentTimeMillis(), quiet);
    }

    boolean checkForUpdates(MavenVersion helidonVersion, Path checkFile, long currentTimeMillis, boolean quiet) throws Exception {
        if (isStale(checkFile, currentTimeMillis)) {
            update(helidonVersion, quiet);
            return true;
        } else {
            return false;
        }
    }

    private boolean isStale(Path file, long currentTimeMillis) {

        // During a single command execution, we may get back here multiple times for the same
        // file within a few milliseconds; if so, we just assume it is not stale to avoid hitting
        // the disk again

        final long sinceLast = currentTimeMillis - lastChecked.getOrDefault(file, 0L);
        if (sinceLast > STALE_RETRY_THRESHOLD) {
            lastChecked.put(file, currentTimeMillis);
            if (Files.exists(file)) {
                if (updateFrequencyMillis < 0) {
                    Log.debug("stale check is false (disabled) for %s", file);
                    return false;
                } else if (updateFrequencyMillis > 0) {
                    final long lastModifiedMillis = lastModifiedTime(file).to(MILLISECONDS);
                    final long elapsedMillis = currentTimeMillis - lastModifiedMillis;
                    final long remainingMillis = updateFrequencyMillis - elapsedMillis;
                    final boolean stale = remainingMillis <= 0;
                    if (Log.isDebug()) {
                        final String lastModifiedTime = TimeUtils.toDateTime(lastModifiedMillis);
                        final String currentTime = TimeUtils.currentDateTime();
                        final Duration elapsed = Duration.ofMillis(elapsedMillis);
                        final String elapsedDays = elapsed.toDaysPart() == 1 ? "day" : "days";
                        if (stale) {
                            Log.debug("stale check is true for %s (last: %s, now: %s, elapsed: %d %s %02d:%02d:%02d)",
                                      file, lastModifiedTime, currentTime,
                                      elapsed.toDaysPart(), elapsedDays, elapsed.toHoursPart(), elapsed.toMinutesPart(),
                                      elapsed.toSecondsPart());
                        } else {
                            final Duration remain = Duration.ofMillis(remainingMillis);
                            final String remainDays = remain.toDaysPart() == 1 ? "day" : "days";
                            Log.debug("stale check is false for %s (last: %s, now: %s, elapsed: %d %s %02d:%02d:%02d, "
                                      + "remain: %d %s %02d:%02d:%02d)",
                                      file, lastModifiedTime, currentTime,
                                      elapsed.toDaysPart(), elapsedDays, elapsed.toHoursPart(), elapsed.toMinutesPart(),
                                      elapsed.toSecondsPart(),
                                      remain.toDaysPart(), remainDays, remain.toHoursPart(), remain.toMinutesPart(),
                                      remain.toSecondsPart());
                        }
                    }
                    return stale;
                } else {
                    Log.debug("stale check forced (zero delay) for %s", file);
                    return true;
                }
            } else {
                Log.debug("stale check forced (not found) for %s", file);
                return true;
            }
        } else {
            Log.debug("stale check is false (retry) for %s", file);
            return false;
        }
    }

    private void update(MavenVersion helidonVersion, boolean quiet) throws Exception {
        final boolean logInfo = Log.isDebug() || !quiet;
        final int maxAttempts = quiet ? 1 : PLUGIN_MAX_ATTEMPTS;
        final List<String> args = new ArrayList<>();
        args.add("--baseUrl");
        args.add(url);
        args.add("--cacheDir");
        args.add(rootDir.toAbsolutePath().toString());
        if (helidonVersion == null) {
            if (logInfo) {
                Log.info("Looking up latest Helidon version");
            }
        } else {
            if (logInfo) {
                Log.info("Updating metadata for Helidon version %s", helidonVersion);
            }
            args.add("--version");
            args.add(helidonVersion.toString());
        }
        args.add("--cliVersion");
        args.add(Config.buildVersion());
        args.add("--maxAttempts");
        args.add(Integer.toString(maxAttempts));
        if (debugPlugin) {
            args.add("--debug");
            Plugins.execute(PLUGIN_NAME, args, PLUGIN_MAX_WAIT_SECONDS, Metadata::info);
        } else {
            Plugins.execute(PLUGIN_NAME, args, PLUGIN_MAX_WAIT_SECONDS);
        }
    }

    private static void info(String line) {
        Log.info(line);
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
}
