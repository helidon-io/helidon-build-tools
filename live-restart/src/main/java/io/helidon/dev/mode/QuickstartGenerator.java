/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.mode;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.helidon.build.util.Constants;
import io.helidon.build.util.Instance;
import io.helidon.build.util.Log;
import io.helidon.build.util.Maven;
import io.helidon.build.util.ProcessMonitor;
import org.eclipse.aether.version.Version;

import static io.helidon.build.util.Constants.DIR_SEP;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;

/**
 * Class ProjectGenerator.
 */
public class QuickstartGenerator {

    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final String HELIDON_GROUP_ID = "io.helidon";
    private static final String HELIDON_PROJECT_ID = "helidon-project";
    private static final String ARCHETYPES_GROUP_ID = "io.helidon.archetypes";
    private static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";
    private static final String QUICKSTART_PACKAGE_PREFIX = "io.helidon.examples.quickstart.";
    private static final String SIGNED_JAR_COORDINATES = "org.bouncycastle:bcpkix-jdk15on:1.60";
    private static final Instance<Maven> MAVEN = new Instance<>(QuickstartGenerator::createMaven);
    private static final Instance<Version> LATEST_HELIDON_VERSION = new Instance<>(QuickstartGenerator::lookupLatestHelidonVersion);
    private final Instance<Path> SE_JAR = new Instance<>(this::getOrCreateQuickstartSeJar);
    private final Instance<Path> MP_JAR = new Instance<>(this::getOrCreateQuickstartMpJar);
    private final Instance<Path> SIGNED_JAR = new Instance<>(QuickstartGenerator::fetchSignedJar);

    public enum HelidonVariant {
        SE("se"),
        MP("mp");

        private final String variant;

        HelidonVariant(String variant) {
            this.variant = variant;
        }

        @Override
        public String toString() {
            return variant;
        }
    }

    private final Path targetDir;

    public QuickstartGenerator(Path targetDir) {
        this.targetDir = targetDir;
    }

    public Path generate(HelidonVariant variant) {
        switch (variant) {
            case SE:
                return helidonSeProject();
            case MP:
                return helidonMpProject();
            default:
                throw new InternalError("Unknown variant " + variant);
        }
    }

    /**
     * Returns the latest Helidon version.
     *
     * @return The version.
     */
    static Version latestHelidonVersion() {
        return LATEST_HELIDON_VERSION.instance();
    }

    /**
     * Returns the quickstart SE main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    private Path helidonSeJar() {
        return SE_JAR.instance();
    }

    /**
     * Returns the quickstart SE project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    private Path helidonSeProject() {
        helidonSeJar(); // ensure created.
        return targetDir.resolve(quickstartId(HelidonVariant.SE.toString()));
    }

    /**
     * Returns the quickstart MP main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    private Path helidonMpJar() {
        return MP_JAR.instance();
    }

    /**
     * Returns the quickstart MP project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    private Path helidonMpProject() {
        helidonMpJar(); // ensure created.
        return targetDir.resolve(quickstartId(HelidonVariant.MP.toString()));
    }

    /**
     * Returns the {@link Maven} instance.
     *
     * @return The instance.
     */
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
        // TODO 2.0.0-M1 doesn't exit MP with -Dexit.on.started
        final List<Version> versions = maven().versions(HELIDON_GROUP_ID, HELIDON_PROJECT_ID);
        final int lastIndex = versions.size() - 1;
        return IntStream.rangeClosed(0, lastIndex)
                .mapToObj(index -> versions.get(lastIndex - index))
                .filter(version -> !version.toString().endsWith("-SNAPSHOT"))
                .filter(version -> !version.toString().startsWith("2"))   //
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no non-snapshot version found!"));

        /* TODO
        Log.info("Looking up latest Helidon release version");
        final Version version = maven().latestVersion(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, false);
        Log.info("Latest Helidon release version is %s", version);
        return version;

         */
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

    private Path getOrCreateQuickstartSeJar() {
        return getOrCreateQuickstartJar("se");
    }

    private Path getOrCreateQuickstartMpJar() {
        return getOrCreateQuickstartJar("mp");
    }

    private Path getOrCreateQuickstartJar(String helidonVariant) {
        final String id = quickstartId(helidonVariant);
        final Path sourceDir = targetDir.resolve(id);
        if (Files.exists(sourceDir)) {
            return quickstartJar(sourceDir, id);
        } else {
            return createQuickstartJar(helidonVariant);
        }
    }

    private Path createQuickstartJar(String helidonVariant) {
        createQuickstartProject(helidonVariant);
        return buildQuickstartProject(helidonVariant);
    }

    private Path buildQuickstartProject(String helidonVariant) {
        final String id = quickstartId(helidonVariant);
        final Path sourceDir = assertDir(targetDir.resolve(id));
        Log.info("Building %s", id);

        // Make sure we use the current JDK by forcing it first in the path and setting JAVA_HOME. This might be required
        // if we're in an IDE whose process was started with a different JDK.

        final ProcessBuilder builder = new ProcessBuilder().directory(sourceDir.toFile())
                .command(List.of(MAVEN_EXEC, "clean", "package", "-DskipTests"));
        final String javaHome = System.getProperty("java.home");
        final String javaHomeBin = javaHome + File.separator + "bin";
        final Map<String, String> env = builder.environment();
        final String path = javaHomeBin + File.pathSeparatorChar + env.get("PATH");
        env.put("PATH", path);
        env.put("JAVA_HOME", javaHome);

        execute(builder);
        return quickstartJar(sourceDir, id);
    }

    private static Path quickstartJar(Path sourceDir, String id) {
        return assertFile(sourceDir.resolve("target" + DIR_SEP + id + ".jar"));
    }

    private static String quickstartId(String helidonVariant) {
        return HELIDON_QUICKSTART_PREFIX + helidonVariant;
    }

    private Path createQuickstartProject(String helidonVariant) {
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

    private Path directory(String helidonVariant, int copyNumber) {
        final String id = quickstartId(helidonVariant) + "-" + copyNumber;
        return targetDir.resolve(id);
    }

    private static void execute(ProcessBuilder builder) {
        try {
            ProcessMonitor.builder()
                    .processBuilder(builder)
                    .capture(true)
                    .build()
                    .execute(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
