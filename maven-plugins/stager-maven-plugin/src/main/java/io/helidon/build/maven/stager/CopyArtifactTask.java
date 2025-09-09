/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
import java.nio.file.StandardCopyOption;
import java.util.Map;

import io.helidon.build.common.Maps;

/**
 * Copy an artifact to a given target location.
 */
final class CopyArtifactTask extends StagingTask {

    private static final String DEFAULT_TARGET = "{artifactId}-{version}.{type}";
    static final String ELEMENT_NAME = "copy-artifact";

    private final ArtifactGAV gav;

    CopyArtifactTask(ActionIterators iterators, Map<String, String> attrs) {
        super(ELEMENT_NAME, null, iterators, Maps.computeIfAbsent(attrs, Map.of("target", t -> DEFAULT_TARGET)));
        this.gav = new ArtifactGAV(attrs);
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
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        ArtifactGAV resolvedGav = resolveGAV(vars);
        Map<String, String> resolvedVars = resolvedGav.variables();
        String resolveTarget = resolveVar(target(), resolvedVars);
        ctx.logInfo("Copying %s to %s", resolvedGav, resolveTarget);
        Path artifact = ctx.resolve(resolvedGav);
        Path targetFile = dir.resolve(resolveTarget);
        ctx.ensureDirectory(targetFile.getParent());
        Files.copy(artifact, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private ArtifactGAV resolveGAV(Map<String, String> variables) {
        return new ArtifactGAV(
                resolveVar(gav.groupId(), variables),
                resolveVar(gav.artifactId(), variables),
                resolveVar(gav.version(), variables),
                resolveVar(gav.type(), variables),
                resolveVar(gav.classifier(), variables));
    }
}
