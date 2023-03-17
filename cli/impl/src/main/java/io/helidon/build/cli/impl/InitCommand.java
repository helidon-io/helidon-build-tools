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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.helidon.build.archetype.engine.v1.Prompter;
import io.helidon.build.archetype.engine.v2.VersionLoader;
import io.helidon.build.archetype.engine.v2.ast.Version;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.impl.InitOptions.BuildSystem;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;
import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.common.Requirements.failed;
import static io.helidon.build.common.Requirements.require;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBrightCyan;
import static io.helidon.build.common.ansi.AnsiTextStyles.ItalicRed;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand extends BaseCommand {
    private static final String HELIDON_RELEASES_URL = "https://github.com/oracle/helidon/releases";
    private static final String VERSION_LOOKUP_FAILED = "$(italic,red Helidon version lookup failed.)";
    private static final String VERSION_NOT_FOUND_MESSAGE = "$(italic Helidon version $(red %s) not found.)";
    private static final String AVAILABLE_VERSIONS_MESSAGE = "Please see $(blue %s) for available versions.";
    private static final String NOT_FOUND_STATUS_MESSAGE = "connection failed with 404";

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

        // Make sure we have a valid Helidon version
        String helidonVersion = initOptions.helidonVersion();
        if (helidonVersion == null) {
            if (context.properties().containsKey(HELIDON_VERSION_PROPERTY)) {
                helidonVersion = context.properties().getProperty(HELIDON_VERSION_PROPERTY);
                assertSupportedVersion(helidonVersion);
            } else if (batch) {
                helidonVersion = defaultHelidonVersion();
                Log.info("Using Helidon version " + helidonVersion);
            } else {
                String defaultHelidonVersion = defaultHelidonVersion();
                FileSystem latestArchetype = latestArchetype();
                List<String> versions = VersionLoader.load(latestArchetype)
                                                     .stream().map(Version::id).collect(Collectors.toList());
                if (versions.size() > 0) {
                    int defaultOption = IntStream.range(0, versions.size())
                                                 .filter(i->versions.get(i).equals(defaultHelidonVersion))
                                                 .findFirst()
                                                 .orElseGet(()->{
                                                     versions.add(defaultHelidonVersion);
                                                     return versions.size() - 1;
                                                 });
                    helidonVersion =  versions.get(prompt("Helidon version", versions, defaultOption));
                } else {
                    helidonVersion = prompt("Helidon version", defaultHelidonVersion, this::isSupportedVersion);
                }
            }
            initOptions.helidonVersion(helidonVersion);
        } else {
            assertSupportedVersion(helidonVersion);
        }

        ArchetypeInvoker archetypeInvoker = ArchetypeInvoker
                .builder()
                .metadata(metadata)
                .initOptions(initOptions)
                .userConfig(config)
                .onResolved(Log::info)
                .initProperties(context.properties())
                .projectDir(this::initProjectDir)
                .build();

        Path projectDir = archetypeInvoker.invoke();

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

    private FileSystem latestArchetype() throws Exception {
        Path latestArchetype = metadata().archetypeV2Of(metadata().latestVersion().toString());
        return FileSystems.newFileSystem(latestArchetype, this.getClass().getClassLoader());
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

    private String defaultHelidonVersion() {
        // Check the system property first, primarily to support tests
        String version = System.getProperty(HELIDON_VERSION_PROPERTY);
        if (version == null) {
            try {
                version = metadata.latestVersion().toString();
                Log.debug("Latest Helidon version found: %s", version);
            } catch (Plugins.PluginFailedUnchecked e) {
                versionLookupFailed(null);
            } catch (Exception e) {
                versionLookupFailed(e.getMessage());
            }
        }
        return version;
    }

    private boolean isSupportedVersion(String helidonVersion) {
        MavenVersion version = MavenVersion.toMavenVersion(helidonVersion);
        return isSupportedVersion(version, false);
    }

    private boolean isSupportedVersion(MavenVersion version, boolean notFoundWillFail) {
        try {
            metadata.assertVersionIsAvailable(version);
            return true;
        } catch (IllegalArgumentException | Metadata.UpdateFailed | Plugins.PluginFailedUnchecked e) {
            String message = e.getMessage();
            boolean messageLogged = LogLevel.isDebug() && e instanceof Plugins.PluginFailedUnchecked;
            if (!message.contains(NOT_FOUND_STATUS_MESSAGE)) {
                versionLookupFailed(messageLogged ? null : message);
            }
            if (!messageLogged) {
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
            Log.info(ItalicRed.apply(e.getMessage()));
            failed(VERSION_LOOKUP_FAILED);
        }

        return false;
    }

    private void assertSupportedVersion(String helidonVersion) {
        MavenVersion version = MavenVersion.toMavenVersion(helidonVersion);
        require(isSupportedVersion(version, true), AVAILABLE_VERSIONS_MESSAGE, HELIDON_RELEASES_URL);
    }

    private void versionLookupFailed(String errorMessage) {
        if (errorMessage != null) {
            Log.info(ItalicRed.apply(errorMessage));
        }
        failed(VERSION_LOOKUP_FAILED);
    }
}
