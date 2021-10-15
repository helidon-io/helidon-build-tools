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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipArchetypeTest {

    private static final String archetypeDirName = "arch";
    public static final String DESCRIPTOR_RESOURCE_NAME = "archetype.xml";

    @TempDir
    Path workDir;

    private Path archDir;
    private String zipFileName;
    private FileSystem fileSystem;
    private ZipArchetype archetype;

    @AfterEach
    public void cleanUp() throws IOException {
        fileSystem.close();
        archetype.close();
    }

    @BeforeEach
    public void setUp() throws IOException {
        Path zipPath = workDir.resolve("test.zip");
        zipFileName = zipPath.toString();
        createContentForZip();
        zipFolder(archDir);
        fileSystem = FileSystems.newFileSystem(zipPath, null);
        archetype = new ZipArchetype(new File(zipFileName));
    }

    @Test
    void testGetPath() {
        String expectedPath = "dir0" + fileSystem.getSeparator() + "file0";

        //relative path
        Path path = archetype.getPath(expectedPath);
        assertThat(path.toString(), is(expectedPath));

        //absolute path
        expectedPath = fileSystem.getSeparator() + expectedPath;
        path = archetype.getPath(expectedPath);
        assertThat(path.toString(), is(expectedPath));

        //incorrect path
        Exception e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentFile";
            archetype.getPath(testValue);
        });
        assertThat(e.getMessage(), containsString("File someNonexistentFile does not exist"));
    }

    @Test
    void testGetInputStream() throws IOException {
        String expectedPath = "dir0" + fileSystem.getSeparator() + "file0";

        //relative path
        InputStream inputStream = archetype.getInputStream(expectedPath);
        assertThat(inputStream, notNullValue());
        inputStream.close();

        //absolute path
        expectedPath = fileSystem.getSeparator() + expectedPath;
        inputStream = archetype.getInputStream(expectedPath);
        assertThat(inputStream, notNullValue());
        inputStream.close();

        //incorrect path
        Exception e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentFile";
            archetype.getPath(testValue);
        });
        assertThat(e.getMessage(), containsString("File someNonexistentFile does not exist"));
    }

    @Test
    void testGetDescriptor() {
        String expectedPath = "schema" + fileSystem.getSeparator() + "archetype.xml";

        //relative path
        ArchetypeDescriptor descriptor = archetype.getDescriptor(expectedPath);
        assertThat(descriptor, notNullValue());

        //absolute path
        expectedPath = fileSystem.getSeparator() + expectedPath;
        descriptor = archetype.getDescriptor(expectedPath);
        assertThat(descriptor, notNullValue());

        //incorrect path
        Exception e = assertThrows(ArchetypeException.class, () -> {
            String testValue = "someNonexistentFile";
            archetype.getDescriptor(testValue);
        });
        assertThat(e.getMessage(), containsString("File someNonexistentFile does not exist"));
    }

    @Test
    void testGetPaths() {
        List<String> paths = archetype.getPaths();

        assertThat(paths.size(), is(101));
    }

    private void createContentForZip() throws IOException {
        archDir = workDir.resolve(archetypeDirName);
        archDir.toFile().mkdir();
        for (int dir = 0; dir < 10; dir++) {
            File directory = archDir.resolve("dir" + dir).toFile();
            directory.mkdir();
            for (int file = 0; file < 10; file++) {
                directory.toPath().resolve("file" + file).toFile().createNewFile();
            }
        }
        copyDescriptor();
    }

    private void zipFolder(Path workDir) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            Files.walkFileTree(workDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attributes
                ) {
                    try (FileInputStream fis = new FileInputStream(file.toFile())) {
                        Path targetFile = workDir.relativize(file);
                        zos.putNextEntry(new ZipEntry(targetFile.toString().replace(File.separator.charAt(0), '/')));
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void copyDescriptor() throws IOException {
        String descriptorFileName = "schema" + workDir.getFileSystem().getSeparator() + "archetype.xml";
        archDir.resolve("schema").toFile().mkdir();
        Path destination = archDir.resolve(descriptorFileName);

        try (InputStream is = ZipArchetypeTest.class.getClassLoader()
                .getResourceAsStream("schema" + workDir.getFileSystem().getSeparator() + DESCRIPTOR_RESOURCE_NAME)) {
            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}