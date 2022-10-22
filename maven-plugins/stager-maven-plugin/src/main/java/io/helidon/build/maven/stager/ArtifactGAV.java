/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.util.HashMap;
import java.util.Map;

import io.helidon.build.common.Strings;

import static io.helidon.build.maven.stager.StagingTask.resolveVar;

/**
 * Artifact GAV.
 */
final class ArtifactGAV {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;
    private final String classifier;
    private final Map<String, String> variables;

    ArtifactGAV(String groupId,
                String artifactId,
                String version,
                String type,
                String classifier,
                Map<String, String> vars) {

        this.groupId = Strings.requireValid(groupId, "groupId is required");
        this.artifactId = Strings.requireValid(artifactId, "artifactId is required");
        this.version = Strings.requireValid(version, "version is required");
        this.type = type == null ? "jar" : type;
        this.classifier = classifier;
        this.variables = addVariables(new HashMap<>(vars));
    }

    ArtifactGAV(String groupId, String artifactId, String version, String type, String classifier) {
        this(groupId, artifactId, version, type, classifier, Map.of());
    }

    ArtifactGAV(Map<String, String> vars) {
        this(
                vars.get("groupId"),
                vars.get("artifactId"),
                vars.get("version"),
                vars.get("type"),
                vars.get("classifier"),
                vars);
    }

    private ArtifactGAV(ArtifactGAV gav, Map<String, String> vars) {
        this(
                resolveVar(gav.groupId, vars),
                resolveVar(gav.artifactId, vars),
                resolveVar(gav.version, vars),
                resolveVar(gav.type, vars),
                resolveVar(gav.classifier, vars),
                vars);
    }

    private Map<String, String> addVariables(Map<String, String> vars) {
        vars.put("groupId", groupId);
        vars.put("artifactId", artifactId);
        vars.put("version", version);
        vars.put("type", type);
        if (Strings.isValid(classifier)) {
            vars.put("classifier", classifier);
        }
        return vars;
    }

    /**
     * Get the groupId.
     *
     * @return groupId, never {@code null}
     */
    String groupId() {
        return groupId;
    }

    /**
     * Get the artifactId.
     *
     * @return artifactId, never {@code null}
     */
    String artifactId() {
        return artifactId;
    }

    /**
     * Get the version.
     *
     * @return artifactId, never {@code null}
     */
    String version() {
        return version;
    }

    /**
     * Get the type.
     *
     * @return type, never {@code null}
     */
    String type() {
        return type;
    }

    /**
     * Get the classifier.
     *
     * @return classifier, may be {@code null}
     */
    String classifier() {
        return classifier;
    }

    /**
     * Get the resolved variables.
     *
     * @return variables
     */
    Map<String, String> variables() {
        return variables;
    }

    /**
     * Resolve this instance against the given variables.
     *
     * @param vars variables
     * @return resolved GAV
     */
    ArtifactGAV resolve(Map<String, String> vars) {
        return new ArtifactGAV(this, vars);
    }

    @Override
    public String toString() {
        String gav = groupId + ":" + artifactId + ":" + version;
        if (classifier != null && !classifier.isEmpty()) {
            gav += ":" + classifier;
        }
        gav += ":" + type;
        return gav;
    }
}
