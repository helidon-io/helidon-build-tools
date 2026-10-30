/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;

/**
 * Simple HTTP server.
 */
public class HttpServer implements AutoCloseable {

    private final Routing routing = new Routing();
    private final com.sun.net.httpserver.HttpServer server;
    private final String url;

    /**
     * Create a new instance.
     *
     * @param consumer routing consumer
     */
    public HttpServer(Consumer<Routing> consumer) {
        try {
            consumer.accept(routing);
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
            server = com.sun.net.httpserver.HttpServer.create(address, 0);
            server.createContext("/", this::handle);
            server.start();
            url = "http://127.0.0.1:" + server.getAddress().getPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get the server URL.
     *
     * @return URL
     */
    public String url() {
        return url;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) {
        Request request = new Request(exchange);
        Response response = new Response(exchange);
        FilterChain chain = new FilterChain(request, response, routing.filters.iterator(), this::handle);
        chain.proceed();
    }

    private void handle(Request request, Response response) {
        Handler handler = routing.routes.getOrDefault(request.method(), Map.of()).get(request.path());
        if (handler != null) {
            handler.handle(request, response);
        } else {
            response.status(404).send();
        }
    }

    /**
     * HTTP request.
     */
    public static final class Request {
        private final HttpExchange exchange;

        private Request(HttpExchange exchange) {
            this.exchange = exchange;
        }

        /**
         * Get the request path.
         *
         * @return path, {@code null} if undefined
         */
        public String path() {
            return exchange.getRequestURI().getPath();
        }

        /**
         * Get the request method.
         *
         * @return method, never {@code null}
         */
        public String method() {
            return exchange.getRequestMethod().toUpperCase();
        }

        /**
         * Get a header value.
         *
         * @param name header name
         * @return value, never {@code null}
         */
        public List<String> header(String name) {
            return exchange.getRequestHeaders().getOrDefault(name, List.of());
        }

        /**
         * Get the request headers.
         *
         * @return headers, never {@code null}
         */
        public Map<String, List<String>> headers() {
            return Map.copyOf(exchange.getRequestHeaders());
        }

        /**
         * Get the request body.
         *
         * @return InputStream
         */
        public InputStream body() {
            return exchange.getRequestBody();
        }
    }

    /**
     * HTTP response.
     */
    public static final class Response {
        private static final byte[] EMPTY_BYTES = new byte[0];
        private final HttpExchange exchange;
        private int status = 200;

        private Response(HttpExchange exchange) {
            this.exchange = exchange;
        }

        /**
         * Add a response header.
         *
         * @param name  header name
         * @param value header value
         * @return this instance
         */
        public Response header(String name, String value) {
            exchange.getResponseHeaders().add(name, value);
            return this;
        }

        /**
         * Set the response status.
         *
         * @param status status
         * @return this instance
         */
        public Response status(int status) {
            this.status = status;
            return this;
        }

        /**
         * Send the response.
         */
        public void send() {
            send(EMPTY_BYTES);
        }

        /**
         * Send the response.
         *
         * @param body body
         */
        public void send(String body) {
            send(body.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Send the response.
         *
         * @param supplier body supplier
         */
        public void send(Supplier<byte[]> supplier) {
            send(supplier.get());
        }

        /**
         * Send the response.
         *
         * @param body body
         */
        public void send(byte[] body) {
            try {
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
    }

    /**
     * Routing.
     */
    public static final class Routing {

        private final List<Filter> filters = new ArrayList<>();
        private final Map<String, Map<String, Handler>> routes = new HashMap<>();

        /**
         * Register a handler for a GET request.
         *
         * @param path    request path
         * @param handler handler
         * @return this instance
         */
        public Routing get(String path, Handler handler) {
            return route("GET", path, handler);
        }

        /**
         * Register a handler for a request.
         *
         * @param method  request method
         * @param path    request path
         * @param handler handler
         * @return this instance
         */
        public Routing route(String method, String path, Handler handler) {
            routes.computeIfAbsent(method.toUpperCase(), k -> new HashMap<>()).put(path, handler);
            return this;
        }

        /**
         * Register a filter.
         *
         * @param filter filter
         * @return this instance
         */
        public Routing filter(Filter filter) {
            filters.add(filter);
            return this;
        }
    }

    /**
     * Filter chain.
     */
    public static final class FilterChain {
        private final Request request;
        private final Response response;
        private final Iterator<Filter> filters;
        private final Handler handler;

        FilterChain(Request request, Response response, Iterator<Filter> filters, Handler handler) {
            this.request = request;
            this.response = response;
            this.filters = filters;
            this.handler = handler;
        }

        /**
         * Proceed with the next filter or route.
         */
        public void proceed() {
            if (filters.hasNext()) {
                Filter filter = filters.next();
                filter.filter(this, request, response);
            } else {
                handler.handle(request, response);
            }
        }
    }

    /**
     * Filter.
     */
    public interface Filter {

        /**
         * Filter a method.
         *
         * @param chain    filter chain
         * @param request  request
         * @param response response
         */
        void filter(FilterChain chain, Request request, Response response);
    }

    /**
     * Handler.
     */
    public interface Handler {
        /**
         * Handle a request.
         *
         * @param request  request
         * @param response response
         */
        void handle(Request request, Response response);
    }
}
