/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
 *
 * @param groupId    groupId
 * @param artifactId artifactId
 * @param version    version
 * @param type       type
 * @param classifier classifier
 * @param variables  variables
 */
record ArtifactGAV(String groupId,
                   String artifactId,
                   String version,
                   String type,
                   String classifier,
                   Map<String, String> variables) {

    ArtifactGAV(String groupId,
                String artifactId,
                String version,
                String type,
                String classifier,
                Map<String, String> variables) {

        this.groupId = Strings.requireValid(groupId, "groupId is required");
        this.artifactId = Strings.requireValid(artifactId, "artifactId is required");
        this.version = Strings.requireValid(version, "version is required");
        this.type = type == null ? "jar" : type;
        this.classifier = classifier;
        this.variables = addVariables(new HashMap<>(variables));
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

    /**
     * Resolve this instance against the given variables.
     *
     * @param vars variables
     * @return resolved GAV
     */
    ArtifactGAV resolve(Map<String, String> vars) {
        return new ArtifactGAV(
                resolveVar(groupId, vars),
                resolveVar(artifactId, vars),
                resolveVar(version, vars),
                resolveVar(type, vars),
                resolveVar(classifier, vars),
                vars);
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
}
