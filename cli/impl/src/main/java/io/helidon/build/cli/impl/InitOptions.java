/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import io.helidon.build.cli.harness.Option;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.cli.impl.ArchetypeInvoker.EngineVersion;
import io.helidon.build.common.SubstitutionVariables;
import io.helidon.build.common.SubstitutionVariables.NotFoundAction;

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
    static final String DEFAULT_ARCHETYPE_NAME = "quickstart";

    private Flavor flavor;
    private String helidonVersion;
    private final BuildSystem build;
    private final BuildSystem buildOption;
    private String archetypeName;
    private final String archetypeNameOption;
    private final Flavor flavorOption;
    private final String projectNameOption;
    private final String groupIdOption;
    private final String artifactIdOption;
    private final String packageNameOption;
    private final boolean batch;
    private String projectName;
    private String groupId;
    private String artifactId;
    private String packageName;


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
            @KeyValue(name = "flavor", description = "Helidon flavor") Flavor flavor,
            @KeyValue(name = "build", description = "Build type", visible = false) BuildSystem build,
            @KeyValue(name = "version", description = "Helidon version") String version,
            @KeyValue(name = "archetype", description = "Archetype name") String archetypeName,
            @KeyValue(name = "groupid", description = "Project's group ID") String groupId,
            @KeyValue(name = "artifactid", description = "Project's artifact ID") String artifactId,
            @KeyValue(name = "package", description = "Project's package name") String packageName,
            @KeyValue(name = "name", description = "Project's name") String projectName,
            @Option.Flag(name = "batch", description = "Enable non-interactive mode") boolean batch) {

        this.buildOption = build;
        this.build = build == null ? BuildSystem.MAVEN : build;
        this.helidonVersion = version;
        this.archetypeNameOption = archetypeName;
        this.archetypeName = archetypeName == null ? DEFAULT_ARCHETYPE_NAME : archetypeName;
        this.flavorOption = flavor;
        this.flavor = flavor == null ? Flavor.valueOf(DEFAULT_FLAVOR) : flavor;
        this.projectNameOption = projectName;
        this.groupIdOption = groupId;
        this.artifactIdOption = artifactId;
        this.packageNameOption = packageName;
        this.batch = batch;

        // The following will be updated by applyConfig:

        this.projectName = projectName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.packageName = packageName;
    }

    /**
     * Get the flavor option.
     *
     * @return Flavor
     */
    Flavor flavorOption() {
        return flavorOption;
    }

    /**
     * Get the flavor, defaults to SE.
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
     * Get the build system option.
     *
     * @return BuildSystem
     */
    BuildSystem buildOption() {
        return buildOption;
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
     * Get the archetype name option.
     *
     * @return archetype name
     */
    String archetypeNameOption() {
        return archetypeNameOption;
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
     * Set the archetype name.
     *
     * @param name archetype name
     */
    void archetypeName(String name) {
        archetypeName = name;
    }

    /**
     * Get the project name. May have been updated from user config.
     *
     * @return project name
     */
    String projectName() {
        return projectName;
    }

    /**
     * Get the groupId. May have been updated from user config.
     *
     * @return groupId
     */
    String groupId() {
        return groupId;
    }

    /**
     * Get the artifactId. May have been updated from user config.
     *
     * @return artifactId
     */
    String artifactId() {
        return artifactId;
    }

    /**
     * Get the package name. May have been updated from user config.
     *
     * @return package name
     */
    String packageName() {
        return packageName;
    }

    /**
     * Get the project name option.
     *
     * @return project name
     */
    String projectNameOption() {
        return projectNameOption;
    }

    /**
     * Get the groupId option.
     *
     * @return groupId
     */
    String groupIdOption() {
        return groupIdOption;
    }

    /**
     * Get the artifactId.
     *
     * @return artifactId
     */
    String artifactIdOption() {
        return artifactIdOption;
    }

    /**
     * Get the package name.
     *
     * @return package name
     */
    String packageNameOption() {
        return packageNameOption;
    }

    /**
     * Whether batch mode is enabled.
     * @return {@code true} if batch mode.
     */
    boolean batch() {
        return batch;
    }

    /**
     * Transform the init options by applying the given user configuration.
     *
     * @param config        user configuration
     * @param engineVersion archetype engine version
     */
    void applyConfig(UserConfig config, EngineVersion engineVersion) {
        SubstitutionVariables substitutions;
        if (engineVersion == EngineVersion.V1) {
            substitutions = SubstitutionVariables.of(NotFoundAction.Fail, key -> {
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
        } else {
            // Cannot substitute anything as the values are not resolved yet.
            // Map the "config" variables to archetype variables to let the engine perform the substitutions
            // when the input values are resolved
            // Note that this mapping is tightly coupled with the helidon archetype
            substitutions = SubstitutionVariables.of(NotFoundAction.AsIs, key -> {
                switch (key.toLowerCase()) {
                    case "init_flavor":
                        return "${flavor}";
                    case "init_archetype":
                        return "${app-type}";
                    case "init_build":
                        return "${build-system}";
                    default:
                        return null;
                }
            }, systemPropertyOrEnvVarSource());
        }
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
