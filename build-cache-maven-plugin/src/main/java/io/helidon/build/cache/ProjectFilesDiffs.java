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
package io.helidon.build.cache;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

/**
 * Project files diffs.
 */
final class ProjectFilesDiffs implements Iterator<Diff> {

    private final Iterator<Diff> iterator;

    /**
     * Create a new project file diffs instance.
     *
     * @param orig   orig files
     * @param actual actual files
     */
    ProjectFilesDiffs(ProjectFiles orig, ProjectFiles actual) {
        LinkedList<Diff> diffs = new LinkedList<>();
        if (actual != null) {
            if (orig.filesCount() != actual.filesCount()) {
                diffs.add(new Diff(orig.filesCount(), actual.filesCount(), "count"));
            }
            if (orig.lastModified() != actual.lastModified()) {
                diffs.add(new Diff(orig.lastModified(), actual.lastModified(), "last-modified"));
            }
            if (!Objects.equals(orig.checksum(), actual.checksum())) {
                diffs.add(new Diff(orig.checksum(), actual.checksum(), "checksum"));
            }
            Map<String, String> ocs = orig.allChecksums();
            Map<String, String> acs = actual.allChecksums();
            ocs.entrySet()
               .stream()
               .filter(e -> !e.getValue().equals(acs.get(e.getKey())))
               .forEach(e -> diffs.add(new Diff(e.getValue(), acs.get(e.getKey()), e.getKey())));
            acs.entrySet()
               .stream()
               .filter(e -> !ocs.containsKey(e.getKey()))
               .forEach(e -> diffs.add(new Diff(ocs.get(e.getKey()), e.getValue(), e.getKey())));
        }
        iterator = diffs.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Diff next() {
        return iterator.next();
    }
}
