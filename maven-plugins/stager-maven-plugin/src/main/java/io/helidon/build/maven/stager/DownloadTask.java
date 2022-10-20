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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.helidon.build.common.NetworkConnection;
import io.helidon.build.common.Strings;

/**
 * Download a file to a given target location.
 */
final class DownloadTask extends StagingTask {

    static final String ELEMENT_NAME = "download";

    private final String url;

    DownloadTask(ActionIterators iterators, Map<String, String> attrs) {
        super(ELEMENT_NAME, null, iterators, attrs);
        this.url = Strings.requireValid(attrs.get("url"), "url is required");
    }

    /**
     * Get the url.
     *
     * @return url, never {@code null}
     */
    String url() {
        return url;
    }

    @Override
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        return execBodyWithTimeout(ctx, dir, vars);
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        String resolvedTarget = resolveVar(target(), vars);
        URL resolvedUrl = new URL(resolveVar(url, vars));
        ctx.logInfo("Downloading %s to %s", resolvedUrl, resolvedTarget);
        Path targetFile = dir.resolve(resolvedTarget);
        Files.createDirectories(targetFile.getParent());
        try (BufferedInputStream bis = new BufferedInputStream(open(resolvedUrl, ctx))) {
            try (OutputStream fos = Files.newOutputStream(targetFile)) {
                int n;
                byte[] buffer = new byte[1024];
                while ((n = bis.read(buffer, 0, buffer.length)) >= 0) {
                    fos.write(buffer, 0, n);
                }
            }
        }
    }

    @Override
    public String describe(Path dir, Map<String, String> vars) {
        return ELEMENT_NAME + "{"
                + "url=" + resolveVar(url, vars)
                + ", target=" + resolveVar(target(), vars)
                + '}';
    }

    private InputStream open(URL url, StagingContext context) throws IOException {
        NetworkConnection.Builder builder = NetworkConnection.builder().url(url);
        int readTimeout = context.readTimeout();
        if (readTimeout > 0) {
            builder.readTimeout(readTimeout);
        }
        int connectTimeout = context.connectTimeout();
        if (connectTimeout > 0) {
            builder.connectTimeout(connectTimeout);
        }
        int maxRetries = context.maxRetries();
        if (maxRetries > 0) {
            builder.maxRetries(maxRetries);
        }
        return builder.open();
    }
}
