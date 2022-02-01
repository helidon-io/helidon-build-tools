/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.archetype.engine.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.ArchetypeEngine;
import io.helidon.build.archetype.engine.ArchetypeLoader;
import io.helidon.build.archetype.engine.Maps;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.cli.impl.FlowNodeControllers.FlowNodeController;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.RequirementFailure;
import io.helidon.build.util.Requirements;
import io.helidon.build.util.SubstitutionVariables;

import static io.helidon.build.cli.impl.ArchetypeBrowser.ARCHETYPE_NOT_FOUND;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.cli.impl.CommonOptions.UPDATE_URL;
import static io.helidon.build.cli.impl.Metadata.HELIDON_3;
import static io.helidon.build.cli.impl.Prompter.prompt;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.Requirements.failed;
import static io.helidon.build.util.Requirements.require;
import static io.helidon.build.util.StyleFunction.BoldBrightCyan;
import static io.helidon.build.util.SubstitutionVariables.systemPropertyOrEnvVarSource;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand extends BaseCommand {
    private static final String HELIDON_RELEASES_URL = "https://github.com/oracle/helidon/releases";
    private static final String VERSION_LOOKUP_FAILED = "$(italic Cannot lookup version, please specify with --version option.)";
    private static final String HELIDON_3_MESSAGE = "$(italic,yellow This version of the CLI does not support Helidon 3.x.)%n"
                                                    + "Please see $(blue %s) to upgrade.";
    private static final String VERSION_NOT_FOUND_MESSAGE = "$(italic Helidon version $(red %s) not found.)";
    private static final String AVAILABLE_VERSIONS_MESSAGE = "Please see $(blue %s) for available versions.";
    private static final String NOT_FOUND_STATUS = "404";

    private final CommonOptions commonOptions;
    private final boolean batch;
    private Flavor flavor;
    private final Build build;
    private String helidonVersion;
    private final String archetypeName;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String projectName;
    private ArchetypeCatalog.ArchetypeEntry archetype;
    private final Metadata metadata;
    private final UserConfig config;

    /**
     * Helidon flavors.
     */
    enum Flavor {
        MP("mp"),
        SE("se");

        private final String flavor;

        Flavor(String flavor) {
            this.flavor = flavor;
        }

        @Override
        public String toString() {
            return flavor;
        }
    }

    /**
     * Build systems.
     */
    enum Build {
        MAVEN,
        GRADLE,
    }

    static final String DEFAULT_FLAVOR = "SE";
    static final String DEFAULT_ARCHETYPE_NAME = "bare";

    @Creator
    InitCommand(CommonOptions commonOptions,
                @Flag(name = "batch", description = "Enables non-interactive mode") boolean batch,
                @KeyValue(name = "flavor", description = "Helidon flavor",
                        defaultValue = DEFAULT_FLAVOR) Flavor flavor,
                @KeyValue(name = "build", description = "Build type",
                        defaultValue = "MAVEN") Build build,
                @KeyValue(name = "version", description = "Helidon version") String version,
                @KeyValue(name = "archetype", description = "Archetype name",
                        defaultValue = DEFAULT_ARCHETYPE_NAME) String archetypeName,
                @KeyValue(name = "groupid", description = "Project's group ID") String groupId,
                @KeyValue(name = "artifactid", description = "Project's artifact ID") String artifactId,
                @KeyValue(name = "package", description = "Project's package name") String packageName,
                @KeyValue(name = "name", description = "Project's name") String projectName) {
        super(commonOptions, version != null);
        this.commonOptions = commonOptions;
        this.batch = batch;
        this.build = build;
        this.helidonVersion = version;
        this.flavor = flavor;
        this.archetypeName = archetypeName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.packageName = packageName;
        this.projectName = projectName;
        this.metadata = metadata();
        this.config = Config.userConfig();
    }

    @Override
    protected void assertPreconditions() {
        if (build == Build.MAVEN) {
            requireMinimumMavenVersion();
        } else {
            failed("$(red Gradle is not yet supported.)");
        }
    }

    @Override
    protected void invoke(CommandContext context) throws Exception {

        // Make sure we have a valid Helidon version
        if (helidonVersion == null) {
            if (context.properties().containsKey(HELIDON_VERSION_PROPERTY)) {
                helidonVersion = context.properties().getProperty(HELIDON_VERSION_PROPERTY);
                assertSupportedVersion(helidonVersion, true);
            } else if (batch) {
                helidonVersion = defaultHelidonVersion();
                Log.info("Using Helidon version " + helidonVersion);
            } else {
                String defaultHelidonVersion = null;
                try {
                    defaultHelidonVersion = defaultHelidonVersion();
                } catch (RequirementFailure e) {
                    throw e;
                } catch (Exception ignored) {
                    // ignore default version lookup error since we always prompt in interactive
                }
                helidonVersion = prompt("Helidon version", defaultHelidonVersion, this::isSupportedVersion);
            }
        } else {
            assertSupportedVersion(helidonVersion, true);
        }

        // Need flavor to proceed
        if (!batch) {
            String[] flavorOptions = new String[]{"SE", "MP"};
            int flavorIndex = prompt("Helidon flavor", flavorOptions, flavor == Flavor.SE ? 0 : 1);
            flavor = Flavor.valueOf(flavorOptions[flavorIndex]);
        }

        // Gather archetype names
        ArchetypeBrowser browser = new ArchetypeBrowser(metadata, flavor, helidonVersion);
        List<ArchetypeCatalog.ArchetypeEntry> archetypes = browser.archetypes();
        require(!archetypes.isEmpty(), "Unable to find archetypes for %s and %s.", flavor, helidonVersion);

        if (!batch) {
            // Select archetype interactively
            List<String> descriptions = archetypes.stream()
                                                  .map(a -> a.name() + " | " + a.description().orElse(a.summary()))
                                                  .collect(Collectors.toList());
            int archetypeIndex = prompt("Select archetype", descriptions, 0);
            archetype = archetypes.get(archetypeIndex);
        } else {
            // find the archetype that matches archetypeName
            archetype = archetypes.stream()
                                  .filter(a -> a.name().equals(archetypeName))
                                  .findFirst()
                                  .orElse(null);

            if (archetype == null) {
                failed(ARCHETYPE_NOT_FOUND, archetypeName, helidonVersion);
            }
        }

        // Initialize arguments with defaults if needed
        initArguments();

        // Find jar and set up loader
        ArchetypeLoader loader;
        File jarFile = browser.archetypeJar(archetype).toFile();
        require(jarFile.exists(), "%s does not exist", jarFile);
        loader = new ArchetypeLoader(jarFile);

        // Initialize mutable set of properties and engine
        Map<String, String> properties = initProperties();
        ArchetypeEngine engine = new ArchetypeEngine(loader, properties);
        // Run input flow if not in batch mode
        if (!batch) {
            ArchetypeDescriptor descriptor = engine.descriptor();
            ArchetypeDescriptor.InputFlow inputFlow = descriptor.inputFlow();

            // Process input flow from template and updates properties
            inputFlow.nodes().stream()
                     .map(n -> FlowNodeControllers.create(n, properties))
                     .forEach(FlowNodeController::execute);
        }

        // Generate project using archetype engine
        Path projectDir = initProjectDir(properties.get("name"));
        engine.generate(projectDir.toFile());

        // Create config file that includes feature information
        ProjectConfig configFile = projectConfig(projectDir);
        configFile.property(PROJECT_DIRECTORY, projectDir.toString());
        configFile.property(PROJECT_FLAVOR, flavor.toString());
        configFile.property(HELIDON_VERSION_PROPERTY, helidonVersion);
        configFile.store();

        String dir = BoldBrightCyan.apply(projectDir);
        Prompter.displayLine("Switch directory to " + dir + " to use CLI");

        if (!batch) {
            Prompter.displayLine("");
            boolean startDev = Prompter.promptYesNo("Start development loop?", false);
            if (startDev) {
                CommonOptions commonOptions = new CommonOptions(projectDir, this.commonOptions);
                DevCommand devCommand = new DevCommand(commonOptions);
                devCommand.execute(context);
            }
        }
    }

    private void initArguments() {
        SubstitutionVariables substitutions = SubstitutionVariables.of(key -> {
            switch (key.toLowerCase()) {
                case "init_flavor":
                    return flavor.toString();
                case "init_archetype":
                    return archetype.name();
                case "init_build":
                    return build.name().toLowerCase();
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

    private Path initProjectDir(String name) {
        boolean projectDirSpecified = commonOptions.projectSpecified();
        Path projectDir = commonOptions.project();

        if (name == null || name.isEmpty()) {
            name = projectName;
        }

        if (!projectDirSpecified) {
            projectDir = projectDir.resolve(name);
        }
        if (Files.exists(projectDir)) {
            if (projectDirSpecified || config.failOnProjectNameCollision()) {
                Requirements.failed("$(red Directory $(plain %s) already exists)", projectDir);
            }
            Log.info();
            Log.info("$(italic,yellow Directory $(plain %s) already exists, generating unique name)", projectDir);
            Path parentDirectory = projectDir.getParent();
            for (int i = 2; i < 128; i++) {
                Path newProjectDir = parentDirectory.resolve(name + "-" + i);
                if (!Files.exists(newProjectDir)) {
                    return newProjectDir;
                }
            }
            Requirements.failed("Too many existing directories named %s-NN", name);
        }
        return projectDir;
    }

    private Map<String, String> initProperties() {
        Map<String, String> result = Maps.fromProperties(System.getProperties());
        result.put("name", projectName);
        result.put("groupId", groupId);
        result.put("artifactId", artifactId);
        result.put("package", packageName);
        result.put("helidonVersion", helidonVersion);
        result.putIfAbsent("maven", "true");        // No gradle support yet
        return result;
    }

    private String defaultHelidonVersion() {
        // Check the system property first, primarily to support tests
        String version = System.getProperty(HELIDON_VERSION_PROPERTY);
        if (version == null) {
            try {
                version = metadata.latestVersion().toString();
                // This should only fail when --url is used pointing to 3.x or newer
                assertSupportedVersion(version, false);
                Log.debug("Latest Helidon version found: %s", version);
            } catch (RequirementFailure e) {
                throw e;
            } catch (Plugins.PluginFailedUnchecked e) {
                failed(VERSION_LOOKUP_FAILED);
            } catch (Exception e) {
                versionLookupFailed(e.getMessage());
            }
        }
        return version;
    }

    private boolean isSupportedVersion(String helidonVersion) {
        MavenVersion version = MavenVersion.toMavenVersion(helidonVersion);
        if (version.isGreaterThanOrEqualTo(HELIDON_3)) {
            Log.info();
            Log.warn(HELIDON_3_MESSAGE, UPDATE_URL);
            Log.info();
            return false;
        }
        return isSupportedVersion(version, false);
    }

    private boolean isSupportedVersion(MavenVersion version, boolean notFoundWillFail) {
        try {
            metadata.assertVersionIsAvailable(version);
            return true;
        } catch (IllegalArgumentException | Metadata.UpdateFailed | Plugins.PluginFailedUnchecked e) {
            String message = e.getMessage();
            if (!message.contains(NOT_FOUND_STATUS)) {
                versionLookupFailed(message);
            }
            if (!(e instanceof Plugins.PluginFailedUnchecked)) {
                Log.debug(message);
            }
            if (notFoundWillFail) {
                Log.info(VERSION_NOT_FOUND_MESSAGE, version);
            } else {
                Log.info();
                Log.info(VERSION_NOT_FOUND_MESSAGE, version);
                Log.info(AVAILABLE_VERSIONS_MESSAGE, HELIDON_RELEASES_URL);
                Log.info();
            }
            return false;
        } catch (Exception e) {
            versionLookupFailed(e.getMessage());
        }

        return false;
    }

    private void versionLookupFailed(String errorMessage) {
        Log.info("$(italic,red %s)", errorMessage);
        failed(VERSION_LOOKUP_FAILED);
    }

    private void assertSupportedVersion(String helidonVersion, boolean checkMetadata) {
        MavenVersion version = MavenVersion.toMavenVersion(helidonVersion);
        require(version.isLessThan(HELIDON_3), HELIDON_3_MESSAGE, UPDATE_URL);
        if (checkMetadata) {
            require(isSupportedVersion(version, true), AVAILABLE_VERSIONS_MESSAGE, HELIDON_RELEASES_URL);
        }
    }
}
