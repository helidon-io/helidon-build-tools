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

package io.helidon.build.cli.common;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.Strings;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;

/**
 * Semantic versioning utility.
 */
final class SemVer {

    private static final Pattern MAJOR_PATTERN = Pattern.compile("^(?<major>[0-9]+).+$");

    private SemVer() {
    }

    /**
     * Get the latest major versions of the given versions.
     *
     * @param versions versions
     * @return latest major versions
     */
    static List<String> latestMajorVersions(List<ArchetypesData.Version> versions) {
        List<String> ids = versions.stream().map(ArchetypesData.Version::id).collect(Collectors.toList());
        // versions grouped by major digit
        Map<String, List<String>> groups = Lists.mappedBy(ids, SemVer::lowestVersionFromMajorDigit);

        // Maven versions grouped by version range
        Map<VersionRange, List<MavenVersion>> ranges =
                Maps.mapEntry(groups, Strings::isValid, VersionRange::higherOrEqual, MavenVersion::toMavenVersion);

        // the latest of each group
        Collection<MavenVersion> latest = Maps.mapEntryValue(ranges, entry -> entry.getKey().resolveLatest(entry.getValue()))
                                              .values();
        List<ArchetypesData.Version> latestVersions = versions.stream()
                .filter(version -> latest.contains(version.toMavenVersion()))
                .collect(Collectors.toList());

        // return String based versions
        return Lists.map(sortVersions(latestVersions), ArchetypesData.Version::id);
    }

    static List<ArchetypesData.Version> sortVersions(List<ArchetypesData.Version> versions) {
        versions.sort(Comparator.comparingInt(ArchetypesData.Version::order)
                .thenComparing((v1, v2) -> v2.id().compareTo(v1.id())));
        return versions;
    }

    private static String lowestVersionFromMajorDigit(String version) {
        Matcher matcher = MAJOR_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group("major") + "-SNAPSHOT";
        }
        return "";
    }
}
