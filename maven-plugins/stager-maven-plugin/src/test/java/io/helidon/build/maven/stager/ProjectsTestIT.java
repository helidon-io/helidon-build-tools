/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import io.helidon.build.common.test.utils.ConfigurationParameterSource;

import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static io.helidon.build.common.Strings.normalizeNewLines;
import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static java.nio.file.Files.readString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class ProjectsTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testArchive(String basedir) throws IOException {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());

        Path archive = stageDir.resolve("cli-data/2.0.0-RC1/cli-data.zip");
        assertThat(archive, fileExists());

        try (FileSystem fs = newZipFileSystem(archive)) {
            Path file = fs.getPath("/").resolve("versions.json");
            assertThat(file, fileExists());
            assertThat(normalizeNewLines(readString(file)),
                       is("""
                                  {
                                      "versions": [
                                          "3.0.0-SNAPSHOT",
                                          "2.5.0",
                                          "2.4.2",
                                          "2.4.0",
                                          "2.0.1",
                                          "2.0.0"
                                      ],
                                      "latest": "3.0.0-SNAPSHOT"
                                  }
                                  """));
        }
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testTemplate(String basedir) throws IOException {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());

        Path file1 = stageDir.resolve("versions1.json");
        assertThat(file1, fileExists());
        assertThat(normalizeNewLines(readString(file1)),
                   is("""
                              {
                                  "versions": [
                                      "3.0.0-SNAPSHOT",
                                      "2.5.0",
                                      "2.4.2",
                                      "2.4.0",
                                      "2.0.1",
                                      "2.0.0"
                                  ],
                                  "preview-versions": [
                                      {
                                          "order": 199,
                                          "version": "4.0.0-M1"
                                      },
                                      {
                                          "order": 200,
                                          "version": "4.0.0-ALPHA6"
                                      }
                                  ],
                                  "latest": "3.0.0-SNAPSHOT"
                              }
                              """));

        Path file2 = stageDir.resolve("versions2.json");
        assertThat(file2, fileExists());
        assertThat(normalizeNewLines(readString(file2)),
                   is("""
                              {
                                  "versions": [
                                      "4.0.0-SNAPSHOT",
                                      "3.0.0"
                                  ],
                                  "preview-versions": [
                                  ],
                                  "latest": "4.0.0-SNAPSHOT"
                              }
                              """));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testCopyArtifact(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());
        assertThat(stageDir.resolve("helidon-bare-mp-2.0.0-RC1.jar"), fileExists());
        assertThat(stageDir.resolve("helidon-bare-se-2.0.0-RC1.jar"), fileExists());
        assertThat(stageDir.resolve("archetype-catalog.xml"), fileExists());
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testDownload(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());
        assertThat(stageDir.resolve("cli/2.0.0-RC1/darwin/helidon"), fileExists());
        assertThat(stageDir.resolve("cli/2.0.0-M4/linux/helidon"), fileExists());
        assertThat(stageDir.resolve("cli/2.0.0-M4/darwin/helidon"), fileExists());
        assertThat(stageDir.resolve("cli/2.0.0-RC1/linux/helidon"), fileExists());
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testFile(String basedir) throws IOException {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());

        Path file1 = stageDir.resolve("cli-data/latest");
        assertThat(file1, fileExists());
        assertThat(readString(file1), is("2.0.0-RC1"));

        Path file2 = stageDir.resolve("CNAME");
        assertThat(file2, fileExists());
        assertThat(readString(file2), is("helidon.io"));

        assertThat(stageDir.resolve("docs/v4/index.html"), fileExists());
        assertThat(stageDir.resolve("docs/v4/apidocs/index.html"), fileExists());
        assertThat(stageDir.resolve("docs/v4/images/foo.svg"), fileExists());

        Path file3 = stageDir.resolve("docs/v4/sitemap.txt");
        assertThat(file3, fileExists());
        assertThat(Files.readAllLines(file3), containsInAnyOrder(
                "docs/v4",
                "docs/v4/apidocs"
        ));

        assertThat(stageDir.resolve("docs/v3/index.html"), fileExists());
        assertThat(stageDir.resolve("docs/v3/apidocs/index.html"), fileExists());

        Path file4 = stageDir.resolve("docs/v3/sitemap.txt");
        assertThat(file4, fileExists());
        assertThat(Files.readAllLines(file4), containsInAnyOrder(
                "docs/v3",
                "docs/v3/apidocs"
        ));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testSymlink(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());

        Path file1 = stageDir.resolve("cli/latest");
        assertThat(symlinkTarget(file1), is("2.0.0-RC1"));

        Path file2 = stageDir.resolve("docs/latest");
        assertThat(symlinkTarget(file2), is("1.4.4"));

        Path file3 = stageDir.resolve("docs/v1");
        assertThat(symlinkTarget(file3), is("1.4.4"));

        Path file4 = stageDir.resolve("docs/v2");
        assertThat(symlinkTarget(file4), is("2.0.0-RC1"));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testUnpackArtifact(String basedir) {
        Path stageDir = Path.of(basedir).resolve("target/stage");
        assertThat(stageDir, fileExists());

        Path docsDir = stageDir.resolve("docs");
        assertThat(docsDir, fileExists());
        assertThat(docsDir.resolve("1.4.0"), fileExists());
        assertThat(docsDir.resolve("1.4.1"), fileExists());
        assertThat(docsDir.resolve("1.4.2"), fileExists());
        assertThat(docsDir.resolve("1.4.3"), fileExists());
        assertThat(docsDir.resolve("1.4.4"), fileExists());
        assertThat(docsDir.resolve("2.0.0-RC1"), fileExists());
    }

    private String symlinkTarget(Path file) {
        try {
            assertThat(file + " is not a symbolic link", Files.isSymbolicLink(file), is(true));
            return Files.readSymbolicLink(file).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
