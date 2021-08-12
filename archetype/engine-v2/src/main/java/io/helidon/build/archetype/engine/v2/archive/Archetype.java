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

package io.helidon.build.archetype.engine.v2.archive;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;

/**
 * Facade over descriptors, scripts and other files.
 */
public interface Archetype extends Closeable {
    /**
     * Get {@link Path} to the file.
     *
     * @param path path
     * @return Path
     */
    Path getFile(String path);

    /**
     * Get ArchetypeDescriptor.
     *
     * @param path path to the ArchetypeDescriptor.
     * @return ArchetypeDescriptor.
     */
    ArchetypeDescriptor getDescriptor(String path);

    /**
     * Get all the paths in the Archetype.
     *
     * @return List of paths.
     */
    List<String> getPaths();

    @Override
    default void close() throws IOException {
    }
}
