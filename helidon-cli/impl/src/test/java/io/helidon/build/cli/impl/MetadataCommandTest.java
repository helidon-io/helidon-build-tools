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

import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.test.TestFiles;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for command tests that require the {@link Metadata}.
 */
public class MetadataCommandTest extends BaseCommandTest {

    private static MetadataTestServer SERVER;
    private static Metadata METADATA;
    private static UserConfig USER_CONFIG;

    @BeforeAll
    public static void startMetadataAccess() {
        Config.setUserHome(TestFiles.targetDir().resolve("alice"));
        USER_CONFIG = Config.userConfig();
        SERVER = new MetadataTestServer(TestVersion.RC1, false).start();
        METADATA = Metadata.newInstance(SERVER.url());
    }

    public String metadataUrl() {
        return SERVER.url();
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
