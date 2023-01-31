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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static io.helidon.build.common.FileUtils.deleteDirectoryContent;
import static io.helidon.build.common.FileUtils.isPosix;
import static io.helidon.build.common.FileUtils.list;
import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static io.helidon.build.common.FileUtils.unzip;
import static io.helidon.build.common.FileUtils.zip;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileUtilsTest {

    private static Path zipDir;
    private static Path outputDir;
    private static Path zipFile;

    @BeforeAll
    static void setup() throws IOException {
        outputDir = Files.createTempDirectory("output");
        zipFile = createZip();
        zipDir = Files.createTempDirectory("zip");
        Files.createDirectories(zipDir.resolve("dir"));
        Files.createFile(zipDir.resolve("file1"));
        Files.createFile(zipDir.resolve("dir/file2"));
    }

    private static Path createZip() throws IOException {
        Path zip = Files.createTempDirectory("").resolve("archive.zip");
        Path source = Files.createTempFile("file", "");
        try (FileSystem fs = newZipFileSystem(zip)) {
            Path target = fs.getPath("file");
            Files.copy(source, target, REPLACE_EXISTING);
            Files.setPosixFilePermissions(target, Set.of(OWNER_READ, OWNER_EXECUTE));
        }
        return zip;
    }

    @AfterEach
    void clean() throws IOException {
        deleteDirectoryContent(outputDir);
    }

    @Test
    void testZipPermissions() {
        Set<PosixFilePermission> permissions = Set.of(
                GROUP_EXECUTE,
                OWNER_EXECUTE,
                OTHERS_EXECUTE);

        Path zip = zip(outputDir.resolve("archive.zip"), zipDir, path -> {
            try {
                if (Files.isRegularFile(path)) {
                    Files.setPosixFilePermissions(path, permissions);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        readZipFileContent(zip, path -> {
            try {
                Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
                assertThat(permissions, is(posixPermissions));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testZipPermissions2() throws IOException {
        Set<PosixFilePermission> permissions = Set.of(OWNER_READ, OWNER_EXECUTE);
        Files.setPosixFilePermissions(zipDir.resolve("file1"), permissions);
        Files.setPosixFilePermissions(zipDir.resolve("dir/file2"), permissions);

        Path zip = zip(outputDir.resolve("archive.zip"), zipDir);

        readZipFileContent(zip, path -> {
            try {
                Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
                assertThat(permissions, is(posixPermissions));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testUnzipPermissions() throws IOException {
        unzip(zipFile, outputDir);
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(outputDir.resolve("file"));
        assertThat(permissions, is(Set.of(OWNER_READ, OWNER_EXECUTE)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testUnzipPermissions1() {
        unzip(zipFile, outputDir);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testE2ePermissions1() throws IOException {
        Set<PosixFilePermission> permissions = Set.of(OWNER_READ, OWNER_EXECUTE);
        Files.setPosixFilePermissions(zipDir.resolve("file1"), permissions);
        Files.setPosixFilePermissions(zipDir.resolve("dir/file2"), permissions);

        zipAndUnzip();

        list(outputDir).stream()
                .filter(p -> !p.equals(outputDir) && Files.isRegularFile(p))
                .forEach(path -> {
                    try {
                        Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
                        assertThat(permissions, is(posixPermissions));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testE2ePermissions2() throws IOException {
        zipAndUnzip();
    }

    private void readZipFileContent(Path zip, Consumer<Path> consumer) {
        try (FileSystem fs = newZipFileSystem(zip)) {
            boolean posix = isPosix(zip);
            if (!posix) {
                String format = "The given path '%s' does not support POSIX file attribute";
                throw new IllegalStateException(String.format(format, zip));
            }
            Path root = fs.getRootDirectories().iterator().next();
            try (Stream<Path> entries = Files.walk(root)) {
                entries.filter(p -> !p.equals(root) && Files.isRegularFile(p))
                       .forEach(consumer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void zipAndUnzip() throws IOException {
        Path zip = zip(outputDir.resolve("archive.zip"), zipDir);
        unzip(zip, outputDir);
        Files.delete(zip);
    }
}
