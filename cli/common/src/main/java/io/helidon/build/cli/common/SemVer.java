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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static List<String> latestMajorVersions(List<String> versions) {
        // versions grouped by major digit
        Map<String, List<String>> groups = Lists.mappedBy(versions, SemVer::majorDigit);

        // Maven versions grouped by version range
        Map<VersionRange, List<MavenVersion>> ranges =
                Maps.mapEntry(groups, Strings::isValid, VersionRange::higherOrEqual, MavenVersion::toMavenVersion);

        // the latest of each group
        Collection<MavenVersion> latest = Maps.mapEntryValue(ranges, entry->entry.getKey().resolveLatest(entry.getValue()))
                                              .values();

        // return String based versions
        return Lists.map(latest, MavenVersion::toString);
    }

    private static String majorDigit(String version) {
        Matcher matcher = MAJOR_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group("major");
        }
        return "";
    }
}
