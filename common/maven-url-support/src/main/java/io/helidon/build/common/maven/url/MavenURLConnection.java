/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Maven URL Connection.
 */
public final class MavenURLConnection extends URLConnection {

    private final MavenFileResolver resolver;
    private final MavenURLParser parser;
    private final Path artifact;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    public MavenURLConnection(URL url) throws IOException {
        super(url);
        Objects.requireNonNull(url, "URL provided is null");
        this.parser = new MavenURLParser(url.toExternalForm());
        this.resolver = new MavenFileResolver();
        this.artifact = resolver.resolveArtifact(parser);
    }

    @Override
    public void connect() {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return resolver.inputStream(artifact, parser.path());
    }

    /**
     * Get the absolute Path of the file targeted by url.
     *
     * @return Path
     */
    public Path artifactFile() {
        return artifact;
    }

    /**
     * Get the path of the file targeted by url.
     *
     * @return Path
     */
    public String pathFromArchive() {
        return parser.path();
    }

    /**
     * Get groupId.
     *
     * @return groupId
     */
    public String groupId() {
        return parser.groupId();
    }

    /**
     * Get artifactId.
     *
     * @return artifactId
     */
    public String artifactId() {
        return parser.artifactId();
    }

    /**
     * Get classifier.
     *
     * @return classifier
     */
    public String classifier() {
        return parser.classifier();
    }

    /**
     * Get version.
     *
     * @return version
     */
    public String version() {
        return parser.version();
    }

    /**
     * Get type.
     *
     * @return type
     */
    public String type() {
        return parser.type();
    }

}
