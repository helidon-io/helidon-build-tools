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

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.util.Requirements;

import static io.helidon.build.cli.impl.CommandRequirements.requireSupportedHelidonVersion;
import static java.util.Objects.requireNonNull;

/**
 * Class ArchetypeBrowser.
 */
class ArchetypeBrowser {

    /**
     * Helidon version not found message.
     */
    private static final String HELIDON_VERSION_NOT_FOUND = "$(red Helidon version) $(RED %s) $(red not found.)";

    /**
     * Archetype not found message.
     */
    static final String ARCHETYPE_NOT_FOUND = "$(red Archetype \")$(RED %s)$(red \" not found.)";

    private final Metadata metadata;
    private final Flavor flavor;
    private final ArchetypeCatalog catalog;

    ArchetypeBrowser(Metadata metadata, Flavor flavor, String helidonVersion) {
        this.metadata = requireNonNull(metadata);
        this.flavor = requireNonNull(flavor);
        ArchetypeCatalog catalog = null;
        try {
            catalog = metadata.catalogOf(requireSupportedHelidonVersion(helidonVersion));
        } catch (Metadata.UpdateFailed | Plugins.PluginFailedUnchecked e) {
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
            return metadata.archetypeOf(archetype);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
