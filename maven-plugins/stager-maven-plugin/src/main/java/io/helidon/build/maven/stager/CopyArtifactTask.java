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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import io.helidon.build.common.xml.XMLElement;

/**
 * Copy an artifact to a given target location.
 */
final class CopyArtifactTask extends StagingTask {

    private final ArtifactGAV gav;
    private final String target;

    CopyArtifactTask(XMLElement element) {
        super(element);
        this.gav = new ArtifactGAV(element);
        this.target = element.attribute("target", "{artifactId}-{version}.{type}");

    }

    @Override
    public String toString() {
        return "CopyArtifactTask{"
               + "gav=" + gav
               + ", target='" + target + '\''
               + '}';
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        var resolvedGav = gav.resolve(vars);
        var resolvedVars = resolvedGav.variables();
        var resolveTarget = resolveVar(target, resolvedVars);
        ctx.logInfo("Copying %s to %s", resolvedGav, resolveTarget);
        var artifact = ctx.resolve(resolvedGav);
        var targetFile = dir.resolve(resolveTarget);
        ctx.ensureDirectory(targetFile.getParent());
        Files.copy(artifact, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }
}
