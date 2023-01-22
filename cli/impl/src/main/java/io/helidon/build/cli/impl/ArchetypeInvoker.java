/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
import io.helidon.build.archetype.engine.v2.ArchetypeEngineV2;
import io.helidon.build.archetype.engine.v2.BatchInputResolver;
import io.helidon.build.archetype.engine.v2.InputResolver;
import io.helidon.build.archetype.engine.v2.InvalidInputException;
import io.helidon.build.archetype.engine.v2.InvocationException;
import io.helidon.build.archetype.engine.v2.TerminalInputResolver;
import io.helidon.build.archetype.engine.v2.UnresolvedInputException;
import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.common.Maps;
import io.helidon.build.common.RequirementFailure;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_ARCHETYPE;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.cli.common.ProjectConfig.createProjectConfig;
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
    private static final MavenVersion HELIDON_V3 = toMavenVersion("3.0.0-alpha");


    private static final String HELIDON_VERSION_NOT_FOUND = "$(red Helidon version) $(RED %s) $(red not found.)";

    private final Metadata metadata;
    private final InitOptions initOptions;
    private final Map<String, String> initProperties;
    private final Function<String, Path> projectDirSupplier;
    private final UserConfig userConfig;
    private final Runnable onResolved;

    private ArchetypeInvoker(Builder builder) {
        metadata = builder.metadata;
        initOptions = builder.initOptions;
        initProperties = unmodifiableMap(builder.initProperties);
        projectDirSupplier = builder.projectDirSupplier;
        userConfig = builder.userConfig;
        onResolved = builder.onResolved;
    }

    /**
     * Get the interactive flag.
     *
     * @return {@code true} if interactive, {@code false} if batch
     */
    protected boolean isInteractive() {
        return !initOptions.batch();
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
     * Get the user config.
     *
     * @return UserConfig
     */
    protected UserConfig userConfig() {
        return userConfig;
    }

    /**
     * Get the {@code onResolved} callback.
     *
     * @return Runnable
     */
    protected Runnable onResolved() {
        return onResolved;
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
    @SuppressWarnings("unused")
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
        private InitOptions initOptions;
        private final Map<String, String> initProperties;
        private Function<String, Path> projectDirSupplier;
        private UserConfig userConfig;
        private Runnable onResolved;

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
         * Set the user config.
         *
         * @param config the config
         * @return this builder
         */
        Builder userConfig(UserConfig config) {
            this.userConfig = config;
            return this;
        }

        /**
         * Set the callback to be invoked when inputs are fully resolved.
         *
         * @param onResolved callback
         * @return this builder
         */
        Builder onResolved(Runnable onResolved) {
            this.onResolved = onResolved;
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

        private boolean isHelidonV3() {
            String rawVersion = initOptions.helidonVersion().replace("-SNAPSHOT", "");
            return toMavenVersion(rawVersion).isGreaterThanOrEqualTo(HELIDON_V3);
        }

        /**
         * Build the invoker instance.
         *
         * @return {@link V1Invoker} if the configured Helidon archetype version is associated with the V1 engine,
         * otherwise {@link V2Invoker}
         */
        ArchetypeInvoker build() {
            if (isHelidonV3()) {
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
            InitOptions initOptions = initOptions();
            String helidonVersion = initOptions.helidonVersion();

            if (isInteractive()) {
                // Select flavor interactively
                String[] flavorOptions = new String[]{"SE", "MP"};
                int flavorIndex = initOptions.flavor() == Flavor.SE ? 0 : 1;
                flavorIndex = prompt("Helidon flavor", flavorOptions, flavorIndex);
                initOptions.flavor(Flavor.valueOf(flavorOptions[flavorIndex]));
            }

            Flavor flavor = initOptions.flavor();

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
                initOptions.archetypeName(archetype.name());
            } else {
                // find the archetype that matches archetypeName
                archetype = archetypes.stream()
                                      .filter(a -> a.name().equals(initOptions().archetypeName()))
                                      .findFirst()
                                      .orElse(null);
            }

            initOptions.applyConfig(userConfig(), EngineVersion.V1);

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
            engine.generate(projectDir);

            // Create config file that includes feature information
            ProjectConfig configFile = createProjectConfig(projectDir, helidonVersion);
            configFile.property(PROJECT_FLAVOR, initOptions.flavor().toString());
            configFile.property(PROJECT_ARCHETYPE, initOptions.archetypeName());
            configFile.store();

            return projectDir;
        }
    }

    /**
     * Invoker for the archetype V2 engine.
     */
    static class V2Invoker extends ArchetypeInvoker {

        private static final String FLAVOR_PROPERTY = "flavor";
        private static final String GROUP_ID_PROPERTY = "groupId";
        private static final String ARTIFACT_ID_PROPERTY = "artifactId";
        private static final String PACKAGE_NAME_PROPERTY = "package";
        private static final String BUILD_SYSTEM_PROPERTY = "build-system";
        private static final String ARCHETYPE_BASE_PROPERTY = "app-type";

        private V2Invoker(Builder builder) {
            super(builder);
        }

        @Override
        Path invoke() {
            InitOptions initOptions = initOptions();
            initOptions.applyConfig(userConfig(), EngineVersion.V2);
            initOptions.applyOverrides(initProperties());
            Map<String, String> externalDefaults = new HashMap<>();

            // Initialize params with any properties passed on the command-line; options will take precedence
            Map<String, String> externalValues = new HashMap<>(initProperties());

            // Ensure that flavor is lower case if present.
            externalValues.computeIfPresent(FLAVOR_PROPERTY, (key, value) -> value.toLowerCase());

            // Set build if provided on command-line
            if (initOptions.buildOption() != null) {
                externalValues.put(BUILD_SYSTEM_PROPERTY, initOptions.buildOption().toString());
            }

            // Set flavor if provided on command-line
            if (initOptions.flavorOption() != null) {
                externalValues.put(FLAVOR_PROPERTY, initOptions.flavorOption().toString());
            }

            // Set base if provided on command-line
            if (initOptions.archetypeNameOption() != null) {
                externalValues.put(ARCHETYPE_BASE_PROPERTY, initOptions.archetypeNameOption());
            }

            // Warn if name provided on command-line
            if (initOptions.projectNameOption() != null) {
                Log.warn("--name option is not used in Helidon 3.x");
            }

            InputResolver resolver;
            if (isInteractive()) {
                resolver = new TerminalInputResolver(System.in);

                // Set remaining command-line options as external values and user config as external defaults
                if (initOptions.groupIdOption() != null) {
                    externalValues.put(GROUP_ID_PROPERTY, initOptions.groupIdOption());
                } else {
                    externalDefaults.put(GROUP_ID_PROPERTY, initOptions.groupId());
                }
                if (initOptions.artifactIdOption() != null) {
                    externalValues.put(ARTIFACT_ID_PROPERTY, initOptions.artifactIdOption());
                } else {
                    externalDefaults.put(ARTIFACT_ID_PROPERTY, initOptions.artifactId());
                }
                if (initOptions.packageNameOption() != null) {
                    externalValues.put(PACKAGE_NAME_PROPERTY, initOptions.packageNameOption());
                } else {
                    externalDefaults.put(PACKAGE_NAME_PROPERTY, initOptions.packageName());
                }
            } else {
                resolver = new BatchInputResolver();

                // Add defaults in case of the user has not specify mandatory options
                externalDefaults.put(ARCHETYPE_BASE_PROPERTY, initOptions.archetypeName());
                externalDefaults.put(FLAVOR_PROPERTY, initOptions.flavor().toString());

                // Batch mode, so pass merged init options as external values
                externalValues.put(GROUP_ID_PROPERTY, initOptions.groupId());
                externalValues.put(ARTIFACT_ID_PROPERTY, initOptions.artifactId());
                externalValues.put(PACKAGE_NAME_PROPERTY, initOptions.packageName());
            }

            //noinspection ConstantConditions
            ArchetypeEngineV2 engine = ArchetypeEngineV2.builder()
                                                        .fileSystem(archetype())
                                                        .inputResolver(resolver)
                                                        .externalValues(externalValues)
                                                        .externalDefaults(externalDefaults)
                                                        .onResolved(onResolved())
                                                        .directorySupplier(projectDirSupplier())
                                                        .build();
            try {
                return engine.generate();
            } catch (InvocationException ie) {
                Throwable cause = ie.getCause();
                if (cause instanceof UnresolvedInputException) {
                    UnresolvedInputException uie = (UnresolvedInputException) cause;
                    String inputPath = uie.inputPath();
                    String option = optionName(inputPath);
                    if (option == null) {
                        throw new RequirementFailure("Missing required option: -D%s=<value>", inputPath);
                    } else {
                        throw new RequirementFailure("Missing required option: %s <value> or -D%s=<value>", option, inputPath);
                    }
                } else if (cause instanceof InvalidInputException) {
                    throw new RequirementFailure(cause.getMessage());
                }
                throw ie;
            }
        }

        @Override
        EngineVersion engineVersion() {
            return EngineVersion.V2;
        }

        private FileSystem archetype() {
            String helidonVersion = initOptions().helidonVersion();
            try {
                Path archetype = metadata().archetypeV2Of(helidonVersion);
                return FileSystems.newFileSystem(archetype, this.getClass().getClassLoader());
            } catch (Metadata.UpdateFailed | Plugins.PluginFailedUnchecked e) {
                Requirements.failed(HELIDON_VERSION_NOT_FOUND, helidonVersion);
                return null;
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
                case ARCHETYPE_BASE_PROPERTY:
                    return "--archetype";
                case GROUP_ID_PROPERTY:
                    return "--groupId";
                case ARTIFACT_ID_PROPERTY:
                    return "--artifactId";
                case PACKAGE_NAME_PROPERTY:
                    return "--package";
                default:
                    return null;
            }
        }
    }
}
