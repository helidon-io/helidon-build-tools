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
package io.helidon.build.stager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Copy an artifact to a given target location.
 */
final class CopyArtifactTask extends StagingTask {

    private final ArtifactGAV gav;

    CopyArtifactTask(List<Map<String, List<String>>> iterators, ArtifactGAV gav, String target) {
        super(iterators, target == null ? "{artifactId}.{type}" : target);
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
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
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
        String resolveTarget = resolveVar(target(), variables);
        context.logInfo("Resolving %s", resolvedGav);
        Path artifact = context.resolve(resolvedGav);
        Path targetFile = dir.resolve(resolveTarget);
        context.logInfo("Copying %s to %s", artifact, targetFile);
        Files.copy(artifact, targetFile);
    }
}