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

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.Maven;
import io.helidon.build.util.ProcessMonitor;

import org.eclipse.aether.version.Version;

import static io.helidon.build.util.FileUtils.assertDir;

/**
 * Generator for a quickstart project.
 */
public class QuickstartGenerator {
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final String HELIDON_GROUP_ID = "io.helidon";
    private static final String HELIDON_PROJECT_ID = "helidon-project";
    private static final String ARCHETYPES_GROUP_ID = "io.helidon.archetypes";
    private static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";
    private static final String QUICKSTART_PACKAGE_PREFIX = "io.helidon.examples.quickstart.";
    private static final String HELIDON_VERSION_RANGE = "[1.0,1.9.999]";

    /**
     * Helidon variants.
     */
    public enum HelidonVariant {
        /**
         * Helidon SE.
         */
        SE("se"),

        /**
         * Helidon MP.
         */
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

    private String helidonVersion;
    private Path projectDirectory;
    private HelidonVariant variant;
    private Maven maven;

    /**
     * Returns a new generator.
     *
     * @return The generator.
     */
    public static QuickstartGenerator generator() {
        return new QuickstartGenerator();
    }

    private QuickstartGenerator() {
    }

    /**
     * Sets the Helidon version to use. The latest is selected if not set.
     *
     * @param helidonVersion The version.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVersion(String helidonVersion) {
        this.helidonVersion = helidonVersion;
        return this;
    }

    /**
     * Sets the Helidon variant to use.
     *
     * @param helidonVariant The variant.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVariant(HelidonVariant helidonVariant) {
        this.variant = helidonVariant;
        return this;
    }

    /**
     * Sets the directory in which to generate the project.
     *
     * @param projectDirectory The project directory.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator projectDirectory(Path projectDirectory) {
        this.projectDirectory = assertDir(projectDirectory);
        return this;
    }

    /**
     * Generate the project.
     *
     * @return The path to the project.
     */
    public Path generate() {
        final Version version = init();
        final String id = HELIDON_QUICKSTART_PREFIX + variant.toString();
        final String pkg = QUICKSTART_PACKAGE_PREFIX + variant.toString();
        Log.info("Creating %s from archetype %s", id, version);
        execute(new ProcessBuilder().directory(projectDirectory.toFile())
                                    .command(List.of(MAVEN_EXEC,
                                                     "archetype:generate",
                                                     "-DinteractiveMode=false",
                                                     "-DarchetypeGroupId=" + ARCHETYPES_GROUP_ID,
                                                     "-DarchetypeArtifactId=" + id,
                                                     "-DarchetypeVersion=" + version,
                                                     "-DgroupId=test",
                                                     "-DartifactId=" + id,
                                                     "-Dpackage=" + pkg
                                    )));
        return assertDir(projectDirectory.resolve(id));
    }

    private Version init() {
        if (variant == null) {
            throw new IllegalStateException("helidonVariant required.");
        }
        if (projectDirectory == null) {
            throw new IllegalStateException("projectDirectory required.");
        }
        this.maven = createMaven();
        return helidonVersion == null ? latestVersion() : version();
    }

    private Version version() {
        Log.info("Looking up Helidon %s", helidonVersion);
        final String range = "[" + helidonVersion + "," + helidonVersion + "]";
        final String coordinates = Maven.toCoordinates(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, range);
        final Version version = maven.latestVersion(coordinates, true);
        Log.info("Using Helidon version %s", version);
        return version;
    }

    private Version latestVersion() {
        Log.info("Looking up latest Helidon 1.x release version (2.0.0-M1 doesn't exit.on.started)");
        final String coordinates = Maven.toCoordinates(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, HELIDON_VERSION_RANGE);
        final Version version = maven.latestVersion(coordinates, false);
        Log.info("Using Helidon release version %s", version);
        return version;
    }

    private Version lookupLatestHelidonVersion() {
        // TODO 2.0.0-M1 doesn't exit MP with -Dexit.on.started
        final List<Version> versions = maven.versions(HELIDON_GROUP_ID, HELIDON_PROJECT_ID);
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

    private static Maven createMaven() {
        if (System.getProperty("dump.env") != null) {
            Log.info("\n--- Environment ---- \n");
            System.getenv().forEach((key, value) -> Log.info("    %s = %s", key, value));
            Log.info("\n--- System Properties ---- \n");
            System.getProperties().forEach((key, value) -> Log.info("    %s = %s", key, value));
        }
        return Maven.builder().build();
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
