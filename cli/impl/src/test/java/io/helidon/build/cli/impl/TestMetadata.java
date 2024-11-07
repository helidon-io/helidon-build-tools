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

import io.helidon.build.common.maven.MavenVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC1;
import static io.helidon.build.cli.impl.TestMetadata.TestVersion.RC2;
import static io.helidon.build.common.FileUtils.pathOf;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static java.util.Objects.requireNonNull;

/**
 * Test metadata constants.
 */
class TestMetadata {

    /**
     * Supported Helidon versions.
     */
    enum TestVersion {

        /**
         * 2.0.0-RC1
         */
        RC1("2.0.0-RC1"),

        /**
         * 2.0.0-RC2
         */
        RC2("2.0.0-RC2"),

        /**
         * 2.0.1
         * Contains ONLY metadata.properties!
         * CLI plugin version: none
         */
        R1("2.0.1"),

        /**
         * 2.0.2
         * Contains ONLY metadata.properties!
         * CLI plugin versions: cli.latest.plugin.version=2.0.3
         */
        R2("2.0.2"),

        /**
         * 2.0.3
         * Contains ONLY metadata.properties!
         * CLI plugin versions: cli.latest.plugin.version=2.2.0, cli.2.1.0.plugin.version=2.0.9, cli.2.0.3.plugin.version=2.0.3
         */
        R3("2.0.3");

        private final String version;

        TestVersion(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return version;
        }
    }

    static final URL TEST_CLI_DATA_URL = requireNonNull(MetadataTest.class.getClassLoader().getResource("cli-data"));
    static final Path TEST_CLI_DATA_PATH = requireDirectory(pathOf(TEST_CLI_DATA_URL));
    static final String VERSIONS_FILE_NAME = "versions.xml";
    static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    static final String CLI_DATA_FILE_NAME = "cli-data.zip";
    static final String PROPERTIES_FILE_NAME = "metadata.properties";
    static final String CATALOG_FILE_NAME = "archetype-catalog.xml";
    static final String HELIDON_BARE_SE = "helidon-bare-se";
    static final String HELIDON_BARE_MP = "helidon-bare-mp";
    static final String VERSION_RC1 = RC1.toString();
    static final String VERSION_RC2 = RC2.toString();
    static final MavenVersion MAVEN_VERSION_RC1 = toMavenVersion(VERSION_RC1);
    static final MavenVersion MAVEN_VERSION_RC2 = toMavenVersion(VERSION_RC2);
    static final Map<TestVersion, byte[]> ZIP_DATA = zipData();
    static final String RC1_ETAG = etag(RC1, ZIP_DATA.get(RC1));
    static final String RC2_ETAG = etag(RC2, ZIP_DATA.get(RC2));

    private static Map<TestVersion, byte[]> zipData() {
        Map<TestVersion, byte[]> result = new HashMap<>();
        for (TestVersion version : TestVersion.values()) {
            result.put(version, readCliDataFile(version + "/" + CLI_DATA_FILE_NAME));
        }
        return result;
    }

    /**
     * Get the content of {@code cli-data.zip} for a given test version.
     *
     * @param version test version
     * @return content of {@code cli-data.zip}
     */
    static byte[] zipData(TestVersion version) {
        return ZIP_DATA.get(version);
    }

    /**
     * Compute the ETAG value for a given test version and {@code cli-data.zip}.
     *
     * @param version test version
     * @param data    content of {@code cli-data.zip}
     * @return ETAG
     */
    static String etag(TestVersion version, byte[] data) {
        return version.toString() + "-" + hash(data);
    }

    /**
     * Compute the MD5 hash for a given byte array.
     *
     * @param data data to compute hash of
     * @return MD5 hash
     */
    static String hash(byte[] data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compute the hexadecimal string for a given byte array.
     *
     * @param data data to represent as hexadecimal
     * @return hexadecimal representation
     */
    static String toHexString(byte[] data) {
        final int length = data.length;
        final StringBuilder builder = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            byte b0 = data[i++];
            byte b1 = i < length ? data[i++] : 0;
            byte b2 = i < length ? data[i++] : 0;
            byte b3 = i < length ? data[i++] : 0;
            int value = ((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
            builder.append(Integer.toHexString(value));
        }
        return builder.toString();
    }

    /**
     * Read the content of the given file under {@link #TEST_CLI_DATA_PATH}.
     *
     * @param file local file path of the file to read
     * @return byte array
     */
    static byte[] readCliDataFile(String file) {
        try {
            return Files.readAllBytes(TEST_CLI_DATA_PATH.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TestMetadata() {
    }
}
