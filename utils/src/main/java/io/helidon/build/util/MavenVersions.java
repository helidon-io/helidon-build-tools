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

package io.helidon.build.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Parses maven metadata to find version numbers for maven artifact builds or releases.
 */
public class MavenVersions {
    private static final String VERSION_START_TAG = "<version>";
    private static final String VERSION_END_TAG = "</version>";
    private static final char TAG_BEGIN = '<';

    private final String source;
    private final List<MavenVersion> versions;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private MavenVersions(Builder builder) {
        this.source = builder.source;
        this.versions = Collections.unmodifiableList(builder.versions);
    }

    /**
     * Returns the source of the versions.
     *
     * @return The source.
     */
    public String source() {
        return source;
    }

    /**
     * Returns the latest version.
     *
     * @return The version.
     */
    public MavenVersion latest() {
        return versions.get(0);
    }

    /**
     * Returns all versions.
     *
     * @return The versions, latest first.
     */
    public List<MavenVersion> versions() {
        return versions;
    }

    @Override
    public String toString() {
        return "MavenVersions{"
               + "source='" + source + '\''
               + ", versions=" + versions + '}';
    }

    /**
     * {@link MavenVersions} builder.
     */
    public static class Builder {

        /**
         * The default repository URI.
         */
        public static final URI DEFAULT_REPOSITORY_URI = toUri("https://repo.maven.apache.org/maven2/");

        /**
         * The default metadata file name.
         */
        public static final String DEFAULT_METADATA_FILE = "maven-metadata.xml";

        /**
         * The default connect timeout, in milliseconds.
         */
        public static final int DEFAULT_CONNECT_TIMEOUT = 500;

        /**
         * The default read timeout, in milliseconds.
         */
        public static final int DEFAULT_READ_TIMEOUT = 500;

        private URI repositoryBaseUri;
        private String artifactGroupId;
        private String artifactId;
        private String source;
        private List<MavenVersion> versions;
        private List<String> fallbackVersions;
        private Predicate<MavenVersion> filter;
        private String metaDataFileName;
        private int connectTimeout;
        private int readTimeout;

        private Builder() {
            this.repositoryBaseUri = DEFAULT_REPOSITORY_URI;
            this.metaDataFileName = DEFAULT_METADATA_FILE;
            this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            this.readTimeout = DEFAULT_READ_TIMEOUT;
            this.filter = v -> true;
        }

        /**
         * Sets the repository base URI.
         *
         * @param repositoryUri The base URI, e.g. {@link #DEFAULT_REPOSITORY_URI}.
         * @return This instance.
         */
        public Builder repository(URI repositoryUri) {
            this.repositoryBaseUri = requireNonNull(repositoryUri);
            return this;
        }

        /**
         * Sets the artifact group id.
         *
         * @param artifactGroupId The id.
         * @return This instance.
         */
        public Builder artifactGroupId(String artifactGroupId) {
            this.artifactGroupId = requireNonNull(artifactGroupId);
            return this;
        }

        /**
         * Sets the artifact id.
         *
         * @param artifactId The id.
         * @return This instance.
         */
        public Builder artifactId(String artifactId) {
            this.artifactId = requireNonNull(artifactId);
            return this;
        }

        /**
         * Sets the metadata file name.
         *
         * @param metaDataFileName The id.
         * @return This instance.
         */
        public Builder metaDataFileName(String metaDataFileName) {
            this.metaDataFileName = requireNonNull(metaDataFileName);
            return this;
        }

        /**
         * Sets the connect timeout.
         *
         * @param connectTimeout The timeout, in milliseconds..
         * @return This instance.
         */
        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = assertValidTimeout(connectTimeout);
            return this;
        }

        /**
         * Sets the connect timeout.
         *
         * @param readTimeout The timeout, in milliseconds..
         * @return This instance.
         */
        public Builder readTimeout(int readTimeout) {
            this.readTimeout = assertValidTimeout(readTimeout);
            return this;
        }

        /**
         * Sets a filter for the versions to include.
         *
         * @param filter The filter.
         * @return This instance.
         */
        public Builder filter(Predicate<MavenVersion> filter) {
            this.filter = requireNonNull(filter);
            return this;
        }

        /**
         * Sets versions to use as a fallback if the metadata file cannot be accessed.
         *
         * @param fallbackVersions The versions.
         * @return This instance.
         */
        public Builder fallbackVersions(List<String> fallbackVersions) {
            if (requireNonNull(fallbackVersions).isEmpty()) {
                throw new IllegalArgumentException("fallback versions empty");
            }
            this.fallbackVersions = fallbackVersions;
            return this;
        }

        /**
         * Builds the {@link MavenVersions} instance.
         *
         * @return The instance.
         * @throws IllegalStateException If there are no versions available.
         */
        public MavenVersions build() {
            requireNonNull(artifactGroupId, "artifactGroupId is required");
            requireNonNull(artifactId, "artifactId is required");
            try {
                final String relativePath = toPath(artifactGroupId) + "/" + toPath(artifactId) + "/" + metaDataFileName;
                final URL url = repositoryBaseUri.resolve(relativePath).toURL();
                source = url.toString();
                versions = convertAndSort(parse(url));
                if (versions.isEmpty()) {
                    useFallbackVersions("No versions found");
                }
            } catch (Exception e) {
                useFallbackVersions(e.toString());
            }
            return new MavenVersions(this);
        }

        private void useFallbackVersions(String reason) {
            if (fallbackVersions == null) {
                throw new IllegalStateException(reason);
            }
            versions = convertAndSort(fallbackVersions);
            if (versions.isEmpty()) {
                throw new IllegalStateException("no fallback versions matching the filter");
            } else {
                source = "fallback";
            }
        }

        private List<MavenVersion> convertAndSort(List<String> versions) {
            return versions.stream()
                           .map(MavenVersion::toMavenVersion)
                           .filter(filter)
                           .sorted(Collections.reverseOrder())
                           .collect(Collectors.toList());
        }

        private List<String> parse(URL url) throws IOException {
            try (InputStream stream = open(url)) {
                return StreamUtils.toLines(stream)
                                  .stream()
                                  .map(String::trim)
                                  .filter(line -> line.startsWith(VERSION_START_TAG) && line.endsWith(VERSION_END_TAG))
                                  .map(line -> {
                                      final int versionStart = VERSION_START_TAG.length();
                                      final int versionEnd = line.indexOf(TAG_BEGIN, VERSION_START_TAG.length());
                                      return line.substring(versionStart, versionEnd);
                                  })
                                  .collect(Collectors.toList());
            }
        }

        private InputStream open(URL url) throws IOException {
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
            }
            return connection.getInputStream();
        }

        private static String toPath(String id) {
            return id.replace('.', '/');
        }

        private static int assertValidTimeout(int timeout) {
             if (timeout < 0) {
                 throw new IllegalArgumentException("negative timeout");
             }
             return timeout;
        }

        private static URI toUri(String uri) {
            try {
                return new URI(uri);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
