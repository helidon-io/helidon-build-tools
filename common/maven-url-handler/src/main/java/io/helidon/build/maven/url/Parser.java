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

package io.helidon.build.maven.url;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Parse the maven url.
 */
public class Parser {

    /**
     * Syntax for the url to be shown on exception messages.
     */
    private static final String SYNTAX =
            "mvn://groupId:artifactId:[version]:[classifier (optional)]:[type (optional)]/.../[file]";

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
    public Parser(String path) throws MalformedURLException {
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
            checkStringHealth(groupId, "groupId");

            artifactId = segments[1];
            checkStringHealth(artifactId, "artifactId");
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
        checkStringHealth(version, "version");

        classifier = segments[3];
        checkStringHealth(classifier, "classifier");

        segments = segments[4].split(FILE_SEPARATOR);

        type = checkTypeHealth(segments[0]) ? segments[0] : DEFAULT_TYPE;
        checkStringHealth(type, "type");

        buildPathFromArchive(segments);
    }

    private void parseWithClassifierOrType(String[] segments) throws MalformedURLException {
        if (segments.length != 4) {
            throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
        }

        version = segments[2];
        checkStringHealth(version, "version");

        segments = segments[3].split(FILE_SEPARATOR);

        if (checkTypeHealth(segments[0])) {
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
        checkStringHealth(version, "version");

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

    private void checkStringHealth(String patient, String patientName) throws MalformedURLException {
        if (patient.trim().length() == 0) {
            throw new MalformedURLException(String.format("Invalid %s. Syntax  %s. ", patientName, SYNTAX));
        }
    }

    private boolean checkTypeHealth(String type) {
        return SUPPORTED_TYPE.contains(type);
    }

    /**
     * Returns the group id of the artifact.
     *
     * @return group Id
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns the artifact id.
     *
     * @return artifact id
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Returns the artifact version.
     *
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the artifact classifier.
     *
     * @return classifier
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Returns the artifact type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Return file path from version directory.
     *
     * @return file path
     */
    public String getPathFromArchive() {
        return pathFromArchive;
    }

    /**
     * Get full path from groupId to file.
     *
     * @return full path
     */
    public String[] getArchivePath() {
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
