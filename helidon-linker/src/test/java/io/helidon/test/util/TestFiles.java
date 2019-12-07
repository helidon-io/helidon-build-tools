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

package io.helidon.test.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    private static final String ARCHETYPES_GROUP = "io.helidon.archetypes";
    private static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";
    private static final String QUICKSTART_PACKAGE_PREFIX = "io.helidon.examples.quickstart.";
    private static final String SIGNED_JAR_COORDINATES = "org.bouncycastle:bcpkix-jdk15on:1.60";
    private static final Instance<Maven> MAVEN = new Instance<>(TestFiles::createMaven);
    private static final Instance<Version> ARCHETYPE_VERSION = new Instance<>(TestFiles::lookupLatestQuickstartVersion);
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
     * Returns the latest quickstart archetype version.
     *
     * @return The version.
     */
    public static Version latestQuickstartArchetypeVersion() {
        return ARCHETYPE_VERSION.instance();
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

    private static Maven maven() {
        return MAVEN.instance();
    }

    private static Maven createMaven() {

        /*
        Installing /pipeline/source/pom.xml to /pipeline/cache/local_repository/io/helidon/build-tools/helidon-build-tools-project/1.0.11-SNAPSHOT/helidon-build-tools-project-1.0.11-SNAPSHOT.pom

            localRepo = /pipeline/cache/local_repository
         */

        Log.info("\n--- Environment ---- \n");
        System.getenv().forEach((key, value) -> Log.info("    %s = %s", key, value));
        Log.info("\n--- System Properties ---- \n");
        System.getProperties().forEach((key, value) -> Log.info("    %s = %s", key, value));

        final Path workDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        if (workDir.getRoot().toString().equals("pipeline")) {
            // Assume pipeline until we learn more TODO cleanup!
            return Maven.builder().localRepositoryDir(Paths.get("/pipeline/cache/local_repository")).build();
        } else {
            return Maven.builder().build();
        }
    }

    private static Version lookupLatestQuickstartVersion() {
        final String artifactId = quickstartId("se");
        Log.info("Looking up latest %s:%s version", ARCHETYPES_GROUP, artifactId);
        final Version version = Maven.builder().build().latestVersion(ARCHETYPES_GROUP, artifactId);
        Log.info("Latest archetype version is %s", version);
        return version;
    }

    private static Path fetchSignedJar() {
        Log.info("Fetching signed jar %s", SIGNED_JAR_COORDINATES);
        return maven().artifact(SIGNED_JAR_COORDINATES);
    }

    private static Path ourTargetDir() {
        final Path ourCodeSource = Paths.get(TestFiles.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        return ourCodeSource.getParent();
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
                                    .command(List.of("mvn", "clean", "package", "-DskipTests")));
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
        final Version archetypeVersion = latestQuickstartArchetypeVersion();
        Log.info("Creating %s from archetype %s", id, archetypeVersion);
        execute(new ProcessBuilder().directory(targetDir.toFile())
                                    .command(List.of("mvn",
                                                     "archetype:generate",
                                                     "-DinteractiveMode=false",
                                                     "-DarchetypeGroupId=" + ARCHETYPES_GROUP,
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
