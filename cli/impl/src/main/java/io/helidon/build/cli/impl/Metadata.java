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

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.cli.common.ArchetypesData;
import io.helidon.build.cli.common.ArchetypesDataLoader;
import io.helidon.build.common.ConfigProperties;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.Time;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.cli.impl.CommandRequirements.requireHelidonVersionDir;
import static io.helidon.build.common.FileUtils.lastModifiedTime;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.PrintStreams.DEVNULL;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
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

    /**
     * The minimum Helidon 3.x version.
     */
    public static final MavenVersion HELIDON_3 = toMavenVersion("3.0.0-alpha");

    private static final String VERSIONS_FILE_NAME = "versions.xml";
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

    private static final String LATEST_CLI_PLUGIN_VERSION_PROPERTY = "cli.latest.plugin.version";
    private static final String CLI_PLUGIN_VERSION_PROPERTY_PREFIX = "cli.";
    private static final String CLI_PLUGIN_VERSION_PROPERTY_SUFFIX = ".plugin.version";
    private static final String CLI_VERSION_PROPERTY = "cli.version";

    private final Path rootDir;
    private final String url;
    private final Path versionsFile;
    private final long updateFrequencyMillis;
    private final boolean debugPlugin;
    private final PrintStream pluginStdOut;
    private final Map<Path, Long> lastChecked;
    private final AtomicReference<Throwable> archetypesDataFailure;
    private final AtomicReference<ArchetypesData> archetypesData;

    private Metadata(Builder builder) {
        rootDir = builder.rootDir;
        url = builder.url;
        versionsFile = rootDir.resolve(VERSIONS_FILE_NAME);
        updateFrequencyMillis = builder.updateFrequencyUnits.toMillis(builder.updateFrequency);
        debugPlugin = builder.debugPlugin;
        pluginStdOut = builder.pluginStdOut;
        lastChecked = new HashMap<>();
        archetypesDataFailure = new AtomicReference<>();
        archetypesData = new AtomicReference<>();
    }

    /**
     * Returns the url.
     *
     * @return The url.
     */
    @SuppressWarnings("unused")
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
        if (versionsFile == null) {
            return FileTime.fromMillis(0);
        } else {
            return lastModifiedTime(versionsFile);
        }
    }

    /**
     * Returns the latest Helidon version.
     *
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion latestVersion() throws UpdateFailed {
        return archetypesData(false).latestVersion();
    }

    /**
     * Returns the latest Helidon version.
     *
     * @param quiet If info messages should be suppressed.
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion latestVersion(boolean quiet) throws UpdateFailed {
        return archetypesData(quiet).latestVersion();
    }

    /**
     * Returns information about archetypes versions.
     *
     * @return ArchetypesData
     * @throws UpdateFailed if the metadata update failed
     */
    ArchetypesData archetypesData() throws UpdateFailed  {
        return archetypesData(false);
    }

    /**
     * Returns information about archetypes versions.
     *
     * @param quiet If info messages should be suppressed
     * @return information about archetypes versions
     * @throws UpdateFailed if the metadata update failed
     */
    ArchetypesData archetypesData(boolean quiet) throws UpdateFailed {
        // If we fail, we only want to do so once per command, so we cache any failure
        Throwable initialFailure = archetypesDataFailure.get();
        if (initialFailure == null) {
            try {
                if (checkForUpdates(null, versionsFile, quiet)) {
                    archetypesData.set(readArchetypesData());
                } else if (archetypesData.get() == null) {
                    archetypesData.set(readArchetypesData());
                }
                return archetypesData.get();
            } catch (UpdateFailed | RuntimeException e) {
                archetypesDataFailure.set(e);
                throw e;
            }
        } else {
            if (initialFailure instanceof UpdateFailed) {
                throw (UpdateFailed) initialFailure;
            }
            throw (RuntimeException) initialFailure;
        }
    }

    /**
     * Asserts that the given Helidon version is available.
     *
     * @param helidonVersion The version.
     * @throws UpdateFailed if the metadata update failed.
     * @throws IllegalArgumentException if the version is not available.
     */
    public void assertVersionIsAvailable(MavenVersion helidonVersion) throws UpdateFailed {
        if (helidonVersion.isLessThan(HELIDON_3)) {
            versionedFile(helidonVersion, CATALOG_FILE_NAME, true);
        } else {
            archetypeV2Of(helidonVersion);
        }
    }

    /**
     * Returns the {@code helidon-cli-maven-plugin} version for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion cliPluginVersion(MavenVersion helidonVersion, boolean quiet) throws UpdateFailed {
        return cliPluginVersion(helidonVersion, toMavenVersion(Config.buildVersion()), quiet);
    }

    /**
     * Returns the {@code helidon-cli-maven-plugin} version for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param thisCliVersion This CLI version.
     * @param quiet If info messages should be suppressed.
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion cliPluginVersion(MavenVersion helidonVersion,
                                         MavenVersion thisCliVersion,
                                         boolean quiet) throws UpdateFailed {

        // Create a map from CLI version to CLI plugin versions, including latest

        final Map<MavenVersion, MavenVersion> cliToPluginVersions = new HashMap<>();
        final ConfigProperties properties = propertiesOf(helidonVersion, quiet);
        final MavenVersion latestPluginVersion = latestPluginVersion(helidonVersion, thisCliVersion, properties);
        cliToPluginVersions.put(latestPluginVersion, latestPluginVersion);
        properties.entrySet()
                  .stream()
                  .filter(e -> isCliPluginVersionKey(e.getKey()))
                  .forEach(e -> cliToPluginVersions.put(toCliPluginVersion(e.getKey()), toMavenVersion(e.getValue())));

        // Short circuit if there is only one

        if (cliToPluginVersions.size() == 1) {
            return latestPluginVersion;
        }

        // Find the maximum CLI version that is <= thisCliVersion

        final Optional<MavenVersion> maxCliVersion = cliToPluginVersions.keySet()
                                                                        .stream()
                                                                        .filter(v -> v.isLessThanOrEqualTo(thisCliVersion))
                                                                        .max(Comparator.naturalOrder());

        // Return the corresponding plugin version

        return cliToPluginVersions.get(maxCliVersion.orElseThrow());
    }

    /**
     * Returns the CLI version for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The properties.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion cliVersionOf(MavenVersion helidonVersion, boolean quiet) throws UpdateFailed {
        return toMavenVersion(requiredProperty(helidonVersion, CLI_VERSION_PROPERTY, quiet));
    }

    /**
     * Checks if there is a more recent CLI version available and returns the version if so.
     *
     * @param thisCliVersion The version of this CLI.
     * @param quiet If info messages should be suppressed.
     * @return A valid CLI version if a more recent CLI is available.
     * @throws UpdateFailed if the metadata update failed
     */
    public Optional<MavenVersion> checkForCliUpdate(MavenVersion thisCliVersion, boolean quiet) throws UpdateFailed {
        final MavenVersion latestHelidonVersion = archetypesData(quiet).latestVersion();
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
     * @throws UpdateFailed if the metadata update failed
     */
    public Map<MavenVersion, String> cliReleaseNotesOf(MavenVersion latestHelidonVersion,
                                                       MavenVersion sinceCliVersion) throws UpdateFailed {

        requireNonNull(latestHelidonVersion, "latestHelidonVersion must not be null");
        requireNonNull(sinceCliVersion, "sinceCliVersion must not be null");
        final ConfigProperties props = propertiesOf(latestHelidonVersion, true);
        final List<MavenVersion> versions = props.keySet()
                                                 .stream()
                                                 .filter(Metadata::isCliMessageKey)
                                                 .map(Metadata::versionOfCliMessageKey)
                                                 .map(MavenVersion::toMavenVersion)
                                                 .filter(v -> v.isGreaterThan(sinceCliVersion))
                                                 .sorted(Comparator.reverseOrder())
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
     * @throws UpdateFailed if the metadata update failed
     */
    public ConfigProperties propertiesOf(String helidonVersion) throws UpdateFailed {
        return propertiesOf(toMavenVersion(helidonVersion));
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The properties.
     * @throws UpdateFailed if the metadata update failed
     */
    public ConfigProperties propertiesOf(MavenVersion helidonVersion) throws UpdateFailed {
        return propertiesOf(helidonVersion, false);
    }

    /**
     * Returns the metadata properties for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The properties.
     * @throws UpdateFailed if the metadata update failed
     */
    public ConfigProperties propertiesOf(MavenVersion helidonVersion, boolean quiet) throws UpdateFailed {
        return new ConfigProperties(versionedFile(helidonVersion, METADATA_FILE_NAME, quiet));
    }

    /**
     * Returns the catalog for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The catalog.
     * @throws UpdateFailed if the metadata update failed
     */
    public ArchetypeCatalog catalogOf(String helidonVersion) throws UpdateFailed {
        return catalogOf(toMavenVersion(helidonVersion));
    }

    /**
     * Returns the catalog for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @return The catalog.
     * @throws UpdateFailed if the metadata update failed
     */
    public ArchetypeCatalog catalogOf(MavenVersion helidonVersion) throws UpdateFailed {
        try {
            return ArchetypeCatalog.read(versionedFile(helidonVersion, CATALOG_FILE_NAME, false));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Returns the path to the archetype V1 jar for the given catalog entry.
     *
     * @param catalogEntry The catalog entry.
     * @return The path to the archetype jar.
     * @throws UpdateFailed if the metadata update failed
     */
    public Path archetypeV1Of(ArchetypeCatalog.ArchetypeEntry catalogEntry) throws UpdateFailed {
        final MavenVersion helidonVersion = toMavenVersion(catalogEntry.version());
        final String fileName = catalogEntry.artifactId() + "-" + helidonVersion + JAR_SUFFIX;
        return versionedFile(helidonVersion, fileName, false);
    }

    /**
     * Returns the path to the archetype V2 jar for the given version.
     *
     * @param version The version.
     * @return The path to the archetype jar.
     * @throws UpdateFailed if the metadata update failed
     */
    public Path archetypeV2Of(String version) throws UpdateFailed {
        return archetypeV2Of(toMavenVersion(version));
    }

    /**
     * Returns the path to the archetype V2 jar for the given version.
     *
     * @param version The version.
     * @return The path to the archetype jar.
     * @throws UpdateFailed if the metadata update failed
     */
    public Path archetypeV2Of(MavenVersion version) throws UpdateFailed {
        final String fileName = "helidon-" + version + JAR_SUFFIX;
        return versionedFile(version, fileName, false);
    }

    @SuppressWarnings("SameParameterValue")
    private String requiredProperty(MavenVersion helidonVersion,
                                    String propertyName,
                                    boolean quiet) throws UpdateFailed {

        ConfigProperties properties = propertiesOf(helidonVersion, quiet);
        return Requirements.requireNonNull(properties.property(propertyName), "missing " + propertyName);
    }

    private static MavenVersion latestPluginVersion(MavenVersion helidonVersion,
                                                    MavenVersion thisCliVersion,
                                                    ConfigProperties properties) {
        final String latest = properties.property(LATEST_CLI_PLUGIN_VERSION_PROPERTY);
        if (latest == null) {
            Log.debug("Helidon version %s does not contain %s, using current CLI version %s", helidonVersion,
                      LATEST_CLI_PLUGIN_VERSION_PROPERTY, thisCliVersion);
            return thisCliVersion;
        } else {
            return toMavenVersion(latest);
        }
    }

    private static boolean isCliPluginVersionKey(String key) {
        return key.startsWith(CLI_PLUGIN_VERSION_PROPERTY_PREFIX)
               && key.endsWith(CLI_PLUGIN_VERSION_PROPERTY_SUFFIX)
               && !key.equals(LATEST_CLI_PLUGIN_VERSION_PROPERTY);
    }

    private static MavenVersion toCliPluginVersion(String key) {
        final int start = CLI_PLUGIN_VERSION_PROPERTY_PREFIX.length();
        final int end = key.length() - CLI_PLUGIN_VERSION_PROPERTY_SUFFIX.length();
        return toMavenVersion(key.substring(start, end));
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
    private Path versionedFile(MavenVersion helidonVersion, String fileName, boolean quiet) throws UpdateFailed {
        final Path versionDir = rootDir.resolve(requireNonNull(helidonVersion).toString());
        final Path checkFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        checkForUpdates(helidonVersion, checkFile, quiet);
        return requireFile(requireHelidonVersionDir(versionDir).resolve(fileName));
    }

    private boolean checkForUpdates(MavenVersion helidonVersion, Path checkFile, boolean quiet) throws UpdateFailed {
        return checkForUpdates(helidonVersion, checkFile, System.currentTimeMillis(), quiet);
    }

    boolean checkForUpdates(MavenVersion helidonVersion,
                            Path checkFile,
                            long currentTimeMillis,
                            boolean quiet) throws UpdateFailed {

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
                    if (LogLevel.isDebug()) {
                        final String lastModifiedTime = Time.toDateTime(lastModifiedMillis);
                        final String currentTime = Time.currentDateTime();
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

    private void update(MavenVersion helidonVersion, boolean quiet) throws UpdateFailed {
        final boolean logInfo = LogLevel.isDebug() || !quiet;
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
        PrintStream stdOut = pluginStdOut;
        if (debugPlugin) {
            args.add("--debug");
            if (stdOut == null) {
                stdOut = PrintStreams.apply(STDOUT, LogFormatter.of(LogLevel.INFO));
            }
        } else if (stdOut == null) {
            stdOut = DEVNULL;
        }
        try {
            Plugins.execute(PLUGIN_NAME, args, PLUGIN_MAX_WAIT_SECONDS, stdOut);
        } catch (Plugins.PluginFailed e) {
            throw new UpdateFailed(e);
        }
    }

    private ArchetypesData readArchetypesData() {
        return ArchetypesDataLoader.load(versionsFile);
    }

    /**
     * Update failed checked exception.
     * This is a checked exception by design to ensure a proper error handling.
     */
    public static class UpdateFailed extends Exception {
        private UpdateFailed(Plugins.PluginFailed ex) {
            super(ex.getMessage(), ex);
        }
    }

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link Metadata} builder.
     */
    public static class Builder {

        private Path rootDir = Config.userConfig().cacheDir();
        private String url = DEFAULT_URL;
        private long updateFrequency = DEFAULT_UPDATE_FREQUENCY;
        private boolean debugPlugin;
        private PrintStream pluginStdOut;
        private TimeUnit updateFrequencyUnits = DEFAULT_UPDATE_FREQUENCY_UNITS;

        /**
         * Create a new builder.
         */
        protected Builder() {
        }

        /**
         * Returns a new instance.
         *
         * @param rootDir The root directory.
         * @return this builder
         */
        public Builder rootDir(Path rootDir) {
            this.rootDir = rootDir;
            return this;
        }

        /**
         * Returns a new instance.
         *
         * @param url The url.
         * @return this builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Returns a new instance.
         *
         * @param updateFrequencyUnits The update frequency units.
         * @return this builder
         */
        public Builder updateFrequencyUnits(TimeUnit updateFrequencyUnits) {
            this.updateFrequencyUnits = updateFrequencyUnits;
            return this;
        }

        /**
         * Returns a new instance.
         *
         * @param updateFrequency The update frequency.
         * @return this builder
         */
        public Builder updateFrequency(long updateFrequency) {
            this.updateFrequency = updateFrequency;
            return this;
        }

        /**
         * Returns a new instance.
         *
         * @param debugPlugin {@code true} if should enable debug logging in plugin.
         * @return this builder
         */
        public Builder debugPlugin(boolean debugPlugin) {
            this.debugPlugin = debugPlugin;
            return this;
        }

        /**
         * Sets the print stream to consume the output from the plugin.
         *
         * @param pluginStdOut print stream
         * @return this builder
         */
        public Builder pluginStdOut(PrintStream pluginStdOut) {
            this.pluginStdOut = pluginStdOut;
            return this;
        }

        /**
         * Builder the metadata instance.
         *
         * @return Metadata
         */
        public Metadata build() {
            return new Metadata(this);
        }
    }
}
