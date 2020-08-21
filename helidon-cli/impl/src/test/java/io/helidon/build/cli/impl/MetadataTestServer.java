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
package io.helidon.build.cli.impl;

import io.helidon.build.cli.impl.TestMetadata.TestVersion;
import io.helidon.build.util.Log;

import org.junit.jupiter.api.Assumptions;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.model.NottableString;
import org.mockserver.netty.MockServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import static io.helidon.build.cli.impl.TestMetadata.CLI_DATA_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.etag;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;

/**
 * Metadata server that serves local test data.
 */
public class MetadataTestServer {
    private static final String USAGE = "Usage: [--port <port>] [--rc1 | --rc2] [--quiet] [--help]";
    private static final int DEFAULT_MAIN_PORT = 8080;
    private static final String VERBOSE_LEVEL = "INFO";
    private static final String NORMAL_LEVEL = "WARN";
    private static final String URL_PREFIX = "http://localhost:";
    private static final String ETAG_HEADER = "Etag";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";

    private static Expectation latestRequest() {
        return new Expectation(request().withMethod("GET")
                                        .withPath("/latest")).withId("latest");
    }

    private static Expectation zipRequestWithoutEtag(TestVersion version) {
        return new Expectation(request().withMethod("GET")
                                        .withHeader(not(IF_NONE_MATCH_HEADER))
                                        .withPath("/" + version + "/" + CLI_DATA_FILE_NAME))
                                        .withId(version + "-zip-without-etag");
    }

    private static Expectation zipRequestWithoutMatchingEtag(TestVersion version, byte[] data) {
        return new Expectation(request().withMethod("GET")
                                        .withHeader(NottableString.string(IF_NONE_MATCH_HEADER), not(etag(version, data)))
                                        .withPath("/" + version + "/" + CLI_DATA_FILE_NAME))
                                        .withId(version + "-zip-without-matching-etag");
    }

    private static Expectation zipRequestWithMatchingEtag(TestVersion version, byte[] data) {
        return new Expectation(request().withMethod("GET")
                                        .withHeader(IF_NONE_MATCH_HEADER, etag(version, data))
                                        .withPath("/" + version + "/" + CLI_DATA_FILE_NAME))
                                        .withId(version + "-zip-with-matching-etag");
    }

    private final int port;
    private final String url;
    private TestVersion latest;
    private ClientAndServer mockServer;

    /**
     * Main entry point to run standalone process.
     *
     * @param args The arguments.
     */
    public static void main(String[] args) {
        int port = DEFAULT_MAIN_PORT;
        TestVersion latest = TestVersion.RC1;
        boolean verbose = true;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--port":
                    port = Integer.parseInt(nextArg(i++, args));
                    break;
                case "--rc1":
                    latest = TestVersion.RC1;
                    break;
                case "--rc2":
                    latest = TestVersion.RC2;
                    break;
                case "--quiet":
                    verbose = false;
                    break;
                case "--help":
                    usage(0, null);
                    break;
                default:
                    usage(1, "Unknown arg: %s", arg);
                    break;
            }
        }

        new MetadataTestServer(port, latest, verbose).start();
    }

    /**
     * Constructor.
     *
     * @param latest The version to return for the "/latest" request.
     * @param verbose Whether or not to do verbose logging.
     */
    public MetadataTestServer(TestVersion latest, boolean verbose) {
        this(freePort(), latest, verbose);
    }

    /**
     * Constructor.
     *
     * @param port The port to listen on.
     * @param latest The version to return for the "/latest" request.
     * @param verbose Whether or not to do verbose logging.
     */
    @SuppressWarnings("ConstantConditions")
    public MetadataTestServer(int port, TestVersion latest, boolean verbose) {
        if (MockServer.class.getClassLoader() != ClassLoader.getSystemClassLoader()) {
            final String reason = "MockServer must be in system class loader";
            Log.info("$(italic,yellow Skipping: %s)", reason);
            Assumptions.assumeTrue(false, reason);
        }
        ConfigurationProperties.logLevel(verbose ? VERBOSE_LEVEL : NORMAL_LEVEL);
        this.port = port;
        this.url = URL_PREFIX + port;
        this.latest = latest;
    }

    /**
     * Start the server.
     *
     * @return This instance, for chaining.
     */
    @SuppressWarnings("BusyWait")
    public MetadataTestServer start() {
        mockServer = ClientAndServer.startClientAndServer(port);

        // Set the response for "/latest"

        latest(latest);

        // Set the responses for the "${version}/cli-data.zip" requests, with and without etags

        for (TestVersion version : TestVersion.values()) {
            zipData(version, TestMetadata.zipData(version));
        }

        // Ensure started

        int retries = 5;
        while (!mockServer.isRunning()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (--retries > 0) {
                Log.info("Waiting for metadata test server to start, remaining retries = %s", retries);
            } else {
                stop();
                throw new IllegalStateException("Metadata test server did not start.");
            }
        }
        Log.info("Started metadata test server with latest=%s at %s", latest, url);
        return this;
    }

    /**
     * Returns the base url.
     *
     * @return The url.
     */
    public String url() {
        return url;
    }

    /**
     * Returns the version in use for the "/latest" request.
     *
     * @return The version.
     */
    public TestVersion latest() {
        return latest;
    }

    /**
     * Sets the response for the "/latest" request.
     *
     * @param latest The version.
     */
    public void latest(TestVersion latest) {
        this.latest = latest;
        mockServer.upsert(latestRequest().thenRespond(response().withBody(latest.toString())));
    }

    /**
     * Sets the response for the "${version}/cli-data.zip" request for the given version, with and without an etag.
     * Return the data when no etag or a 304 when etag matches.
     *
     * @param version The version.
     * @param data The zip data.
     */
    public void zipData(TestVersion version, byte[] data) {
        final String etag = etag(version, data);
        mockServer.upsert(zipRequestWithoutEtag(version).thenRespond(response().withHeader(ETAG_HEADER, etag)
                                                                               .withBody(data)));
        mockServer.upsert(zipRequestWithoutMatchingEtag(version, data).thenRespond(response().withHeader(ETAG_HEADER, etag)
                                                                                             .withBody(data)));
        mockServer.upsert(zipRequestWithMatchingEtag(version, data).thenRespond(response().withHeader(ETAG_HEADER, etag)
                                                                                          .withStatusCode(304)));
    }

    /**
     * Stop the server.
     */
    public void stop() {
        mockServer.stop();
    }

    /**
     * Get the next argument or return the usage.
     *
     * @param currentIndex current index
     * @param allArgs      the arguments array
     * @return next arguments at {@code currentIndex + 1}, or the usage if the next argument is not found in
     * {@code allArgs}
     */
    static String nextArg(int currentIndex, String[] allArgs) {
        final int nextIndex = currentIndex + 1;
        if (nextIndex < allArgs.length) {
            return allArgs[nextIndex];
        } else {
            usage(1, allArgs[currentIndex] + ": missing required argument");
            return "";
        }
    }

    private static void usage(int exitCode, String errorMsg, Object... errorArgs) {
        if (errorMsg != null) {
            Log.error(errorMsg, errorArgs);
        }
        Log.info(USAGE);
        System.exit(exitCode);
    }

    private static int freePort() {
        ServerSocket s = null;
        try {
            s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                s.close();
            } catch (IOException ignore) {
            }
        }
    }
}
