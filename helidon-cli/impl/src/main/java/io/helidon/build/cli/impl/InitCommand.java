/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.cli.impl.ArchetypeBrowser.ARCHETYPE_NOT_FOUND;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.cli.impl.Prompter.prompt;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.Requirements.failed;
import static io.helidon.build.util.Requirements.require;
import static io.helidon.build.util.Style.BoldBrightCyan;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand extends BaseCommand {

    private final CommonOptions commonOptions;
    private final boolean batch;
    private Flavor flavor;
    private final Build build;
    private final String archetypeName;
    private String helidonVersion;
    private final String groupId;
    private final String artifactId;
    private final String packageName;
    private final String name;
    private final Metadata metadata;

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
    static final String DEFAULT_ARCHETYPE_ID = "bare";
    static final String DEFAULT_GROUP_ID = "mygroupid";
    static final String DEFAULT_ARTIFACT_ID = "myartifactid";
    static final String DEFAULT_PACKAGE = "mypackage";
    static final String DEFAULT_NAME = "myproject";

    @Creator
    InitCommand(CommonOptions commonOptions,
                @Flag(name = "batch", description = "Enables non-interactive mode") boolean batch,
                @KeyValue(name = "flavor", description = "Helidon flavor",
                        defaultValue = DEFAULT_FLAVOR) Flavor flavor,
                @KeyValue(name = "build", description = "Build type",
                        defaultValue = "MAVEN") Build build,
                @KeyValue(name = "version", description = "Helidon version") String version,
                @KeyValue(name = "archetype", description = "Archetype name",
                        defaultValue = DEFAULT_ARCHETYPE_ID) String archetypeName,
                @KeyValue(name = "groupid", description = "Project's group ID",
                        defaultValue = DEFAULT_GROUP_ID) String groupId,
                @KeyValue(name = "artifactid", description = "Project's artifact ID",
                        defaultValue = DEFAULT_ARTIFACT_ID) String artifactId,
                @KeyValue(name = "package", description = "Project's package name",
                        defaultValue = DEFAULT_PACKAGE) String packageName,
                @KeyValue(name = "name", description = "Project's name",
                        defaultValue = DEFAULT_NAME) String projectName) {
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
        this.name = projectName;
        this.metadata = metadata();
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

        // Attempt to find default Helidon version if none provided
        if (helidonVersion == null) {
            try {
                helidonVersion = defaultHelidonVersion();
                Log.info("Using Helidon version " + helidonVersion);
            } catch (Exception e) {
                // If in batch mode we cannot proceed
                if (batch) {
                    throw e;
                }
            }
        } else {
            Log.info("Using Helidon version " + helidonVersion);
        }

        // Need Helidon version and flavor to proceed
        if (!batch) {
            if (helidonVersion == null) {
                helidonVersion = prompt("Helidon version", helidonVersion);
            }
            String[] flavorOptions = new String[]{"SE", "MP"};
            int flavorIndex = prompt("Helidon flavor", flavorOptions, flavor == Flavor.SE ? 0 : 1);
            flavor = Flavor.valueOf(flavorOptions[flavorIndex]);
        }

        // Gather archetype names
        ArchetypeBrowser browser = new ArchetypeBrowser(metadata, flavor, helidonVersion);
        List<ArchetypeCatalog.ArchetypeEntry> archetypes = browser.archetypes();
        require(!archetypes.isEmpty(), "Unable to find archetypes for %s and %s.", flavor, helidonVersion);

        ArchetypeCatalog.ArchetypeEntry archetype;
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
        Path parentDirectory = commonOptions.project();
        Path projectDir = parentDirectory.resolve(properties.get("name"));
        require(!projectDir.toFile().exists(), "Directory %s already exists", projectDir);
        engine.generate(projectDir.toFile());

        // Create config file that includes feature information
        ProjectConfig configFile = projectConfig(projectDir);
        configFile.property(PROJECT_DIRECTORY, projectDir.toString());
        configFile.property(PROJECT_FLAVOR, flavor.toString());
        configFile.property(HELIDON_VERSION_PROPERTY, helidonVersion);
        configFile.store();

        String dir = BoldBrightCyan.apply(parentDirectory + Constants.DIR_SEP + projectDir.getFileName());
        Prompter.displayLine("Switch directory to " + dir + " to use CLI");

        if (!batch) {
            Prompter.displayLine("");
            boolean startDev = Prompter.promptYesNo("Start development loop?", false);
            if (startDev) {
                CommonOptions commonOptions = new CommonOptions(projectDir, this.commonOptions);
                DevCommand devCommand = new DevCommand(commonOptions,
                        true, false, null, false);
                devCommand.execute(context);
            }
        }
    }

    private Map<String, String> initProperties() {
        Map<String, String> properties = Maps.fromProperties(System.getProperties());
        properties.put("groupId", groupId);
        properties.put("artifactId", artifactId);
        properties.put("package", packageName);
        properties.put("name", name);
        properties.put("helidonVersion", helidonVersion);
        properties.putIfAbsent("maven", "true");        // No gradle support yet
        return properties;
    }

    private String defaultHelidonVersion() {
        // Check the system property first, primarily to support tests
        String version = System.getProperty(HELIDON_VERSION_PROPERTY);
        if (version == null) {
            try {
                version = metadata.latestVersion(true).toString();
                Log.debug("Latest Helidon version found: %s", version);
            } catch (Plugins.PluginFailed e) {
                Log.info(e.getMessage());
                failed("$(italic Cannot lookup version, please specify with --version option.)");
            } catch (Exception e) {
                Log.info("$(italic,red %s)", e.getMessage());
                failed("$(italic Cannot lookup version, please specify with --version option.)");
            }
        }
        return version;
    }
}
