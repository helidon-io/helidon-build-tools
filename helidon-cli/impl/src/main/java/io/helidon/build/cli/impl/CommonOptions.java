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

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.Strings;
import io.helidon.build.util.StyleFunction;

import static io.helidon.build.cli.harness.GlobalOptions.DEBUG_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.DEBUG_FLAG_NAME;
import static io.helidon.build.cli.harness.GlobalOptions.PLAIN_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.PLAIN_FLAG_NAME;
import static io.helidon.build.cli.harness.GlobalOptions.VERBOSE_FLAG_DESCRIPTION;
import static io.helidon.build.cli.harness.GlobalOptions.VERBOSE_FLAG_NAME;
import static io.helidon.build.util.FileUtils.WORKING_DIR;
import static io.helidon.build.util.MavenVersion.toMavenVersion;

/**
 * Common options.
 */
@CommandFragment
final class CommonOptions {
    private static final String UPDATE_URL = "https://github.com/oracle/helidon-build-tools/blob/master/helidon-cli/CHANGELOG.md";

    private final boolean verbose;
    private final boolean debug;
    private final boolean plain;
    private final boolean projectDirSpecified;
    private final Path projectDir;
    private final String metadataUrl;
    private final boolean resetCache;
    private final MavenVersion sinceCliVersion;
    private Metadata metadata;

    @Creator
    CommonOptions(@Option.Flag(name = VERBOSE_FLAG_NAME, description = VERBOSE_FLAG_DESCRIPTION, visible = false) boolean verbose,
                  @Option.Flag(name = DEBUG_FLAG_NAME, description = DEBUG_FLAG_DESCRIPTION, visible = false) boolean debug,
                  @Option.Flag(name = PLAIN_FLAG_NAME, description = PLAIN_FLAG_DESCRIPTION, visible = false) boolean plain,
                  @KeyValue(name = "project", description = "The project directory") File projectDir,
                  @KeyValue(name = "url", description = "Metadata base URL", visible = false) String metadataUrl,
                  @Option.Flag(name = "reset", description = "Reset metadata cache", visible = false) boolean resetCache,
                  @KeyValue(name = "since", description = "Check for updates since this version",
                          visible = false) String since) {
        this.verbose = verbose || debug;
        this.debug = debug;
        this.plain = plain || Config.userConfig().richTextDisabled();
        this.projectDirSpecified = projectDir != null;
        this.projectDir = projectDirSpecified ? projectDir.toPath().toAbsolutePath() : WORKING_DIR;
        this.metadataUrl = Strings.isValid(metadataUrl) ? metadataUrl : Config.userConfig().updateUrl();
        this.resetCache = resetCache || since != null;
        this.sinceCliVersion = toMavenVersion(since == null ? Config.buildVersion() : since);
    }

    CommonOptions(Path projectDir, CommonOptions options) {
        this.verbose = options.verbose;
        this.debug = options.debug;
        this.plain = options.plain;
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
                MavenVersion latestHelidonVersion = metadata().latestVersion();
                Map<Object, Object> releaseNotes = releaseNotes(latestHelidonVersion);
                Log.info();
                if (releaseNotes.isEmpty()) {
                    Log.info("$(bold Version %s of this CLI is now available.)", newCliVersion);
                } else {
                    Log.info("$(bold Version %s of this CLI is now available:)", newCliVersion);
                    Log.info();
                    Log.info(releaseNotes, StyleFunction.Italic, StyleFunction.Plain);
                    Log.info();
                }
                Log.info("Please see $(blue %s) to update.", UPDATE_URL);
                Log.info();
            } else {
                Log.debug("no update available");
            }
        } catch (Plugins.PluginFailed ignore) {
            // message has already been logged
        } catch (Exception e) {
            Log.debug("check for updates failed: %s", e.toString());
        }
    }

    private Map<Object, Object> releaseNotes(MavenVersion latestHelidonVersion) {
        try {
            Map<Object, Object> notes = new LinkedHashMap<>();
            metadata().cliReleaseNotesOf(latestHelidonVersion, sinceCliVersion).forEach((v, m) -> notes.put("    " + v, m));
            return notes;
        } catch (Plugins.PluginFailed e) {
            Log.debug("accessing release notes for %s failed: %s", latestHelidonVersion, e.getMessage());
        } catch (Exception e) {
            Log.debug("accessing release notes for %s failed: %s", latestHelidonVersion, e.toString());
        }
        return Collections.emptyMap();
    }
}
