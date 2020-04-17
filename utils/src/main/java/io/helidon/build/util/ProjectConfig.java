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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertExists;

/**
 * Class ConfigFile.
 */
public class ProjectConfig extends ConfigProperties {

    /**
     * Helidon CLI config file.
     */
    public static final String DOT_HELIDON = ".helidon";

    /**
     * Project's directory property.
     */
    public static final String PROJECT_DIRECTORY = "project.directory";

    /**
     * Project's flavor.
     */
    public static final String PROJECT_FLAVOR = "project.flavor";

    /**
     * Prefix for all feature properties.
     */
    public static final String FEATURE_PREFIX = "feature.";

    /**
     * Project's classpath property.
     */
    public static final String PROJECT_CLASSPATH = "project.classpath";

    /**
     * Project's source directories property.
     */
    public static final String PROJECT_SOURCEDIRS = "project.sourcedirs";

    /**
     * Project's class directories property.
     */
    public static final String PROJECT_CLASSDIRS = "project.classdirs";

    /**
     * Project's resource directories property.
     */
    public static final String PROJECT_RESOURCEDIRS = "project.resourcedirs";

    /**
     * Project's main class.
     */
    public static final String PROJECT_MAINCLASS = "project.mainclass";

    /**
     * Project's features.
     */
    public static final String PROJECT_FEATURES = "project.features";

    /**
     * Project's version.
     */
    public static final String PROJECT_VERSION = "project.version";

    /**
     * Project last successful build time.
     */
    public static final String PROJECT_LAST_BUILD_SUCCESS_TIME = "project.last.build.success.time";

    /**
     * Tests whether or not the configuration from the {@link #DOT_HELIDON} file in the given project directory exists.
     *
     * @param projectDir The project directory.
     * @return {@code true} if file exists.
     */
    public static boolean helidonCliConfigExists(Path projectDir) {
        return Files.isRegularFile(assertDir(projectDir).resolve(DOT_HELIDON));
    }

    /**
     * Loads and returns configuration from the {@link #DOT_HELIDON} file in the given project directory.
     *
     * @param projectDir The project directory.
     * @return The configuration.
     */
    public static ProjectConfig loadHelidonCliConfig(Path projectDir) {
        final Path dotHelidon = assertExists(assertDir(projectDir).resolve(DOT_HELIDON));
        return new ProjectConfig(dotHelidon.toFile());
    }

    /**
     * Constructor.
     *
     * @param file The file.
     */
    public ProjectConfig(File file) {
        super(file);
    }

    /**
     * Project's directory.
     *
     * @return Project's directory as optional.
     */
    public Optional<Path> projectDir() {
        String dir = property(PROJECT_DIRECTORY);
        return dir == null ? Optional.empty() : Optional.of(Paths.get(dir));
    }

    /**
     * Set project's directory.
     *
     * @param projectDir The directory.
     */
    public void projectDir(Path projectDir) {
        property(PROJECT_DIRECTORY, projectDir.toString());
    }

    /**
     * List of features available to this project.
     *
     * @return List of features.
     */
    public List<String> listFeatures() {
        return keySet().stream()
                       .filter(k -> (k).startsWith(FEATURE_PREFIX))
                       .map(k -> (k).substring(FEATURE_PREFIX.length()))
                       .collect(Collectors.toList());
    }

    /**
     * List of dependencies for a certain feature.
     *
     * @param feature The feature.
     * @return List of deps.
     */
    public List<ProjectDependency> featureDeps(String feature) {
        return entrySet()
            .stream()
            .filter(e -> {
                String s = e.getKey();
                return s.equals(FEATURE_PREFIX + feature);
            })
            .flatMap(e -> {
                String v = e.getValue();
                return Arrays.stream(v.split(","))
                             .map(d -> {
                                 String[] ds = d.split(":");
                                 return new ProjectDependency(ds[0], ds[1], ds.length > 2 ? ds[2] : null);
                             });
            }).collect(Collectors.toList());
    }

    /**
     * Record that a build failed.
     */
    public void buildFailed() {
        remove(PROJECT_LAST_BUILD_SUCCESS_TIME);
    }

    /**
     * Record that a build completed successfully.
     */
    public void buildSucceeded() {
        property(PROJECT_LAST_BUILD_SUCCESS_TIME, Long.toString(System.currentTimeMillis()));
    }

    /**
     * Returns the last successful build time.
     *
     * @return The time (millis since epoch), or 0L if last build did not succeed.
     */
    public long lastSuccessfulBuildTime() {
        final String time = property(PROJECT_LAST_BUILD_SUCCESS_TIME);
        return time == null ? 0L : Long.parseLong(time);
    }
}
