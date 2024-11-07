/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Base class for command tests that require the {@link Metadata}.
 */
class MetadataAccessTestBase extends CommandTestBase {

    private static final boolean DEBUG_PLUGIN = false;

    private static MetadataTestServer SERVER;
    private static String METADATA_URL;
    private static Metadata METADATA;
    private static UserConfig USER_CONFIG;

    /**
     * Start the metadata server.
     */
    @BeforeAll
    static void startMetadataAccess() {
        Path userHome = unique(TestFiles.targetDir(MetadataAccessTestBase.class), "alice");
        Config.setUserHome(userHome);
        USER_CONFIG = UserConfig.create(userHome);
        Config.setUserConfig(USER_CONFIG);
        Plugins.reset(false);
        if (canUseMetadataTestServer()) {
            SERVER = new MetadataTestServer(TestVersion.RC1, false).start();
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

    private static boolean canUseMetadataTestServer() {
        MavenVersion testServerVersion = toMavenVersion(TestVersion.RC1.toString());
        MavenVersion helidonRelease = toMavenVersion(Config.buildVersion());
        return helidonRelease.isLessThanOrEqualTo(testServerVersion);
    }

    /**
     * Stop the metadata server.
     */
    @AfterAll
    static void stopMetadataAccess() {
        if (SERVER != null) {
            SERVER.stop();
        }
    }

    /**
     * Get the metadata URL.
     * @return metadata URL, never {@code null}
     */
    String metadataUrl() {
        return METADATA_URL;
    }

    /**
     * Get the metadata.
     * @return metadata, never {@code null}
     */
    Metadata metadata() {
        return METADATA;
    }

    /**
     * Get the user config.
     * @return config, never {@code null}
     */
    UserConfig userConfig() {
        return USER_CONFIG;
    }
}
