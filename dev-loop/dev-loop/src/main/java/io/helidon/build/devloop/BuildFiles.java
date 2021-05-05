/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.devloop;

import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import static io.helidon.build.devloop.FileChangeAware.changedTimeOf;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A collection of {@link BuildFile}s that can be polled for changes.
 */
public class BuildFiles implements FileChangeAware {
    private final List<BuildFile> buildFiles;

    /**
     * Constructor.
     *
     * @param buildFiles The build files.
     */
    public BuildFiles(List<BuildFile> buildFiles) {
        if (requireNonNull(buildFiles).isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        this.buildFiles = unmodifiableList(buildFiles);
    }

    @Override
    public Optional<FileTime> changedTime() {
        return changedTimeOf(buildFiles);
    }

    /**
     * Returns the build files.
     *
     * @return The files.
     */
    public List<BuildFile> list() {
        return buildFiles;
    }
}
