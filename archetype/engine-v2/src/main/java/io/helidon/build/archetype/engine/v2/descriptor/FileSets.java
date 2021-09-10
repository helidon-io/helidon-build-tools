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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * Archetype files in {@link Output} archetype.
 */
public class FileSets extends Conditional {

    private final LinkedList<String> transformations;
    private final LinkedList<String> includes;
    private final LinkedList<String> excludes;
    private String directory;

    FileSets(String transformations, String ifProperties) {
        super(ifProperties);
        this.transformations = parseTransformation(transformations);
        this.includes = new LinkedList<>();
        this.excludes = new LinkedList<>();
    }

    private LinkedList<String> parseTransformation(String transformations) {
        if (transformations == null) {
            return new LinkedList<String>();
        }
        return new LinkedList<String>(Arrays.asList(transformations.split(",")));
    }

    /**
     * Get the directory of this file set.
     *
     * @return directory optional, never {@code null}
     */
    public Optional<String> directory() {
        return Optional.ofNullable(directory);
    }

    /**
     * Set the directory.
     * @param directory directory
     */
    void directory(String directory) {
        this.directory = Objects.requireNonNull(directory, "directory is null");
    }

    /**
     * Get the exclude filters.
     *
     * @return list of exclude filter, never {@code null}
     */
    public LinkedList<String> excludes() {
        return excludes;
    }

    /**
     * Get the include filters.
     *
     * @return list of include filter, never {@code null}
     */
    public LinkedList<String> includes() {
        return includes;
    }

    /**
     * Get the applied transformations.
     *
     * @return list of transformation, never {@code null}
     */
    public LinkedList<String> transformations() {
        return transformations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FileSets fileSet = (FileSets) o;
        return transformations.equals(fileSet.transformations)
                && includes.equals(fileSet.includes)
                && excludes.equals(fileSet.excludes)
                && directory.equals(fileSet.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), transformations, includes, excludes, directory);
    }

    @Override
    public String toString() {
        return "FileSet{"
                + ", transformations=" + transformations
                + ", includes=" + includes
                + ", excludes=" + excludes
                + ", directory='" + directory + '\''
                + ", if=" + ifProperties()
                + '}';
    }
}
