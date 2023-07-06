/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLException;

/**
 * A plugin base to update metadata.
 */
abstract class UpdateBase extends Plugin {
    private static final int DEFAULT_TIMEOUT_MILLIS = 500;
    private static final String CLI_VERSION_ARG = "--cliVersion";
    private static final String BASE_URL_ARG = "--baseUrl";
    private static final String CACHE_DIR_ARG = "--cacheDir";
    private static final String CONNECT_TIMEOUT_ARG = "--connectTimeout";
    private static final String READ_TIMEOUT_ARG = "--readTimeout";
    private static final String MAX_ATTEMPTS_ARG = "--maxAttempts";
    private static final String VERSIONS_FILE_NAME = "versions.xml";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final int STATUS_OK = 200;
    private static final String USER_AGENT_PREFIX = "Helidon-CLI/";

    /**
     * {@code --version}.
     */
    static final String VERSION_ARG = "--version";

    private String version;
    private String cliVersion;
    private URL baseUrl;
    private Path cacheDir;
    private int connectTimeout;
    private int readTimeout;
    private int maxAttempts;
    private Path versionsFile;

    /**
     * Constructor.
     */
    UpdateBase() {
        this.connectTimeout = DEFAULT_TIMEOUT_MILLIS;
        this.readTimeout = DEFAULT_TIMEOUT_MILLIS;
        this.maxAttempts = NetworkConnection.DEFAULT_MAXIMUM_ATTEMPTS;
    }

    /**
     * Get the value of {@code --version}.
     *
     * @return version
     */
    protected String version() {
        return version;
    }

    /**
     * Get the versions file.
     *
     * @return version file
     */
    protected Path versionsFile() {
        return versionsFile;
    }

    /**
     * Get the value of {@code --cacheDir}.
     *
     * @return cache directory
     */
    public Path cacheDir() {
        return cacheDir;
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
        versionsFile = cacheDir.toAbsolutePath().resolve(VERSIONS_FILE_NAME);
    }

    @Override
    void execute() throws Exception {
        try {
            doExecute();
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

    /**
     * Make a {@code GET} HTTP request.
     *
     * @param url     URL
     * @param headers headers
     * @return URL connection
     * @throws IOException if an IO error occurs
     */
    protected URLConnection httpGet(URL url, Map<String, String> headers) throws IOException {
        Log.debug("downloading %s, headers=%s", url, headers);
        return NetworkConnection.builder()
                .url(url)
                .headers(headers)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .maxAttempts(maxAttempts)
                .connect();
    }

    /**
     * Execute the update.
     *
     * @throws IOException if an IO error occurs
     */
    protected abstract void doExecute() throws IOException;

    /**
     * Resolve a URI against {@code --baseUrl}.
     *
     * @param uri uri
     * @return URL
     * @throws IOException if an IO error occurs
     */
    protected URL resolve(String uri) throws IOException {
        return new URL(baseUrl.toExternalForm() + "/" + uri);
    }

    /**
     * Get the common headers.
     *
     * @return common headers
     */
    protected Map<String, String> headers() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(USER_AGENT_HEADER, userAgent());
        return headers;
    }

    /**
     * Get the status code for a given {@link URLConnection}.
     *
     * @param connection connection
     * @return status code
     * @throws IOException if an IO error occurs
     */
    protected static int status(URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection) {
            return ((HttpURLConnection) connection).getResponseCode();
        } else {
            return STATUS_OK;
        }
    }

    private String userAgent() {
        return USER_AGENT_PREFIX + cliVersion + " ("
                + System.getProperty("os.name") + "; "
                + System.getProperty("os.version") + "; "
                + System.getProperty("os.arch") + "; "
                + "jvm:" + System.getProperty("java.vm.version")
                + ")";
    }

    private static Throwable cause(Exception e) {
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
