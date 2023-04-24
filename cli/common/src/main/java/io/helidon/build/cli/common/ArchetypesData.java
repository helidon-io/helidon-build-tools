/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.common.Lists;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;

/**
 * Information about archetypes versions.
 */
public class ArchetypesData {

    private final List<Version> versions;
    private final List<Rule> rules;

    private ArchetypesData(Builder builder) {
        this.versions = builder.versions;
        this.rules = builder.rules;
    }

    /**
     * Get the list of the latest major versions from the current instance of ArchetypesData.
     *
     * @return list of the latest major versions
     */
    public List<String> latestMajorVersions() {
        List<String> versionsId = Lists.map(versions, Version::id);
        return SemVer.latestMajorVersions(versionsId);
    }

    /**
     * Get the latest version.
     *
     * @return latest version
     */
    public MavenVersion latestVersion() {
        List<MavenVersion> mavenVersions = Lists.map(versions, Version::toMavenVersion);
        VersionRange versionRange = VersionRange.createFromVersionSpec("[0,)");
        return versionRange.resolveLatest(mavenVersions);
    }

    /**
     * Get the list of available Helidon versions.
     *
     * @return list of available Helidon versions
     */
    public List<String> versions() {
        return Lists.map(versions, Version::id);
    }

    /**
     * Get the list of available Helidon versions.
     *
     * @return list of available Helidon versions
     */
    public List<Version> rawVersions() {
        return versions;
    }

    /**
     * Get rules for the available Helidon versions.
     *
     * @return rules
     */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * Get default version or the latest one if default version is not set.
     *
     * @return version
     */
    public String defaultVersion() {
        return versions.stream()
                       .filter(Version::isDefault).findFirst()
                       .map(Version::id)
                       .orElse(latestVersion().toString());
    }

    /**
     * Get index of default version from the list of versions.
     *
     * @param versions list of versions
     * @return index of default version
     */
    public int defaultVersionIndex(List<String> versions) {
        int defaultOption = -1;
        var defaultHelidonVersion = defaultVersion();
        for (int x = 0; x < versions.size(); x++) {
            if (versions.get(x).equals(defaultHelidonVersion)) {
                defaultOption = x;
            }
        }
        if (defaultOption == -1) {
            versions.add(defaultHelidonVersion);
            defaultOption = versions.size() - 1;
        }
        return defaultOption;
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * ArchetypesData builder.
     */
    static class Builder {

        private final List<Version> versions = new ArrayList<>();
        private final List<Rule> rules = new ArrayList<>();

        private Builder() {
        }

        Builder version(Version version) {
            versions.add(version);
            return this;
        }

        Builder versions(String... versions) {
            this.versions.addAll(Arrays.stream(versions).map(ArchetypesData.Version::new).collect(Collectors.toList()));
            return this;
        }

        Builder rule(Rule rule) {
            rules.add(rule);
            return this;
        }

        /**
         * Create new instance of {@link ArchetypesData}.
         *
         * @return version
         */
        ArchetypesData build() {
            return new ArchetypesData(this);
        }
    }

    /**
     * Helidon version.
     */
    public static class Version {
        private final String id;
        private final boolean isDefault;

        Version(String id) {
            this.id = id;
            this.isDefault = false;
        }

        Version(String id, boolean isDefault) {
            this.id = id;
            this.isDefault = isDefault;
        }

        MavenVersion toMavenVersion() {
            return MavenVersion.toMavenVersion(id);
        }

        /**
         * Get Helidon version id.
         *
         * @return version id
         */
        public String id() {
            return id;
        }

        /**
         * Mark is the current version is default.
         *
         * @return true if the version is default and false otherwise
         */
        public boolean isDefault() {
            return isDefault;
        }
    }

    /**
     * Rule for compatibility Helidon versions range and a range of Helidon CLI versions.
     */
    static class Rule {
        private final VersionRange archetypeRange;
        private final VersionRange cliRange;

        Rule(VersionRange archetypeRange, VersionRange cliRange) {
            this.archetypeRange = archetypeRange;
            this.cliRange = cliRange;
        }

        VersionRange archetypeRange() {
            return archetypeRange;
        }

        VersionRange cliRange() {
            return cliRange;
        }
    }
}
