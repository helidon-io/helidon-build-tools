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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v1.ArchetypeEngine;
import io.helidon.build.archetype.engine.v1.ArchetypeLoader;
import io.helidon.build.archetype.engine.v1.FlowNodeControllers;
import io.helidon.build.archetype.engine.v1.FlowNodeControllers.FlowNodeController;
import io.helidon.build.archetype.engine.v1.Maps;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.util.MavenVersion;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;
import static io.helidon.build.util.Requirements.require;

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
    private final Supplier<Path> projectDirSupplier;

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
    protected Supplier<Path> projectDirSupplier() {
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
        private Supplier<Path> projectDirSupplier;

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
         * @param projectDirSupplier project directory supplier
         * @return this builder
         */
        Builder projectDir(Supplier<Path> projectDirSupplier) {
            this.projectDirSupplier = projectDirSupplier;
            return this;
        }

        /**
         * Build the invoker instance.
         *
         * @return {@link V1Invoker} if the configured Helidon version is associated with the V1 engine,
         * otherwise {@link V2Invoker}
         */
        ArchetypeInvoker build() {
            if (MavenVersion.toMavenVersion(initOptions.helidonVersion()).isLessThan(HELIDON_V3)) {
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

            Path projectDir = projectDirSupplier().get();
            engine.generate(projectDir.toFile());
            return projectDir;
        }
    }

    /**
     * Invoker for the archetype V1 engine.
     */
    static class V2Invoker extends ArchetypeInvoker {

        private V2Invoker(Builder builder) {
            super(builder);
        }

        @Override
        Path invoke() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        EngineVersion engineVersion() {
            return EngineVersion.V2;
        }
    }
}
