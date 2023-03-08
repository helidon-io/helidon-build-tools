/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.common.Strings;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ProjectsTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);

        Path archive = stageDir.resolve("cli-data/2.0.0-RC1/cli-data.zip");
        assertExist(archive);

        try (FileSystem fs = newZipFileSystem(archive)) {
            Path file1 = fs.getPath("/").resolve("versions.json");
            assertExist(file1);

            assertEquals(Files.readString(file1),"{\n" +
                    "    \"versions\": [\n" +
                    "            \"3.0.0-SNAPSHOT\",\n" +
                    "            \"2.5.0\",\n" +
                    "            \"2.4.2\",\n" +
                    "            \"2.4.0\",\n" +
                    "            \"2.0.1\",\n" +
                    "            \"2.0.0\"\n" +
                    "    ],\n" +
                    "    \"latest\": \"3.0.0-SNAPSHOT\"\n" +
                    "}\n");
        }
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);

        Path file1 = stageDir.resolve("versions1.json");
        assertExist(file1);

        assertEquals(Files.readString(file1), "{\n" +
                "    \"versions\": [\n" +
                "            \"3.0.0-SNAPSHOT\",\n" +
                "            \"2.5.0\",\n" +
                "            \"2.4.2\",\n" +
                "            \"2.4.0\",\n" +
                "            \"2.0.1\",\n" +
                "            \"2.0.0\"\n" +
                "    ],\n" +
                "    \"latest\": \"3.0.0-SNAPSHOT\"\n" +
                "}\n");

        Path file2 = stageDir.resolve("versions2.json");
        assertEquals(Files.readString(file2), "{\n" +
                "    \"versions\": [\n" +
                "            \"4.0.0-SNAPSHOT\",\n" +
                "            \"3.0.0\"\n" +
                "    ],\n" +
                "    \"latest\": \"4.0.0-SNAPSHOT\"\n" +
                "}\n");
    }

    private void assertExist(Path path) {
        assertThat(path + " does not exist", Files.exists(path), is(true));
    }

    private void assertEquals(String actual, String expected) {
        assertThat(Strings.normalizeNewLines(actual), is(expected));
    }
}
