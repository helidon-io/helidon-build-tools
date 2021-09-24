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

package io.helidon.build.archetype.maven.url.mvn;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class HandlerTest {

    private static final String CONTENT = "Archetype content";
    private static final String ARCHIVE_DIRECTORY = "io/helidon/archetypes/helidon-archetype/3.0.0-SNAPSHOT";
    private static final String ZIP_NAME = "helidon-archetype-3.0.0-SNAPSHOT.zip";
    private static final String JAR_NAME = "helidon-archetype-3.0.0-SNAPSHOT.jar";

    @TempDir
    static Path workDir;

    @BeforeAll
    static void bootstrap() throws IOException {
        System.setProperty("io.helidon.archetype.mvn.local.repository", workDir.toString());
        System.setProperty("java.protocol.handler.pkgs", "io.helidon.build.archetype.maven.url");
        generateZipAndJar();
    }

    @Test
    public void testMavenUrlJar() throws IOException {
        InputStream is = new URL("mvn://io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:jar/helidon-archetype.xml").openStream();
        assertThat(is,  is(notNullValue()));
        assertThat(CONTENT, is(new String(is.readNBytes(CONTENT.length()))));
    }

    @Test
    public void testMavenUrlJarInsideDirectory() throws IOException {
        InputStream is = new URL("mvn://io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:jar/archetype/helidon-archetype.xml").openStream();
        assertThat(is,  is(notNullValue()));
        assertThat(CONTENT, is(new String(is.readNBytes(CONTENT.length()))));
    }

    @Test
    public void testMavenUrlZip() throws IOException {
        InputStream is = new URL("mvn://io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:zip/archetype/helidon-archetype.xml").openStream();
        assertThat(is,  is(notNullValue()));
        assertThat(CONTENT, is(new String(is.readNBytes(CONTENT.length()))));
    }

    private static void generateZipAndJar() throws IOException {
        Path archDir = workDir.resolve(ARCHIVE_DIRECTORY);
        archDir.toFile().mkdirs();
        copyDescriptor(archDir);
        zipFolder(archDir);
        JarFolder(archDir);
    }

    private static void JarFolder(Path workDir) throws IOException{
        try (JarOutputStream zos = new JarOutputStream(new FileOutputStream(workDir.resolve(JAR_NAME).toString()))) {
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

    private static void zipFolder(Path workDir) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(workDir.resolve(ZIP_NAME).toString()))) {
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

    private static void copyDescriptor(Path directory) throws IOException {
        String descriptorFileName = "helidon-archetype.xml";
        directory.resolve("archetype").toFile().mkdir();
        Path destination = directory.resolve(descriptorFileName);
        Path secondDest = directory.resolve("archetype").resolve(descriptorFileName);
        try (
                InputStream is = HandlerTest.class.getClassLoader().getResourceAsStream(descriptorFileName);
                InputStream second = HandlerTest.class.getClassLoader().getResourceAsStream(descriptorFileName);
        ) {
            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(second, secondDest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
