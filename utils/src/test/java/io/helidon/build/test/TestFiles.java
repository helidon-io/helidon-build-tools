/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.util.Constants;
import io.helidon.build.util.Instance;
import io.helidon.build.util.Log;
import io.helidon.build.util.Maven;
import io.helidon.build.util.ProcessMonitor;

import org.eclipse.aether.version.Version;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.helidon.build.util.Constants.DIR_SEP;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static java.util.Objects.requireNonNull;

/**
 * Test file utilities.
 */
public class TestFiles implements BeforeAllCallback {
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final String HELIDON_GROUP_ID = "io.helidon";
    private static final String HELIDON_PROJECT_ID = "helidon-project";
    private static final String ARCHETYPES_GROUP_ID = "io.helidon.archetypes";
    private static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";
    private static final String QUICKSTART_PACKAGE_PREFIX = "io.helidon.examples.quickstart.";
    private static final String SIGNED_JAR_COORDINATES = "org.bouncycastle:bcpkix-jdk15on:1.60";
    private static final String VERSION_1_4_1 = "1.4.1";
    private static final AtomicReference<Path> TARGET_DIR = new AtomicReference<>();
    private static final Instance<Maven> MAVEN = new Instance<>(TestFiles::createMaven);
    private static final Instance<Version> LATEST_HELIDON_VERSION = new Instance<>(TestFiles::lookupLatestHelidonVersion);
    private static final Instance<Path> SE_JAR = new Instance<>(TestFiles::getOrCreateQuickstartSeJar);
    private static final Instance<Path> MP_JAR = new Instance<>(TestFiles::getOrCreateQuickstartMpJar);
    private static final Instance<Path> SIGNED_JAR = new Instance<>(TestFiles::fetchSignedJar);

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (TARGET_DIR.get() == null) {
            TARGET_DIR.set(targetDir(requireNonNull(ctx.getRequiredTestClass())));
        }
    }

    /**
     * Returns the target directory, set from the location of the first test class to execute. This approach ensures that
     * each project using this class will have its own target directory used.
     *
     * @return The directory.
     */
    public static Path targetDir() {
        return requireNonNull(TARGET_DIR.get());
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
        return latestHelidonVersion().toString().equals(VERSION_1_4_1) ? "✅" : "!";
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
     * Returns the quickstart SE project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    public static Path helidonSeProject() {
        helidonSeJar(); // ensure created.
        return targetDir().resolve(quickstartId("se"));
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
     * Returns the quickstart MP project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    public static Path helidonMpProject() {
        helidonMpJar(); // ensure created.
        return targetDir().resolve(quickstartId("mp"));
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
    public static Path ensureFile(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    /**
     * Returns the {@link Maven} instance.
     *
     * @return The instance.
     */
    public static Maven maven() {
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
        Log.info("Looking up latest Helidon 1.x release version (2.0.0-M1 doesn't exit.on.started)");
        // final Version version = maven().latestVersion(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, false);
        final String coordinates = Maven.toCoordinates(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, "[1.0,1.9.999]");
        final Version version = maven().latestVersion(coordinates, false);
        Log.info("Using Helidon release version %s", version);
        return version;
    }

    private static Path fetchSignedJar() {
        Log.info("Fetching signed jar %s", SIGNED_JAR_COORDINATES);
        return maven().artifact(SIGNED_JAR_COORDINATES);
    }

    private static Path targetDir(Class<?> testClass) {
        try {
            final Path codeSource = Paths.get(testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            return ensureDirectory(codeSource.getParent());
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
        final Path sourceDir = targetDir().resolve(id);
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
        final Path sourceDir = assertDir(targetDir().resolve(id));
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
        final Path targetDir = targetDir();
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
                          .execute(5, TimeUnit.MINUTES); // May need to download a lot of dependencies.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
