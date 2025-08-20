/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import io.helidon.build.common.maven.MavenModel;

import static io.helidon.build.common.SourcePath.wildcardMatch;

/**
 * Maven pattern with wildcard support.
 *
 * @param groupId    groupId
 * @param artifactId artifactId
 * @param classifier classifier
 * @param type       type
 */
public record MavenPattern(String groupId, String artifactId, String classifier, String type) {

    /**
     * Create a new pattern from a formatted string ({@code groupId:artifactId[:classifier[:type]]}.
     *
     * @param filter filter
     * @return MavenPattern
     */
    public static MavenPattern create(String filter) {
        String[] args = parse(filter, 4, "*");
        return new MavenPattern(args[0], args[1], args[2], args[3]);
    }

    /**
     * Test if this pattern matches the given artifact.
     *
     * @param artifact artifact
     * @return {@code true} if the pattern matches
     */
    public boolean matches(MavenArtifact artifact) {
        return matches(artifact.groupId(), artifact.artifactId(), artifact.classifier(), artifact.type());
    }

    /**
     * Test if this pattern matches the given pom.
     *
     * @param pom pom
     * @return {@code true} if the pattern matches
     */
    public boolean matches(MavenModel pom) {
        return matches(pom.groupId(), pom.artifactId(), "", pom.packaging());
    }

    /**
     * Test if this pattern matches the given coordinates.
     *
     * @param groupId    groupId
     * @param artifactId artifactId
     * @param classifier classifier, may be {@code null}
     * @param type       type
     * @return {@code true} if the pattern matches
     */
    public boolean matches(String groupId, String artifactId, String classifier, String type) {
        return wildcardMatch(groupId, this.groupId)
               && wildcardMatch(artifactId, this.artifactId)
               && wildcardMatch(classifier != null ? classifier : "", this.classifier)
               && wildcardMatch(type, this.type);
    }

    static String[] parse(String pattern, int size, String defaultValue) {
        String[] args = new String[size];
        int i = 0;
        int index = 0;
        for (; i < size; i++) {
            if (index >= 0) {
                int endIndex = pattern.indexOf(':', index);
                if (index == endIndex + 1 || index == endIndex) {
                    break;
                }
                if (endIndex > 0) {
                    args[i] = pattern.substring(index, endIndex);
                    int nextIndex = endIndex + 1;
                    if (nextIndex == pattern.length()) {
                        break;
                    }
                    index = nextIndex;
                } else {
                    args[i] = pattern.substring(index);
                    index = -1;
                }
            } else {
                args[i] = defaultValue;
            }
        }
        if (i > 0 && index == -1) {
            return args;
        }
        throw new IllegalArgumentException("Invalid pattern at index %d: %s".formatted(i > 0 ? index : 0, pattern));
    }
}
