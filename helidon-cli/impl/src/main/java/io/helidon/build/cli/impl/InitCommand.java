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

import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.archetype.engine.v1.Prompter;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.impl.InitOptions.BuildSystem;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.Requirements;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;
import static io.helidon.build.cli.impl.ArchetypeInvoker.EngineVersion.V1;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.Requirements.failed;
import static io.helidon.build.util.StyleFunction.BoldBrightCyan;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand extends BaseCommand {

    private final CommonOptions commonOptions;
    private final InitOptions initOptions;
    private final Metadata metadata;
    private final UserConfig config;
    private final boolean batch;

    @Creator
    InitCommand(CommonOptions commonOptions, InitOptions initOptions,
                @Flag(name = "batch", description = "Enables non-interactive mode") boolean batch) {

        super(commonOptions, initOptions.helidonVersion() != null);
        this.commonOptions = commonOptions;
        this.initOptions = initOptions;
        this.batch = batch;
        this.metadata = metadata();
        this.config = Config.userConfig();
    }

    @Override
    protected void assertPreconditions() {
        if (initOptions.build() == BuildSystem.MAVEN) {
            requireMinimumMavenVersion();
        } else {
            failed("$(red Gradle is not yet supported.)");
        }
    }

    @Override
    protected void invoke(CommandContext context) throws Exception {

        // Attempt to find default Helidon version if none provided
        String helidonVersion = initOptions.helidonVersion();
        if (helidonVersion == null) {
            if (batch) {
                helidonVersion = defaultHelidonVersion();
            } else {
                String defaultHelidonVersion = null;
                try {
                    defaultHelidonVersion = defaultHelidonVersion();
                } catch (Exception ignored) {
                    // ignore default version lookup error
                    // since we always prompt in interactive
                }
                helidonVersion = prompt("Helidon version", defaultHelidonVersion);
            }
            initOptions.helidonVersion(helidonVersion);
        }
        Log.info("Using Helidon version " + helidonVersion);

        ArchetypeInvoker archetypeInvoker = ArchetypeInvoker
                .builder()
                .batch(batch)
                .metadata(metadata)
                .initOptions(initOptions)
                .projectDir(this::initProjectDir)
                .build();

        if (archetypeInvoker.engineVersion() == V1 && !batch) {
            String[] flavorOptions = new String[]{"SE", "MP"};
            int flavorIndex = initOptions.flavor() == Flavor.SE ? 0 : 1;
            flavorIndex = prompt("Helidon flavor", flavorOptions, flavorIndex);
            initOptions.flavor(Flavor.valueOf(flavorOptions[flavorIndex]));
        }
        initOptions.applyConfig(config);

        Path projectDir = archetypeInvoker.invoke();

        // Create config file that includes feature information
        ProjectConfig configFile = projectConfig(projectDir);
        configFile.property(PROJECT_DIRECTORY, projectDir.toString());
        configFile.property(PROJECT_FLAVOR, initOptions.flavor().toString());
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

    private Path initProjectDir() {
        boolean projectDirSpecified = commonOptions.projectSpecified();
        Path projectDir = commonOptions.project();
        String projectName = initOptions.projectName();
        if (!projectDirSpecified) {
            projectDir = projectDir.resolve(projectName);
        }
        if (Files.exists(projectDir)) {
            if (projectDirSpecified || config.failOnProjectNameCollision()) {
                Requirements.failed("$(red Directory $(plain %s) already exists)", projectDir);
            }
            Log.info();
            Log.info("$(italic,yellow Directory $(plain %s) already exists, generating unique name)", projectDir);
            Path parentDirectory = projectDir.getParent();
            for (int i = 2; i < 128; i++) {
                Path newProjectDir = parentDirectory.resolve(projectName + "-" + i);
                if (!Files.exists(newProjectDir)) {
                    return newProjectDir;
                }
            }
            Requirements.failed("Too many existing directories named %s-NN", projectName);
        }
        return projectDir;
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
