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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return dir == null ? Optional.empty() : Optional.of(Path.of(dir));
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
                .filter(k -> ((String) k).startsWith(FEATURE_PREFIX))
                .map(k -> ((String) k).substring(FEATURE_PREFIX.length()))
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
                    String s = (String) e.getKey();
                    return s.equals(FEATURE_PREFIX + feature);
                })
                .flatMap(e -> {
                    String v = (String) e.getValue();
                    return Arrays.stream(v.split(","))
                            .map(d -> {
                                String[] ds = d.split(":");
                                ProjectDependency dep = new ProjectDependency(ds[0], ds[1],
                                        ds.length > 2 ? ds[2] : null);
                                return dep;
                            });
                }).collect(Collectors.toList());
    }
}
