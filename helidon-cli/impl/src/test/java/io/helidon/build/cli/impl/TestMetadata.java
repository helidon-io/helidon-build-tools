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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.helidon.build.util.FileUtils.assertDir;
import static java.util.Objects.requireNonNull;

/**
 * Test metadata constants.
 */
public class TestMetadata {

    static final URL TEST_CLI_DATA_URL = requireNonNull(MetadataTest.class.getClassLoader().getResource("cli-data"));
    static final Path TEST_CLI_DATA_PATH = assertDir(Path.of(TEST_CLI_DATA_URL.getFile()));
    static final String LATEST_FILE_NAME = "latest";
    static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    static final String LAST_UPDATE = "/" + LAST_UPDATE_FILE_NAME;
    static final String CLI_DATA_FILE_NAME = "cli-data.zip";
    static final String CLI_DATA = "/" + CLI_DATA_FILE_NAME;
    static final String PROPERTIES_FILE_NAME = "metadata.properties";
    static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    static final String HELIDON_BARE_SE = "helidon-bare-se";
    static final String HELIDON_BARE_MP = "helidon-bare-mp";
    static final String RC1 = "2.0.0-RC1";
    static final String RC1_LAST_UPDATE = RC1 + LAST_UPDATE;
    static final String RC1_CLI_DATA_ZIP = RC1 + CLI_DATA;
    static final String RC2 = "2.0.0-RC2";
    static final String RC2_LAST_UPDATE = RC2 + LAST_UPDATE;
    static final String RC2_CLI_DATA_ZIP = RC2 + CLI_DATA;
    static final byte[] RC1_ZIP = readCliDataFile(RC1_CLI_DATA_ZIP);
    static final byte[] RC2_ZIP = readCliDataFile(RC2_CLI_DATA_ZIP);
    static final String NO_ETAG = "<no-etag>";
    static final String INITIAL_ETAG = "<initial>";

    /**
     * Supported Helidon versions.
     */
    enum TestVersion {

        /**
         * 2.0.0-RC1
         */
        RC1(TestMetadata.RC1),

        /**
         * 2.0.0-RC2
         */
        RC2(TestMetadata.RC2);

        private final String version;

        TestVersion(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return version;
        }
    }

    private static byte[] readCliDataFile(String file) {
        try {
            return Files.readAllBytes(TEST_CLI_DATA_PATH.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TestMetadata() {
    }
}
