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

import java.io.IOException;
import java.io.UncheckedIOException;

import io.helidon.build.cli.harness.Config;
import io.helidon.build.cli.harness.UserConfig;
import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.test.TestFiles;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.Proxies;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static io.helidon.build.util.MavenVersion.toMavenVersion;

/**
 * Base class for command tests that require the {@link Metadata}.
 */
public class MetadataCommandTest extends BaseCommandTest {

    private static MetadataTestServer SERVER;
    private static String METADATA_URL;
    private static Metadata METADATA;
    private static UserConfig USER_CONFIG;

    @BeforeAll
    public static void startMetadataAccess() {
        Config.setUserHome(TestFiles.targetDir().resolve("alice"));
        USER_CONFIG = Config.userConfig();
        if (canUseMetadataTestServer()) {
            SERVER = new MetadataTestServer(TestVersion.RC1, false).start();
            METADATA_URL = SERVER.url();
        } else {
            SERVER = null;
            METADATA_URL = Metadata.DEFAULT_URL;
            Proxies.setProxyPropertiesFromEnv();
        }
        METADATA = Metadata.newInstance(METADATA_URL, true);
    }

    private static boolean canUseMetadataTestServer() {
        MavenVersion testServerVersion = toMavenVersion(TestVersion.RC1.toString());
        MavenVersion helidonRelease = toMavenVersion(helidonTestVersion());
        return helidonRelease.isLessThanOrEqualTo(testServerVersion);
    }

    public String metadataUrl() {
        return METADATA_URL;
    }

    public Metadata metadata() {
        return METADATA;
    }

    public UserConfig userConfig() {
        return USER_CONFIG;
    }

    public void clearCache() {
        try {
            USER_CONFIG.clearCache();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    public static void stopMetadataAccess() {
        if (SERVER != null) {
            SERVER.stop();
        }
    }
}
