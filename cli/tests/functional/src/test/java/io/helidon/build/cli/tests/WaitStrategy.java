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
package io.helidon.build.cli.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.logging.Log;

abstract class WaitStrategy {

    static WaitStrategy waitForPort(int port) {
        return new PortWaitStrategy(port);
    }

    static WaitStrategy waitForUrl(String url, Predicate<String> predicate) {
        return new UrlWaitStrategy(url) {
            @Override
            boolean test() {
                String body = get();
                return body != null && predicate.test(body);
            }
        };
    }

    void await(ProcessMonitor monitor) {
        long timeout = TimeUnit.MINUTES.toMillis(5);
        long startTime = System.currentTimeMillis();
        while (monitor.isAlive() && (System.currentTimeMillis() - startTime <= timeout)) {
            sleep();
            if (test()) {
                return;
            }
        }
        throw fail();
    }

    boolean test() {
        return true;
    }

    void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    abstract AwaitException fail();

    static class PortWaitStrategy extends WaitStrategy {

        private final int port;
        private IOException error;

        private PortWaitStrategy(int port) {
            this.port = port;
        }

        @Override
        boolean test() {
            SocketAddress sa = new InetSocketAddress("localhost", port);
            try (Socket socket = new Socket()) {
                socket.connect(sa, 1000);
                Log.debug("Socket localhost:%d is ready", port);
                return true;
            } catch (IOException ex) {
                error = ex;
                Log.debug("Socket localhost:%d exception: %s", port, ex.getMessage());
                return false;
            }
        }

        @Override
        AwaitException fail() {
            String msg = String.format("Fail to wait for port: %d", port);
            return new AwaitException(msg, error);
        }
    }

    static class UrlWaitStrategy extends WaitStrategy {

        private final HttpClient client;
        private int status = -1;
        private IOException error;

        private UrlWaitStrategy(String url) {
            this.client = new HttpClient(url);
        }

        @Override
        boolean test() {
            return get() != null;
        }

        String get() {
            try (HttpClient.Response response = client.request()) {
                status = response.status();
                if (status == 200) {
                    return response.body();
                }
            } catch (IOException ex) {
                error = ex;
            }
            return null;
        }

        @Override
        AwaitException fail() {
            String msg = String.format("Fail to wait for URL: %s, status: %d", client.url(), status);
            return new AwaitException(msg, error);
        }
    }

    static class AwaitException extends IllegalStateException {
        private AwaitException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
