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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.SSLException;

import io.helidon.build.cli.common.LatestVersion;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A plugin that fetches the latest metadata and updates the cache.
 */
class UpdateMetadata extends Plugin {
    private static final int DEFAULT_TIMEOUT_MILLIS = 500;
    private static final String VERSION_ARG = "--version";
    private static final String CLI_VERSION_ARG = "--cliVersion";
    private static final String BASE_URL_ARG = "--baseUrl";
    private static final String CACHE_DIR_ARG = "--cacheDir";
    private static final String CONNECT_TIMEOUT_ARG = "--connectTimeout";
    private static final String READ_TIMEOUT_ARG = "--readTimeout";
    private static final String MAX_ATTEMPTS_ARG = "--maxAttempts";
    private static final String LATEST_VERSION_FILE_NAME = "latest";
    private static final String VERSIONS_FILE_NAME = "versions.xml";
    private static final String LAST_UPDATE_FILE_NAME = ".lastUpdate";
    private static final String ETAG_HEADER = "Etag";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String NO_ETAG = "<no-etag>";
    private static final String ZIP_FILE_NAME = "cli-data.zip";
    private static final String REMOTE_DATA_FILE_SUFFIX = "/" + ZIP_FILE_NAME;
    private static final int STATUS_OK = 200;
    private static final int STATUS_NOT_MODIFIED = 304;
    private static final String USER_AGENT_PREFIX = "Helidon-CLI/";

    private String version;
    private String cliVersion;
    private URL baseUrl;
    private Path cacheDir;
    private int connectTimeout;
    private int readTimeout;
    private int maxAttempts;
    private Path latestVersionFile;
    private Path versionsFile;

