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
         * The Maven central repository URI; used as the default if not specified.
         */
        public static final URI MAVEN_CENTRAL_URI = toUri("https://repo.maven.apache.org/maven2/");

        /**
         * The metadata file name for remote repositories; used as the default if not specified.
         */
        public static final String REMOTE_METADATA_FILE = "maven-metadata.xml";

        private URI repositoryBaseUri;
        private String artifactGroupId;
        private String artifactId;
        private String source;
        private List<MavenVersion> versions;
        private List<String> fallbackVersions;
        private Predicate<MavenVersion> filter;
        private String metaDataFileName;

        private Builder() {
            this.repositoryBaseUri = MAVEN_CENTRAL_URI;
            this.filter = v -> true;
            this.metaDataFileName = REMOTE_METADATA_FILE;
        }

        /**
         * Sets the repository base URI.
         *
         * @param repositoryUri The base URI, e.g. {@link #MAVEN_CENTRAL_URI}.
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
         * Sets the metadata file name. Defaults to
         *
         * @param metaDataFileName The id.
         * @return This instance.
         */
        public Builder metaDataFileName(String metaDataFileName) {
            this.metaDataFileName = requireNonNull(metaDataFileName);
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

        private static List<String> parse(URL url) throws IOException {
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

        private static InputStream open(URL url) throws IOException {
            final boolean followingRedirects = HttpURLConnection.getFollowRedirects();
            try {
                if (!followingRedirects) {
                    HttpURLConnection.setFollowRedirects(true);
                }
                return url.openStream();
            } finally {
                if (!followingRedirects) {
                    HttpURLConnection.setFollowRedirects(false);
                }
            }
        }

        private static String toPath(String id) {
            return id.replace('.', '/');
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
