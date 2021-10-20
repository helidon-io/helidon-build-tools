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

package io.helidon.build.url.mvn;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.Matchers.is;

public class HandlerTest {

    private static final String ARCHIVE_DIRECTORY = "io/helidon/handler/maven-url-handler/3.0.0";
    private static final String ZIP_NAME = "maven-url-handler-3.0.0.zip";
    private static final String JAR_NAME = "maven-url-handler-3.0.0.jar";

    static Path workDir;

    @BeforeAll
    static void bootstrap() throws IOException {
        workDir = Files.createTempDirectory("archive");
        System.setProperty("io.helidon.mvn.local.repository", workDir.toString());
        System.setProperty("java.protocol.handler.pkgs", "io.helidon.build.url");
        generateZipAndJar();
    }

    @Test
    public void testMavenUrlJar() throws IOException {
        URLConnection urlConnection = URI.create("mvn://io.helidon.handler:maven-url-handler:3.0.0/test-file.xml")
                .toURL()
                .openConnection();
        if (urlConnection instanceof MavenURLConnection) {
            MavenURLConnection mvnCon = (MavenURLConnection) urlConnection;
            assertThat(mvnCon.groupId(), is("io.helidon.handler"));
            assertThat(mvnCon.artifactId(), is("maven-url-handler"));
            assertThat(mvnCon.version(), is("3.0.0"));
            assertThat(mvnCon.type(), is("jar"));
            assertThat(mvnCon.pathFromArchive(), is("test-file.xml"));
            assertThat(mvnCon.getInputStream(), is(notNullValue()));
            assertThat(mvnCon.artifactFile(), is(workDir.toAbsolutePath()
                    .resolve(ARCHIVE_DIRECTORY).resolve(JAR_NAME)));
        } else {
            fail();
        }
    }

    @Test
    public void testMavenUrlJarInsideDirectory() throws IOException {
        URLConnection urlConnection = new URL("mvn://io.helidon.handler:maven-url-handler:3.0.0:jar/xml/test-file.xml")
                .openConnection();
        if (urlConnection instanceof MavenURLConnection) {
            MavenURLConnection mvnCon = (MavenURLConnection) urlConnection;
            assertThat(mvnCon.groupId(), is("io.helidon.handler"));
            assertThat(mvnCon.artifactId(), is("maven-url-handler"));
            assertThat(mvnCon.version(), is("3.0.0"));
            assertThat(mvnCon.type(), is("jar"));
            assertThat(mvnCon.getInputStream(), is(notNullValue()));
            assertThat(mvnCon.artifactFile(), is(workDir.toAbsolutePath()
                    .resolve(ARCHIVE_DIRECTORY).resolve(JAR_NAME)));
        } else {
            fail();
        }
    }

    @Test
    public void testMavenUrlZip() throws IOException {
        URLConnection urlConnection = new URL("mvn://io.helidon.handler:maven-url-handler:3.0.0:zip/xml/test-file.xml")
                .openConnection();
        if (urlConnection instanceof MavenURLConnection) {
            MavenURLConnection mvnCon = (MavenURLConnection) urlConnection;
            assertThat(mvnCon.groupId(), is("io.helidon.handler"));
            assertThat(mvnCon.artifactId(), is("maven-url-handler"));
            assertThat(mvnCon.version(), is("3.0.0"));
            assertThat(mvnCon.type(), is("zip"));
            assertThat(mvnCon.getInputStream(), is(notNullValue()));
            assertThat(mvnCon.artifactFile(), is(workDir.toAbsolutePath()
                    .resolve(ARCHIVE_DIRECTORY).resolve(ZIP_NAME)));
        } else {
            fail();
        }
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
        String descriptorFileName = "test-file.xml";
        directory.resolve("xml").toFile().mkdir();
        Path destination = directory.resolve(descriptorFileName);
        Path secondDest = directory.resolve("xml").resolve(descriptorFileName);
        try (
                InputStream first = HandlerTest.class.getClassLoader().getResourceAsStream(descriptorFileName);
                InputStream second = HandlerTest.class.getClassLoader().getResourceAsStream(descriptorFileName);
        ) {
            Files.copy(first, destination, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(second, secondDest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
