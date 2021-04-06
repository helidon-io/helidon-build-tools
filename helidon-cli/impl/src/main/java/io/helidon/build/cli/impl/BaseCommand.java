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

import java.nio.file.Path;

import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.common.Log;
import io.helidon.build.common.RequirementFailure;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.cli.common.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Class BaseCommand.
 */
public abstract class BaseCommand implements CommandExecution {

    static final String HELIDON_VERSION_PROPERTY = "helidon.version";
    static final String HELIDON_PLUGIN_VERSION_PROPERTY_PREFIX = "-Dversion.plugin.helidon=";
    static final String HELIDON_CLI_PLUGIN_VERSION_PROPERTY_PREFIX = "-Dversion.plugin.helidon-cli=";

    private final CommonOptions commonOptions;
    private final boolean quietCheckForUpdates;
    private ProjectConfig projectConfig;
    private Path projectDir;

    /**
     * Constructor.
     *
     * @param commonOptions The common options.
     * @param quietCheckForUpdates {@code true} if check for updates should be quiet.
     */
    protected BaseCommand(CommonOptions commonOptions, boolean quietCheckForUpdates) {
        this.commonOptions = commonOptions;
        this.quietCheckForUpdates = quietCheckForUpdates;
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        assertPreconditions();
        checkForUpdates();
        invoke(context);
    }

    /**
     * Assert command preconditions.
     */
    protected abstract void assertPreconditions();

    /**
     * Checks for updates.
     */
    protected void checkForUpdates() {
        commonOptions.checkForUpdates(quietCheckForUpdates);
    }

    /**
     * Invoke the command.
     *
     * @param context The command context.
     * @throws Exception If the command fails.
     */
    protected abstract void invoke(CommandContext context) throws Exception;

    /**
     * Returns the project configuration.
     *
     * @return The configuration.
     */
    protected ProjectConfig projectConfig() {
        return projectConfig(commonOptions.project());
    }

    /**
     * Returns the project configuration in the given directory.
     *
     * @param dir The project directory.
     * @return The configuration.
     */
    protected ProjectConfig projectConfig(Path dir) {
        if (projectConfig != null && dir.equals(projectDir)) {
            return projectConfig;
        }
        Path dotHelidon = dir.resolve(DOT_HELIDON);
        projectConfig = new ProjectConfig(dotHelidon);
        projectDir = dir;
        return projectConfig;
    }

    /**
     * Returns the metadata instance.
     *
     * @return The instance.
     */
    protected Metadata metadata() {
        return commonOptions.metadata();
    }

    /**
     * Returns the default Helidon plugin version if not supplied on the command-line.
     *
     * @param commandLinePluginVersion The supplied plugin version.
     * @param useCurrentPluginVersion {@code true} if should use the build version if not supplied.
     * @return The version. May be {@code null}.
     */
    protected static String defaultHelidonPluginVersion(String commandLinePluginVersion, boolean useCurrentPluginVersion) {
        if (commandLinePluginVersion == null && useCurrentPluginVersion) {
            return Config.buildVersion();
        } else {
            return commandLinePluginVersion;
        }
    }

    /**
     * Returns the version for the `helidon-cli-maven-plugin` if not already selected.
     *
     * @param selectedVersion The selected version. May be {@code null}.
     * @return The version. May be {@code null} to .
     */
    protected String cliPluginVersion(String selectedVersion) {
        if (selectedVersion == null) {

            // The plugin version has not already been selected, so we need to do so from metadata
            // based on the Helidon version for the project.

            // First, try to find the Helidon version from the project config (.helidon file). Note
            // that if this file was deleted and no build has occurred, a minimal project config is
            // generated in our preconditions, which will attempt to find and store the Helidon
            // version by reading the pom and checking for a Helidon parent pom. Though a Helidon
            // parent pom will normally be present, it is not required so we may not find it in the
            // config; in that case, fallback to the latest version. If we fail to get that, use our
            // build version.

            Metadata meta = metadata();
            ProjectConfig projectConfig = projectConfig();
            String helidonVersionProperty = projectConfig.property(ProjectConfig.HELIDON_VERSION);
            String buildVersion = Config.buildVersion();
            MavenVersion helidonVersion;
            if (helidonVersionProperty == null) {
                try {
                    helidonVersion = meta.latestVersion();
                    Log.debug("helidon.version missing in %s, using latest: %s", projectConfig.file(), helidonVersion);
                } catch (Exception e) {
                    helidonVersion = toMavenVersion(buildVersion);
                    Log.debug("unable to lookup latest Helidon version, using build version %s: %s",
                              buildVersion, e.getMessage());
                }
            } else {
                helidonVersion = toMavenVersion(helidonVersionProperty);
            }

            // Short circuit if Helidon version is qualified since metadata only exists for releases

            if (helidonVersion.isQualified()) {
                Log.debug("Helidon version %s not a release, using current CLI version %s", helidonVersion, buildVersion);
                return buildVersion;
            }

            // Now lookup and return the CLI plugin version (which will short circuit if Helidon version is
            // prior to the existence of the CLI plugin).

            try {
                Log.debug("using Helidon version %s to find CLI plugin version", helidonVersion);
                return meta.cliPluginVersion(helidonVersion, true).toString();
            } catch (Plugins.PluginFailed e) {
                Log.debug("unable to lookup CLI plugin version for Helidon version %s: %s", helidonVersion, e.getMessage());
            } catch (RequirementFailure e) {
                Log.debug("CLI plugin version not specified for Helidon version %s: %s", helidonVersion);
            } catch (Exception e) {
                Log.debug("unable to lookup CLI plugin version for Helidon version %s: %s", helidonVersion, e.toString());
            }

            // We failed so return null to let the project pom dictate the version (which will fail if not configured)

            return null;
        } else {
            return selectedVersion;
        }
    }
}
