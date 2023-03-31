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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Argument;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.Strings;
import io.helidon.build.common.ansi.AnsiTextStyles;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.cli.harness.GlobalOptions.DEBUG_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.DEBUG_FLAG_NAME;
import static io.helidon.build.cli.harness.GlobalOptions.ERROR_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.ERROR_FLAG_NAME;
import static io.helidon.build.cli.harness.GlobalOptions.PLAIN_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.PLAIN_FLAG_NAME;
import static io.helidon.build.cli.harness.GlobalOptions.VERBOSE_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.VERBOSE_FLAG_NAME;
import static io.helidon.build.common.FileUtils.WORKING_DIR;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Common options.
 */
@CommandFragment
final class CommonOptions {

    /**
     * Where to look for CLI update information.
     */
    private static final String UPDATE_URL = "https://github.com/oracle/helidon-build-tools/blob/master/cli/CHANGELOG.md";

    private static final String MISMATCHED_PROJECT_DIRS = "Different project directories provided: '--project %s' and '%s'";

    private final boolean verbose;
    private final boolean debug;
    private final boolean plain;
    private final boolean error;
    private final boolean projectDirSpecified;
    private final Path projectDir;
    private final String metadataUrl;
    private final boolean resetCache;
    private final MavenVersion sinceCliVersion;
    private Metadata metadata;

    @Creator
    CommonOptions(@Flag(name = VERBOSE_FLAG_NAME, description = VERBOSE_FLAG_DESCRIPTION, visible = false) boolean verbose,
                  @Flag(name = DEBUG_FLAG_NAME, description = DEBUG_FLAG_DESCRIPTION, visible = false) boolean debug,
                  @Flag(name = PLAIN_FLAG_NAME, description = PLAIN_FLAG_DESCRIPTION, visible = false) boolean plain,
                  @Flag(name = ERROR_FLAG_NAME, description = ERROR_FLAG_DESCRIPTION, visible = false) boolean error,
                  @Argument(description = "project_dir") File projectDirArgument,
                  @KeyValue(name = "project", description = "Project directory") File projectDirOption,
                  @KeyValue(name = "url", description = "Metadata base URL", visible = false) String metadataUrl,
                  @Flag(name = "reset", description = "Reset metadata cache", visible = false) boolean resetCache,
                  @KeyValue(name = "since", description = "Check for updates since this version",
                          visible = false) String since) {
        this.verbose = verbose || debug;
        this.debug = debug;
        this.error = error;
        this.plain = plain || Config.userConfig().richTextDisabled();
        this.projectDirSpecified = projectDirOption != null || projectDirArgument != null;
        this.projectDir = projectDir(projectDirOption, projectDirArgument);
        this.metadataUrl = Strings.isValid(metadataUrl) ? metadataUrl : Config.userConfig().updateUrl();
        this.resetCache = resetCache || since != null;
        this.sinceCliVersion = toMavenVersion(since == null ? Config.buildVersion() : since);
    }

    CommonOptions(Path projectDir, CommonOptions options) {
        this.verbose = options.verbose;
        this.debug = options.debug;
        this.plain = options.plain;
        this.error = options.error;
        this.projectDirSpecified = options.projectDirSpecified;
        this.projectDir = projectDir;
        this.metadataUrl = options.metadataUrl;
        this.resetCache = false; // Don't do it again
        this.sinceCliVersion = options.sinceCliVersion;
        this.metadata = options.metadata;
    }

    boolean verbose() {
        return verbose;
    }

    boolean debug() {
        return debug;
    }

    boolean plain() {
        return plain;
    }

    boolean error() {
        return error;
    }

    boolean projectSpecified() {
        return projectDirSpecified;
    }

    Path project() {
        return projectDir;
    }

    Metadata metadata() {
        if (metadata == null) {
            UserConfig config = Config.userConfig();
            if (resetCache) {
                try {
                    Log.debug("clearing plugins and metadata cache");
                    config.clearCache();
                    config.clearPlugins();
                } catch (Exception e) {
                    Log.warn("reset failed: ", e);
                }
            }
            if (!metadataUrl.equals(Metadata.DEFAULT_URL)) {
                Log.debug("using metadata url %s", metadataUrl);
            }
            metadata = Metadata.builder()
                               .url(metadataUrl)
                               .debugPlugin(debug)
                               .updateFrequency(config.checkForUpdatesIntervalHours())
                               .build();
        }
        return metadata;
    }

    void checkForUpdates(boolean quiet) {
        try {
            Optional<MavenVersion> cliUpdate = metadata().checkForCliUpdate(sinceCliVersion, quiet);
            if (cliUpdate.isPresent()) {
                MavenVersion newCliVersion = cliUpdate.get();
                MavenVersion latestHelidonVersion = metadata().archetypesData().latestVersion();//latestVersion();
                Map<Object, Object> releaseNotes = releaseNotes(latestHelidonVersion);
                Log.info();
                if (releaseNotes.isEmpty()) {
                    Log.info("$(bold Version %s of this CLI is now available.)", newCliVersion);
                } else {
                    Log.info("$(bold Version %s of this CLI is now available:)", newCliVersion);
                    Log.info();
                    Log.log(LogLevel.INFO, releaseNotes, AnsiTextStyles.Italic, AnsiTextStyles.Plain);
                    Log.info();
                }
                Log.info("Please see $(blue %s) to update.", UPDATE_URL);
                Log.info();
            } else {
                Log.debug("no update available");
            }
        } catch (Plugins.PluginFailedUnchecked ignore) {
            // debug message has already been logged
        } catch (Exception e) {
            Log.debug("check for updates failed: %s", e.toString());
        }
    }

    private Map<Object, Object> releaseNotes(MavenVersion latestHelidonVersion) {
        try {
            Map<Object, Object> notes = new LinkedHashMap<>();
            metadata().cliReleaseNotesOf(latestHelidonVersion, sinceCliVersion).forEach((v, m) -> notes.put("    " + v, m));
            return notes;
        } catch (Plugins.PluginFailedUnchecked e) {
            Log.debug("accessing release notes for %s failed: %s", latestHelidonVersion, e.getMessage());
        } catch (Exception e) {
            Log.debug("accessing release notes for %s failed: %s", latestHelidonVersion, e.toString());
        }
        return Collections.emptyMap();
    }

    private static Path projectDir(File projectDirOption, File projectDirArgument) {
        if (projectDirOption != null) {
            if (projectDirArgument != null) {
                // Both were passed, make sure they are equal
                try {
                    if (!projectDirOption.getCanonicalPath().equals(projectDirArgument.getCanonicalPath())) {
                        Requirements.failed(MISMATCHED_PROJECT_DIRS, projectDirOption, projectDirArgument);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return projectDirOption.toPath().toAbsolutePath();
        } else if (projectDirArgument != null) {
            return projectDirArgument.toPath().toAbsolutePath();
        } else {
            return WORKING_DIR;
        }
    }
}
