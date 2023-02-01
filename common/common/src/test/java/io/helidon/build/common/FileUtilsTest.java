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
package io.helidon.build.common;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static io.helidon.build.common.FileUtils.list;
import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static io.helidon.build.common.FileUtils.unzip;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.Unchecked.unchecked;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link FileUtils}.
 */
public class FileUtilsTest {

    private static Path zipDir;
    private static Path outputDir;
    private static Path zipFile;

    @BeforeAll
    static void setup() throws IOException {
        outputDir = Files.createDirectories(
                TestFiles.targetDir(FileUtilsTest.class).resolve("test-classes/archives"));
        zipFile = createZip();
        zipDir = Files.createTempDirectory("zip");
        Files.createDirectories(zipDir.resolve("dir"));
        Files.createFile(zipDir.resolve("file1"));
        Files.createFile(zipDir.resolve("dir/file2"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testZipPermissions() {
        Set<PosixFilePermission> permissions = Set.of(
                GROUP_EXECUTE,
                OWNER_EXECUTE,
                OTHERS_EXECUTE);

        Path zip = zip(outputDir.resolve("zip-permissions/archive.zip"), zipDir, path -> unchecked(() -> {
            if (Files.isRegularFile(path)) {
                Files.setPosixFilePermissions(path, permissions);
            }
        }));

        readZipFileContent(zip, path -> unchecked(() -> {
            Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
            assertThat(permissions, is(posixPermissions));
        }));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testZipPermissions2() {
        zip(outputDir.resolve("zip-permissions/archive.zip"), zipDir,
                p -> unchecked(() -> Files.setPosixFilePermissions(p, Set.of(OWNER_EXECUTE))));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testZipOriginalPermissions() throws IOException {
        Set<PosixFilePermission> permissions = setPosixPermissions();
        Path zip = zip(outputDir.resolve("original-permissions/archive.zip"), zipDir);

        readZipFileContent(zip, path -> unchecked(() -> {
            Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
            assertThat(permissions, is(posixPermissions));
        }));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testZipOriginalPermissions2() {
        zip(outputDir.resolve("original-permissions2/archive.zip"), zipDir);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testUnzipPermissions() throws IOException {
        Path directory = outputDir.resolve("unzip-permissions");
        unzip(zipFile, directory);
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(directory.resolve("file"));
        assertThat(permissions, is(Set.of(OWNER_READ, OWNER_EXECUTE)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testUnzipPermissions2() {
        Path directory = outputDir.resolve("unzip-permissions2");
        unzip(zipFile, directory);
        File file = directory.resolve("file").toFile();
        assertThat(file.canRead(), is(true));
        assertThat(file.canExecute(), is(true));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testE2ePermissions() throws IOException {
        Path directory = outputDir.resolve("e2e-permissions");
        Set<PosixFilePermission> permissions = setPosixPermissions();
        zipAndUnzip(directory);
        list(directory).stream()
                .filter(p -> !p.equals(outputDir) && Files.isRegularFile(p))
                .forEach(path -> unchecked(() -> {
                    Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
                    assertThat(permissions, is(posixPermissions));
                }));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testE2ePermissions2() {
        Path directory = outputDir.resolve("e2e-permissions2");
        setPermissions();
        zipAndUnzip(directory);
        list(directory).stream()
                .filter(p -> !p.equals(directory) && Files.isRegularFile(p))
                .forEach(path -> {
                    File file = path.toFile();
                    assertThat(file.canRead(), is(true));
                    assertThat(file.canExecute(), is(true));
                });
    }

    private static void readZipFileContent(Path zip, Consumer<Path> consumer) {
        try (FileSystem fs = newZipFileSystem(zip)) {
            Path root = fs.getRootDirectories().iterator().next();
            try (Stream<Path> entries = Files.walk(root)) {
                entries.filter(p -> !p.equals(root) && Files.isRegularFile(p))
                       .forEach(consumer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void zipAndUnzip(Path directory) {
        Path zip = zip(directory.resolve("archive.zip"), zipDir);
        unzip(zip, directory);
    }

    private static Path createZip() throws IOException {
        Path zip = outputDir.resolve("archive.zip");
        Path source = Files.createTempFile("file", "");
        try (FileSystem fs = newZipFileSystem(zip)) {
            Path target = fs.getPath("file");
            Files.copy(source, target, REPLACE_EXISTING);
            if (OSType.currentOS().isPosix()) {
                Files.setPosixFilePermissions(target, Set.of(OWNER_READ, OWNER_EXECUTE));
            }
        }
        return zip;
    }

    private static Set<PosixFilePermission> setPosixPermissions() throws IOException {
        Set<PosixFilePermission> permissions = Set.of(OWNER_READ, OWNER_EXECUTE);
        Files.setPosixFilePermissions(zipDir.resolve("file1"), permissions);
        Files.setPosixFilePermissions(zipDir.resolve("dir/file2"), permissions);
        return permissions;
    }

    private static void setPermissions() {
        File file1 = zipDir.resolve("file1").toFile();
        File file2 = zipDir.resolve("dir/file2").toFile();
        file1.setReadable(true);
        file2.setReadable(true);
        file1.setExecutable(true);
        file2.setExecutable(true);
    }
}
