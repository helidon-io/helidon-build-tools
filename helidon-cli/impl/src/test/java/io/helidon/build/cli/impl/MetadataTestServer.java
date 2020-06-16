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
import org.mockserver.netty.MockServer;

import static io.helidon.build.cli.impl.TestMetadata.INITIAL_ETAG;
import static io.helidon.build.cli.impl.TestMetadata.RC1_CLI_DATA_ZIP;
import static io.helidon.build.cli.impl.TestMetadata.RC2_CLI_DATA_ZIP;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Metadata server that serves local test data.
 */
public class MetadataTestServer {
    private static final String USAGE = "Usage: [--port <port>] [--rc1 | --rc2] [--quiet] [--help]";
    private static final int DEFAULT_MAIN_PORT = 8080;
    private static final int DEFAULT_TEST_PORT = 8088;
    private static final String VERBOSE_LEVEL = "INFO";
    private static final String NORMAL_LEVEL = "WARN";
    private static final String URL_PREFIX = "http://localhost:";
    private static final String ETAG_HEADER = "Etag";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String NO_FILE_ETAG = "<no-file>";

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
        this(DEFAULT_TEST_PORT, latest, verbose);
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
        Log.info("Starting metadata test server with latest=%s at %s", latest, url);
        mockServer = ClientAndServer.startClientAndServer(port);

        // Set the version to return for "/latest"

        latest(latest);

        // Always return zip data when If-None-Match header is "<no-file>" (plugin defines this)

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/" + RC1_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(TestMetadata.RC1_ZIP));

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/" + RC2_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(TestMetadata.RC2_ZIP));

        // Always return 304 when If-None-Match is "<initial>"

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, INITIAL_ETAG)
                                 .withPath("/" + RC1_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withStatusCode(304));

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, INITIAL_ETAG)
                                 .withPath("/" + RC2_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withStatusCode(304));

        // Ensure started

        int retries = 5;
        while (!mockServer.isRunning()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (--retries > 0) {
                Log.info("Waiting for server start, remaining retries = %s", retries);
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
     * Sets the version to return for the "/latest" request.
     *
     * @param latest The version.
     */
    public void latest(TestVersion latest) {
        if (this.latest != null && this.latest != latest) {
            throw new Error("not yet supported, need to deactivate/deregister previous expectation!");
        }
        this.latest = latest;
        mockServer.when(request().withMethod("GET")
                                 .withPath("/latest"))
                  .respond(response().withBody(latest.toString()));
    }

    /**
     * Stop the server.
     */
    public void stop() {
        mockServer.stop();
    }

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

}
