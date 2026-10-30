/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.archetype.engine.v1.Prompter;
import io.helidon.build.cli.common.ArchetypesData;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.impl.InitOptions.BuildSystem;
import io.helidon.build.common.Lists;
import io.helidon.build.common.RequirementFailure;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;

import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.common.Requirements.failed;
import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBrightCyan;

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
    InitCommand(CommonOptions commonOptions, InitOptions initOptions) {
        super(commonOptions, initOptions.helidonVersion() != null);
        this.commonOptions = commonOptions;
        this.initOptions = initOptions;
        this.batch = initOptions.batch();
        this.metadata = metadata();
        this.config = UserConfig.current();
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Prompter prompter = new Prompter(reader);

        // Make sure we have a valid Helidon version
        String helidonVersion = initOptions.helidonVersion();
        if (helidonVersion == null) {
            ArchetypesData archetypesData = metadata.archetypesData();
            String helidonVersionPropValue = context.properties().getProperty(HELIDON_VERSION_PROPERTY);
            if (helidonVersionPropValue != null) {
                helidonVersion = resolveHelidonVersion(helidonVersionPropValue);
            } else if (batch) {
                helidonVersion = archetypesData.defaultVersion().toString();
                Log.info("Using Helidon version " + helidonVersion);
            } else {
                helidonVersion = promptHelidonVersion(archetypesData, true, prompter);
            }
            initOptions.helidonVersion(helidonVersion);
        } else {
            helidonVersion = resolveHelidonVersion(helidonVersion);
            initOptions.helidonVersion(helidonVersion);
        }

        prompter.displayLine("");
        prompter.displayLine(Bold.apply("Helidon version: ") + BoldBlue.apply(helidonVersion));

        ArchetypeInvoker archetypeInvoker = ArchetypeInvoker
                .builder()
                .metadata(metadata)
                .initOptions(initOptions)
                .userConfig(config)
                .onResolved(Log::info)
                .initProperties(context.properties())
                .reader(reader)
                .prompter(prompter)
                .projectDir(this::initProjectDir)
                .build();

        Path projectDir = archetypeInvoker.invoke();

        String dir = BoldBrightCyan.apply(projectDir);
        prompter.displayLine("Switch directory to " + dir + " to use CLI");

        if (!batch) {
            prompter.displayLine("");
            boolean startDev = prompter.promptYesNo("Start development loop?", false);
            if (startDev) {
                CommonOptions commonOptions = new CommonOptions(projectDir, this.commonOptions);
                DevCommand devCommand = new DevCommand(commonOptions);
                devCommand.execute(context);
            }
        }
    }

    private String promptHelidonVersion(ArchetypesData archetypesData, boolean showLatest, Prompter prompter) {
        List<String> versions = archetypesData.versions();
        if (showLatest) {
            versions = archetypesData.latestMajorVersions();
        }
        int defaultOption = archetypesData.defaultVersionIndex(versions);
        if (showLatest) {
            versions.add("Show all versions");
        }
        var result = versions.get(prompter.prompt("Helidon versions", versions, defaultOption));
        if (showLatest && "Show all versions".equals(result)) {
            return promptHelidonVersion(archetypesData, false, prompter);
        }
        return result;
    }

    private Path initProjectDir(String name) {
        boolean projectDirSpecified = commonOptions.projectSpecified();
        Path projectDir = commonOptions.project();

        String projectName = name;
        if (projectName == null || projectName.isEmpty()) {
            projectName = initOptions.projectName();
        }

        if (!projectDirSpecified) {
            projectDir = projectDir.resolve(projectName);
        }
        if (Files.exists(projectDir)) {
            if (projectDirSpecified || config.failOnProjectNameCollision()) {
                failed("$(red Directory $(plain %s) already exists)", projectDir);
            }
            Log.info("$(italic,yellow Directory $(plain %s) already exists, generating unique name)", projectDir);
            Path parentDirectory = projectDir.getParent();
            for (int i = 2; i < 128; i++) {
                Path newProjectDir = parentDirectory.resolve(projectName + "-" + i);
                if (!Files.exists(newProjectDir)) {
                    return newProjectDir;
                }
            }
            failed("Too many existing directories named %s-NN", projectName);
        }
        return projectDir;
    }

    private String resolveHelidonVersion(String helidonVersion) throws Metadata.UpdateFailed {
        ArchetypesData data = metadata().archetypesData();
        MavenVersion resolved = VersionRange.wildcard(helidonVersion)
                .matchVersion(Lists.map(data.versions(), MavenVersion::toMavenVersion));
        if (resolved != null) {
            return resolved.toString();
        }
        throw new RequirementFailure("$(italic Helidon version $(red %s) not found.)", helidonVersion);
    }
}
