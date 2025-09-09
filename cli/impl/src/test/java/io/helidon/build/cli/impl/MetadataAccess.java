/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.common.Proxies;
import io.helidon.build.common.maven.MavenVersion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static io.helidon.build.cli.common.CliProperties.HELIDON_PLUGIN_VERSION_PROPERTY;
import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.cli.impl.TestUtils.helidonTestVersion;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;

/**
 * Base class for tests that require the {@link Metadata}.
 */
class MetadataAccess {

    private static final boolean DEBUG_PLUGIN = false;

    private static MetadataServer SERVER;
    private static String METADATA_URL;
    private static Metadata METADATA;

    /**
     * Start the metadata server.
     */
    @BeforeAll
    static void startMetadataAccess() {
        System.setProperty(HELIDON_VERSION_PROPERTY, helidonTestVersion());
        System.setProperty(HELIDON_PLUGIN_VERSION_PROPERTY, helidonTestVersion());

        Path cwd = targetDir(MetadataAccess.class).resolve("metadata-access");
        Path userHome = unique(cwd, "server");
        Config.setUserHome(userHome);
        Config.setUserConfig(UserConfig.create(userHome));
        Plugins.reset(false);

        MavenVersion testServerVersion = toMavenVersion(TestVersion.RC1.toString());
        MavenVersion helidonRelease = toMavenVersion(Config.buildVersion());
        if (helidonRelease.isLessThanOrEqualTo(testServerVersion)) {
            SERVER = new MetadataServer(TestVersion.RC1, false).start();
            METADATA_URL = SERVER.url();
        } else {
            SERVER = null;
            METADATA_URL = Metadata.DEFAULT_URL;
            Proxies.setProxyPropertiesFromEnv();
        }
        METADATA = Metadata.builder()
                .url(METADATA_URL)
                .debugPlugin(DEBUG_PLUGIN)
                .build();
    }

    @AfterAll
    static void stopMetadataAccess() {
        if (SERVER != null) {
            SERVER.stop();
        }
    }

    String metadataUrl() {
        return METADATA_URL;
    }

    Metadata metadata() {
        return METADATA;
    }
}
