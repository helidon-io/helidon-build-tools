/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.helidon.build.common.Strings;

import static io.helidon.build.common.FileUtils.fileExt;
import static io.helidon.build.maven.stager.DownloadTask.download;

/**
 * Download an unpack to a given target location.
 */
final class UnpackTask extends StagingTask {

    static final String ELEMENT_NAME = "unpack";

    private final String ext;
    private final String url;
    private final String includes;
    private final String excludes;

    UnpackTask(ActionIterators iterators, Map<String, String> attrs) {
        super(ELEMENT_NAME, null, iterators, attrs);
        this.url = Strings.requireValid(attrs.get("url"), "url is required");
        this.ext = Strings.requireValid(Optional.ofNullable(attrs.get("ext"))
                .orElseGet(() -> fileExt(url)), "ext is required");
        this.includes = attrs.get("includes");
        this.excludes = attrs.get("excludes");
    }

    /**
     * Get the url.
     *
     * @return url, never {@code null}
     */
    String url() {
        return url;
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
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        return execBodyWithTimeout(ctx, dir, vars);
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        Path tempFile = ctx.createTempFile("." + ext);
        URL url = new URL(resolveVar(this.url, vars));
        download(ctx, url, tempFile);

        String resolvedTarget = resolveVar(target(), vars);
        Path targetDir = dir.resolve(resolvedTarget).normalize();
        ctx.logInfo("Unpacking %s to %s", tempFile, targetDir);
        ctx.ensureDirectory(targetDir);
        ctx.unpack(tempFile, targetDir, excludes, includes);
    }
}
