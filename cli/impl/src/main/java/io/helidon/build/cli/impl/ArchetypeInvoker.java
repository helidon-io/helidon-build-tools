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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import io.helidon.build.archetype.engine.v2.InvocationException;
import io.helidon.build.archetype.engine.v2.TerminalInputResolver;
import io.helidon.build.archetype.engine.v2.UnresolvedInputException;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.common.RequirementFailure;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;
import static io.helidon.build.common.Requirements.require;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static java.util.Collections.unmodifiableMap;

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
    private static final MavenVersion HELIDON_V3 = toMavenVersion("3.0.0");

    private final Metadata metadata;
    private final boolean batch;
    private final InitOptions initOptions;
    private final Map<String, String> initProperties;
    private final Function<String, Path> projectDirSupplier;

    private ArchetypeInvoker(Builder builder) {
        metadata = builder.metadata;
        batch = builder.batch;
        initOptions = builder.initOptions;
        initProperties = unmodifiableMap(builder.initProperties);
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
     * Get the init properties.
     *
     * @return The properties.
     */
    protected Map<String, String> initProperties() {
        return initProperties;
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
        private final Map<String, String> initProperties;
        private Function<String, Path> projectDirSupplier;

        private Builder() {
            initProperties = new HashMap<>();
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
         * Set any properties passed on the command line.
         *
         * @param properties the properties
         * @return this builder
         */
        Builder initProperties(Properties properties) {
            properties.forEach((key, value) -> initProperties.put((String) key, (String) value));
            return this;
        }

        /**
         * Set the project directory supplier.
         * <p>
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
            if (EngineVersion.V2.equals(initOptions.engineVersion())
                    || initOptions.archetypePath() != null
                    || toMavenVersion(initOptions.helidonVersion()).isGreaterThanOrEqualTo(HELIDON_V3)) {
                return new V2Invoker(this);
            }
            return new V1Invoker(this);
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
        Path invoke() {
            InitOptions initOptions = initOptions();
            Map<String, String> defaults = new HashMap<>();

            // Initialize params with any properties passed on the command-line; options will take precedence
            Map<String, String> params = new HashMap<>(initProperties());

            // We've already got helidon version, don't prompt again. Note that this will not override
            // any "helidon.version" command-line property as that is already set in InitOptions
            params.put(HELIDON_VERSION_PROPERTY, initOptions.helidonVersion());

            // Ensure that flavor is lower case if present.
            params.computeIfPresent(FLAVOR_PROPERTY, (key, value) -> value.toLowerCase());

            // Don't prompt for build system since we only support one for now
            params.put(BUILD_SYSTEM_PROPERTY, SUPPORTED_BUILD_SYSTEM);

            // Set flavor if provided on command-line
            if (initOptions.flavorOption() != null) {
                params.put(FLAVOR_PROPERTY, initOptions.flavorOption().toString());
            }

            // Set base if provided on command-line
            if (initOptions.archetypeNameOption() != null) {
                params.put(ARCHETYPE_BASE_PROPERTY, initOptions.archetypeNameOption());
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

            ArchetypeEngineV2 engine = new ArchetypeEngineV2(archetype());
            Map<String, String> initProperties = initOptions().initProperties();
            try {
                return engine.generate(new TerminalInputResolver(System.in), initProperties, defaults, projectDirSupplier());
            } catch (InvocationException ie) {
                if (ie.getCause() instanceof UnresolvedInputException) {
                    UnresolvedInputException uie = (UnresolvedInputException) ie.getCause();
                    String inputPath = uie.inputPath();
                    String option = optionName(inputPath);
                    if (option == null) {
                        throw new RequirementFailure("Missing required option: -D%s=<value>", inputPath);
                    } else {
                        throw new RequirementFailure("Missing required option: %s <value> or -D%s=<value>", option, inputPath);
                    }
                }
                throw ie;
            }
        }

        @Override
        EngineVersion engineVersion() {
            return EngineVersion.V2;
        }

        private static FileSystem archetype() {
            // TODO This is a temporary method which need to be removed
            //  Instead, a mechanism for passing archetype to cli has to be found.


            // TODO grab the archetype path from initOptions if it is there
            //   If the path ends with .zip, create a zip filesystem
            //   Otherwise it is a directory create a virtual filesystem
            // TODO otherwise use a new method in metadata to get the path fo the archetype.zip
            try {
                Path tempDirectory = Files.createTempDirectory("archetype");
                Path data = tempDirectory.resolve("cli-data.zip");
                InputStream is = ArchetypeInvoker.class.getResourceAsStream("/cli-data.zip");
                if (is == null) {
                    throw new IllegalArgumentException("cli-data.zip not found in class-path");
                }
                Files.copy(is, data);
                return FileSystems.newFileSystem(data, null);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private static String optionName(String inputPath) {
            switch (inputPath) {
                case FLAVOR_PROPERTY:
                    return "--flavor";
                case BUILD_SYSTEM_PROPERTY:
                    return "--build";
                case HELIDON_VERSION_PROPERTY:
                    return "--version";
                case ARCHETYPE_BASE_PROPERTY:
                    return "--archetype";
                case GROUP_ID_PROPERTY:
                    return "--groupId";
                case ARTIFACT_ID_PROPERTY:
                    return "--artifactId";
                case PACKAGE_NAME_PROPERTY:
                    return "--package";
                case PROJECT_NAME_PROPERTY:
                    return "--name";
                default:
                    return null;
            }
        }
    }
}
