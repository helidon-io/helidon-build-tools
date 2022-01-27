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
import java.nio.charset.StandardCharsets;
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
import static io.helidon.build.common.maven.VersionRange.createFromVersionSpec;
import static java.util.Collections.unmodifiableMap;
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
 *       <li>lines containing additional version numbers</li>
 *       <li>lines starting with '#', which are treated as comments and ignored</li>
 *       <li>lines containing key=value properties</li>
 *   </ul>
 * </ol>
 * If properties exist whose keys begin with "cli." and end with ".latest", they are treated as rules, where
 * a version range is supplied in the key and one in the value, e.g.:
 * <pre>
 *  cli.[2-alpha,3-alpha).latest=[2-alpha,3-alpha)
 *  cli.[3-alpha,).latest=[3-alpha,)
 * </pre>
 * The range in the key is used to match the current CLI version, and the value is used to match against
 * the latest version list.
 * <p>
 * Version ranges use the following syntax:
 * <pre>
 *  [1.0,2.0) versions 1.0 (included) to 2.0 (not included)
 *  [1.0,2.0] versions 1.0 to 2.0 (both included)
 *  [1.5,) versions 1.5 and higher
 *  (,1.0],[1.2,) versions up to 1.0 (included) and 1.2 or higher
 * </pre>
 * Notes that:
 * <ul>
 *     <li>1 == 1.0 == 1.0.0</li>
 *     <li>X-alpha is the lowest version of X</li>
 * </ul>
 * Here is a complete example "latest" file:
 * <pre>
 *  2.4.1
 *  3.4.12
 *
 *  # Selection rules.
 *  #   The CLI selects the one rule that applies to itself based on its version
 *  #   and filters out the latest from the list above.
 *  #
 *  # Version range works as follows:
 *  #   [1.0,2.0) versions 1.0 (included) to 2.0 (not included)
 *  #   [1.0,2.0] versions 1.0 to 2.0 (both included)
 *  #   [1.5,) versions 1.5 and higher
 *  #   (,1.0],[1.2,) versions up to 1.0 (included) and 1.2 or higher
 *  #
 *  # Notes:
 *  #   1 == 1.0 == 1.0.0
 *  #   X-alpha is the lowest version of X
 *
 *  cli.[2-alpha,3-alpha).latest=[2-alpha,3-alpha)
 *  cli.[3-alpha,).latest=[3-alpha,)
 * </pre>
 */
public class LatestVersion {
    private static final VersionRange HELIDON_2 = createFromVersionSpec("[2-alpha,3-alpha)");
    private static final String COMMENT = "#";
    private static final String PROPERTY_SEP = "=";
    private static final String CLI_PREFIX = "cli.";
    private static final String LATEST_SUFFIX = ".latest";

    private final List<MavenVersion> versions;
    private final List<VersionRule> rules;
    private final Map<String, String> properties;

    private LatestVersion(List<MavenVersion> versions, Map<String, String> properties) {
        this.versions = requireNonNull(versions);
        this.properties = unmodifiableMap(requireNonNull(properties));
        if (properties.isEmpty()) {
            rules = List.of();
        } else {
            rules = toRules(properties);
        }
    }

    /**
     * Creates a new instance from the given "latest" file.
     *
     * @param latestFile The file.
     * @return The instance.
     */
    public static LatestVersion create(Path latestFile) {
        try {
            return create(Files.readAllLines(latestFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new instance from the given "latest" file lines.
     *
     * @param latestFileLines The lines.
     * @return The instance.
     */
    public static LatestVersion create(List<String> latestFileLines) {
        assertValid(latestFileLines);
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
     *
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
            throw new IllegalStateException("No rule matches CLI version " + cliVersion);
        }
    }

    /**
     * Returns the latest versions.
     *
     * @return The versions.
     */
    public List<MavenVersion> versions() {
        return Collections.unmodifiableList(versions);
    }

    /**
     * Returns the properties.
     *
     * @return The properties.
     */
    public Map<String, String> properties() {
        return properties;
    }

    List<VersionRule> rules() {
        return rules;
    }

    private static void assertValid(List<String> latestFileLines) {

        // Make sure the first non-empty line is a 2.x version.
        // Here, we exactly duplicate the way 2.x versions find the first line, which
        // should have trimmed the line prior to the isEmpty test but did not, so any
        // blank lines prior to the version MUST not contain whitespace.

        boolean foundVersion = false;
        for (String line : latestFileLines) {
            if (!line.isEmpty()) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    throw new IllegalStateException("The first non-empty line must be a 2.x version, but is only whitespace.");
                }
                MavenVersion version = toMavenVersion(trimmedLine);
                if (HELIDON_2.containsVersion(version)) {
                    foundVersion = true;
                    break;
                } else {
                    throw new IllegalStateException("The first non-empty line must be a 2.x version, is: " + version);
                }
            }
        }
        if (!foundVersion) {
            throw new IllegalStateException("No versions found.");
        }
    }

    private static List<VersionRule> toRules(Map<String, String> properties) {
        List<VersionRule> rules = new ArrayList<>();
        properties.forEach((key, value) -> {
            if (key.startsWith(CLI_PREFIX) && key.endsWith(LATEST_SUFFIX)) {
                String cliSpec = key.substring(CLI_PREFIX.length(), key.length() - LATEST_SUFFIX.length());
                VersionRule rule = new VersionRule(cliSpec, value);
                rules.add(rule);
            }
        });
        return rules;
    }

    static class VersionRule {
        private final VersionRange cliRange;
        private final VersionRange helidonRange;

        VersionRule(String cliRange, String helidonRange) {
            this(createFromVersionSpec(cliRange), createFromVersionSpec(helidonRange));
        }

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
                String message = String.format("No matching version for CLI version %s with rule %s. Versions: %s",
                                               cliVersion, helidonRange, helidonVersions);
                throw new IllegalStateException(message);
            }
            return latest;
        }
    }
}
