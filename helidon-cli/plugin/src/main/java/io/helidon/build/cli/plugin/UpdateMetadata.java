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
package io.helidon.build.cli.plugin;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A plugin that fetches the latest metadata and updates the cache.
 */
public class UpdateMetadata extends Plugin {
    private static final int DEFAULT_TIMEOUT_MILLIS = 500;
    private static final String VERSION_ARG = "--version";
    private static final String BASE_URL_ARG = "--baseUrl";
    private static final String CACHE_DIR_ARG = "--cacheDir";
    private static final String CONNECT_TIMEOUT_ARG = "--connectTimeout";
    private static final String READ_TIMEOUT_ARG = "--readTimeout";
    private static final String LATEST_VERSION_FILE_NAME = "latest";
    private static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    private static final String ETAG_HEADER = "Etag";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String ZIP_FILE_NAME = "cli-data.zip";
    private static final String REMOTE_DATA_FILE_SUFFIX = "/" + ZIP_FILE_NAME;

    private String version;
    private URL baseUrl;
    private Path cacheDir;
    private int connectTimeout;
    private int readTimeout;
    private Path latestVersionFile;

    /**
     * Constructor.
     */
    public UpdateMetadata() {
        this.connectTimeout = DEFAULT_TIMEOUT_MILLIS;
        this.readTimeout = DEFAULT_TIMEOUT_MILLIS;
    }

    @Override
    int parseArg(String arg, int argIndex, String[] allArgs) throws Exception {
        switch (arg) {
            case VERSION_ARG:
                version = nextArg(argIndex, allArgs);
                return argIndex + 1;
            case BASE_URL_ARG:
                baseUrl = new URL(nextArg(argIndex, allArgs));
                return argIndex + 1;
            case CACHE_DIR_ARG:
                cacheDir = Path.of(nextArg(argIndex, allArgs));
                return argIndex + 1;
            case CONNECT_TIMEOUT_ARG:
                connectTimeout = Integer.parseInt(nextArg(argIndex, allArgs));
                return argIndex + 1;
            case READ_TIMEOUT_ARG:
                readTimeout = Integer.parseInt(nextArg(argIndex, allArgs));
                return argIndex + 1;
            default:
                return argIndex;
        }
    }

    @Override
    void validateArgs() throws Exception {
        assertRequiredArg(BASE_URL_ARG, baseUrl);
        assertRequiredArg(CACHE_DIR_ARG, cacheDir);
        if (!Files.exists(cacheDir)) {
            throw new FileNotFoundException(cacheDir.toString());
        }
        latestVersionFile = cacheDir.toAbsolutePath().resolve(LATEST_VERSION_FILE_NAME);
    }

    @Override
    void execute() throws Exception {
        if (version == null) {
            updateLatestVersion();
            updateVersion(readLatestVersion());
        } else {
            updateVersion(version);
        }
    }

    private String readLatestVersion() throws Exception {
        final List<String> lines = Files.readAllLines(latestVersionFile, UTF_8);
        for (String line : lines) {
            if (!line.isEmpty()) {
                return line.trim();
            }
        }
        throw new IllegalStateException("No version in " + latestVersionFile);
    }

    private URL resolve(String fileName) throws Exception {
        return new URL(baseUrl.toExternalForm() + "/" + fileName);
    }

    private void updateLatestVersion() throws Exception {
        // Always update
        final URL url = resolve(LATEST_VERSION_FILE_NAME);
        Log.debug("downloading %s", url);
        final URLConnection connection = NetworkConnection.builder()
                                                          .url(url)
                                                          .connectTimeout(connectTimeout)
                                                          .readTimeout(readTimeout)
                                                          .connect();
        Files.copy(connection.getInputStream(), latestVersionFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void updateVersion(String version) throws Exception {
        final Path versionDir = cacheDir.resolve(version);
        final Path lastUpdatedFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        final URL url = resolve(version + REMOTE_DATA_FILE_SUFFIX);
        final Map<String, String> headers = new HashMap<>();
        if (Files.exists(lastUpdatedFile)) {
            final String etag = Files.readString(lastUpdatedFile);
            headers.put(IF_NONE_MATCH_HEADER, etag);
            Log.debug("maybe downloading %s, etag=%s", url, etag);
        } else {
            Log.debug("downloading %s", url);
        }
        final URLConnection connection = NetworkConnection.builder()
                                                          .url(url)
                                                          .headers(headers)
                                                          .connectTimeout(connectTimeout)
                                                          .readTimeout(readTimeout)
                                                          .connect();
        download(connection, versionDir);

        // Update the .lastUpdate file with etag if available or a constant if not

        final String etag = connection.getHeaderField(ETAG_HEADER);
        final String content = etag == null ? "<none>" : etag;
        final InputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));
        Files.copy(input, lastUpdatedFile, StandardCopyOption.REPLACE_EXISTING);
        Log.debug("updated %s with etag %s", lastUpdatedFile, content);
    }

    private void download(URLConnection connection, Path versionDir) throws IOException {

        // Prepare directory

        if (Files.exists(versionDir)) {
            deleteDirectoryContent(versionDir);
        } else {
            Files.createDirectories(versionDir);
        }

        // Download zip file

        final Path zipFile = versionDir.resolve(ZIP_FILE_NAME);
        Files.copy(connection.getInputStream(), zipFile);

        // Unzip

        unzip(zipFile, versionDir);

        // Delete zip file

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
