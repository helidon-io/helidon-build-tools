/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.PomUtils.ensureHelidonPluginConfig;
import static io.helidon.build.util.ProjectConfig.ensureHelidonCliConfig;
import static io.helidon.build.util.Style.Blue;
import static io.helidon.build.util.Style.BoldBrightCyan;

/**
 * Simple generator for a quickstart project. This class does not import any Maven classes
 * and is currently used to avoid problems with Graal native compilation.
 */
public class QuickstartGenerator {

    /**
     * Maven executable.
     */
    protected static final String MAVEN_EXEC = Constants.OS.mavenExec();

    /**
     * Group ID for archetypes.
     */
    protected static final String ARCHETYPES_GROUP_ID = "io.helidon.archetypes";

    /**
     * Helidon quickstart prefix.
     */
    protected static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";

    /**
     * Quickstart package prefix.
     */
    protected static final String QUICKSTART_PACKAGE_PREFIX = "io.helidon.examples.quickstart.";

    private Path parentDirectory;
    private HelidonVariant variant;
    private boolean quiet;
    private String groupId;
    private String artifactId;
    private String version;
    private String packageName;
    private String pluginVersion;

    /**
     * Returns a new generator.
     *
     * @return The generator.
     */
    public static QuickstartGenerator generator() {
        return new QuickstartGenerator();
    }

    protected QuickstartGenerator() {
        pluginVersion = BuildToolsProperties.instance().version();
    }

    protected Path parentDirectory() {
        return parentDirectory;
    }

    protected HelidonVariant variant() {
        return variant;
    }

    protected String groupId() {
        return groupId;
    }

    /**
     * Set the project's group ID.
     *
     * @param groupId The artifact ID.
     * @return The generator.
     */
    public QuickstartGenerator groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    protected String artifactId() {
        return artifactId;
    }

    /**
     * Set the project's artifact ID.
     *
     * @param artifactId The artifact ID.
     * @return The generator.
     */
    public QuickstartGenerator artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    protected String packageName() {
        return packageName;
    }

    /**
     * Set the project's package.
     *
     * @param packageName The package.
     * @return The generator.
     */
    public QuickstartGenerator packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    /**
     * Sets the Helidon version to use.
     *
     * @param version The version.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVersion(String version) {
        Objects.requireNonNull(version);
        this.version = version;
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
     * Sets the Helidon plugin version to use.
     *
     * @param pluginVersion The version.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator pluginVersion(String pluginVersion) {
        Objects.requireNonNull(pluginVersion);
        this.pluginVersion = pluginVersion;
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
        Log.info("Creating %s using version %s", BoldBrightCyan.apply(artifactId), Blue.apply(version));
        String archetypeId = HELIDON_QUICKSTART_PREFIX + variant.toString();
        execute(new ProcessBuilder().directory(parentDirectory.toFile())
                                    .command(List.of(MAVEN_EXEC,
                                                     "archetype:generate",
                                                     "-DinteractiveMode=false",
                                                     "-DarchetypeGroupId=" + ARCHETYPES_GROUP_ID,
                                                     "-DarchetypeArtifactId=" + archetypeId,
                                                     "-DarchetypeVersion=" + version,
                                                     "-DgroupId=" + groupId,
                                                     "-DartifactId=" + artifactId,
                                                     "-Dpackage=" + packageName
                                    )));
        final Path projectDir = assertDir(parentDirectory.resolve(artifactId));
        ensureHelidonCliConfig(projectDir, version);
        ensureHelidonPluginConfig(projectDir, pluginVersion);  // NOTE: Remove this once new archetype is completed!
        log("Created %s", projectDir);
        return projectDir;
    }

    protected void initialize() {
        if (version == null) {
            throw new IllegalStateException("version required.");
        }
        if (variant == null) {
            throw new IllegalStateException("helidonVariant required.");
        }
        if (parentDirectory == null) {
            throw new IllegalStateException("projectDirectory required.");
        }
        if (pluginVersion == null) {
            throw new IllegalStateException("pluginVersion required.");
        }
        if (groupId == null) {
            groupId = "test";
        }
        if (artifactId == null) {
            artifactId = HELIDON_QUICKSTART_PREFIX + variant.toString();
        }
        if (packageName == null) {
            packageName = QUICKSTART_PACKAGE_PREFIX + variant.toString();
        }
        final Path projectDir = parentDirectory.resolve(artifactId);
        if (Files.exists(projectDir)) {
            throw new IllegalStateException(projectDir + " already exists");
        }
    }

    protected void log(String message, Object... args) {
        if (!quiet) {
            Log.info(message, args);
        }
    }

    protected static void execute(ProcessBuilder builder) {
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
