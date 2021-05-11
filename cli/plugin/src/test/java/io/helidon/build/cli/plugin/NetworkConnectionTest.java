/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link NetworkConnection}.
 */
class NetworkConnectionTest {

    private static final String NON_EXISTENT = "http://this.is.extremelybogus";

    private static class TestRetryDelay implements NetworkConnection.RetryDelay {
        int lastAttempt;
        int maxAttempts;

        @Override
        public void execute(int attempt, int maxAttempts) {
            lastAttempt = attempt;
            if (this.maxAttempts > 0) {
                assertThat(maxAttempts, is(this.maxAttempts));
            }
            this.maxAttempts = maxAttempts;
        }
    }

    private static class FailConnector implements NetworkConnection.Connector {
        IOException failure;

        FailConnector(IOException failure) {
            this.failure = failure;
        }

        @Override
        public URLConnection connect(URL url,
                                     Map<String, String> headers,
                                     int connectTimeout,
                                     int readTimeout) throws IOException {
            throw failure;
        }
    }

    @Test
    void testUnknownHost() {
        TestRetryDelay delay = new TestRetryDelay();
        IOException expected = new UnknownHostException("foo.bar");
        Throwable thrown = null;
        try {
            NetworkConnection.builder()
                             .connector(new FailConnector(expected))
                             .retryDelay(delay)
                             .url(NON_EXISTENT)
                             .open();
            fail("should have failed");
        } catch (IOException e) {
            thrown = e;
        }
        assertThat(thrown, is(not(nullValue())));
        assertThat(thrown, is(expected));
        assertThat(delay.maxAttempts, is(NetworkConnection.DEFAULT_MAXIMUM_ATTEMPTS));
        assertThat(delay.lastAttempt, is(NetworkConnection.DEFAULT_MAXIMUM_ATTEMPTS));
    }
}
