/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.io.PrintStream;
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
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.cli.common.ArchetypesData;
import io.helidon.build.common.ConfigProperties;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.Time;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.xml.XMLElement;

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
     * The default time source.
     */
    public static final LongSupplier DEFAULT_TIME_SOURCE = System::currentTimeMillis;

    /**
     * The minimum Helidon 3.x version.
     */
    public static final MavenVersion HELIDON_3 = toMavenVersion("3.0.0-alpha");

    private final Path rootDir;
    private final String url;
    private final Path versionsFile;
    private final long updateFrequencyMillis;
    private final boolean debugPlugin;
    private final PrintStream pluginStdOut;
    private final LongSupplier timeSource;
    private final Map<Path, Long> lastChecked;
    private final AtomicReference<Throwable> archetypesDataFailure;
    private final AtomicReference<ArchetypesData> archetypesData;

    private Metadata(Builder builder) {
        rootDir = builder.rootDir;
        url = builder.url;
        versionsFile = rootDir.resolve("versions.xml");
        updateFrequencyMillis = builder.updateFrequencyUnits.toMillis(builder.updateFrequency);
        debugPlugin = builder.debugPlugin;
        pluginStdOut = builder.pluginStdOut;
        timeSource = builder.timeSource;
        lastChecked = new HashMap<>();
        archetypesDataFailure = new AtomicReference<>();
        archetypesData = new AtomicReference<>();
    }

    /**
     * Returns metadata root directory.
     *
     * @return The root directory
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
        if (!Files.exists(versionsFile)) {
            return FileTime.fromMillis(0);
        } else {
            return lastModifiedTime(versionsFile);
        }
    }

    /**
     * Returns the default Helidon version.
     *
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion defaultVersion() throws UpdateFailed {
        return defaultVersion(false);
    }

    /**
     * Returns the default Helidon version.
     *
     * @param quiet If info messages should be suppressed.
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion defaultVersion(boolean quiet) throws UpdateFailed {
        return archetypesData(quiet).defaultVersion();
    }

    /**
     * Returns the latest Helidon version.
     *
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion latestVersion() throws UpdateFailed {
        return latestVersion(false);
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
                update(null, versionsFile, quiet);
                if (archetypesData.get() == null) {
                    archetypesData.set(ArchetypesData.load(versionsFile));
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
     * Returns the {@code helidon-cli-maven-plugin} version for the given Helidon version.
     *
     * @param helidonVersion The version.
     * @param quiet If info messages should be suppressed.
     * @return The version.
     * @throws UpdateFailed if the metadata update failed
     */
    public MavenVersion cliPluginVersion(MavenVersion helidonVersion, boolean quiet) throws UpdateFailed {
        return cliPluginVersion(helidonVersion, toMavenVersion(BuildProperties.buildVersion()), quiet);
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
        Map<MavenVersion, MavenVersion> cliToPluginVersions = new HashMap<>();
        ConfigProperties properties = propertiesOf(helidonVersion, quiet);
        MavenVersion latestPluginVersion = latestPluginVersion(helidonVersion, thisCliVersion, properties);
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
        Optional<MavenVersion> maxCliVersion = cliToPluginVersions.keySet()
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
        ConfigProperties props = propertiesOf(helidonVersion, quiet);
        return toMavenVersion(Requirements.requireNonNull(props.property("cli.version"), "missing cli.version"));
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
        MavenVersion defaultHelidonVersion = archetypesData(quiet).defaultVersion();
        MavenVersion latestCliVersion = cliVersionOf(defaultHelidonVersion, quiet);
        if (latestCliVersion.isGreaterThan(thisCliVersion)) {
            return Optional.of(latestCliVersion);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the release notes that are more recent than the given CLI version.
     *
     * @param helidonVersion The Helidon version.
     * @param sinceCliVersion The CLI version to start with.
     * @return The notes, in sorted order.
     * @throws UpdateFailed if the metadata update failed
     */
    public Map<MavenVersion, String> cliReleaseNotesOf(MavenVersion helidonVersion,
                                                       MavenVersion sinceCliVersion) throws UpdateFailed {

        requireNonNull(helidonVersion, "helidonVersion must not be null");
        requireNonNull(sinceCliVersion, "sinceCliVersion must not be null");
        ConfigProperties props = propertiesOf(helidonVersion, true);
        List<MavenVersion> versions = props.keySet()
                .stream()
                .filter(Metadata::isCliMessageKey)
                .map(Metadata::versionOfCliMessageKey)
                .map(MavenVersion::toMavenVersion)
                .filter(v -> v.isGreaterThan(sinceCliVersion))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        Map<MavenVersion, String> result = new LinkedHashMap<>();
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
        return new ConfigProperties(versionedFile(helidonVersion, "metadata.properties", quiet));
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
        Path catalogFile = versionedFile(helidonVersion, "archetype-catalog.xml", false);
        return ArchetypeCatalog.create(XMLElement.read(catalogFile));
    }

    /**
     * Returns the path to the archetype V1 jar for the given catalog entry.
     *
     * @param catalogEntry The catalog entry.
     * @return The path to the archetype jar.
     * @throws UpdateFailed if the metadata update failed
     */
    public Path archetypeV1Of(ArchetypeCatalog.ArchetypeEntry catalogEntry) throws UpdateFailed {
        MavenVersion helidonVersion = toMavenVersion(catalogEntry.version());
        String fileName = catalogEntry.artifactId() + "-" + helidonVersion + ".jar";
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
        return versionedFile(version, "helidon-" + version + ".jar", false);
    }

    private Path versionedFile(MavenVersion helidonVersion, String fileName, boolean quiet) throws UpdateFailed {
        Path versionDir = rootDir.resolve(requireNonNull(helidonVersion).toString());
        Path checkFile = versionDir.resolve(".lastUpdate");
        update(helidonVersion, checkFile, quiet);
        return requireFile(requireHelidonVersionDir(versionDir).resolve(fileName));
    }

    private void update(MavenVersion helidonVersion, Path checkFile, boolean quiet) throws UpdateFailed {
        if (isStale(checkFile, timeSource.getAsLong())) {
            if (helidonVersion == null) {
                Log.info("Looking up default Helidon version");

                // update versions.xml
                execPlugin(updateArgs(null, quiet), "UpdateVersions");
                ArchetypesData data = ArchetypesData.load(versionsFile);
                archetypesData.set(data);
                helidonVersion = data.defaultVersion();

                // fetch cli-data
                execPlugin(updateArgs(helidonVersion.toString(), quiet), "UpdateVersion");
            } else {
                Log.info("Updating metadata for Helidon version %s", helidonVersion);

                // fetch cli-data
                execPlugin(updateArgs(helidonVersion.toString(), quiet), "UpdateVersion");

                // update versions.xml
                execPlugin(updateArgs(null, quiet), "UpdateVersions");
            }
        }
    }

    private boolean isStale(Path file, long currentTimeMillis) {

        // During a single command execution, we may get back here multiple times for the same
        // file within a few milliseconds; if so, we just assume it is not stale to avoid hitting
        // the disk again

        Long previousCheck = lastChecked.get(file);
        if (previousCheck == null || currentTimeMillis < previousCheck || currentTimeMillis - previousCheck > 1000L) {
            lastChecked.put(file, currentTimeMillis);
            if (Files.exists(file)) {
                if (updateFrequencyMillis < 0) {
                    Log.debug("stale check is false (disabled) for %s", file);
                    return false;
                } else if (updateFrequencyMillis > 0) {
                    long lastModifiedMillis = lastModifiedTime(file).to(MILLISECONDS);
                    long elapsedMillis = currentTimeMillis - lastModifiedMillis;
                    long remainingMillis = updateFrequencyMillis - elapsedMillis;
                    boolean stale = remainingMillis <= 0;
                    if (LogLevel.isDebug()) {
                        String lastModifiedTime = Time.toDateTime(lastModifiedMillis);
                        String currentTime = Time.currentDateTime();
                        Duration elapsed = Duration.ofMillis(elapsedMillis);
                        String elapsedDays = elapsed.toDaysPart() == 1 ? "day" : "days";
                        if (stale) {
                            Log.debug("stale check is true for %s (last: %s, now: %s, elapsed: %d %s %02d:%02d:%02d)",
                                      file, lastModifiedTime, currentTime,
                                      elapsed.toDaysPart(), elapsedDays, elapsed.toHoursPart(), elapsed.toMinutesPart(),
                                      elapsed.toSecondsPart());
                        } else {
                            Duration remain = Duration.ofMillis(remainingMillis);
                            String remainDays = remain.toDaysPart() == 1 ? "day" : "days";
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

    private List<String> updateArgs(String helidonVersion, boolean quiet) {
        List<String> args = new ArrayList<>();
        args.add("--baseUrl");
        args.add(url);
        args.add("--cacheDir");
        args.add(rootDir.toAbsolutePath().toString());
        if (helidonVersion != null) {
            args.add("--version");
            args.add(helidonVersion);
        }
        args.add("--cliVersion");
        args.add(BuildProperties.buildVersion());
        args.add("--maxAttempts");
        args.add(Integer.toString(quiet ? 1 : 3));
        return args;
    }

    private void execPlugin(List<String> args, String pluginName) throws UpdateFailed {
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
            Plugins.execute(pluginName, args, 30, stdOut);
        } catch (Plugins.PluginFailed e) {
            throw new UpdateFailed(e);
        }
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

    private static MavenVersion latestPluginVersion(MavenVersion helidonVersion,
                                                    MavenVersion thisCliVersion,
                                                    ConfigProperties properties) {

        String latest = properties.property("cli.latest.plugin.version");
        if (latest == null) {
            Log.debug("Helidon version %s does not contain %s, using current CLI version %s",
                    helidonVersion, "cli.latest.plugin.version", thisCliVersion);
            return thisCliVersion;
        } else {
            return toMavenVersion(latest);
        }
    }

    private static boolean isCliPluginVersionKey(String key) {
        return key.startsWith("cli.")
               && key.endsWith(".plugin.version")
               && !key.equals("cli.latest.plugin.version");
    }

    private static MavenVersion toCliPluginVersion(String key) {
        int start = "cli.".length();
        int end = key.length() - ".plugin.version".length();
        return toMavenVersion(key.substring(start, end));
    }

    private static boolean isCliMessageKey(String key) {
        return key.startsWith("cli.") && key.endsWith(".message");
    }

    private static String versionOfCliMessageKey(String key) {
        return key.substring("cli.".length(), key.length() - ".message".length());
    }

    private static String toCliMessageKey(MavenVersion version) {
        return "cli." + version.toString() + ".message";
    }

    /**
     * {@link Metadata} builder.
     */
    public static class Builder {

        private Path rootDir = UserConfig.current().cacheDir();
        private String url = DEFAULT_URL;
        private long updateFrequency = DEFAULT_UPDATE_FREQUENCY;
        private boolean debugPlugin;
        private PrintStream pluginStdOut;
        private TimeUnit updateFrequencyUnits = DEFAULT_UPDATE_FREQUENCY_UNITS;
        private LongSupplier timeSource = DEFAULT_TIME_SOURCE;

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
         * Set the time source.
         * @param timeSource time source
         * @return this builder
         */
        public Builder timeSource(LongSupplier timeSource) {
            this.timeSource = requireNonNull(timeSource);
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
