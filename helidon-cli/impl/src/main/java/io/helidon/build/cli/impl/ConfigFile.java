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

package io.helidon.build.cli.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.build.util.ConfigProperties;

/**
 * Class ConfigFile.
 */
class ConfigFile extends ConfigProperties {

    static final String DOT_HELIDON = ".helidon";
    static final String PROJECT_DIRECTORY = "project.directory";
    static final String HELIDON_FLAVOR = "helidon.flavor";
    static final String FEATURE_PREFIX = "feature.";

    static class Dependency {
        private String groupId;
        private String artifactId;
        private String version;

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

    ConfigFile(File file) {
        super(file);
    }

    Optional<Path> projectDir() {
        String dir = property(PROJECT_DIRECTORY);
        return dir == null ? Optional.empty() : Optional.of(Path.of(dir));
    }

    void projectDir(Path projectDir) {
        property(PROJECT_DIRECTORY, projectDir.toString());
    }

    void clearProjectDir() {
        property(PROJECT_DIRECTORY);
    }

    List<String> listFeatures() {
        return keySet().stream()
                .filter(k -> ((String) k).startsWith(FEATURE_PREFIX))
                .map(k -> ((String) k).substring(FEATURE_PREFIX.length()))
                .collect(Collectors.toList());
    }

    List<Dependency> featureDeps(String feature) {
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
}

