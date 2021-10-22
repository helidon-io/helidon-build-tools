/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import java.util.HashMap;
import java.util.Map;

import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.common.SubstitutionVariables;

import static io.helidon.build.common.SubstitutionVariables.systemPropertyOrEnvVarSource;

/**
 * Init options.
 */
@CommandFragment
public final class InitOptions {

    private static final String PROJECT_NAME_PROPERTY = "name";
    private static final String GROUP_ID_PROPERTY = "groupId";
    private static final String ARTIFACT_ID_PROPERTY = "artifactId";
    private static final String PACKAGE_NAME_PROPERTY = "package";
    private static final String HELIDON_VERSION_PROPERTY = "helidonVersion";
    private static final String MAVEN_PROPERTY = "maven";

    /**
     * The default flavor.
     */
    static final String DEFAULT_FLAVOR = "SE";

    /**
     * The default archetype name.
     */
    static final String DEFAULT_ARCHETYPE_NAME = "bare";

    private Flavor flavor;
    private String helidonVersion;
    private final BuildSystem build;
    private final String archetypeName;
    private final String archetypePath;
    private final ArchetypeInvoker.EngineVersion engineVersion;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String projectName;

    /**
     * Helidon flavors.
     */
    enum Flavor {
        MP,
        SE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * Build systems.
     */
    enum BuildSystem {
        MAVEN,
        GRADLE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    @Creator
    InitOptions(
            @KeyValue(name = "flavor", description = "Helidon flavor", defaultValue = DEFAULT_FLAVOR) Flavor flavor,
            @KeyValue(name = "build", description = "Build type", defaultValue = "MAVEN") BuildSystem build,
            @KeyValue(name = "version", description = "Helidon version") String version,
            @KeyValue(name = "archetype", description = "Archetype name", defaultValue = DEFAULT_ARCHETYPE_NAME)
                    String archetypeName,
            @KeyValue(name = "groupid", description = "Project's group ID") String groupId,
            @KeyValue(name = "artifactid", description = "Project's artifact ID") String artifactId,
            @KeyValue(name = "package", description = "Project's package name") String packageName,
            @KeyValue(name = "name", description = "Project's name") String projectName,
            @KeyValue(name = "archetype-path", description = "Archetype's path", defaultValue = "") String archetypePath,
            @KeyValue(name = "engine-version", description = "Archetype's engine version", defaultValue = "v1")
                    String engineVersion) {

        this.build = build;
        this.helidonVersion = version;
        this.flavor = flavor;
        this.archetypeName = archetypeName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.packageName = packageName;
        this.projectName = projectName;
        this.archetypePath = archetypePath;
        this.engineVersion = getEngineVersion(engineVersion);
    }

    private ArchetypeInvoker.EngineVersion getEngineVersion(String version) {
        return version.equalsIgnoreCase("v2")
                ? ArchetypeInvoker.EngineVersion.V2
                : ArchetypeInvoker.EngineVersion.V1;
    }

    /**
     * Get the flavor.
     *
     * @return Flavor
     */
    Flavor flavor() {
        return flavor;
    }

    /**
     * Set the flavor.
     *
     * @param flavor flavor
     */
    void flavor(Flavor flavor) {
        this.flavor = flavor;
    }

    /**
     * Get the Helidon version.
     *
     * @return version
     */
    String helidonVersion() {
        return helidonVersion;
    }

    /**
     * Set the Helidon version.
     *
     * @param helidonVersion Helidon version
     */
    void helidonVersion(String helidonVersion) {
        this.helidonVersion = helidonVersion;
    }

    /**
     * Get the build system.
     *
     * @return BuildSystem
     */
    BuildSystem build() {
        return build;
    }

    /**
     * Get the archetype name.
     *
     * @return archetype name
     */
    String archetypeName() {
        return archetypeName;
    }

    /**
     * Get the project name.
     *
     * @return project name
     */
    String projectName() {
        return projectName;
    }

    /**
     * Get the groupId.
     *
     * @return groupId
     */
    String groupId() {
        return groupId;
    }

    /**
     * Get the artifactId.
     *
     * @return artifactId
     */
    String artifactId() {
        return artifactId;
    }

    /**
     * Get the package name.
     *
     * @return package name
     */
    String packageName() {
        return packageName;
    }

    /**
     * Get the archetype path.
     *
     * @return archetype path
     */
    String archetypePath() {
        return archetypePath;
    }

    /**
     * Get the archetype engine version.
     *
     * @return archetype version
     */
    ArchetypeInvoker.EngineVersion engineVersion() {
        return engineVersion;
    }

    /**
     * Transform the init options by applying the given user configuration.
     *
     * @param config user configuration
     */
    void applyConfig(UserConfig config) {
        SubstitutionVariables substitutions = SubstitutionVariables.of(key -> {
            switch (key.toLowerCase()) {
                case "init_flavor":
                    return flavor.toString();
                case "init_archetype":
                    return archetypeName;
                case "init_build":
                    return build.toString();
                default:
                    return null;
            }
        }, systemPropertyOrEnvVarSource());
        String projectNameArg = projectName;
        projectName = config.projectName(projectNameArg, artifactId, substitutions);
        groupId = config.groupId(groupId, substitutions);
        artifactId = config.artifactId(artifactId, projectNameArg, substitutions);
        packageName = config.packageName(packageName, substitutions);
    }

    /**
     * Create init properties.
     *
     * @return Map of init options
     */
    Map<String, String> initProperties() {
        Map<String, String> result = new HashMap<>();
        result.put(PROJECT_NAME_PROPERTY, projectName);
        result.put(GROUP_ID_PROPERTY, groupId);
        result.put(ARTIFACT_ID_PROPERTY, artifactId);
        result.put(PACKAGE_NAME_PROPERTY, packageName);
        result.put(HELIDON_VERSION_PROPERTY, helidonVersion);
        result.putIfAbsent(MAVEN_PROPERTY, "true");        // No gradle support yet
        return result;
    }
}
