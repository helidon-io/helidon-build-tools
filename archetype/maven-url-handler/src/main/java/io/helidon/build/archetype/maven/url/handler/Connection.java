/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.maven.url.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * Connection.
 */
public class Connection extends URLConnection {

    private final MavenResolver resolver;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url       the specified URL.
     * @param resolver  resolver to use to resolve url.
     */
    protected Connection(URL url, MavenResolver resolver) {
        super(url);
        Objects.requireNonNull(url, "URL provided is null");
        Objects.requireNonNull(resolver, "Maven resolver provided is null");
        this.resolver = resolver;
    }

    @Override
    public void connect() throws IOException {
    }

    /**
     * Get the file input stream targeted by the url.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        File file = resolver.resolve(url.toExternalForm());
        return new FileInputStream(file);
    }
}
