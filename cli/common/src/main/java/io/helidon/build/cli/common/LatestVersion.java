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
import java.util.Comparator;
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
 * Supports the "latest" file format, which <em>must</em> contain at least one line containing only a version, and may contain
 * zero or more of the following:
 * <ul>
 *      <li>lines containing additional version numbers</li>
 *      <li>lines starting with '#', which are treated as comments and ignored</li>
 *      <li>lines containing key=value properties</li>
 * </ul>
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
 * <h2>Example "latest" File</h2>
 * <pre>
 *  2.4.1
 *  3.4.12
 *
 *  # Selection rules.
 *  #
 *  #   The CLI selects the one rule that applies to itself based on its version
 *  #   and filters out the latest from the list above.
 *  #
 *  # Version range works as follows:
 *  #
 *  #   [1.0,2.0) versions 1.0 (included) to 2.0 (not included)
 *  #   [1.0,2.0] versions 1.0 to 2.0 (both included)
 *  #   [1.5,) versions 1.5 and higher
 *  #   (,1.0],[1.2,) versions up to 1.0 (included) and 1.2 or higher
 *  #
 *  # Notes:
 *  #
 *  #   1 == 1.0 == 1.0.0
 *  #   X-alpha is the lowest version of X
 *
 *  cli.[2-alpha,3-alpha).latest=[2-alpha,3-alpha)
 *  cli.[3-alpha,).latest=[3-alpha,)
 * </pre>
 */
public class LatestVersion {
    private static final String COMMENT = "#";
    private static final String PROPERTY_SEP = "=";

    private final List<MavenVersion> versions;
    private final List<VersionRule> rules;
    private final Map<String, String> properties;

    private LatestVersion(List<MavenVersion> versions, Map<String, String> properties) {
        this.versions = requireNonNull(versions);
        this.properties = unmodifiableMap(requireNonNull(properties));
        if (properties.isEmpty()) {
            rules = List.of();
        } else {
            rules = VersionRule.parse(properties, versions);
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
        List<MavenVersion> versions = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();

        for (String rawLine : latestFileLines) {
            String line = rawLine.trim();
            if (!line.isEmpty() && !line.startsWith(COMMENT)) {
                int index = line.indexOf(PROPERTY_SEP);
                if (index > 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    if (properties.containsKey(key)) {
                        throw new IllegalStateException("Duplicate property '" + key + "'");
                    }
                    properties.put(key, value);
                } else if (Character.isDigit(line.charAt(0))) {
                    versions.add(toMavenVersion(line));
                } else {
                    throw new IllegalStateException("Unknown entry: " + line);
                }
            }
        }
        if (versions.isEmpty()) {
            throw new IllegalStateException("No versions found.");
        }
        versions.sort(Comparator.reverseOrder());
        return new LatestVersion(versions, properties);
    }

    /**
     * Returns the latest Helidon version for the given CLI version. If no rule exists for this version, the
     * most recent Helidon version will be returned. If a rule does exist, the most recent Helidon version that
     * matches the rule will be returned.
     *
     * @param cliVersion The CLI version.
     * @return The Helidon version.
     */
    public MavenVersion latest(MavenVersion cliVersion) {
        for (VersionRule rule : rules) {
            if (rule.matches(cliVersion)) {
                return rule.latest(versions);
            }
        }
        return versions.get(0);
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

    static class VersionRule {
        private static final String CLI_PREFIX = "cli.";
        private static final String LATEST_SUFFIX = ".latest";

        private final VersionRange cliRange;
        private final VersionRange helidonRange;

        static List<VersionRule> parse(Map<String, String> properties, List<MavenVersion> versions) {
            List<VersionRule> rules = new ArrayList<>();
            properties.forEach((key, value) -> {
                if (key.startsWith(CLI_PREFIX) && key.endsWith(LATEST_SUFFIX)) {
                    String cliSpec = key.substring(CLI_PREFIX.length(), key.length() - LATEST_SUFFIX.length());
                    rules.add(new VersionRule(cliSpec, value, versions));
                }
            });
            return rules;
        }

        VersionRule(String cliRange, String helidonRange, List<MavenVersion> helidonVersions) {
            this(createFromVersionSpec(cliRange), createFromVersionSpec(helidonRange), helidonVersions);
        }

        VersionRule(VersionRange cliRange, VersionRange helidonRange, List<MavenVersion> helidonVersions) {
            this.cliRange = cliRange;
            this.helidonRange = helidonRange;
            if (helidonRange.matchVersion(helidonVersions) == null) {
                throw new IllegalStateException(String.format("Rule '%s' does not match any version: %s", this, helidonVersions));
            }
        }

        boolean matches(MavenVersion cliVersion) {
            return cliRange.containsVersion(cliVersion);
        }

        MavenVersion latest(List<MavenVersion> helidonVersions) {
            return helidonRange.matchVersion(helidonVersions);
        }

        @Override
        public String toString() {
            return CLI_PREFIX + cliRange + LATEST_SUFFIX + PROPERTY_SEP + helidonRange;
        }
    }
}
