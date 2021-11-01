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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v1.ArchetypeEngine;
import io.helidon.build.archetype.engine.v1.ArchetypeLoader;
import io.helidon.build.archetype.engine.v1.FlowNodeControllers;
import io.helidon.build.archetype.engine.v1.FlowNodeControllers.FlowNodeController;
import io.helidon.build.archetype.engine.v1.Maps;
import io.helidon.build.archetype.engine.v2.ArchetypeEngineV2;
import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import io.helidon.build.archetype.engine.v2.prompter.CLIPrompter;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;
import static io.helidon.build.common.Requirements.require;

/**
 * Class ArchetypeInvoker.
 */
abstract class ArchetypeInvoker {

    /**
     * Archetype engine versions.
     */
    enum EngineVersion {
        V1,
        V2
    }

    /**
     * The first Helidon version that uses the archetype engine V2.
     */
    private static final MavenVersion HELIDON_V3 = MavenVersion.toMavenVersion("3.0.0");

    private final Metadata metadata;
    private final boolean batch;
    private final InitOptions initOptions;
    private final Function<String, Path> projectDirSupplier;

    private ArchetypeInvoker(Builder builder) {
        metadata = builder.metadata;
        batch = builder.batch;
        initOptions = builder.initOptions;
        projectDirSupplier = builder.projectDirSupplier;
    }

    /**
     * Get the interactive flag.
     *
     * @return {@code true} if interactive, {@code false} if batch
     */
    protected boolean isInteractive() {
        return !batch;
    }

    /**
     * Get the metadata.
     *
     * @return Metadata
     */
    protected Metadata metadata() {
        return metadata;
    }

    /**
     * Get the init options.
     *
     * @return InitOptions
     */
    protected InitOptions initOptions() {
        return initOptions;
    }

    /**
     * Get the project directory supplier.
     *
     * @return Supplier of Path
     */
    protected Function<String, Path> projectDirSupplier() {
        return projectDirSupplier;
    }

    /**
     * Invoke the archetype engine to generate the project.
     *
     * @return project directory
     * @throws IOException if an IO error occurs
     */
    abstract Path invoke() throws IOException;

    /**
     * Get the archetype engine version of this invoker.
     *
     * @return EngineVersion
     */
    abstract EngineVersion engineVersion();

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Archetype invoker builder.
     */
    static class Builder {

        private Metadata metadata;
        private boolean batch;
        private InitOptions initOptions;
        private Function<String, Path> projectDirSupplier;

        private Builder() {
        }

        /**
         * Set the metadata.
         *
         * @param metadata metadata
         * @return this builder
         */
        Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Set the batch flag.
         *
         * @param batch batch flag
         * @return this builder
         */
        Builder batch(boolean batch) {
            this.batch = batch;
            return this;
        }

        /**
         * Set the init options.
         *
         * @param initOptions init options
         * @return this builder
         */
        Builder initOptions(InitOptions initOptions) {
            this.initOptions = initOptions;
            return this;
        }

        /**
         * Set the project directory supplier.
         *
         * The project directory supplier takes a project name and returns
         * a Path to the project directory.
         *
         * @param projectDirSupplier project directory supplier
         * @return this builder
         */
        Builder projectDir(Function<String, Path> projectDirSupplier) {
            this.projectDirSupplier = projectDirSupplier;
            return this;
        }

        /**
         * Build the invoker instance.
         *
         * @return {@link V1Invoker} if the configured Helidon archetype version is associated with the V1 engine,
         * otherwise {@link V2Invoker}
         */
        ArchetypeInvoker build() {
            if (initOptions.engineVersion().equals(EngineVersion.V1)) {
                return new V1Invoker(this);
            }
            return new V2Invoker(this);
        }
    }

    /**
     * Invoker for the archetype V1 engine.
     */
    static class V1Invoker extends ArchetypeInvoker {

        private V1Invoker(Builder builder) {
            super(builder);
        }

        @Override
        EngineVersion engineVersion() {
            return EngineVersion.V1;
        }

        @Override
        Path invoke() throws IOException {
            String helidonVersion = initOptions().helidonVersion();
            Flavor flavor = initOptions().flavor();

            // Gather archetype names
            ArchetypeBrowser browser = new ArchetypeBrowser(metadata(), flavor, helidonVersion);
            List<ArchetypeCatalog.ArchetypeEntry> archetypes = browser.archetypes();
            require(!archetypes.isEmpty(), "Unable to find archetypes for %s and %s.", flavor, helidonVersion);

            ArchetypeEntry archetype;
            if (isInteractive()) {
                // Select archetype interactively
                List<String> descriptions = archetypes.stream()
                                                      .map(a -> a.name() + " | " + a.description().orElse(a.summary()))
                                                      .collect(Collectors.toList());
                int archetypeIndex = prompt("Select archetype", descriptions, 0);
                archetype = archetypes.get(archetypeIndex);
            } else {
                // find the archetype that matches archetypeName
                archetype = archetypes.stream()
                                      .filter(a -> a.name().equals(initOptions().archetypeName()))
                                      .findFirst()
                                      .orElse(null);
            }

            // Find jar and set up loader
            File jarFile = browser.archetypeJar(archetype).toFile();
            require(jarFile.exists(), "%s does not exist", jarFile);
            ArchetypeLoader loader = new ArchetypeLoader(jarFile);

            Map<String, String> initProperties = new HashMap<>();
            initProperties.putAll(Maps.fromProperties(System.getProperties()));
            initProperties.putAll(initOptions().initProperties());

            ArchetypeEngine engine = new ArchetypeEngine(loader, initProperties);

            // Run input flow if not in batch mode
            if (isInteractive()) {
                ArchetypeDescriptor descriptor = engine.descriptor();
                ArchetypeDescriptor.InputFlow inputFlow = descriptor.inputFlow();

                // Process input flow from template and updates properties
                inputFlow.nodes().stream()
                         .map(n -> FlowNodeControllers.create(n, initProperties))
                         .forEach(FlowNodeController::execute);
            }

            Path projectDir = projectDirSupplier().apply(initProperties.get("name"));
            engine.generate(projectDir.toFile());
            return projectDir;
        }
    }

