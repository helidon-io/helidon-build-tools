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

package io.helidon.dev.build.clidemo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Class CliConfig.
 */
class CliConfig {

    static final String PROJECT_DIRECTORY = "project.directory";
    static final String HELIDON_VARIANT = "helidon.variant";
    static final String FEATURE_PREFIX = "feature.";

    private File file;
    private Properties properties;

    static class Dependency {
        String groupId;
        String artifactId;
        String version;

        public String groupId() {
            return groupId;
        }

        public void groupId(String groupId) {
            this.groupId = groupId;
        }

        public String artifactId() {
            return artifactId;
        }

        public void artifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String version() {
            return version;
        }

        public void version(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + (version != null ? ":" + version : "");
        }
    }

    CliConfig(File file) {
        this.file = file;
        ensureProperties();
    }

    Optional<Path> projectDir() {
        String dir = properties.getProperty(PROJECT_DIRECTORY);
        return dir == null ? Optional.empty() : Optional.of(Path.of(dir));
    }

    void projectDir(Path projectDir) {
        properties.setProperty(PROJECT_DIRECTORY, projectDir.toString());
    }

    void clearProjectDir() {
        properties.remove(PROJECT_DIRECTORY);
    }

    List<String> listFeatures() {
        return properties.keySet().stream()
                .filter(k -> ((String) k).startsWith(FEATURE_PREFIX))
                .map(k -> ((String) k).substring(FEATURE_PREFIX.length()))
                .collect(Collectors.toList());
    }

    List<Dependency> featureDeps(String feature) {
        return properties.entrySet()
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
                                Dependency dep = new Dependency();
                                dep.groupId(ds[0]);
                                dep.artifactId(ds[1]);
                                if (ds.length > 2) {
                                    dep.version(ds[2]);
                                }
                                return dep;
                            });
                }).collect(Collectors.toList());
    }

    void store() {
        try {
            try (FileWriter writer = new FileWriter(file)) {
                properties.store(writer, "CLI Demo Properties");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureProperties() {
        if (properties == null) {
            try {
                if (file.exists()) {
                    try (FileReader reader = new FileReader(file)) {
                        properties = new Properties();
                        properties.load(reader);
                    }
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        properties = new Properties();
    }
}

