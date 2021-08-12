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

package io.helidon.build.archetype.engine.v2.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirectoryArchetypeTest {

    private static final String archetypeDirName = "arch";
    private static final String wrongDirName = "wrong";
    public static final String DESCRIPTOR_RESOURCE_NAME = "archetype.xml";

    @TempDir
    Path workDir;

    private Path archDir;
    private Path wrongDir;

    @BeforeEach
    public void setUp() throws IOException {
        archDir = workDir.resolve(archetypeDirName);
        archDir.toFile().mkdir();
        wrongDir = workDir.resolve(wrongDirName);
        wrongDir.toFile().mkdir();
        for (int dir = 0; dir < 10; dir++) {
            File directory = archDir.resolve("dir" + dir).toFile();
            directory.mkdir();
            for (int file = 0; file < 10; file++) {
                directory.toPath().resolve("file" + file).toFile().createNewFile();
            }
        }
    }

    @Test
    public void testGetPaths() {
        Archetype archetype = new DirectoryArchetype(new File(archDir.toString()));

        List<String> paths = archetype.getPaths();

        assertThat(paths.size(), is(100));
        assertThat(paths.stream().allMatch(path -> path.contains("file")), is(true));
    }

    @Test
    public void testGetDescriptor() throws IOException {
        String fileName = "schema" + workDir.getFileSystem().getSeparator() + "archetype.xml";
        archDir.resolve("schema").toFile().mkdir();
        Path path = archDir.resolve(fileName);
        copyDescriptor(path);
        Archetype archetype = new DirectoryArchetype(new File(archDir.toString()));

        //absolute path
        ArchetypeDescriptor descriptor = archetype.getDescriptor(path.toString());
        assertThat(descriptor, notNullValue());

        //relative path
        descriptor = archetype.getDescriptor(fileName);
        assertThat(descriptor, notNullValue());

        //incorrect path
        Exception e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentFile";
            archetype.getDescriptor(testValue);
        });
        assertThat(e.getMessage(), containsString("File someNonexistentFile does not exist"));
    }

    @Test
    public void testGetFile() {
        Archetype archetype = new DirectoryArchetype(new File(archDir.toString()));
        String testPathString;
        Path expectedPath;
        Path resultPath;
        FileSystem workFileSystem = workDir.getFileSystem();

        //relative path
        testPathString = "dir0" + workFileSystem.getSeparator() + "file5";
        expectedPath = archDir.resolve(testPathString);
        resultPath = archetype.getFile(testPathString);
        assertThat(resultPath, is(expectedPath));

        //absolute path
        resultPath = archetype.getFile(expectedPath.toString());
        assertThat(resultPath, is(expectedPath));

        //Nonexistent file
        Exception e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentFile";
            archetype.getFile(testValue);
        });
        assertThat(e.getMessage(), containsString("File someNonexistentFile does not exist"));

        //existing file but in other directory
        e = assertThrows(ArchetypeException.class, () -> {
            File testFile = wrongDir.resolve("wrongFile").toFile();
            testFile.createNewFile();
            archetype.getFile(testFile.getPath());
        });
        assertThat(e.getMessage(), containsString(
                String.format("Requested file %s is not inside %s",
                        wrongDir + workFileSystem.getSeparator() + "wrongFile",
                        archDir.toString()
                )));
    }

    private void copyDescriptor(Path destination) throws IOException {
        InputStream is = DirectoryArchetypeTest.class.getClassLoader()
                .getResourceAsStream("schema" + workDir.getFileSystem().getSeparator() + DESCRIPTOR_RESOURCE_NAME);
        Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}