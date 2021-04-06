/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;

/**
 * Pom file utilities.
 */
public class PomUtils {
    private static final String BUILD_TOOLS_GROUP_ID = "io.helidon.build-tools";
    private static final String BUILD_TOOLS_PLUGIN_ARTIFACT_ID = "helidon-maven-plugin";
    private static final String POM = "pom.xml";

    /**
     * The Helidon plugin version property name.
     */
    public static final String HELIDON_PLUGIN_VERSION_PROPERTY = "version.helidon.plugin";

    /**
     * Returns the pom file from the given project.
     *
     * @param projectDir The project directory.
     * @return The pom file.
     */
    public static Path toPomFile(Path projectDir) {
        return requireFile(requireDirectory(projectDir).resolve(POM));
    }

    /**
     * Ensures that the helidon plugin is configured in the pom file of the given project.
     *
     * @param projectDir The project directory.
     * @param helidonPluginVersion The plugin version.
     */
    public static void ensureHelidonPluginConfig(Path projectDir, String helidonPluginVersion) {
        // Support a system property override of the version here for testing
        String pluginVersion = System.getProperty(HELIDON_PLUGIN_VERSION_PROPERTY, helidonPluginVersion);
        Path pomFile = toPomFile(projectDir);
        Model model = readPomModel(pomFile);
        boolean propertyAdded = ensurePluginVersion(model, pluginVersion);
        boolean extensionAdded = ensurePlugin(model);
        if (extensionAdded || propertyAdded) {
            writePomModel(pomFile, model);
        }
    }

    /**
     * Reads the pom model.
     *
     * @param pomPath The pom path.
     * @return The model.
     * @throws RuntimeException on error.
     */
    public static Model readPomModel(Path pomPath) {
        return readPomModel(pomPath.toFile());
    }

    /**
     * Reads the pom model.
     *
     * @param pomFile The pom file.
     * @return The model.
     * @throws RuntimeException on error.
     */
    public static Model readPomModel(File pomFile) {
        try {
            try (FileReader fr = new FileReader(pomFile)) {
                MavenXpp3Reader mvnReader = new MavenXpp3Reader();
                return mvnReader.read(fr);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the pom model.
     *
     * @param pomPath The pom path.
     * @param model The model.
     * @throws RuntimeException on error.
     */
    public static void writePomModel(Path pomPath, Model model) {
        writePomModel(pomPath.toFile(), model);
    }

    /**
     * Writes the pom model.
     *
     * @param pomFile The pom file.
     * @param model The model.
     * @throws RuntimeException on error.
     */
    public static void writePomModel(File pomFile, Model model) {
        try {
            try (FileWriter fw = new FileWriter(pomFile)) {
                MavenXpp3Writer mvnWriter = new MavenXpp3Writer();
                mvnWriter.write(fw, model);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean ensurePluginVersion(Model model, String helidonPluginVersion) {
        Properties properties = model.getProperties();
        String existing = properties.getProperty(HELIDON_PLUGIN_VERSION_PROPERTY);
        if (existing == null || !existing.equals(helidonPluginVersion)) {
            model.addProperty(HELIDON_PLUGIN_VERSION_PROPERTY, helidonPluginVersion);
            return true;
        } else {
            return false;
        }
    }

    private static boolean ensurePlugin(Model model) {
        org.apache.maven.model.Build build = model.getBuild();
        boolean isPresent = build.getPlugins()
                                 .stream()
                                 .anyMatch(p -> p.getGroupId().equals(BUILD_TOOLS_GROUP_ID)
                                         && p.getArtifactId().equals(BUILD_TOOLS_PLUGIN_ARTIFACT_ID));
        if (isPresent) {
            // Assume it is what we want rather than updating if not equal, since
            // that could undo future archetype changes.
            return false;
        } else {
            Plugin helidonPlugin = new Plugin();
            helidonPlugin.setGroupId(BUILD_TOOLS_GROUP_ID);
            helidonPlugin.setArtifactId(BUILD_TOOLS_PLUGIN_ARTIFACT_ID);
            helidonPlugin.setVersion("${" + HELIDON_PLUGIN_VERSION_PROPERTY + "}");
            helidonPlugin.setExtensions(true);
            build.addPlugin(helidonPlugin);
            return true;
        }
    }

    private PomUtils() {
    }
}
