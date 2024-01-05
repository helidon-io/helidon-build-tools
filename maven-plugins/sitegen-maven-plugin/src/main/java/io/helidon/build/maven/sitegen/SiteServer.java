/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.MediaTypes;
import io.helidon.build.common.Strings;
import io.helidon.build.common.logging.Log;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Site server.
 */
public final class SiteServer {

    private final int port;
    private final HttpServer server;
    private volatile Thread blockerThread;

    /**
     * Create a new server instance.
     *
     * @param port port
     * @param dir  directory
     */
    public SiteServer(int port, Path dir) {
        this.port = port;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new Handler(dir));
            server.setExecutor(Executors.newWorkStealingPool());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Get the port.
     *
     * @return port
     */
    @SuppressWarnings("unused")
    public int port() {
        return port;
    }

    /**
     * Start the server.
     */
    public void start() {
        server.start();
        Log.info("Site server is up! $(blue http://localhost:%d)", port);
        blockerThread = Thread.currentThread();
        LockSupport.park();
    }

    /**
     * Stop the server.
     */
    @SuppressWarnings("unused")
    public void stop() {
        LockSupport.unpark(blockerThread);
        server.stop(0);
    }

    /**
     * Main entry-point.
     *
     * @param args args
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            Path dir = Path.of(args[0]);
            int port = 8080;
            if (args.length > 1) {
                port = Integer.parseInt(args[1]);
            }
            new SiteServer(port, dir).start();
        }
        throw new IllegalArgumentException("usage: dir [port]");
    }

    private static final class Handler implements HttpHandler {

        private final Path dir;

        Handler(Path dir) {
            this.dir = dir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String path = exchange.getRequestURI().getPath();
                if (path.matches("^/.*\\.\\w{2,}$")) {
                    serveFile(exchange, path);
                } else {
                    serveFile(exchange, "/index.html");
                }
            } else {
                // bad request
                exchange.sendResponseHeaders(400, 0);
            }
            exchange.close();
        }

        private void serveFile(HttpExchange exchange, String path) throws IOException {
            Path file = dir.resolve(Strings.stripLeading(path, '/'));
            if (Files.exists(file)) {
                String mediaType = MediaTypes.of(FileUtils.fileExt(file)).orElse("application/octet-stream");
                exchange.getResponseHeaders().add("Content-Type", mediaType);
                exchange.sendResponseHeaders(200, Files.size(file));
                try (OutputStream os = exchange.getResponseBody(); InputStream is = Files.newInputStream(file)) {
                    is.transferTo(os);
                }
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
        }
    }
}
