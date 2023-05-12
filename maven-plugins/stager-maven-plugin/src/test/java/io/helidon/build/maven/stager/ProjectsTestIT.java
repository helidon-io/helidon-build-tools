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
import java.io.UncheckedIOException;
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

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test3(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);
        assertExist(stageDir.resolve("helidon-bare-mp-2.0.0-RC1.jar"));
        assertExist(stageDir.resolve("helidon-bare-se-2.0.0-RC1.jar"));
        assertExist(stageDir.resolve("archetype-catalog.xml"));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test4(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);
        assertExist(stageDir.resolve("cli/2.0.0-RC1/darwin/helidon"));
        assertExist(stageDir.resolve("cli/2.0.0-M4/linux/helidon"));
        assertExist(stageDir.resolve("cli/2.0.0-M4/darwin/helidon"));
        assertExist(stageDir.resolve("cli/2.0.0-RC1/linux/helidon"));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test5(String basedir) throws IOException {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);

        Path file1 = stageDir.resolve("cli-data/latest");
        assertExist(file1);
        assertEquals("2.0.0-RC1", Files.readString(file1));

        Path file2 = stageDir.resolve("CNAME");
        assertExist(file2);
        assertEquals("helidon.io", Files.readString(file2));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test6(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);

        Path file1 = stageDir.resolve("cli/latest");
        assertEquals(symlinkTarget(file1), "2.0.0-RC1");

        Path file2 = stageDir.resolve("docs/latest");
        assertEquals(symlinkTarget(file2), "1.4.4");

        Path file3 = stageDir.resolve("docs/v1");
        assertEquals(symlinkTarget(file3), "1.4.4");

        Path file4 = stageDir.resolve("docs/v2");
        assertEquals(symlinkTarget(file4), "2.0.0-RC1");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test7(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertExist(stageDir);

        Path docsDir = stageDir.resolve("docs");
        assertExist(docsDir);
        assertExist(docsDir.resolve("1.4.0"));
        assertExist(docsDir.resolve("1.4.1"));
        assertExist(docsDir.resolve("1.4.2"));
        assertExist(docsDir.resolve("1.4.3"));
        assertExist(docsDir.resolve("1.4.4"));
        assertExist(docsDir.resolve("2.0.0-RC1"));
    }

    private String symlinkTarget(Path file) {
        try {
            assertThat(file + " is not a symbolic link", Files.isSymbolicLink(file), is(true));
            return Files.readSymbolicLink(file).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void assertExist(Path path) {
        assertThat(path + " does not exist", Files.exists(path), is(true));
    }

    private void assertEquals(String actual, String expected) {
        assertThat(Strings.normalizeNewLines(actual), is(expected));
    }
}
