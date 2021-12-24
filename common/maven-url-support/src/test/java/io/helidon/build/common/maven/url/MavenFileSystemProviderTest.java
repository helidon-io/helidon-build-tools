/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven.url;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static java.nio.file.FileSystems.getFileSystem;
import static java.nio.file.FileSystems.newFileSystem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link MavenFileSystemProvider}.
 */
class MavenFileSystemProviderTest {

    private static final String ARTIFACT_DIR = "com/example/test-artifact/1.2.3";
    private static final String ZIP_ARTIFACT_FILE = "test-artifact-1.2.3.zip";

    private static Path localRepo;

    @BeforeAll
    static void beforeAllTests() throws IOException {
        Path targetDir = targetDir(HandlerTest.class);
        localRepo = unique(targetDir.resolve("handler-ut"), "repo");
        System.setProperty(MavenFileResolver.LOCAL_REPO_PROPERTY, localRepo.toString());
        Path artifact = localRepo.resolve(ARTIFACT_DIR).resolve(ZIP_ARTIFACT_FILE);
        Files.createDirectories(artifact.getParent());
        zip(artifact, targetDir.resolve("test-classes/test-artifact"));
    }

    @Test
    void testRoot() throws IOException {
        URI uri = URI.create("mvn://com.example:test-artifact:1.2.3");
        FileSystem fileSystem = fileSystem(uri);
        assertThat(Files.exists(fileSystem.getPath("file1.xml")), is(true));
    }

    @Test
    void testRoot2() throws IOException {
        URI uri = URI.create("mvn://com.example:test-artifact:1.2.3!/");
        FileSystem fileSystem = fileSystem(uri);
        assertThat(Files.exists(fileSystem.getPath("file1.xml")), is(true));
    }

    @Test
    void testRoot3() throws IOException {
        URI uri = URI.create("mvn://com.example:test-artifact:1.2.3!/non-existent");
        FileSystem fileSystem = fileSystem(uri);
        assertThat(Files.exists(fileSystem.getPath("file1.xml")), is(true));
    }

    private FileSystem fileSystem(URI uri) throws IOException {
        try {
            return newFileSystem(uri, Map.of(), this.getClass().getClassLoader());
        } catch (FileSystemAlreadyExistsException ex) {
            return getFileSystem(uri);
        }
    }
}
