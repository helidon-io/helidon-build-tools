/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.cli.common;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static java.util.Objects.requireNonNull;

/**
 * Provides "latest" version selection for a given CLI version given a set of "latest" versions and
 * rules for given CLI versions.
 * <p>
 * Supports the "latest" file format:
 * <ol>
 *   <li>The first non-empty line must be a 2.x version number for backwards compatibility.</li>
 *   <li>May be followed by any of the following:</li>
 *   <ul>
 *       <li>lines staring with '#', which are treated as comments and ignored</li>
 *       <li>lines containing key=value properties</li>
 *   </ul>
 * </ol>
 * TODO: describe rule properties.
 */
public class LatestVersion {
    private static final VersionRange HELIDON_2 = VersionRange.createFromVersionSpec("[2-alpha,3-alpha)");
    private static final String COMMENT = "#";
    private static final String PROPERTY_SEP = "=";
    private final List<MavenVersion> versions;
    private final List<VersionRule> rules;
    private final Map<String, String> properties;

    private LatestVersion(List<MavenVersion> versions, Map<String, String> properties) {
        this.versions = requireNonNull(versions);
        this.properties = requireNonNull(properties);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("versions may not be empty");
        } else if (!HELIDON_2.containsVersion(versions.get(0))) {
            throw new IllegalArgumentException("a 2.x version must be first: " + versions);
        }
        if (properties.isEmpty()) {
            rules = List.of();
        } else {
            rules = toRules(properties);
        }
    }

    private static List<VersionRule> toRules(Map<String, String> properties) {
        List<VersionRule> rules = new ArrayList<>();
        // TODO PARSE
        return rules;
    }

    /**
     * Creates a new instance from the given "latest" file.
     * @param latestFile The file.
     * @return The instance.
     */
    public static LatestVersion create(Path latestFile) {
        try {
            return create(Files.readAllLines(latestFile));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new instance from the given "latest" file lines.
     * @param latestFileLines The lines.
     * @return The instance.
     */
    public static LatestVersion create(List<String> latestFileLines) {
        List<MavenVersion> versions = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();

        for (String rawLine : latestFileLines) {
            String line = rawLine.trim();
            if (!line.isEmpty() && !line.startsWith(COMMENT)) {
                if (line.contains(PROPERTY_SEP)) {
                    int index = line.indexOf(PROPERTY_SEP);
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    if (properties.containsKey(key)) {
                        throw new IllegalStateException("Duplicate property '" + key + "'");
                    }
                    properties.put(key, value);
                } else {
                    versions.add(toMavenVersion(line));
                }
            }
        }
        return new LatestVersion(versions, properties);
    }

    /**
     * Returns the latest Helidon version for the given CLI version.
     * @param cliVersion The CLI version.
     * @return The latest Helidon version.
     * @throws IllegalStateException If an error occurs.
     */
    public MavenVersion latest(MavenVersion cliVersion) {
        if (rules.isEmpty()) {
            return versions.get(0);
        } else {
            for (VersionRule rule : rules) {
                if (rule.matches(cliVersion)) {
                    return rule.latest(versions, cliVersion);
                }
            }
            throw new IllegalStateException("no rule matches CLI version " + cliVersion);
        }
    }

    /**
     * Returns the properties.
     * @return The properties.
     */
    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    static class VersionRule {
        private final VersionRange cliRange;
        private final VersionRange helidonRange;

        VersionRule(VersionRange cliRange, VersionRange helidonRange) {
            this.cliRange = cliRange;
            this.helidonRange = helidonRange;
        }

        boolean matches(MavenVersion cliVersion) {
            return cliRange.containsVersion(cliVersion);
        }

        MavenVersion latest(List<MavenVersion> helidonVersions, MavenVersion cliVersion) {
            MavenVersion latest = helidonRange.matchVersion(helidonVersions);
            if (latest == null) {
                String message = String.format("No matching version for CLI version %s with rule %s", cliVersion, helidonRange);
                throw new IllegalStateException(message);
            }
            return latest;
        }
    }
}
