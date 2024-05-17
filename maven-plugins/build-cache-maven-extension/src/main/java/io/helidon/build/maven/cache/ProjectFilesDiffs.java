/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Project files diffs.
 */
final class ProjectFilesDiffs implements Iterable<ConfigDiff> {

    private final List<ConfigDiff> diffs;

    /**
     * Create a new project file diffs instance.
     *
     * @param orig   orig files
     * @param actual actual files
     */
    ProjectFilesDiffs(ProjectFiles orig, ProjectFiles actual) {
        diffs = new ArrayList<>();
        if (actual != null) {
            if (orig.filesCount() != actual.filesCount()) {
                diffs.add(new ConfigDiff.Update("count", orig.filesCount(), actual.filesCount()));
            }
            if (orig.lastModified() != actual.lastModified()) {
                diffs.add(new ConfigDiff.Update("last-modified", orig.lastModified(), actual.lastModified()));
            }
            if (!Objects.equals(orig.checksum(), actual.checksum())) {
                diffs.add(new ConfigDiff.Update("checksum", orig.checksum(), actual.checksum()));
            }
            Map<String, String> ocs = orig.allChecksums();
            Map<String, String> acs = actual.allChecksums();
            ocs.entrySet()
               .stream()
               .filter(e -> !e.getValue().equals(acs.get(e.getKey())))
               .forEach(e -> {
                   if (acs.containsKey(e.getKey())) {
                       diffs.add(new ConfigDiff.Update(e.getKey(), e.getValue(), acs.get(e.getKey())));
                   } else {
                       diffs.add(new ConfigDiff.Remove(e.getKey()));
                   }
               });
            acs.entrySet()
               .stream()
               .filter(e -> !ocs.containsKey(e.getKey()))
               .forEach(e -> diffs.add(new ConfigDiff.Add(e.getKey())));
        }
    }

    @Override
    public Iterator<ConfigDiff> iterator() {
        return diffs.iterator();
    }
}
