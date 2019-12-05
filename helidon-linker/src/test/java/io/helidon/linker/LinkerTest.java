/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import io.helidon.linker.util.FileUtils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Unit test for class {@link Linker}.
 */
class LinkerTest {
     
    @Test
    void testQuickstartSeNoCDS() throws Exception {
        Path mainJar = TestFiles.helidonSeJar();
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("se-jri-no-cds"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .cds(false)
                                            .build();
        Path jri = Linker.linker(config).link();

        FileUtils.assertDir(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, false);
        assertScript(jri);
    }

    @Test
    void testQuickstartSeNoCDSStripDebug() throws Exception {
        Path mainJar = TestFiles.helidonSeJar();
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("se-jri-no-cds-or-debug"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .cds(false)
                                            .stripDebug(true)
                                            .build();
        Path jri = Linker.linker(config).link();

        FileUtils.assertDir(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, false);
        assertScript(jri);
        long origSize = FileUtils.sizeOf(mainJar);
        long copySize = FileUtils.sizeOf(jri.resolve("app").resolve(mainJar.getFileName()));
        assertThat(copySize, is(lessThan(origSize)));
    }

    @Test
    void testQuickstartSe() throws Exception {
        Path mainJar = TestFiles.helidonSeJar();
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("se-jri"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .verbose(false)
                                            .cds(true)
                                            .build();
        Path jri = Linker.linker(config).link();

        FileUtils.assertDir(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, true);
        assertScript(jri);
    }

    @Test
    void testQuickstartMp() throws Exception {
        Path mainJar = TestFiles.helidonMpJar();
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("mp-jri"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .cds(true)
                                            .build();
        Path jri = Linker.linker(config).link();

        FileUtils.assertDir(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, true);
        assertScript(jri);
    }

    private static void assertApplication(Path jri, String mainJarName) throws IOException {
        FileUtils.assertDir(jri);
        Path appDir = FileUtils.assertDir(jri.resolve("app"));
        Path mainAppJar = FileUtils.assertFile(appDir.resolve(mainJarName));
        assertReadOnly(mainAppJar);
        Path appLibDir = FileUtils.assertDir(appDir.resolve("libs"));
        for (Path file : FileUtils.listFiles(appLibDir, name -> true)) {
            assertReadOnly(file);
        }
    }

    private static void assertScript(Path jri) throws IOException {
        Path binDir = FileUtils.assertDir(jri.resolve("bin"));
        Path scriptFile = FileUtils.assertFile(binDir.resolve("start"));
        assertExecutable(scriptFile);
    }

    private static void assertCdsArchive(Path jri, boolean archiveExists) {
        Path libDir = FileUtils.assertDir(jri.resolve("lib"));
        Path archiveFile = libDir.resolve("start.jsa");
        assertThat(Files.exists(archiveFile), is(archiveExists));
    }

    private static void assertReadOnly(Path file) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        assertThat(file.toString(), perms, is(Set.of(PosixFilePermission.OWNER_READ,
                                                     PosixFilePermission.OWNER_WRITE,
                                                     PosixFilePermission.GROUP_READ,
                                                     PosixFilePermission.OTHERS_READ)));
    }

    private static void assertExecutable(Path file) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        assertThat(file.toString(), perms, is(Set.of(PosixFilePermission.OWNER_READ,
                                                     PosixFilePermission.OWNER_EXECUTE,
                                                     PosixFilePermission.OWNER_WRITE,
                                                     PosixFilePermission.GROUP_READ,
                                                     PosixFilePermission.GROUP_EXECUTE,
                                                     PosixFilePermission.OTHERS_READ,
                                                     PosixFilePermission.OTHERS_EXECUTE)));
    }
}
