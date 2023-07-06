/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.cli.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A plugin that updates the cli data for a given version.
 */
class UpdateVersion extends UpdateBase {
    private static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    private static final String ETAG_HEADER = "Etag";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String NO_ETAG = "<no-etag>";
    private static final String ZIP_FILE_NAME = "cli-data.zip";
    private static final String REMOTE_DATA_FILE_SUFFIX = "/" + ZIP_FILE_NAME;
    private static final int STATUS_OK = 200;
    private static final int STATUS_NOT_MODIFIED = 304;

    /**
     * Constructor.
     */
    UpdateVersion() {
        super();
    }

    @Override
    void validateArgs() throws Exception {
        super.validateArgs();
        assertRequiredArg(VERSION_ARG, version());
    }

    @Override
    protected void doExecute() throws IOException {
        String version = version();
        Path versionDir = cacheDir().resolve(version);
        Path lastUpdateFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        URL url = resolve(version + REMOTE_DATA_FILE_SUFFIX);
        Map<String, String> headers = headers(lastUpdateFile);
        URLConnection connection = httpGet(url, headers);
        int status = status(connection);
        if (status == STATUS_OK) {
            download(connection, versionDir);
            writeLastUpdate(connection, lastUpdateFile);
        } else if (status == STATUS_NOT_MODIFIED) {
            Log.debug("not modified %s", url);
            writeLastUpdate(connection, lastUpdateFile); // just to touch it
        } else {
            throw new IllegalStateException("connection failed with " + status + " " + url);
        }
    }

    private Map<String, String> headers(Path lastUpdateFile) throws IOException {
        final Map<String, String> headers = headers();
        if (Files.exists(lastUpdateFile)) {
            final String etag = Files.readString(lastUpdateFile);
            headers.put(IF_NONE_MATCH_HEADER, etag);
        }
        return headers;
    }

    private void writeLastUpdate(URLConnection connection, Path lastUpdateFile) throws IOException {
        final String etag = connection.getHeaderField(ETAG_HEADER);
        final String content = etag == null ? NO_ETAG : etag;
        final InputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));
        Files.copy(input, lastUpdateFile, REPLACE_EXISTING);
        Log.debug("updated %s with etag %s", lastUpdateFile, content);
    }

    private void download(URLConnection connection, Path versionDir) throws IOException {
        if (Files.exists(versionDir)) {
            deleteDirectoryContent(versionDir);
        } else {
            Files.createDirectories(versionDir);
        }

        Path zipFile = versionDir.resolve(ZIP_FILE_NAME);
        Files.copy(connection.getInputStream(), zipFile);

        unzip(zipFile, versionDir);

        Log.debug("deleting %s", zipFile);
        Files.delete(zipFile);
    }

    private static void unzip(Path zipFile, Path destDir) throws IOException {
        Log.debug("unzipping %s to %s", zipFile, destDir);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                final Path destFile = destDir.resolve(entry.getName());
                Files.copy(zip, destFile);
                entry = zip.getNextEntry();
            }
            zip.closeEntry();
        }
    }

    private static void deleteDirectoryContent(Path directory) throws IOException {
        Log.debug("deleting %s", directory);
        //noinspection DuplicatedCode
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                  .filter(file -> !file.equals(directory))
                  .forEach(file -> {
                      try {
                          Files.delete(file);
                      } catch (IOException e) {
                          throw new UncheckedIOException(e);
                      }
                  });
        }
    }
}
