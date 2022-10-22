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

import io.helidon.build.common.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Unpack an artifact to a given target location.
 */
final class UnpackArtifactTask extends StagingTask {

    static final String ELEMENT_NAME = "unpack-artifact";

    private final ArtifactGAV gav;
    private final String includes;
    private final String excludes;

    UnpackArtifactTask(ActionIterators iterators, Map<String, String> attrs) {
        super(ELEMENT_NAME, null, iterators, attrs);
        this.gav = new ArtifactGAV(attrs);
        this.includes = attrs.get("includes");
        this.excludes = attrs.get("excludes");
    }

    /**
     * Get the GAV.
     *
     * @return GAV, never {@code null}
     */
    ArtifactGAV gav() {
        return gav;
    }

    /**
     * Get the excludes.
     *
     * @return excludes, may be {@code null}
     */
    String excludes() {
        return excludes;
    }

    /**
     * Get the includes.
     *
     * @return includes, may be {@code null}
     */
    String includes() {
        return includes;
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        ArtifactGAV resolvedGav = gav.resolve(vars);
        Map<String, String> resolvedVars = resolvedGav.variables();
        String resolvedTarget = resolveVar(target(), resolvedVars);
        Path artifact = ctx.resolve(resolvedGav);
        Path targetDir = dir.resolve(resolvedTarget).normalize();
        ctx.logInfo("Unpacking %s to %s", artifact, targetDir);
        FileUtils.deleteDirectory(targetDir);
        Files.createDirectories(targetDir);
        ctx.unpack(artifact, targetDir, excludes, includes);
    }
}
