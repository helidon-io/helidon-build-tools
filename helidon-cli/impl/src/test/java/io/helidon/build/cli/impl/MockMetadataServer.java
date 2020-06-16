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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import io.helidon.build.util.Log;

import org.junit.jupiter.api.Assumptions;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

import static io.helidon.build.cli.impl.MetadataTest.INITIAL_ETAG;
import static io.helidon.build.cli.impl.MetadataTest.RC1;
import static io.helidon.build.cli.impl.MetadataTest.RC1_CLI_DATA_ZIP;
import static io.helidon.build.cli.impl.MetadataTest.RC2_CLI_DATA_ZIP;
import static io.helidon.build.cli.impl.MetadataTest.TEST_CLI_DATA_PATH;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Mock metadata server.
 */
public class MockMetadataServer {
    private static final int DEFAULT_PORT = 8087;
    private static final String VERBOSE_LEVEL = "INFO";
    private static final String NORMAL_LEVEL = "WARN";
    private static final String BASE_URL_PREFIX = "http://localhost:";
    private static final String ETAG_HEADER = "Etag";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String NO_FILE_ETAG = "<no-file>";
    private static final byte[] RC1_ZIP = readCliDataFile(RC1_CLI_DATA_ZIP);
    private static final byte[] RC2_ZIP = readCliDataFile(RC2_CLI_DATA_ZIP);

    /**
     * The default url.
     */
    public static final String DEFAULT_URL = BASE_URL_PREFIX + DEFAULT_PORT;

    private static byte[] readCliDataFile(String file) {
        try {
            return Files.readAllBytes(TEST_CLI_DATA_PATH.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final int port;
    private final String baseUrl;
    private ClientAndServer mockServer;

    public static void main(String[] args) {
        new MockMetadataServer(8080, true).start(RC1);
    }

    public MockMetadataServer(boolean verbose) {
        this(DEFAULT_PORT, verbose);
    }

    @SuppressWarnings("ConstantConditions")
    public MockMetadataServer(int port, boolean verbose) {
        if (Thread.currentThread().getContextClassLoader() != ClassLoader.getSystemClassLoader()) {
            final String reason = "don't yet know how to run MockServer when not in system class loader";
            Log.info("$(italic,yellow Skipping: %s)", reason);
            Assumptions.assumeTrue(false, reason);
        }
        this.port = port;
        this.baseUrl = BASE_URL_PREFIX + port;
        ConfigurationProperties.logLevel(verbose ? VERBOSE_LEVEL : NORMAL_LEVEL);
    }

    public MockMetadataServer start(String latestVersion) {
        Log.info("Starting mock metadata server with latest=%s at %s", latestVersion, baseUrl);
        mockServer = ClientAndServer.startClientAndServer(port);

        // Always return latest version

        mockServer.when(request().withMethod("GET")
                                 .withPath("/latest"))
                  .respond(response().withBody(latestVersion));

        // Always return zip data when If-None-Match header is "<no-file>" (plugin defines this)

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/" + RC1_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(RC1_ZIP));

        mockServer.when(request().withMethod("GET")
                                 .withHeader(IF_NONE_MATCH_HEADER, NO_FILE_ETAG)
                                 .withPath("/" + RC2_CLI_DATA_ZIP))
                  .respond(response().withHeader(ETAG_HEADER, INITIAL_ETAG)
                                     .withBody(RC2_ZIP));

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

        return this;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public void stop() {
        mockServer.stop();
    }
}
