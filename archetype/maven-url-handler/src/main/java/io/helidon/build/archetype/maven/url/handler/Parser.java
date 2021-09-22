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

import java.net.MalformedURLException;

/**
 * Parse the maven url.
 */
public class Parser {

    /**
     * Default version if none present in the url.
     */
    private static final String VERSION_LATEST = "LATEST";

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "mvn://groupId:artifactId:[version]/.../[file]";

    /**
     * Final artifact path separator.
     */
    private static final String FILE_SEPARATOR = "/";

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
    private String version;

    /**
     * Path from version directory to target file.
     */
    private String[] filePath;

    /**
     * Creates a new protocol parser.
     *
     * @param path                   the path part of the url (without starting mvn://)
     *
     * @throws MalformedURLException if provided path does not comply to expected syntax or an malformed repository URL
     */
    public Parser(String path) throws MalformedURLException {
        if (path == null) {
            throw new MalformedURLException("Path cannot be null. Syntax " + SYNTAX);
        }
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
        if (segments.length < 3) {
            throw new MalformedURLException("Invalid path. Syntax " + SYNTAX);
        }

        groupId = segments[0];
        if (groupId.trim().length() == 0) {
            throw new MalformedURLException("Invalid groupId. Syntax " + SYNTAX);
        }

        artifactId = segments[1];
        if (artifactId.trim().length() == 0) {
            throw new MalformedURLException("Invalid artifactId. Syntax " + SYNTAX);
        }

        segments = segments[2].split(FILE_SEPARATOR);

        if (segments.length < 2) {
            throw new MalformedURLException("Invalid version. Syntax " + SYNTAX);
        }

        version = VERSION_LATEST;
        if (segments[0].trim().length() > 0) {
            version = segments[0];
        }

        filePath = new String[segments.length - 1];
        System.arraycopy(segments, 1, filePath, 0, segments.length - 1);
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
     * Return file path from version directory.
     *
     * @return file path
     */
    public String[] filePath() {
        return filePath;
    }

    /**
     * Get full path from groupId to file.
     *
     * @return full path
     */
    public String[] fullPath() {
        String[] groupIdPath = groupId.split("\\.");
        String[] completePath = new String[2 + groupIdPath.length + filePath.length];

        System.arraycopy(groupIdPath, 0, completePath, 0, groupIdPath.length);

        completePath[groupIdPath.length] = artifactId;
        completePath[groupIdPath.length + 1] = version;

        System.arraycopy(filePath, 0, completePath, groupIdPath.length + 2, filePath.length);

        return completePath;
    }

}
