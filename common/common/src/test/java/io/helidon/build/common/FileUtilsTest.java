/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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

import static io.helidon.build.common.FileUtils.containsLine;
import static io.helidon.build.common.FileUtils.ensureFile;
import static io.helidon.build.common.FileUtils.list;
import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static io.helidon.build.common.FileUtils.unique;
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
class FileUtilsTest {

    private static Path outputDir;

    @BeforeAll
    static void setup() throws IOException {
        outputDir = Files.createDirectories(
                TestFiles.targetDir(FileUtilsTest.class).resolve("test-classes/file-utils"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testZipPermissions() throws IOException {
        Path wd = unique(outputDir, "zip-permissions");
        Path zipDir = createZipDirectory(wd);
        Set<PosixFilePermission> permissions = Set.of(
                GROUP_EXECUTE,
                OWNER_EXECUTE,
                OTHERS_EXECUTE);

        Path zip = zip(wd.resolve("archive.zip"), zipDir, path -> unchecked(() -> {
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testZipPermissions2() throws IOException {
        Path wd = unique(outputDir, "zip-permissions");
        Path zipDir = createZipDirectory(wd);
        Path zip = wd.resolve("archive.zip");
        zip(zip, zipDir,
                p -> unchecked(() -> Files.setPosixFilePermissions(p, Set.of(OWNER_EXECUTE))));
        assertThat(Files.exists(zip), is(true));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testZipOriginalPermissions() throws IOException {
        Path wd = unique(outputDir, "original-permissions");
        Path zipDir = createZipDirectory(wd);
        Set<PosixFilePermission> permissions = setPosixPermissions(zipDir);

        Path zip = zip(wd.resolve("archive.zip"), zipDir);

        readZipFileContent(zip, path -> unchecked(() -> {
            Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
            assertThat(permissions, is(posixPermissions));
        }));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testUnzipPermissions() throws IOException {
        Path wd = unique(outputDir, "unzip-permissions");
        Path zipFile = createZipFile(wd);
        unzip(zipFile, wd);
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(wd.resolve("file"));
        assertThat(permissions, is(Set.of(OWNER_READ, OWNER_EXECUTE)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testUnzipPermissions2() throws IOException {
        Path wd = unique(outputDir, "unzip-permissions");
        Path zipFile = createZipFile(wd);
        unzip(zipFile, wd);
        File file = wd.resolve("file").toFile();
        assertThat(file.canRead(), is(true));
        assertThat(file.canExecute(), is(true));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testE2ePermissions() throws IOException {
        Path wd = unique(outputDir, "e2e-permissions");
        Path zipDir = createZipDirectory(wd);
        Set<PosixFilePermission> permissions = setPosixPermissions(zipDir);
        zipAndUnzip(zipDir);
        list(wd).stream()
                .filter(Files::isRegularFile)
                .forEach(path -> unchecked(() -> {
                    Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(path);
                    assertThat(permissions, is(posixPermissions));
                }));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testE2ePermissions2() throws IOException {
        Path wd = unique(outputDir, "e2e-permissions");
        Path zipDir = createZipDirectory(wd);
        zipAndUnzip(zipDir);
    }

    @Test
    void testContainsLine() throws IOException {
        Path blue = TestFiles.targetDir(FileUtilsTest.class).resolve("test-classes/vfs/blue");
        Path submodule = ensureFile(outputDir.resolve("submodule"));
        Files.write(submodule, "[submodule ...".getBytes());

        assertThat(containsLine(submodule, line -> line.startsWith("[submodule")), is(true));
        assertThat(containsLine(blue, line -> false), is(false));
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
        Path zip = zip(directory.resolve("archive.zip"), directory);
        unzip(zip, directory.getParent());
    }

    private static Path createZipFile(Path directory) throws IOException {
        Path zip = directory.resolve("archive.zip");
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        Path source = Files.createTempFile("file", "");
        try (FileSystem fs = newZipFileSystem(zip)) {
            Path target = fs.getPath("file");
            Files.copy(source, target, REPLACE_EXISTING);
            Files.setPosixFilePermissions(target, Set.of(OWNER_READ, OWNER_EXECUTE));
        }
        return zip;
    }

    private static Set<PosixFilePermission> setPosixPermissions(Path directory) throws IOException {
        Set<PosixFilePermission> permissions = Set.of(OWNER_READ, OWNER_EXECUTE);
        Files.setPosixFilePermissions(directory.resolve("file1"), permissions);
        Files.setPosixFilePermissions(directory.resolve("dir/file2"), permissions);
        return permissions;
    }

    private static Path createZipDirectory(Path wd) throws IOException {
        Path zipDir = Files.createDirectories(wd.resolve("zip-data"));
        Files.createDirectories(zipDir.resolve("dir"));
        Files.createFile(zipDir.resolve("file1"));
        Files.createFile(zipDir.resolve("dir/file2"));
        return zipDir;
    }
}
