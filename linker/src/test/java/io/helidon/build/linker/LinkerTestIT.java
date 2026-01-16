/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.linker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;
import io.helidon.build.linker.util.Constants;
import io.helidon.build.linker.util.JavaRuntime;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.listFiles;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.FileUtils.sizeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test for class {@link Linker}.
 */
@Order(4)
@TestMethodOrder(OrderAnnotation.class)
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
class LinkerTestIT {

    @Tag("se")
    @Order(1)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testQuickstartSeNoCDS(String basedir) throws Exception {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-se.jar");
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("se-jri-no-cds"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .cds(false)
                                            .build();
        Path jri = Linker.linker(config).link();

        requireDirectory(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, false);
        assertScript(jri);
    }

    @Tag("se")
    @Order(2)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testQuickstartSeNoCDSStripDebug(String basedir) throws Exception {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-se.jar");
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("se-jri-no-cds-or-debug"))
                                            .mainJar(mainJar)
                                            .additionalModules("jdk.crypto.ec")
                                            .replace(true)
                                            .cds(false)
                                            .stripDebug(true)
                                            .build();
        Path jri = Linker.linker(config).link();

        requireDirectory(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, false);
        assertScript(jri);
        long origSize = sizeOf(mainJar);
        long copySize = sizeOf(jri.resolve("app").resolve(mainJar.getFileName()));
        assertThat(copySize, is(lessThan(origSize)));
    }

    @Tag("se")
    @Order(3)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testQuickstartSe(String basedir) throws Exception {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-se.jar");
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("se-jri"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .verbose(false)
                                            .cds(true)
                                            .build();
        Path jri = Linker.linker(config).link();

        requireDirectory(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, true);
        assertScript(jri);
    }

    @Tag("mp")
    @Order(4)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testQuickstartMp(String basedir) throws Exception {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-mp.jar");
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("mp-jri"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .cds(true)
                                            .build();
        Path jri = Linker.linker(config).link();

        requireDirectory(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertCdsArchive(jri, true);
        assertScript(jri);
    }

    @Tag("mp")
    @Order(5)
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testQuickstartMpNoCds(String basedir) throws Exception {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-mp.jar");
        Path targetDir = mainJar.getParent();
        Configuration config = Configuration.builder()
                                            .jriDirectory(targetDir.resolve("mp-jri"))
                                            .mainJar(mainJar)
                                            .replace(true)
                                            .cds(false)
                                            .build();
        Path jri = Linker.linker(config).link();

        requireDirectory(jri);
        assertApplication(jri, mainJar.getFileName().toString());
        assertScript(jri);
    }

    private static void assertApplication(Path jri, String mainJarName) throws IOException {
        requireDirectory(jri);
        Path appDir = requireDirectory(jri.resolve("app"));
        Path mainAppJar = requireFile(appDir.resolve(mainJarName));
        assertReadOnly(mainAppJar);
        Path appLibDir = requireDirectory(appDir.resolve("libs"));
        for (Path file : listFiles(appLibDir, (path, attrs) -> attrs.isRegularFile())) {
            assertReadOnly(file);
        }
    }

    private static void assertScript(Path jri) throws IOException {
        Path binDir = requireDirectory(jri.resolve("bin"));
        Path scriptFile = requireFile(binDir.resolve(Constants.OS.withScriptExtension("start")));
        assertExecutable(scriptFile);
    }

    private static void assertCdsArchive(Path jri, boolean archiveExists) {
        Path libDir = requireDirectory(jri.resolve("lib"));
        Path archiveFile = libDir.resolve("start.jsa");
        assertThat(Files.exists(archiveFile), is(archiveExists));
    }

    private static void assertReadOnly(Path file) throws IOException {
        if (Constants.OS.isPosix()) {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
            assertThat(file.toString(), perms, is(Set.of(PosixFilePermission.OWNER_READ,
                                                         PosixFilePermission.OWNER_WRITE,
                                                         PosixFilePermission.GROUP_READ,
                                                         PosixFilePermission.OTHERS_READ)));
        }
    }

    private static void assertExecutable(Path file) throws IOException {
        if (Constants.OS.isPosix()) {
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
}
