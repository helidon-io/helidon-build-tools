/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.dev;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A collection of {@link BuildFile}s that can be polled for changes.
 */
public class BuildFiles {
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

    /**
     * Returns whether or not any build file has changed.
     *
     * @return {@code true} if any build file has changed.
     */
    public boolean haveChanged() {
        for (final BuildFile file : buildFiles) {
            if (file.hasChanged()) {
                return true;
            }
        }
        return false;
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
