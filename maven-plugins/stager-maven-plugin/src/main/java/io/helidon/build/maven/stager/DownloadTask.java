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

import static io.helidon.build.common.FileUtils.measuredSize;

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
        try {
            throw new IOException("boo");
//            download(ctx, dir, vars);
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void download(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        String path = resolveVar(target(), vars);
        Path file = dir.resolve(path).normalize();
        Files.createDirectories(file.getParent());
        URL url = new URL(resolveVar(this.url, vars));
        try (BufferedInputStream bis = new BufferedInputStream(open(url, ctx))) {
            try (OutputStream fos = Files.newOutputStream(file)) {
                int n;
                long startTime = System.currentTimeMillis();
                long progressTime = startTime;
                long currentTime = startTime;
                long totalSize = 0;
                long totalTime = 1;
                byte[] buffer = new byte[1024];
                while ((n = bis.read(buffer, 0, buffer.length)) >= 0) {
                    fos.write(buffer, 0, n);
                    totalSize += n;
                    currentTime = System.currentTimeMillis();
                    if (currentTime - progressTime >= 1000) {
                        totalTime = (currentTime - startTime) / 1000;
                        progressTime = currentTime;
                        ctx.logInfo("Downloading %s to %s (%s at %s/s)",
                                url, path, measuredSize(totalSize), measuredSize(totalSize / totalTime));
                    }
                }
                if (currentTime - progressTime >= 1000) {
                    totalTime = (currentTime - startTime) / 1000;
                }
                ctx.logInfo("Downloaded %s to %s (%s at %s/s)",
                        url, path, measuredSize(totalSize), measuredSize(totalSize / totalTime));
            }
        }
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
