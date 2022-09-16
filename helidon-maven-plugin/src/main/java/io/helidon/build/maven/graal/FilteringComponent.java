/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.graal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.util.SourcePath;

/**
 * A configuration filtering component using excludes and includes list.
 */
public class FilteringComponent {

    private List<String> excludes;
    private List<String> includes;
    private List<String> additionalEntries;

    private void excludes(List<String> excludes) {
        this.excludes = excludes;
    }

    private void includes(List<String> includes) {
        this.includes = includes;
    }

    private void additionalEntries(List<String> includes) {
        this.additionalEntries = includes;
    }

    private List<String> excludes() {
        return this.excludes;
    }

    private List<String> includes() {
        return this.includes;
    }

    private List<String> additionalEntries() {
        return this.additionalEntries;
    }

    /**
     * Filter the provided list with includes and excludes and add additional entries if exists.
     *
     * @param list to be filtered
     * @return the filtered list
     */
    public List<String> filter(List<String> list) {
        Objects.requireNonNull(list);
        List<SourcePath> paths = list.stream()
                .map(SourcePath::new)
                .collect(Collectors.toList());
        List<String> result = SourcePath.filter(paths, includes, excludes)
                .stream()
                .map(p -> p.asString(false))
                .collect(Collectors.toList());
        if (Objects.nonNull(additionalEntries)) {
            result.addAll(additionalEntries);
        }
        return result;
    }
}
