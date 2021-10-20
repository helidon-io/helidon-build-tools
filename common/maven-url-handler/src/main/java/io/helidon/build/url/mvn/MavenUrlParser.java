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

package io.helidon.build.url.mvn;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class MavenUrlParser {

    /**
     * Syntax for the url to be shown on exception messages.
     */
    private static final String SYNTAX =
            "mvn://groupId:artifactId:version:[classifier]:[type]/filePath";

    private static final String PROTOCOL = "mvn";

    private static final String DEFAULT_TYPE = "jar";

    private static final List<String> SUPPORTED_TYPE = List.of(DEFAULT_TYPE, "zip");

    private String groupId;

    private String artifactId;

    private String version = "LATEST";

    private String classifier = null;

    private String type = DEFAULT_TYPE;

    private String pathFromArchive;

    /**
     * Creates a new protocol parser.
     *
     * @param url url
     *
     * @throws MalformedURLException if provided path does not comply to expected syntax or an malformed repository URL
     */
    MavenUrlParser(String url) throws MalformedURLException {
        Objects.requireNonNull(url, "Maven url provided to Parser is null");
        parseUrl(url);
    }

    private void parseUrl(String url) throws MalformedURLException {
        if (!url.startsWith(PROTOCOL + ":")) {
            throw new IllegalArgumentException("URL should be a mvn based protocol");
        }
        url = url.substring((PROTOCOL + "://").length());

        String[] segments = url.split(":");

        if (segments.length > 2) {
            groupId = segments[0];
            checkString(groupId, "groupId");

            artifactId = segments[1];
            checkString(artifactId, "artifactId");
        } else {
            throw new MalformedURLException("Missing element in maven URL. Syntax " + SYNTAX);
        }

        switch (segments.length) {
            case 3:
                parseNoClassifierOrType(segments);
                break;
            case 4:
                parseWithClassifierOrType(segments);
                break;
            case 5:
                parseCompleteUrl(segments);
                break;
            default:
                throw new MalformedURLException("Invalid URL. Syntax " + SYNTAX);
        }
    }

    private void parseCompleteUrl(String[] segments) throws MalformedURLException {
        if (segments.length != 5) {
            throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
        }
        version = segments[2];
        checkString(version, "version");

        classifier = segments[3];
        checkString(classifier, "classifier");

        segments = segments[4].split("/");

        type = SUPPORTED_TYPE.contains(segments[0]) ? segments[0] : DEFAULT_TYPE;
        checkString(type, "type");

        buildPathFromArchive(segments);
    }

    private void parseWithClassifierOrType(String[] segments) throws MalformedURLException {
        if (segments.length != 4) {
            throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
        }

        version = segments[2];
        checkString(version, "version");

        segments = segments[3].split("/");

        if (SUPPORTED_TYPE.contains(segments[0])) {
            type = segments[0];
        } else {
            classifier = segments[0];
        }

        buildPathFromArchive(segments);
    }

    private void parseNoClassifierOrType(String[] segments) throws MalformedURLException {
        if (segments.length != 3) {
            throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
        }
        segments = segments[2].split("/");

        version = segments[0];
        checkString(version, "version");

        buildPathFromArchive(segments);
    }

    private void buildPathFromArchive(String[] segments) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < segments.length - 1; i++) {
            builder.append(segments[i]).append("/");
        }
        builder.append(segments[segments.length - 1]);
        pathFromArchive = builder.toString();
    }

    private void checkString(String arg, String name) throws MalformedURLException {
        if (arg.trim().length() == 0) {
            throw new MalformedURLException(String.format("Invalid %s. Syntax  %s. ", name, SYNTAX));
        }
    }

    String groupId() {
        return groupId;
    }

    String artifactId() {
        return artifactId;
    }

    String version() {
        return version;
    }

    Optional<String> classifier() {
        return Optional.ofNullable(classifier);
    }

    String type() {
        return type;
    }

    String pathFromArchive() {
        return pathFromArchive;
    }

    List<String> archivePath() {
        ArrayList<String> path = new ArrayList<>();
        path.addAll(Arrays.asList(groupId.split("\\.")));
        path.add(artifactId);
        path.add(version);
        if (classifier != null) {
            path.add(classifier);
        }
        return path;
    }

}
