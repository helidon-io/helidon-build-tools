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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static io.helidon.build.cli.plugin.UpdateMetadata.escapedHeaders;
import static java.util.Objects.requireNonNull;


/**
 * A builder for accessing a network stream. Supports retries.
 */
public class NetworkConnection {

    /**
     * Connects to a URL.
     */
    public interface Connector {
        /**
         * Returns the connection after connecting to the given url.
         *
         * @param url The url.
         * @param headers The headers.
         * @param connectTimeout The connect timeout, in milliseconds.
         * @param readTimeout The read timeout, in milliseconds.
         * @return The connection.
         * @throws IOException If an error occurs.
         */
        URLConnection connect(URL url, Map<String, String> headers, int connectTimeout, int readTimeout) throws IOException;
    }

    /**
     * Performs retry delays.
     */
    public interface RetryDelay {
        /**
         * Performs a delay.
         *
         * @param attempt The attempt number. Always > 0.
         * @param maxAttempts The maximum number of attempts.
         */
        void execute(int attempt, int maxAttempts);
    }

    /**
     * The default connector.
     */
    public static final Connector DEFAULT_CONNECTOR = (url, headers, connectTimeout, readTimeout) -> {
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        headers.forEach(connection::addRequestProperty);
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
        }
        return connection;
    };

    /**
     * A {@link RetryDelay} that supports a linearly increasing delay.
     */
    public static final class LinearRetryDelay implements RetryDelay {
        private final long initialDelay;
        private final long increment;

        /**
         * Constructor.
         *
         * @param initialDelay The initial delay, in milliseconds.
         * @param increment The number of milliseconds to add to the initial delay for each retry.
         */
        public LinearRetryDelay(long initialDelay, long increment) {
            this.initialDelay = initialDelay;
            this.increment = increment;
        }

        @Override
        public void execute(int attempt, int maxAttempts) {
            try {
                final long delay = initialDelay + (attempt * increment);
                if (Log.isVerbose()) {
                    final float seconds = delay / 1000F;
                    Log.info("  $(italic retry %d of %d, sleeping for %.1f seconds)", attempt, maxAttempts, seconds);
                } else {
                    Log.info("  $(italic retry %d of %d)", attempt, maxAttempts);
                }
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The default maximum number of retries.
     */
    public static final int DEFAULT_MAXIMUM_RETRIES = 5;

    /**
     * The default connect timeout, in milliseconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 500;

    /**
     * The default read timeout, in milliseconds.
     */
    public static final int DEFAULT_READ_TIMEOUT = 500;

    /**
     * The default retry delay. Linear, with an initial delay of 500 milliseconds, incrementing by 500 on each retry.
     */
    public static final RetryDelay DEFAULT_RETRY_DELAY = new LinearRetryDelay(500, 500);

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     */
    public static class Builder {
        private URL url;
        private int maxRetries;
        private int connectTimeout;
        private int readTimeout;
        private Connector connector;
        private RetryDelay delay;
        private Map<String, String> headers;

        private Builder() {
            this.maxRetries = DEFAULT_MAXIMUM_RETRIES;
            this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            this.readTimeout = DEFAULT_READ_TIMEOUT;
            this.connector = DEFAULT_CONNECTOR;
            this.delay = DEFAULT_RETRY_DELAY;
            this.headers = new HashMap<>();
        }

        /**
         * Sets the url to open.
         *
         * @param url The url to open.
         * @return This instance, for chaining.
         */
        public Builder url(String url) {
            try {
                return url(new URL(requireNonNull(url)));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Sets the url to open.
         *
         * @param url The url to open.
         * @return This instance, for chaining.
         */
        public Builder url(URL url) {
            this.url = requireNonNull(url);
            return this;
        }

        /**
         * Add a header.
         *
         * @param name The header name.
         * @param value The header value.
         * @return This instance, for chaining.
         */
        public Builder header(String name, String value) {
            headers.put(requireNonNull(name), requireNonNull(value));
            return this;
        }

        /**
         * Add headers.
         *
         * @param headers The headers.
         * @return This instance, for chaining.
         */
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(requireNonNull(headers));
            return this;
        }

        /**
         * Sets the number of retries.
         *
         * @param maxRetries The maximum number of retries.
         * @return This instance, for chaining.
         */
        public Builder maxRetries(int maxRetries) {
            if (maxRetries <= 0) {
                throw new IllegalArgumentException("maxRetries must be > 0");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the connect timeout.
         *
         * @param connectTimeout The timeout.
         * @return This instance, for chaining.
         */
        public Builder connectTimeout(int connectTimeout) {
            if (connectTimeout <= 0) {
                throw new IllegalArgumentException("connect timeout must be > 0");
            }
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets the read timeout.
         *
         * @param readTimeout The timeout.
         * @return This instance, for chaining.
         */
        public Builder readTimeout(int readTimeout) {
            if (readTimeout <= 0) {
                throw new IllegalArgumentException("read timeout must be > 0");
            }
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * Sets the connector .
         *
         * @param connector The connector.
         * @return This instance, for chaining.
         */
        public Builder connector(Connector connector) {
            this.connector = requireNonNull(connector);
            return this;
        }

        /**
         * Sets the retry delay.
         *
         * @param retryDelay The delay.
         * @return This instance, for chaining.
         */
        public Builder retryDelay(RetryDelay retryDelay) {
            this.delay = requireNonNull(retryDelay);
            return this;
        }

        /**
         * Connect, retrying if needed.
         *
         * @return The connection.
         */
        public URLConnection connect() throws IOException {
            if (url == null) {
                throw new IllegalStateException("url is required");
            }
            Log.debug("connecting to %s, headers=%s", url, escapedHeaders(headers));
            IOException lastCaught = null;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    URLConnection result = connector.connect(url, headers, connectTimeout, readTimeout);
                    Log.debug("connected to %s, headers=%s", url, escapedHeaders(result.getHeaderFields()));
                    return result;
                } catch (UnknownHostException | SocketException | SocketTimeoutException e) {
                    lastCaught = e;
                    delay.execute(attempt, maxRetries);
                }
            }
            throw requireNonNull(lastCaught);
        }

        /**
         * Open the stream, retrying if needed.
         *
         * @return The stream.
         */
        public InputStream open() throws IOException {
            return connect().getInputStream();
        }
    }

    private NetworkConnection() {
    }
}
