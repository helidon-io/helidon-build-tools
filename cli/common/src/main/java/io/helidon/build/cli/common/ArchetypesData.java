/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.common.Lists;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.common.xml.XMLReaderException;

import static io.helidon.build.cli.common.SemVer.sortVersions;

/**
 * Information about archetypes versions.
 */
public class ArchetypesData {

    static final ArchetypesData EMPTY = new ArchetypesData(List.of(), List.of());
    private final List<Version> versions;
    private final List<Rule> rules;

    ArchetypesData(List<Version> versions, List<Rule> rules) {
        this.versions = versions;
        this.rules = rules;
    }

    private ArchetypesData(XMLElement elt) {
        if (!"data".equals(elt.name())) {
            throw new XMLReaderException(String.format("Invalid root element: " + elt.name()));
        }
        versions = sortVersions(Lists.map(elt.childrenAt("archetypes", "version"), Version::new));
        rules = Lists.map(elt.childrenAt("rules", "rule"), Rule::new);
    }

    /**
     * Get the data about archetype versions from the given file.
     *
     * @param versionsFile versionsFile
     * @return data about archetype versions
     */
    public static ArchetypesData load(Path versionsFile) {
        try {
            InputStream is = Files.newInputStream(versionsFile);
            XMLElement elt = XMLElement.parse(is);
            return new ArchetypesData(elt);
        } catch (IOException ex) {
            return ArchetypesData.EMPTY;
        }
    }

    /**
     * Get the list of the latest major versions from the current instance of ArchetypesData.
     *
     * @return list of the latest major versions
     */
    public List<String> latestMajorVersions() {
        return SemVer.latestMajorVersions(versions);
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
    public MavenVersion defaultVersion() {
        return versions.stream()
                .filter(Version::isDefault)
                .findFirst()
                .map(Version::toMavenVersion)
                .orElse(latestVersion());
    }

    /**
     * Get index of default version from the list of versions.
     *
     * @param versions list of versions
     * @return index of default version
     */
    public int defaultVersionIndex(List<String> versions) {
        int defaultOption = -1;
        var defaultHelidonVersion = defaultVersion().toString();
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
     * Helidon version.
     */
    public static class Version {
        private final String id;
        private final boolean isDefault;
        private final int order;

        Version(String id, boolean isDefault, int order) {
            this.id = id;
            this.isDefault = isDefault;
            this.order = order;
        }

        private Version(XMLElement elt) {
            this.id = elt.value();
            this.isDefault = elt.attributeBoolean("default", false);
            this.order = elt.attribute("order", Integer::parseInt, 0);
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

        /**
         * Get version order.
         *
         * @return order
         */
        public int order() {
            return order;
        }
    }

    /**
     * Rule for compatibility Helidon versions range and a range of Helidon CLI versions.
     */
    public static class Rule {
        private final VersionRange archetypeRange;
        private final VersionRange cliRange;

        private Rule(XMLElement elt) {
            this.archetypeRange = VersionRange.createFromVersionSpec(elt.attribute("archetype"));
            this.cliRange = VersionRange.createFromVersionSpec(elt.attribute("cli"));
        }

        VersionRange archetypeRange() {
            return archetypeRange;
        }

        VersionRange cliRange() {
            return cliRange;
        }
    }
}
