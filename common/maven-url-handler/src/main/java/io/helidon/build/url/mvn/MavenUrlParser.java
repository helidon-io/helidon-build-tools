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

    /**
     * Final artifact path separator.
     */
    private static final String FILE_SEPARATOR = "/";

    /**
     * Default artifact type.
     */
    private static final String DEFAULT_TYPE = "jar";

    /**
     * List of supported artifact types.
     */
    private static final List<String> SUPPORTED_TYPE = Arrays.asList(DEFAULT_TYPE, "zip");

    /**
     * Artifact group id.
     */
    private String groupId;

    /**
     * Artifact id.
     */
    private String artifactId;

    /**
     * Artifact version.
     */
    private String version = "LATEST";

    /**
     * Artifact classifier.
     */
    private String classifier = null;

    /**
     * Artifact type.
     */
    private String type = "jar";

    /**
     * Path from version directory to target file.
     */
    private String pathFromArchive;

    /**
     * Creates a new protocol parser.
     *
     * @param path                   the path part of the url (without starting mvn://)
     *
     * @throws MalformedURLException if provided path does not comply to expected syntax or an malformed repository URL
     */
    MavenUrlParser(String path) throws MalformedURLException {
        Objects.requireNonNull(path, "Maven url provided to Parser is null");
        parseArtifactPart(path);
    }

    /**
     * Parses the artifact part of the url.
     *
     * @param part                   url part without protocol and repository.
     *
     * @throws MalformedURLException if provided path does not comply to syntax.
     */
    private void parseArtifactPart(String part) throws MalformedURLException {
        String[] segments = part.split(":");

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
                throw new MalformedURLException("Invalid path. Syntax " + SYNTAX);
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

        segments = segments[4].split(FILE_SEPARATOR);

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

        segments = segments[3].split(FILE_SEPARATOR);

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
        segments = segments[2].split(FILE_SEPARATOR);

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

    private void checkString(String patient, String patientName) throws MalformedURLException {
        if (patient.trim().length() == 0) {
            throw new MalformedURLException(String.format("Invalid %s. Syntax  %s. ", patientName, SYNTAX));
        }
    }

    /**
     * Returns the group id of the artifact.
     *
     * @return group Id
     */
    String groupId() {
        return groupId;
    }

    /**
     * Returns the artifact id.
     *
     * @return artifact id
     */
    String artifactId() {
        return artifactId;
    }

    /**
     * Returns the artifact version.
     *
     * @return version
     */
    String version() {
        return version;
    }

    /**
     * Returns the artifact classifier.
     *
     * @return classifier
     */
    Optional<String> classifier() {
        return Optional.ofNullable(classifier);
    }

    /**
     * Returns the artifact type.
     *
     * @return type
     */
    String type() {
        return type;
    }

    /**
     * Return file path from version or classifier directory.
     *
     * @return file path
     */
    String pathFromArchive() {
        return pathFromArchive;
    }

    /**
     * Get full path from groupId to file.
     *
     * @return full path
     */
    String[] archivePath() {
        ArrayList<String> path = new ArrayList<>(Arrays.asList(groupId.split("\\.")));
        path.add(artifactId);
        path.add(version);
        if (classifier != null) {
            path.add(classifier);
        }
        String[] pathArray = new String[path.size()];
        return path.toArray(pathArray);
    }

}
