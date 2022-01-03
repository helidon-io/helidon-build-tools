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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Utility to parse {@code mvn://} urls.
 */
final class MavenURLParser {

    /**
     * Syntax for the url to be shown on exception messages.
     */
    private static final String SYNTAX = "mvn://groupId:artifactId:version:[classifier]:[type]!/filePath";

    private static final String PROTOCOL = "mvn";
    private static final String DEFAULT_TYPE = "jar";
    private static final List<String> SUPPORTED_TYPE = List.of(DEFAULT_TYPE, "zip");

    private String groupId;
    private String artifactId;
    private String version = "LATEST";
    private String classifier = null;
    private String type = DEFAULT_TYPE;
    private String path;

    /**
     * Creates a new protocol parser.
     *
     * @param url url
     * @throws MalformedURLException if provided path does not comply to expected syntax or an malformed repository URL
     */
    MavenURLParser(String url) throws MalformedURLException {
        Objects.requireNonNull(url, "Maven url provided to Parser is null");
        parseUrl(url);
    }

    /**
     * Get the groupId.
     *
     * @return groupId
     */
    String groupId() {
        return groupId;
    }

    /**
     * Get the artifactId.
     *
     * @return artifactId
     */
    String artifactId() {
        return artifactId;
    }

    /**
     * Get the version.
     *
     * @return version
     */
    String version() {
        return version;
    }

    /**
     * Get the classifier.
     *
     * @return classifier, may be {@code null}
     */
    String classifier() {
        return classifier;
    }

    /**
     * Get the type.
     *
     * @return type
     */
    String type() {
        return type;
    }

    /**
     * Get the path.
     *
     * @return path, may be {@code null}
     */
    String path() {
        return path;
    }

    /**
     * Resolve the local repo path of the parsed coordinate.
     *
     * @param localRepo local repository
     * @return artifact file
     */
    Path resolveArtifact(Path localRepo) {
        StringBuilder sb = new StringBuilder(artifactId).append("-").append(version);
        if (classifier != null) {
            sb.append("-").append(classifier);
        }
        sb.append(".").append(type);
        return localRepo.resolve(groupId.replaceAll("\\.", "/"))
                        .resolve(artifactId)
                        .resolve(version)
                        .resolve(sb.toString());
    }

    private void parseUrl(String url) throws MalformedURLException {
        if (!url.startsWith(PROTOCOL + ":")) {
            throw new IllegalArgumentException("URL should be a mvn based protocol");
        }
        int pathIndex = url.indexOf("!/");
        String[] coords = url.substring(6, pathIndex == -1 ? url.length() : pathIndex).split(":");
        if (coords.length > 2) {
            groupId = coords[0];
            checkString(groupId, "groupId");
            artifactId = coords[1];
            checkString(artifactId, "artifactId");
        } else {
            throw new MalformedURLException("Missing element in maven URL. Syntax " + SYNTAX);
        }
        switch (coords.length) {
            case 3:
                parseNoClassifierOrType(coords);
                break;
            case 4:
                parseWithClassifierOrType(coords);
                break;
            case 5:
                parseCompleteUrl(coords);
                break;
            default:
                throw new MalformedURLException("Invalid URL. Syntax " + SYNTAX);
        }
        path = pathIndex != -1 ? url.substring(pathIndex + 1) : null;
    }

    private void parseCompleteUrl(String[] coords) throws MalformedURLException {
        if (coords.length != 5) {
            throw new MalformedURLException("Invalid url, expected format is " + SYNTAX);
        }
        version = coords[2];
        checkString(version, "version");
        classifier = coords[3];
        checkString(classifier, "classifier");
        coords = coords[4].split("/");
        type = SUPPORTED_TYPE.contains(coords[0]) ? coords[0] : DEFAULT_TYPE;
        checkString(type, "type");
    }

    private void parseWithClassifierOrType(String[] coords) throws MalformedURLException {
        if (coords.length != 4) {
            throw new MalformedURLException("Invalid url, expected format is " + SYNTAX);
        }
        version = coords[2];
        checkString(version, "version");
        coords = coords[3].split("/");
        if (SUPPORTED_TYPE.contains(coords[0])) {
            type = coords[0];
        } else {
            classifier = coords[0];
        }
    }

    private void parseNoClassifierOrType(String[] coords) throws MalformedURLException {
        if (coords.length != 3) {
            throw new MalformedURLException("Invalid syntax " + SYNTAX);
        }
        coords = coords[2].split("/");
        version = coords[0];
        checkString(version, "version");
    }

    private void checkString(String arg, String name) throws MalformedURLException {
        if (arg.trim().length() == 0) {
            throw new MalformedURLException(String.format("Invalid %s, expected format is %s", name, SYNTAX));
        }
    }
}