    /**
     * Invoker for the archetype V2 engine.
     */
    static class V2Invoker extends ArchetypeInvoker {
        private static final String ENTRY_POINT_DESCRIPTOR = "flavor.xml";
        private static final String FLAVOR_PROPERTY = "flavor";
        private static final String PROJECT_NAME_PROPERTY = "project.name";
        private static final String GROUP_ID_PROPERTY = "project.groupId";
        private static final String ARTIFACT_ID_PROPERTY = "project.artifactId";
        private static final String PACKAGE_NAME_PROPERTY = "package";
        private static final String HELIDON_VERSION_PROPERTY = "helidon.version";
        private static final String BUILD_SYSTEM_PROPERTY = "build-system";
        private static final String ARCHETYPE_BASE_PROPERTY = "base";
        private static final String SUPPORTED_BUILD_SYSTEM = "maven"; // We only support one

        private V2Invoker(Builder builder) {
            super(builder);
        }

        @Override
        Path invoke() throws IOException {
            InitOptions initOptions = initOptions();
            Map<String, String> params = new HashMap<>();
            Map<String, String> defaults = new HashMap<>();

            // We've already got helidon version, don't prompt again
            params.put(HELIDON_VERSION_PROPERTY, initOptions.helidonVersion());

            // Don't prompt for build system since we only support one for now
            params.put(BUILD_SYSTEM_PROPERTY, SUPPORTED_BUILD_SYSTEM);

            // Set flavor if provided on command-line
            if (initOptions.flavor() != null) {
                params.put(FLAVOR_PROPERTY, initOptions.flavor().toString());
            }

            // Set base if provided on command-line
            if (initOptions.archetypeName() != null) {
                params.put(ARCHETYPE_BASE_PROPERTY, initOptions.archetypeName());
            }
            if (isInteractive()) {

                // Set remaining command-line options as params and user config as defaults

                if (initOptions.projectNameOption() != null) {
                    params.put(PROJECT_NAME_PROPERTY, initOptions.projectNameOption());
                } else {
                    defaults.put(PROJECT_NAME_PROPERTY, initOptions.projectName());
                }
                if (initOptions.groupIdOption() != null) {
                    params.put(GROUP_ID_PROPERTY, initOptions.groupIdOption());
                } else {
                    defaults.put(GROUP_ID_PROPERTY, initOptions.groupId());
                }
                if (initOptions.artifactIdOption() != null) {
                    params.put(ARTIFACT_ID_PROPERTY, initOptions.artifactIdOption());
                } else {
                    defaults.put(ARTIFACT_ID_PROPERTY, initOptions.artifactId());
                }
                if (initOptions.packageNameOption() != null) {
                    params.put(PACKAGE_NAME_PROPERTY, initOptions.packageNameOption());
                } else {
                    defaults.put(PACKAGE_NAME_PROPERTY, initOptions.packageName());
                }

            } else {

                // Batch mode, so pass merged init options as params

                params.put(PROJECT_NAME_PROPERTY, initOptions.projectName());
                params.put(GROUP_ID_PROPERTY, initOptions.groupId());
                params.put(ARTIFACT_ID_PROPERTY, initOptions.artifactId());
                params.put(PACKAGE_NAME_PROPERTY, initOptions.packageName());
            }

            ArchetypeEngineV2 engine = new ArchetypeEngineV2(
                    getArchetype(initOptions().archetypePath()),
                    ENTRY_POINT_DESCRIPTOR,
                    new CLIPrompter(),
                    params,
                    defaults,
                    false,
                    List.of());

            return engine.generate(projectDirSupplier());
        }

        @Override
        EngineVersion engineVersion() {
            return EngineVersion.V2;
        }

        /**
         * Get the archetype file.
         *
         * @param archetypePath path to archetype
         * @return archetype
         */
        private Archetype getArchetype(String archetypePath) throws IOException {
            File archetype = Path.of(archetypePath).toFile();
            if (!archetype.exists()) {
                throw new IOException("Archetype archive does not exist at path : " + archetypePath);
            }
            return ArchetypeFactory.create(archetype);
        }
    }
}
