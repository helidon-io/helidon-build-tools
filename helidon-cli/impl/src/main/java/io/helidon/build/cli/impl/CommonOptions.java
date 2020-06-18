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

import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;

/**
 * Common options.
 */
@CommandFragment
final class CommonOptions {

    private static final Path CWD = FileUtils.WORKING_DIR;

    private final Path projectDir;
    private final String metadataUrl;
    private final boolean updateMetadata;
    private Metadata metadata;

    @Creator
    CommonOptions(@KeyValue(name = "project", description = "The project directory") File projectDir,
                  @KeyValue(name = "url", description = "Metadata base URL",
                          defaultValue = Metadata.DEFAULT_BASE_URL, visible = false) String metadataUrl,
                  @Option.Flag(name = "update", description = "Force metadata update", visible = false) boolean updateMetadata) {
        this.projectDir = projectDir != null ? projectDir.toPath().toAbsolutePath() : CWD;
        this.metadataUrl = metadataUrl;
        this.updateMetadata = updateMetadata;
    }

    CommonOptions(Path projectDir, CommonOptions options) {
        this.projectDir = projectDir;
        this.metadataUrl = options.metadataUrl;
        this.updateMetadata = options.updateMetadata;
        this.metadata = options.metadata;
    }

    Path project() {
        return projectDir;
    }

    String metadataUrl() {
        return metadataUrl;
    }

    boolean updateMetadata() {
        return updateMetadata;
    }

    Metadata metadata() {
        if (metadata == null) {
            if (Log.isDebug()) {
                if (updateMetadata) {
                    Log.debug("Forcing metadata update");
                }
                if (!metadataUrl.equals(Metadata.DEFAULT_BASE_URL)) {
                    Log.debug("Using metadata url %s", metadataUrl);
                }
            }
            metadata = Metadata.newInstance(metadataUrl, updateMetadata ? 0 : Metadata.DEFAULT_UPDATE_FREQUENCY);
        }
        return metadata;
    }
}
