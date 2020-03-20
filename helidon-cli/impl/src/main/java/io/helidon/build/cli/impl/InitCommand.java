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
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.Constants;
import io.helidon.build.util.HelidonVariant;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.ProjectDependency;
import io.helidon.build.util.SimpleQuickstartGenerator;

import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;

import static io.helidon.build.cli.harness.CommandContext.ExitStatus;
import static io.helidon.build.util.ProjectConfig.FEATURE_PREFIX;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand extends BaseCommand implements CommandExecution {

    static final String DEVLOOP_EXTENSION = "devloop.extension";

    private final CommonOptions commonOptions;
    private final Flavor flavor;
    private final Build build;
    private String version;

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

    @Creator
    InitCommand(
            CommonOptions commonOptions,
            @KeyValue(name = "flavor", description = "Helidon flavor", defaultValue = "SE") Flavor flavor,
            @KeyValue(name = "build", description = "Build type", defaultValue = "MAVEN") Build build,
            @KeyValue(name = "version", description = "Helidon version") String version) {
        this.commonOptions = commonOptions;
        this.flavor = flavor;
        this.build = build;
        this.version = version;
    }

    @Override
    public void execute(CommandContext context) {
        // Check build type
        if (build == Build.GRADLE) {
            context.exitAction(ExitStatus.FAILURE, "Gradle support is not implemented");
            return;
        }

        Properties cliConfig = cliConfig();

        // Ensure archetype version
        if (version == null || version.isEmpty()) {
            version = helidonVersion();
            Objects.requireNonNull(version);
            context.logInfo("Using Helidon version " + version);
        }

        // Generate project using Maven archetype
        Path dir;
        Path parentDirectory = commonOptions.project().toPath();
        try {
            dir = SimpleQuickstartGenerator.generator()
                    .parentDirectory(parentDirectory)
                    .helidonVariant(HelidonVariant.parse(flavor.name()))
                    .helidonVersion(version)
                    .generate();
        } catch (IllegalStateException e) {
            context.exitAction(ExitStatus.FAILURE, e.getMessage());
            return;
        }
        Objects.requireNonNull(dir);

        // Archetype pom needs an extension for devloop
        File pomFile = dir.resolve("pom.xml").toFile();
        ensurePomExtension(pomFile, cliConfig);

        // Create config file that includes feature information
        ProjectConfig configFile = projectConfig(dir);
        configFile.property(PROJECT_DIRECTORY, dir.toString());
        configFile.property(PROJECT_FLAVOR, flavor.toString());
        configFile.property(HELIDON_VERSION, version);
        cliConfig.forEach((key, value) -> {
            String propName = (String) key;
            if (propName.startsWith(FEATURE_PREFIX)) {      // Applies to both SE or MP
                configFile.property(propName, (String) value);
            } else if (propName.startsWith(flavor.toString())) {       // Project's flavor
                configFile.property(
                        propName.substring(flavor.toString().length() + 1),
                        (String) value);
            }
        });
        configFile.store();

        context.logInfo("Switch directory to " + parentDirectory + Constants.DIR_SEP
                + dir.getFileName() + " to use CLI");
    }

    private void ensurePomExtension(File pomFile, Properties properties) {
        Model model = readPomModel(pomFile);
        ProjectDependency ext = ProjectDependency.fromString(properties.getProperty(DEVLOOP_EXTENSION));
        ext.version(helidonVersion());
        Objects.requireNonNull(ext);
        List<Extension> extensions = model.getBuild().getExtensions();
        Optional<Extension> found = extensions.stream().filter(
                e -> e.getGroupId().equals(ext.groupId())
                        && e.getArtifactId().equals(ext.artifactId())
                        && Objects.equals(e.getVersion(), ext.version())).findFirst();
        if (found.isPresent()) {
            return;
        }
        Extension newExt = new Extension();
        newExt.setGroupId(ext.groupId());
        newExt.setArtifactId(ext.artifactId());
        newExt.setVersion(ext.version());
        extensions.add(newExt);
        writePomModel(pomFile, model);
    }
}
