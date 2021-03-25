/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.cli.impl.Plugins.PluginFailed;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.Requirements;

import static io.helidon.build.cli.impl.CommandRequirements.requireSupportedHelidonVersion;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static java.util.Objects.requireNonNull;

/**
 * Archetype browser for the V1 archetypes.
 */
class ArchetypeBrowser {

    private static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    private static final String JAR_SUFFIX = ".jar";
    private static final String HELIDON_VERSION_NOT_FOUND = "$(red Helidon version) $(RED %s) $(red not found.)";

    private final Metadata metadata;
    private final Flavor flavor;
    private final ArchetypeCatalog catalog;

    /**
     * Create a new archetype browser for the V1 engine.
     *
     * @param metadata       metadata
     * @param flavor         flavor
     * @param helidonVersion Helidon version
     * @throws IOException  If an IO error occurs.
     */
    ArchetypeBrowser(Metadata metadata, Flavor flavor, String helidonVersion) throws IOException {
        this.metadata = requireNonNull(metadata);
        this.flavor = requireNonNull(flavor);
        ArchetypeCatalog catalog = null;
        try {
            MavenVersion version = toMavenVersion(requireSupportedHelidonVersion(helidonVersion));
            catalog = ArchetypeCatalog.read(metadata.versionedFile(version, CATALOG_FILE_NAME, false));
        } catch (PluginFailed e) {
            Requirements.failed(HELIDON_VERSION_NOT_FOUND, helidonVersion);
        }
        this.catalog = catalog;
    }

    /**
     * Returns list of archetypes available. Checks remote repo if local cache
     * does not include a catalog file. For convenience, it also checks the local
     * Maven repo to support unreleased versions.
     *
     * @return List of available archetype.
     */
    List<ArchetypeCatalog.ArchetypeEntry> archetypes() {
        return catalog.entries()
                      .stream()
                      .filter(e -> e.tags().contains(flavor.toString()))
                      .collect(Collectors.toList());
    }

    /**
     * Returns path to archetype jar in local cache or {@code null} if not found
     * and not available for download. Checks remote and then local repo to
     * handle unreleased versions.
     *
     * @param archetype The archetype.
     * @return Path to archetype jar or {@code null} if not found.
     */
    Path archetypeJar(ArchetypeCatalog.ArchetypeEntry archetype) {
        try {
            MavenVersion helidonVersion = toMavenVersion(archetype.version());
            String fileName = archetype.artifactId() + "-" + helidonVersion + JAR_SUFFIX;
            return metadata.versionedFile(helidonVersion, fileName, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