    /**
     * Constructor.
     */
    UpdateMetadata() {
        this.connectTimeout = DEFAULT_TIMEOUT_MILLIS;
        this.readTimeout = DEFAULT_TIMEOUT_MILLIS;
        this.maxAttempts = NetworkConnection.DEFAULT_MAXIMUM_ATTEMPTS;
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
            case CLI_VERSION_ARG:
                cliVersion = nextArg(argIndex, allArgs);
                return argIndex + 1;
            case CONNECT_TIMEOUT_ARG:
                connectTimeout = Integer.parseInt(nextArg(argIndex, allArgs));
                return argIndex + 1;
            case READ_TIMEOUT_ARG:
                readTimeout = Integer.parseInt(nextArg(argIndex, allArgs));
                return argIndex + 1;
            case MAX_ATTEMPTS_ARG:
                maxAttempts = Integer.parseInt(nextArg(argIndex, allArgs));
                return argIndex + 1;
            default:
                return -1;
        }
    }

    @Override
    void validateArgs() throws Exception {
        assertRequiredArg(BASE_URL_ARG, baseUrl);
        assertRequiredArg(CACHE_DIR_ARG, cacheDir);
        assertRequiredArg(CLI_VERSION_ARG, cliVersion);
        if (!Files.exists(cacheDir)) {
            throw new FileNotFoundException(cacheDir.toString());
        }
        latestVersionFile = cacheDir.toAbsolutePath().resolve(LATEST_VERSION_FILE_NAME);
        versionsFile = cacheDir.toAbsolutePath().resolve(VERSIONS_FILE_NAME);
    }

    @Override
    void execute() throws Exception {
        try {
            if (version == null) {
                updateLatestVersion();
                updateVersions();
                updateVersion(readLatestVersion());
            } else {
                updateVersion(version);
                updateVersions();
                updateLatestVersion(); // since we're here already, also update the latest
            }
        } catch (UnknownHostException e) {
            throw new Failed("host " + baseUrl.getHost() + " not found when accessing " + baseUrl);
        } catch (SocketTimeoutException e) {
            throw new Failed("timeout accessing " + baseUrl);
        } catch (SSLException e) {
            throw new Failed("accessing " + baseUrl + " failed: " + cause(e).getMessage());
        } catch (FileNotFoundException e) {
            final String message = e.getMessage();
            throw new Failed(message.contains("No such file") ? message : message + " not found");
        } catch (IOException e) {
            final String message = e.getMessage();
            if (message.toLowerCase(Locale.ENGLISH).contains("proxy")) {
                throw new Failed("accessing " + baseUrl + " failed: " + message);
            } else {
                throw new Failed(e.toString());
            }
        }
    }

    private static Throwable cause(Exception e) {
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    private String readLatestVersion()  {
        MavenVersion cliVersion = toMavenVersion(this.cliVersion);
        return LatestVersion.create(latestVersionFile)
                            .latest(cliVersion)
                            .toString();
    }

    private URL resolve(String fileName) throws Exception {
        return new URL(baseUrl.toExternalForm() + "/" + fileName);
    }

    private void updateVersions() throws Exception {
        // Always update
        final URL url = resolve(VERSIONS_FILE_NAME);
        final Map<String, String> headers = commonHeaders();
        debugDownload(url, headers, false);
        final URLConnection connection = NetworkConnection.builder()
                                                          .url(url)
                                                          .headers(headers)
                                                          .connectTimeout(connectTimeout)
                                                          .readTimeout(readTimeout)
                                                          .connect();
        if (connection instanceof HttpURLConnection && ((HttpURLConnection) connection).getResponseCode() == 404) {
            //TODO remove it when version.xml will be implemented and added to the helidon.io
            Files.copy(new ByteArrayInputStream(new byte[]{}), versionsFile, REPLACE_EXISTING);
        } else {
            Files.copy(connection.getInputStream(), versionsFile, REPLACE_EXISTING);
        }
        if (Log.isDebug()) {
            Log.debug("wrote information about archetype versions to %s", versionsFile);
        }
    }

    private void updateLatestVersion() throws Exception {
        // Always update
        final URL url = resolve(LATEST_VERSION_FILE_NAME);
        final Map<String, String> headers = commonHeaders();
        debugDownload(url, headers, false);
        final URLConnection connection = NetworkConnection.builder()
                                                          .url(url)
                                                          .headers(headers)
                                                          .connectTimeout(connectTimeout)
                                                          .readTimeout(readTimeout)
                                                          .connect();
        Files.copy(connection.getInputStream(), latestVersionFile, REPLACE_EXISTING);
        if (Log.isDebug()) {
            Log.debug("wrote %s to %s", readLatestVersion(), latestVersionFile);
        }
    }

    private void updateVersion(String version) throws Exception {
        final Path versionDir = cacheDir.resolve(version);
        final Path lastUpdateFile = versionDir.resolve(LAST_UPDATE_FILE_NAME);
        final URL url = resolve(version + REMOTE_DATA_FILE_SUFFIX);
        final Map<String, String> headers = headers(lastUpdateFile, url);
        final URLConnection connection = NetworkConnection.builder()
                                                          .url(url)
                                                          .headers(headers)
                                                          .connectTimeout(connectTimeout)
                                                          .readTimeout(readTimeout)
                                                          .maxAttempts(maxAttempts)
                                                          .connect();
        final int status = status(connection);
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

    private Map<String, String> headers(Path lastUpdateFile, URL url) throws IOException {
        final Map<String, String> headers = commonHeaders();
        if (Files.exists(lastUpdateFile)) {
            final String etag = Files.readString(lastUpdateFile);
            headers.put(IF_NONE_MATCH_HEADER, etag);
            debugDownload(url, headers, true);
        } else {
            debugDownload(url, headers, false);
        }
        return headers;
    }

    private Map<String, String> commonHeaders() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(USER_AGENT_HEADER, userAgent());
        return headers;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private String userAgent() {
        StringBuilder sb = new StringBuilder();
        sb.append(USER_AGENT_PREFIX).append(cliVersion).append(" (");
        sb.append(System.getProperty("os.name")).append("; ");
        sb.append(System.getProperty("os.version")).append("; ");
        sb.append(System.getProperty("os.arch")).append("; ");
        sb.append("jvm:").append(System.getProperty("java.vm.version"));
        sb.append(")");
        return sb.toString();
    }

    private void debugDownload(URL url, Map<String, String> headers, boolean conditional) {
        final String prefix = conditional ? "maybe " : "";
        Log.debug("%s downloading %s, headers=%s", prefix, url, headers);
    }

    private int status(URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection) {
            return ((HttpURLConnection) connection).getResponseCode();
        } else {
            return STATUS_OK;
        }
    }

    private void writeLastUpdate(URLConnection connection, Path lastUpdateFile) throws IOException {
        final String etag = connection.getHeaderField(ETAG_HEADER);
        final String content = etag == null ? NO_ETAG : etag;
        final InputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));
        Files.copy(input, lastUpdateFile, REPLACE_EXISTING);
        Log.debug("updated %s with etag %s", lastUpdateFile, content);
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
