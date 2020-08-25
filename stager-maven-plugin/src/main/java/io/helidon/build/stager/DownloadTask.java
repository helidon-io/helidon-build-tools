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

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.util.NetworkConnection;

/**
 * Download a file to a given target location.
 */
final class DownloadTask extends StagingTask {

    /**
     * Reusable byte buffer.
     */
    private static final byte[] BUFFER = new byte[8 * 1024];

    static final String ELEMENT_NAME = "download";

    private final String url;

    DownloadTask(ActionIterators iterators, String url, String target) {
        super(iterators, target);
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        this.url = url;
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
    public String elementName() {
        return ELEMENT_NAME;
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        String resolvedTarget = resolveVar(target(), variables);
        URL resolvedUrl = new URL(resolveVar(url, variables));
        context.logInfo("Downloading %s to %s", resolvedUrl, resolvedTarget);
        Path targetFile = dir.resolve(resolvedTarget);
        Files.createDirectories(targetFile.getParent());
        try (BufferedInputStream bis = new BufferedInputStream(open(resolvedUrl))) {
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                int n;
                while ((n = bis.read(BUFFER, 0, BUFFER.length)) >= 0) {
                    fos.write(BUFFER, 0, n);
                }
            }
        }
    }

    @Override
    public String describe(Path dir, Map<String, String> variables) {
        return ELEMENT_NAME + "{"
                + "url=" + resolveVar(url, variables)
                + ", target=" + resolveVar(target(), variables)
                + '}';
    }

    private InputStream open(URL url) throws IOException {
        return NetworkConnection.builder()
                .url(url)
                .open();
    }
}
