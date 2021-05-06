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
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Copy an artifact to a given target location.
 */
final class CopyArtifactTask extends StagingTask {

    static final String ELEMENT_NAME = "copy-artifact";

    private final ArtifactGAV gav;

    CopyArtifactTask(ActionIterators iterators, ArtifactGAV gav, String target) {
        super(iterators, target == null ? "{artifactId}-{version}.{type}" : target);
        this.gav = Objects.requireNonNull(gav);
    }

    /**
     * Get the GAV.
     *
     * @return GAV, never {@code null}
     */
    ArtifactGAV gav() {
        return gav;
    }

    @Override
    public String elementName() {
        return ELEMENT_NAME;
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        ArtifactGAV resolvedGav = resolveGAV(variables);
        String resolveTarget = resolveVar(target(), variables);
        context.logInfo("Resolving %s", resolvedGav);
        Path artifact = context.resolve(resolvedGav);
        Path targetFile = dir.resolve(resolveTarget);
        context.logInfo("Copying %s to %s", artifact, targetFile);
        Files.copy(artifact, targetFile);
    }

    private ArtifactGAV resolveGAV(Map<String, String> variables) {
        ArtifactGAV resolvedGav = new ArtifactGAV(
                resolveVar(gav.groupId(), variables),
                resolveVar(gav.artifactId(), variables),
                resolveVar(gav.version(), variables),
                resolveVar(gav.type(), variables),
                resolveVar(gav.classifier(), variables));
        variables.put("groupId", resolvedGav.groupId());
        variables.put("artifactId", resolvedGav.artifactId());
        variables.put("version", resolvedGav.version());
        variables.put("type", resolvedGav.type());
        String resolvedClassifier = resolvedGav.classifier();
        if (resolvedClassifier != null && !resolvedClassifier.isEmpty()) {
            variables.put("classifier", resolvedClassifier);
        }
        return resolvedGav;
    }

    @Override
    public String describe(Path dir, Map<String, String> variables) {
        return ELEMENT_NAME + "{"
                + "gav=" + resolveGAV(variables)
                + ", target=" + resolveVar(target(), variables)
                + '}';
    }
}
