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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;

/**
 * Unpack an artifact to a given target location.
 */
final class UnpackArtifactTask extends StagingTask {

    private final ArtifactGAV gav;
    private final String target;
    private final String includes;
    private final String excludes;
    private final List<Mapper> mappers;

    UnpackArtifactTask(XMLElement element) {
        super(element);
        this.gav = new ArtifactGAV(element);
        this.target = Strings.requireValid(element.attribute("target", null), "target is required");
        this.includes = element.attribute("includes", null);
        this.excludes = element.attribute("excludes", null);
        this.mappers = Lists.map(elements(element, "mapper", "mappers"), Mapper::new);
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        ArtifactGAV resolvedGav = gav.resolve(vars);
        Map<String, String> resolvedVars = resolvedGav.variables();
        String resolvedTarget = resolveVar(target, resolvedVars);
        Path artifact = ctx.resolve(resolvedGav);
        Path targetDir = dir.resolve(resolvedTarget).normalize();
        ctx.logInfo("Unpacking %s to %s", artifact, targetDir);
        ctx.ensureDirectory(targetDir);
        ctx.unpack(artifact, targetDir, excludes, includes, mappers, resolvedVars);
    }

    ArtifactGAV gav() {
        return gav;
    }

    String target() {
        return target;
    }

    String excludes() {
        return excludes;
    }

    String includes() {
        return includes;
    }

    List<Mapper> mappers() {
        return mappers;
    }
}
