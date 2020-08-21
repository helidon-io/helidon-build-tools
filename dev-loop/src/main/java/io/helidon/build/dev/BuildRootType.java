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

import java.nio.file.Path;
import java.util.function.BiPredicate;

import io.helidon.build.util.PathPredicates;

import static io.helidon.build.util.PathPredicates.matchesResource;

/**
 * A build root type.
 */
public class BuildRootType {
    /**
     * Java sources.
     */
    public static final BuildRootType JAVA_SOURCES = create(DirectoryType.JavaSources, PathPredicates.matchesJavaSource());

    /**
     * Classes.
     */
    public static final BuildRootType JAVA_CLASSES = create(DirectoryType.JavaClasses, PathPredicates.matchesJavaClass());

    /**
     * Resources.
     */
    public static final BuildRootType RESOURCES = BuildRootType.create(DirectoryType.Resources, matchesResource());

    private final DirectoryType directoryType;
    private final BiPredicate<Path, Path> fileType;

    /**
     * Creates a new type.
     *
     * @param directoryType The directory type.
     * @param fileType The file type predicate.
     * @return The type.
     */
    public static BuildRootType create(DirectoryType directoryType, BiPredicate<Path, Path> fileType) {
        return new BuildRootType(directoryType, fileType);
    }

    private BuildRootType(DirectoryType directoryType, BiPredicate<Path, Path> fileType) {
        this.directoryType = directoryType;
        this.fileType = fileType;
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
     * Returns the associated file type predicate.
     *
     * @return The file type.
     */
    public BiPredicate<Path, Path> fileType() {
        return fileType;
    }
}
