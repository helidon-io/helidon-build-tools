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
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;

import static io.helidon.build.common.FileUtils.fileExt;
import static io.helidon.build.maven.stager.DownloadTask.download;

/**
 * Download an unpack to a given target location.
 */
final class UnpackTask extends StagingTask {

    private final String ext;
    private final String url;
    private final String target;
    private final String includes;
    private final String excludes;
    private final List<Mapper> mappers;

    UnpackTask(XMLElement element) {
        super(element);
        this.url = Strings.requireValid(element.attribute("url", null), "url is required");
        this.target = Strings.requireValid(element.attribute("target", null), "target is required");
        this.ext = Strings.requireValid(Optional.ofNullable(element.attribute("ext", null))
                .orElseGet(() -> fileExt(url)), "ext is required");
        this.includes = element.attribute("includes", null);
        this.excludes = element.attribute("excludes", null);
        this.mappers = Lists.map(elements(element, "mapper", "mappers"), Mapper::new);
    }

    @Override
    public String toString() {
        return "UnpackTask{"
               + "ext='" + ext + '\''
               + ", url='" + url + '\''
               + ", target='" + target + '\''
               + ", includes='" + includes + '\''
               + ", excludes='" + excludes + '\''
               + ", mappers=" + mappers
               + '}';
    }

    @Override
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        return execBodyWithTimeout(ctx, dir, vars);
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        var tempFile = ctx.createTempFile("." + ext);
        var url = new URL(resolveVar(this.url, vars));
        download(ctx, url, tempFile);

        var resolvedTarget = resolveVar(target, vars);
        var targetDir = dir.resolve(resolvedTarget).normalize();
        ctx.logInfo("Unpacking %s to %s", tempFile, targetDir);
        ctx.ensureDirectory(targetDir);
        ctx.unpack(tempFile, targetDir, includes, excludes, mappers, vars);
    }
}
