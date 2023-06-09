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
package io.helidon.build.cli.impl;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;

/**
 * Support Maven version ranges for archetypes.
 */
class HelidonVersion {

    private static final Pattern MAVEN_PATTERN =
            Pattern.compile("^(?<major>[0-9]+)(?<minor>\\.[0-9]+)?(?<micro>\\.[0-9]+(-[0-9a-z]+)*)?(?<latest>-LATEST)?$",
                    Pattern.CASE_INSENSITIVE);
    private static final VersionRange versionRange = VersionRange.createFromVersionSpec("[0,)");
    private final Integer major;
    private final Integer minor;
    private final String micro;
    private final boolean latest;

    static HelidonVersion INVALID_VERSION = new HelidonVersion(null, null, null, false);

    HelidonVersion(Integer major, Integer minor, String micro, boolean latest) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.latest = latest;
    }

    static HelidonVersion of(String input) {
        Matcher matcher = MAVEN_PATTERN.matcher(input);
        if (matcher.find()) {
            try {
                var version = matcher.group("major");
                Integer major = Integer.parseInt(version);
                version = matcher.group("minor");
                Integer minor = version != null ? Integer.parseInt(version.substring(1)) : null;
                version = matcher.group("micro");
                String micro = version != null ? version.substring(1).toLowerCase() : null;
                boolean latest = matcher.group("latest") != null;
                if (micro != null && micro.endsWith("-latest")) {
                    return INVALID_VERSION;
                }
                return new HelidonVersion(major, minor, micro, micro == null || latest);
            } catch (Exception e) {
                return INVALID_VERSION;
            }
        } else {
            return INVALID_VERSION;
        }
    }

    /**
     * Find the best match from the given list of versions with the version that current object represents and return it
     * as a {@link MavenVersion} object or {@code null} if no match was found.
     *
     * @param versions list of versions to match against.
     * @return the best match or null if no match.
     */
    MavenVersion mavenVersionFromList(List<String> versions) {
        if (major == null) {
            return null;
        }
        var pattern = pattern();
        List<MavenVersion> mavenVersions = versions.stream()
                .filter(version -> pattern.matcher(version).matches())
                .map(MavenVersion::toMavenVersion)
                .collect(Collectors.toList());
        return versionRange.matchVersion(mavenVersions);
    }

    private Pattern pattern() {
        String patternBuilder = major.toString() + "\\." +
                (minor != null ? minor.toString() : ".+\\.") +
                (micro != null ? "\\." + micro : ".+");
        return Pattern.compile(patternBuilder, Pattern.CASE_INSENSITIVE);
    }

    Integer major() {
        return major;
    }

    Integer minor() {
        return minor;
    }

    String micro() {
        return micro;
    }

    boolean latest() {
        return latest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HelidonVersion that = (HelidonVersion) o;
        return latest == that.latest
                && Objects.equals(major, that.major)
                && Objects.equals(minor, that.minor)
                && Objects.equals(micro, that.micro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, micro, latest);
    }
}
