/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.test.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import io.helidon.linker.Application;
import io.helidon.linker.util.Constants;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;

import org.eclipse.aether.version.Version;

import static io.helidon.linker.util.Constants.DIR_SEP;
import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;

/**
 * Test file utilities.
 */
public class TestFiles {
    private static final Path OUR_TARGET_DIR = ourTargetDir();
    private static final String MAVEN_EXEC = Constants.OS_TYPE.mavenExec();
    private static final String HELIDON_GROUP_ID = "io.helidon";
    private static final String HELIDON_PROJECT_ID = "helidon-project";
    private static final String ARCHETYPES_GROUP_ID = "io.helidon.archetypes";
    private static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";
    private static final String QUICKSTART_PACKAGE_PREFIX = "io.helidon.examples.quickstart.";
    private static final String SIGNED_JAR_COORDINATES = "org.bouncycastle:bcpkix-jdk15on:1.60";
    private static final Instance<Maven> MAVEN = new Instance<>(TestFiles::createMaven);
    private static final Instance<Version> LATEST_HELIDON_VERSION = new Instance<>(TestFiles::lookupLatestHelidonVersion);
    private static final Instance<Path> SE_JAR = new Instance<>(TestFiles::getOrCreateQuickstartSeJar);
    private static final Instance<Path> MP_JAR = new Instance<>(TestFiles::getOrCreateQuickstartMpJar);
    private static final Instance<Path> SIGNED_JAR = new Instance<>(TestFiles::fetchSignedJar);

    /**
     * Returns the target directory.
     *
     * @return The directory.
     */
    public static Path targetDir() {
        return OUR_TARGET_DIR;
    }

    /**
     * Returns the latest Helidon version.
     *
     * @return The version.
     */
    public static Version latestHelidonVersion() {
        return LATEST_HELIDON_VERSION.instance();
    }

    /**
     * Returns the value required for the {@code -Dexit.on.started} property to trigger on
     * the latest Helidon version.
     *
     * @return The value.
     */
    public static String exitOnStartedValue() {
        final Runtime.Version version = Runtime.Version.parse(latestHelidonVersion().toString());
        return Application.exitOnStartedValue(version);
    }

    /**
     * Returns the quickstart SE main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    public static Path helidonSeJar() {
        return SE_JAR.instance();
    }

    /**
     * Returns the quickstart MP main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    public static Path helidonMpJar() {
        return MP_JAR.instance();
    }

    /**
     * Returns a signed jar.
     *
     * @return The jar.
     */
    public static Path signedJar() {
        return SIGNED_JAR.instance();
    }

    /**
     * Creates the given file (with no content) if it does not already exist.
     *
     * @param file The file.
     * @return The file.
     */
    public static Path ensureMockFile(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    private static Maven maven() {
        return MAVEN.instance();
    }

    private static Maven createMaven() {
        if (System.getProperty("dump.env") != null) {
            Log.info("\n--- Environment ---- \n");
            System.getenv().forEach((key, value) -> Log.info("    %s = %s", key, value));
            Log.info("\n--- System Properties ---- \n");
            System.getProperties().forEach((key, value) -> Log.info("    %s = %s", key, value));
        }
        return Maven.builder().build();
    }

    private static Version lookupLatestHelidonVersion() {
        Log.info("Looking up latest Helidon release version");
        final Version version = maven().latestVersion(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, false);
        Log.info("Latest Helidon release version is %s", version);
        return version;
    }

    private static Path fetchSignedJar() {
        Log.info("Fetching signed jar %s", SIGNED_JAR_COORDINATES);
        return maven().artifact(SIGNED_JAR_COORDINATES);
    }

    private static Path ourTargetDir() {
        try {
            final Path ourCodeSource = Paths.get(TestFiles.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return ourCodeSource.getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path getOrCreateQuickstartSeJar() {
        return getOrCreateQuickstartJar("se");
    }

    private static Path getOrCreateQuickstartMpJar() {
        return getOrCreateQuickstartJar("mp");
    }

    private static Path getOrCreateQuickstartJar(String helidonVariant) {
        final String id = quickstartId(helidonVariant);
        final Path sourceDir = ourTargetDir().resolve(id);
        if (Files.exists(sourceDir)) {
            return quickstartJar(sourceDir, id);
        } else {
            return createQuickstartJar(helidonVariant);
        }
    }

    private static Path createQuickstartJar(String helidonVariant) {
        createQuickstartProject(helidonVariant);
        return buildQuickstartProject(helidonVariant);
    }

    private static Path buildQuickstartProject(String helidonVariant) {
        final String id = quickstartId(helidonVariant);
        final Path sourceDir = assertDir(ourTargetDir().resolve(id));
        Log.info("Building %s", id);
        execute(new ProcessBuilder().directory(sourceDir.toFile())
                                    .command(List.of(MAVEN_EXEC, "clean", "package", "-DskipTests")));
        return quickstartJar(sourceDir, id);
    }

    private static Path quickstartJar(Path sourceDir, String id) {
        return assertFile(sourceDir.resolve("target" + DIR_SEP + id + ".jar"));
    }

    private static String quickstartId(String helidonVariant) {
        return HELIDON_QUICKSTART_PREFIX + helidonVariant;
    }

    private static Path createQuickstartProject(String helidonVariant) {
        final Path targetDir = ourTargetDir();
        final String id = quickstartId(helidonVariant);
        final String pkg = QUICKSTART_PACKAGE_PREFIX + helidonVariant;
        final Version archetypeVersion = latestHelidonVersion();
        Log.info("Creating %s from archetype %s", id, archetypeVersion);
        execute(new ProcessBuilder().directory(targetDir.toFile())
                                    .command(List.of(MAVEN_EXEC,
                                                     "archetype:generate",
                                                     "-DinteractiveMode=false",
                                                     "-DarchetypeGroupId=" + ARCHETYPES_GROUP_ID,
                                                     "-DarchetypeArtifactId=" + id,
                                                     "-DarchetypeVersion=" + archetypeVersion,
                                                     "-DgroupId=test",
                                                     "-DartifactId=" + id,
                                                     "-Dpackage=" + pkg
                                    )));
        return assertDir(targetDir.resolve(id));
    }

    private static void execute(ProcessBuilder builder) {
        try {
            ProcessMonitor.builder()
                          .processBuilder(builder)
                          .capture(true)
                          .build()
                          .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
