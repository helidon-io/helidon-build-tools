/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.javadoc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.helidon.build.common.maven.MavenModel;

import org.apache.maven.artifact.Artifact;

import static io.helidon.build.common.SourcePath.wildcardMatch;

/**
 * Maven pattern with wildcard support.
 *
 * @param groupId    groupId
 * @param artifactId artifactId
 * @param classifier classifier
 * @param type       type
 */
record MavenPattern(String groupId, String artifactId, String classifier, String type) {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<groupId>[^:]+):(?<artifactId>[^:]+)(:(?<classifier>[^:]*))?(:(?<type>[^:]+))?");

    /**
     * Create a new pattern from a formatted string ({@code groupId:artifactId[:classifier[:type]}.
     *
     * @param filter filter
     * @return MavenPattern
     */
    static MavenPattern create(String filter) {
        Matcher m = PATTERN.matcher(filter);
        if (m.matches()) {
            String classifier = m.group("classifier");
            String type = m.group("type");
            return new MavenPattern(
                    m.group("groupId"),
                    m.group("artifactId"),
                    classifier != null ? classifier : "*",
                    type != null ? type : "*");
        }
        throw new IllegalArgumentException("Invalid filter: " + filter);
    }

    /**
     * Test if this pattern matches the given artifact.
     *
     * @param artifact artifact
     * @return {@code true} if the pattern matches
     */
    boolean matches(Artifact artifact) {
        return matches(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType());
    }

    /**
     * Test if this pattern matches the given pom.
     *
     * @param pom pom
     * @return {@code true} if the pattern matches
     */
    boolean matches(MavenModel pom) {
        return matches(pom.getGroupId(), pom.getArtifactId(), "", pom.getPackaging());
    }

    /**
     * Test if this pattern matches the given coordinates.
     *
     * @param groupId    groupId
     * @param artifactId artifactId
     * @param classifier classifier
     * @param type       type
     * @return {@code true} if the pattern matches
     */
    boolean matches(String groupId, String artifactId, String classifier, String type) {
        return wildcardMatch(groupId, this.groupId)
               && wildcardMatch(artifactId, this.artifactId)
               && wildcardMatch(classifier != null ? classifier : "", this.classifier)
               && wildcardMatch(type, this.type);
    }
}
