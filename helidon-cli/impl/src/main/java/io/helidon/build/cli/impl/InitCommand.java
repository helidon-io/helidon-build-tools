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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.Constants;
import io.helidon.build.util.HelidonVariant;
import io.helidon.build.util.HelidonVersions;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.SimpleQuickstartGenerator;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

import static io.helidon.build.cli.harness.CommandContext.ExitStatus;
import static io.helidon.build.util.MavenVersion.unqualifiedMinimum;
import static io.helidon.build.util.ProjectConfig.FEATURE_PREFIX;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.Style.Cyan;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand extends BaseCommand implements CommandExecution {

    /*
    build-tools.groupId=io.helidon.build-tools
    build-tools.plugin.artifactId=helidon-maven-plugin

         */
    private static final String MINIMUM_HELIDON_VERSION = "2.0.0";
    private static final int LATEST_HELIDON_VERSION_LOOKUP_RETRIES = 5;
    private static final long HELIDON_VERSION_LOOKUP_INITIAL_RETRY_DELAY = 500;
    private static final long HELIDON_VERSION_LOOKUP_RETRY_DELAY_INCREMENT = 500;
    private static final String BUILD_TOOLS_GROUP_ID = "io.helidon.build-tools";
    private static final String BUILD_TOOLS_PLUGIN_ARTIFACT_ID = "helidon-maven-plugin";
    private static final String HELIDON_PLUGIN_VERSION_PROPERTY = "version.helidon.plugin";
    private static final String POM = "pom.xml";

    private final CommonOptions commonOptions;
    private final Flavor flavor;
    private final Build build;
    private String version;
    private final String groupId;
    private final String artifactId;
    private final String packageName;

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
        @KeyValue(name = "version", description = "Helidon version") String version,
        @KeyValue(name = "groupid", description = "Project's group ID") String groupId,
        @KeyValue(name = "artifactid", description = "Project's artifact ID") String artifactId,
        @KeyValue(name = "package", description = "Project's package name") String packageName) {
        this.commonOptions = commonOptions;
        this.flavor = flavor;
        this.build = build;
        this.version = version;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.packageName = packageName;
    }

    @Override
    public void execute(CommandContext context) {
        // Check build type
        if (build == Build.GRADLE) {
            context.exitAction(ExitStatus.FAILURE, "Gradle support is not implemented");
            return;
        }

        Properties cliConfig = cliConfig();

        // Ensure version
        if (version == null || version.isEmpty()) {
            try {
                version = defaultHelidonVersion();
            } catch (Exception e) {
                context.exitAction(ExitStatus.FAILURE, e.getMessage());
                return;
            }
        }

        // Generate project using Maven archetype
        Path projectDir;
        Path parentDirectory = commonOptions.project().toPath();
        try {
            projectDir = SimpleQuickstartGenerator.generator()
                                                  .parentDirectory(parentDirectory)
                                                  .helidonVariant(HelidonVariant.parse(flavor.name()))
                                                  .helidonVersion(version)
                                                  .groupId(groupId)
                                                  .artifactId(artifactId)
                                                  .packageName(packageName)
                                                  .quiet(true)
                                                  .generate();
        } catch (IllegalStateException e) {
            context.exitAction(ExitStatus.FAILURE, e.getMessage());
            return;
        }
        Objects.requireNonNull(projectDir);

        // Pom needs correct plugin version, with extensions enabled for devloop
        ensurePomContent(projectDir);

        // Create config file that includes feature information
        ProjectConfig configFile = projectConfig(projectDir);
        configFile.property(PROJECT_DIRECTORY, projectDir.toString());
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


        String dir = Cyan.apply(parentDirectory + Constants.DIR_SEP + projectDir.getFileName());
        context.logInfo("Switch directory to " + dir + " to use CLI");
    }

    private void ensurePomContent(Path projectDir) {
        // Support a system property override of the version here for testing
        String helidonVersion = System.getProperty(HELIDON_VERSION, version);
        File pomFile = projectDir.resolve(POM).toFile();
        Model model = readPomModel(pomFile);
        boolean propertyAdded = ensurePluginVersion(model, helidonVersion);
        boolean extensionAdded = ensurePlugin(model);
        if (extensionAdded || propertyAdded) {
            writePomModel(pomFile, model);
        }
    }

    private boolean ensurePluginVersion(Model model, String helidonVersion) {
        Properties properties = model.getProperties();
        String existing = properties.getProperty(HELIDON_PLUGIN_VERSION_PROPERTY);
        if (existing == null || !existing.equals(helidonVersion)) {
            model.addProperty(HELIDON_PLUGIN_VERSION_PROPERTY, helidonVersion);
            return true;
        } else {
            return false;
        }
    }

    private boolean ensurePlugin(Model model) {
        org.apache.maven.model.Build build = model.getBuild();
        boolean isPresent = build.getPlugins()
                                 .stream()
                                 .anyMatch(p -> p.getGroupId().equals(BUILD_TOOLS_GROUP_ID)
                                                && p.getArtifactId().equals(BUILD_TOOLS_PLUGIN_ARTIFACT_ID));
        if (isPresent) {
            // Assume it is what we want rather than updating if not equal, since
            // that could undo future archetype changes.
            return false;
        } else {
            Plugin helidonPlugin = new Plugin();
            helidonPlugin.setGroupId(BUILD_TOOLS_GROUP_ID);
            helidonPlugin.setArtifactId(BUILD_TOOLS_PLUGIN_ARTIFACT_ID);
            helidonPlugin.setVersion("${" + HELIDON_PLUGIN_VERSION_PROPERTY + "}");
            helidonPlugin.setExtensions(true);
            build.addPlugin(helidonPlugin);
            return true;
        }
    }

    private static String defaultHelidonVersion() throws InterruptedException {
        // Check the system property first, primarily to support tests
        String version = System.getProperty(HELIDON_VERSION);
        if (version == null) {
            version = lookupLatestHelidonVersion(LATEST_HELIDON_VERSION_LOOKUP_RETRIES,
                                                 HELIDON_VERSION_LOOKUP_INITIAL_RETRY_DELAY,
                                                 HELIDON_VERSION_LOOKUP_RETRY_DELAY_INCREMENT);
        }
        return version;
    }

    private static String lookupLatestHelidonVersion(int retries,
                                                     long retryDelay,
                                                     long retryDelayIncrement) throws InterruptedException {
        Log.info("Looking up latest Helidon version");
        Predicate<MavenVersion> filter = unqualifiedMinimum(MINIMUM_HELIDON_VERSION);
        int remainingRetries = retries;
        while (remainingRetries > 0) {
            try {
                String version = HelidonVersions.releases(filter).latest().toString();
                Log.debug("Latest Helidon version found: %s", version);
                return version;
            } catch (UnknownHostException | SocketException | SocketTimeoutException e) {
                if (--remainingRetries > 0) {
                    Log.info("  retry %d of %d", retries - remainingRetries + 1, retries);
                    Thread.sleep(retryDelay);
                    retryDelay += retryDelayIncrement;
                }
            } catch (IllegalStateException e) {
                throw new IllegalStateException("No versions >= "
                                                + MINIMUM_HELIDON_VERSION
                                                + " found, please specify with --version option.");
            } catch (Exception e) {
                Log.debug("Lookup failed: %s", e.toString());
                break;
            }
        }
        throw new IllegalStateException("Version lookup failed, please specify with --version option.");
    }
}
