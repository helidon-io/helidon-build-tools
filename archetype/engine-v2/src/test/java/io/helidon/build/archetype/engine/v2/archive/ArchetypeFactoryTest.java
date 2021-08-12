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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class ArchetypeFactoryTest {

    private static final String archetypeDirName = "arch";

    @TempDir
    Path workDir;

    private Path archDir;
    private String zipFileName;
    private Archetype archetype;

    @BeforeEach
    public void setUp() throws IOException {
        Path zipPath = workDir.resolve("test.zip");
        zipFileName = zipPath.toString();
        archDir = workDir.resolve(archetypeDirName);
        archDir.toFile().mkdir();
        zipFolder(archDir);
    }

    @Test
    void testCreateStringArg() throws IOException {
        archetype = ArchetypeFactory.create(zipFileName);
        assertThat(archetype instanceof ZipArchetype, is(true));
        archetype.close();

        archetype = ArchetypeFactory.create(archDir.toString());
        assertThat(archetype instanceof DirectoryArchetype, is(true));
        archetype.close();

        // not a zip file
        Exception e = assertThrows(ArchetypeException.class, () -> {
            File file = archDir.resolve("wrong_file").toFile();
            file.createNewFile();
            ArchetypeFactory.create(file.getPath());
        });
        assertThat(e.getMessage(), containsString("Cannot create new ZipArchetype instance with file"));

        // incorrect path
        e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentPath";
            ArchetypeFactory.create(testValue);
        });
        assertThat(e.getMessage(), containsString("File someNonexistentPath does not exist"));
    }

    @Test
    void testCreateFileArg() throws IOException {
        Archetype archetype = ArchetypeFactory.create(new File(zipFileName));
        assertThat(archetype instanceof ZipArchetype, is(true));
        archetype.close();

        archetype = ArchetypeFactory.create(new File(archDir.toString()));
        assertThat(archetype instanceof DirectoryArchetype, is(true));
        archetype.close();

        // not a zip file
        Exception e = assertThrows(ArchetypeException.class, () -> {
            File file = archDir.resolve("wrong_file").toFile();
            file.createNewFile();
            ArchetypeFactory.create(file);
        });
        assertThat(e.getMessage(), containsString("Cannot create new ZipArchetype instance with file"));

        // nonexistent file
        e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentPath";
            ArchetypeFactory.create(new File(testValue));
        });
        assertThat(e.getMessage(), containsString("File someNonexistentPath does not exist"));
    }

    private void zipFolder(Path archDir) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            zos.putNextEntry(new ZipEntry(archDir.toString()));
            zos.closeEntry();
        }
    }
}