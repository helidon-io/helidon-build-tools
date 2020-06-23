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
import io.helidon.build.util.Style;

import static io.helidon.build.util.FileUtils.WORKING_DIR;
import static io.helidon.build.util.MavenVersion.toMavenVersion;

/**
 * Common options.
 */
@CommandFragment
final class CommonOptions {
    private static final String UPDATE_URL = "https://github.com/oracle/helidon-build-tools/blob/master/helidon-cli/CHANGELOG.md";

    private final Path projectDir;
    private final String metadataUrl;
    private final boolean resetCache;
    private final MavenVersion sinceCliVersion;
    private Metadata metadata;

    @Creator
    CommonOptions(@KeyValue(name = "project", description = "The project directory") File projectDir,
                  @KeyValue(name = "url", description = "Metadata base URL",
                          defaultValue = Metadata.DEFAULT_URL, visible = false) String metadataUrl,
                  @Option.Flag(name = "reset", description = "Reset metadata cache", visible = false) boolean resetCache,
                  @KeyValue(name = "since", description = "Check for updates since this version",
                          visible = false) String since) {
        this.projectDir = projectDir != null ? projectDir.toPath().toAbsolutePath() : WORKING_DIR;
        this.metadataUrl = metadataUrl;
        this.resetCache = resetCache || since != null;
        this.sinceCliVersion = toMavenVersion(since == null ? Config.buildVersion() : since);
    }

    CommonOptions(Path projectDir, CommonOptions options) {
        this.projectDir = projectDir;
        this.metadataUrl = options.metadataUrl;
        this.resetCache = false; // Don't do it again
        this.sinceCliVersion = options.sinceCliVersion;
        this.metadata = options.metadata;
    }

    Path project() {
        return projectDir;
    }

    Metadata metadata() {
        if (metadata == null) {
            if (resetCache) {
                try {
                    Log.debug("Clearing metadata cache");
                    Config.userConfig().clearCache();
                } catch (Exception e) {
                    Log.warn("Failed to clear cache: ", e);
                }
            }
            if (!metadataUrl.equals(Metadata.DEFAULT_URL)) {
                Log.debug("Using metadata url %s", metadataUrl);
            }
            metadata = Metadata.newInstance(metadataUrl);
        }
        return metadata;
    }

    void checkForUpdates() {
        try {
            Optional<MavenVersion> update = metadata().checkForCliUpdate(sinceCliVersion, false);
            if (update.isPresent()) {
                MavenVersion newVersion = update.get();
                Map<Object, Object> releaseNotes = releaseNotes(newVersion);
                Log.info();
                if (releaseNotes.isEmpty()) {
                    Log.info("$(bold Version %s of this CLI is now available.)", newVersion);
                } else {
                    Log.info("$(bold Version %s of this CLI is now available:)", newVersion);
                    Log.info();
                    Log.info(releaseNotes, Style.Italic, Style.Plain);
                    Log.info();
                }
                Log.info("Please see $(blue %s) for updates.", UPDATE_URL);
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

    private Map<Object, Object> releaseNotes(MavenVersion latestVersion) {
        try {
            Map<Object, Object> notes = new LinkedHashMap<>();
            metadata().cliReleaseNotesOf(latestVersion, sinceCliVersion).forEach((v, m) -> notes.put("    " + v, m));
            return notes;
        } catch (Plugins.PluginFailed e) {
            Log.debug("accessing release notes for %s failed: %s", latestVersion, e.getMessage());
        } catch (Exception e) {
            Log.debug("accessing release notes for %s failed: %s", latestVersion, e.toString());
        }
        return Collections.emptyMap();
    }
}
