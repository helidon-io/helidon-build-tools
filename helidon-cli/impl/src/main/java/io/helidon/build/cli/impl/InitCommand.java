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
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.HelidonVariant;
import io.helidon.build.util.QuickstartGenerator;

import static io.helidon.build.cli.impl.ConfigFile.DOT_HELIDON;
import static io.helidon.build.cli.impl.ConfigFile.FEATURE_PREFIX;
import static io.helidon.build.cli.impl.ConfigFile.HELIDON_FLAVOR;
import static io.helidon.build.cli.impl.ConfigFile.PROJECT_DIRECTORY;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand implements CommandExecution {

    static final String HELIDON_PROPERTIES = "helidon.properties";
    static final String DEFAULT_VERSION = "1.4.1";
    static final Path CWD = Path.of(".");

    private final CommonOptions commonOptions;
    private final boolean batch;
    private final Flavor flavor;
    private final Build build;
    private final String version;

    /**
     * Helidon flavors.
     */
    enum Flavor {
        MP,
        SE
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
            @KeyValue(name = "version", description = "Helidon version", defaultValue = DEFAULT_VERSION) String version,
            @Flag(name = "batch", description = "Non interactive, user input is passed as system properties") boolean batch) {
        this.commonOptions = commonOptions;
        this.flavor = flavor;
        this.build = build;
        this.batch = batch;
        this.version = version;
    }

    @Override
    public void execute(CommandContext context) {
        // Generate project
        Path dir = null;
        try {
            dir = QuickstartGenerator.generator()
                    .parentDirectory(CWD)
                    .helidonVariant(HelidonVariant.parse(flavor.name()))
                    .helidonVersion(version)
                    .generate();
        } catch (IllegalStateException e) {
            context.logError(e.getMessage());
            System.exit(2);
        }
        Objects.requireNonNull(dir);

        // Create config file that includes feature information
        File sourceFile = new File(Objects.requireNonNull(
                getClass().getClassLoader().getResource(HELIDON_PROPERTIES)).getFile());
        File dotHelidon = Path.of(dir.toString(), DOT_HELIDON).toFile();
        ConfigFile configFile = new ConfigFile(dotHelidon);
        try (FileReader fr = new FileReader(sourceFile)) {
            Properties sourceProps = new Properties();
            sourceProps.load(fr);
            configFile.property(PROJECT_DIRECTORY, ".");
            configFile.property(HELIDON_FLAVOR, flavor.toString());
            sourceProps.forEach((key, value) -> {
                String propName = (String) key;
                if (propName.startsWith(FEATURE_PREFIX)) {      // Applies to both SE or MP
                    configFile.property(propName, (String) value);
                } else if (propName.startsWith(flavor.toString())) {       // Project's variant
                    configFile.property(
                            propName.substring(flavor.toString().length() + 1),
                            (String) value);
                }
            });
            configFile.store();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.logInfo("Switch directory to ./" + dir.getFileName() + " to use CLI");
    }
}
