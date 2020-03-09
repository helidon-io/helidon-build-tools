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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandContext.ExitStatus;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValues;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

/**
 * The {@code features} command.
 */
@Command(name = "features", description = "List or add features to the project")
public final class FeaturesCommand extends BaseCommand implements CommandExecution {

    private final CommonOptions commonOptions;
    private final Collection<String> add;
    private final boolean list;
    private final boolean all;

    @Creator
    FeaturesCommand(
            CommonOptions commonOptions,
            @KeyValues(name = "add", description = "Add features to the project") Collection<String> add,
            @Flag(name = "list", description = "List the features used in the project") boolean list,
            @Flag(name = "all", description = "List all available features") boolean all) {

        this.commonOptions = commonOptions;
        this.add = add;
        this.list = list;
        this.all = all;
    }

    @Override
    public void execute(CommandContext context) {
        if (!add.isEmpty()) {
            if (list || all) {
                exitAction(context);
            }
            addFeatures(context);
        } else if (list ^ all) {
            if (list) {
                listProjectFeatures(context);
            } else if (all) {
                listAllFeatures(context);
            }
        } else {
            exitAction(context);
        }
    }

    private void listAllFeatures(CommandContext context) {
        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());
        projectConfig.listFeatures().forEach(context::logInfo);
    }

    private void listProjectFeatures(CommandContext context) {
        context.exitAction(ExitStatus.WARNING, "Option --list not implemented");
    }

    private void addFeatures(CommandContext context) {
        Path projectDir = commonOptions.project().toPath();
        File pomFile = projectDir.resolve("pom.xml").toFile();
        Model model = readPomModel(pomFile);
        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());

        // Update pom model adding dependencies for each feature
        add.forEach(featureName -> {
            // Get list of deps for this feature -- must be non-empty
            List<ProjectDependency> featureDeps = projectConfig.featureDeps(featureName);
            if (featureDeps.isEmpty()) {
                context.exitAction(ExitStatus.FAILURE, "Feature '" + featureName + "' does not exist");
            }

            List<Dependency> existingDeps = model.getDependencies();
            featureDeps.forEach(fd -> {
                // Check if dependency already there
                Optional<Dependency> found = existingDeps.stream().filter(d ->
                        d.getGroupId().equals(fd.groupId())
                                && d.getArtifactId().equals(fd.artifactId())
                                && Objects.equals(d.getVersion(), fd.version())).findFirst();

                // Now add dependency if necessary
                if (found.isEmpty()) {
                    Dependency newDep = new Dependency();
                    newDep.setGroupId(fd.groupId());
                    newDep.setArtifactId(fd.artifactId());
                    newDep.setVersion(fd.version());
                    model.addDependency(newDep);
                    context.logInfo("Adding '" + fd + "' to the project's pom");
                } else {
                    context.logInfo("Dependency '" + fd + "' already in project's pom");
                }
            });
        });

        // Write model back to pom file
        writePomModel(pomFile, model);
    }

    private void exitAction(CommandContext context) {
        context.exitAction(ExitStatus.FAILURE, "Invalid command options, use --help");
    }
}
