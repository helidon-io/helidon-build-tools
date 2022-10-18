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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.common.NetworkConnection;

/**
 * Download a file to a given target location.
 */
final class DownloadTask extends StagingTask {

    /**
     * Constant for the readTimeout property.
     */
    static final String READ_TIMEOUT_PROP = "stager.readTimeout";

    /**
     * Constant for the connectTimeout property.
     */
    static final String CONNECT_TIMEOUT_PROP = "stager.connectTimeout";

    /**
     * Constant for the maxRetries property.
     */
    static final String MAX_RETRIES = "stager.maxRetries";

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
        try (BufferedInputStream bis = new BufferedInputStream(open(resolvedUrl, context))) {
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                int n;
                byte[] buffer = new byte[1024];
                while ((n = bis.read(buffer, 0, buffer.length)) >= 0) {
                    fos.write(buffer, 0, n);
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

    private InputStream open(URL url, StagingContext context) throws IOException {
        NetworkConnection.Builder builder = NetworkConnection.builder().url(url);
        String readTimeout = context.property(READ_TIMEOUT_PROP);
        if (readTimeout != null) {
            builder.readTimeout(Integer.parseInt(readTimeout));
        }
        String connectTimeout = context.property(CONNECT_TIMEOUT_PROP);
        if (connectTimeout != null) {
            builder.connectTimeout(Integer.parseInt(connectTimeout));
        }
        String maxRetries = context.property(MAX_RETRIES);
        if (maxRetries != null) {
            builder.maxRetries(Integer.parseInt(maxRetries));
        }
        return builder.open();
    }
}
