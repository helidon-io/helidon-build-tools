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

package io.helidon.build.maven.component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.common.SourcePath;

/**
 * A configuration filtering component using excludes and includes list.
 */
public class FilteringComponent {

    private List<String> excludes;
    private List<String> includes;

    private void excludes(List<String> excludes) {
        this.excludes = excludes;
    }

    private void includes(List<String> includes) {
        this.includes = includes;
    }

    private List<String> excludes() {
        return this.excludes;
    }

    private List<String> includes() {
        return this.includes;
    }

    /**
     * Merge includes and excludes list.
     *
     * @return the merged list
     */
    public List<String> filter() {
        Objects.requireNonNull(includes);
        List<SourcePath> paths = includes.stream()
                .map(SourcePath::new)
                .collect(Collectors.toList());
        return SourcePath.filter(paths, includes, excludes)
                .stream()
                .map(p -> p.asString(false))
                .collect(Collectors.toList());
    }
}
