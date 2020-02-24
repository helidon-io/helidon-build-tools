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

package io.helidon.build.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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

    private Predicate<Version> versionSelector;
    private Path parentDirectory;
    private HelidonVariant variant;
    private boolean quiet;
    private String id;
    private Version version;

    /**
     * Returns a new generator.
     *
     * @return The generator.
     */
    public static QuickstartGenerator generator() {
        return new QuickstartGenerator();
    }

    private QuickstartGenerator() {
        this.versionSelector = Maven.LATEST_RELEASE;
    }

    /**
     * Sets the Helidon version to use. The latest release is selected if not set.
     *
     * @param helidonVersion The version.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVersion(String helidonVersion) {
        return helidonVersion(v -> v.toString().equals(helidonVersion));
    }

    /**
     * Sets a selector for the Helidon version to use. The latest release is selected if not set.
     *
     * @param helidonVersionSelector The version.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVersion(Predicate<Version> helidonVersionSelector) {
        this.versionSelector = helidonVersionSelector;
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
     * Sets whether or not log messages should be suppressed. Default is {@code false}.
     *
     * @param quiet {@code true} if log messages should not be written.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator quiet(boolean quiet) {
        this.quiet = quiet;
        return this;
    }

    /**
     * Sets the directory in which to generate the project.
     *
     * @param parentDirectory The parent directory.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator parentDirectory(Path parentDirectory) {
        this.parentDirectory = assertDir(parentDirectory);
        return this;
    }

    /**
     * Generate the project.
     *
     * @return The path to the project.
     */
    public Path generate() {
        initialize();
        final String pkg = QUICKSTART_PACKAGE_PREFIX + variant.toString();
        Log.info("Generating %s from archetype %s", id, version);
        execute(new ProcessBuilder().directory(parentDirectory.toFile())
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
        final Path result = assertDir(parentDirectory.resolve(id));
        log("Generated %s", result);
        return result;
    }

    private void initialize() {
        if (variant == null) {
            throw new IllegalStateException("helidonVariant required.");
        }
        if (parentDirectory == null) {
            throw new IllegalStateException("projectDirectory required.");
        }
        this.id = HELIDON_QUICKSTART_PREFIX + variant.toString();
        final Path projectDir = parentDirectory.resolve(id);
        if (Files.exists(projectDir)) {
            throw new IllegalStateException(projectDir + " already exists");
        } else {
            final Maven maven = Maven.instance();
            this.version = maven.latestVersion(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, versionSelector);
        }
    }

    private void log(String message, Object... args) {
        if (!quiet) {
            Log.info(message, args);
        }
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
