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

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiPredicate;

import io.helidon.build.common.PathFilters;

import static io.helidon.build.common.PathFilters.matchesFileNameSuffix;

/**
 * A build root type.
 */
public class BuildRootType {

    private static final BiPredicate<Path, Path> JAVA_SOURCE = matchesFileNameSuffix(".java");
    private static final BiPredicate<Path, Path> JAVA_CLASS = matchesFileNameSuffix(".class");
    private static final BiPredicate<Path, Path> RESOURCE_FILE = (path, root) -> {
        final String fileName = path.getFileName().toString();
        return !fileName.startsWith(".") && !fileName.endsWith(".class") && !fileName.endsWith(".swp") && !fileName.endsWith("~");
    };
    private static final BuildRootType JAVA_SOURCES = create(DirectoryType.JavaSources, matchesJavaSource());
    private static final BuildRootType JAVA_CLASSES = create(DirectoryType.JavaClasses, matchesJavaClass());
    private static final BuildRootType RESOURCES = BuildRootType.create(DirectoryType.Resources, matchesResource());

    /**
     * Returns the Java sources instance.
     *
     * @return The instance.
     */
    public static BuildRootType javaSources() {
        return JAVA_SOURCES;
    }

    /**
     * Returns the Java classes instance.
     *
     * @return The instance.
     */
    public static BuildRootType javaClasses() {
        return JAVA_CLASSES;
    }

    /**
     * Returns the resources instance.
     *
     * @return The instance.
     */
    public static BuildRootType resources() {
        return RESOURCES;
    }

    /**
     * Returns a filter that returns {@code true} for any filename ending with {@code ".java"}.
     *
     * @return The filter. The second path parameter is always ignored; a {@code BiPredicate<Path,Path>} is used for symmetry
     * with other uses of {@link PathFilters}.
     */
    public static BiPredicate<Path, Path> matchesJavaSource() {
        return JAVA_SOURCE;
    }

    /**
     * Returns a filter that returns {@code true} for any filename ending with {@code ".class"}.
     *
     * @return The filter. The second path parameter is always ignored; a {@code BiPredicate<Path,Path>} is used for symmetry
     * with other uses of {@link PathFilters}.
     */
    public static BiPredicate<Path, Path> matchesJavaClass() {
        return JAVA_CLASS;
    }

    /**
     * Returns a filter that returns {@code true} for any filename that does not start with {@code "."} and does not end with
     * {@code ".class"}, {@code ".swp"} or {@code "~"}.
     *
     * @return The filter. The second path parameter is always ignored; a {@code BiPredicate<Path,Path>} is used for symmetry
     * with other uses of {@link PathFilters}.
     */
    public static BiPredicate<Path, Path> matchesResource() {
        return RESOURCE_FILE;
    }

    private final DirectoryType directoryType;
    private final BiPredicate<Path, Path> filter;

    /**
     * Creates a new type.
     *
     * @param directoryType The directory type.
     * @param filter The file filter.
     * @return The type.
     */
    public static BuildRootType create(DirectoryType directoryType, BiPredicate<Path, Path> filter) {
        return new BuildRootType(directoryType, filter);
    }

    private BuildRootType(DirectoryType directoryType, BiPredicate<Path, Path> filter) {
        this.directoryType = directoryType;
        this.filter = filter;
    }

    /**
     * Returns the associated directory type.
     *
     * @return The directory type.
     */
    public DirectoryType directoryType() {
        return directoryType;
    }

    /**
     * Returns the associated file filter.
     *
     * @return The filter.
     */
    public BiPredicate<Path, Path> filter() {
        return filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BuildRootType that = (BuildRootType) o;
        return directoryType == that.directoryType
               && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directoryType, filter);
    }

    @Override
    public String toString() {
        return "BuildRootType{"
               + "directoryType=" + directoryType
               + '}';
    }
}
