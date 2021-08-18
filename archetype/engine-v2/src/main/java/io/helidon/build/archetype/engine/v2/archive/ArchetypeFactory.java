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

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory to create new instances of the classes that implements {@link Archetype}.
 */
public class ArchetypeFactory {

    private ArchetypeFactory() {
    }

    /**
     * Create new instance of the Archetype using path to the file or directory.
     *
     * @param pathname path to the file or directory.
     * @return Archetype
     */
    public static Archetype create(String pathname) {
        Objects.requireNonNull(pathname);
        File file = Path.of(pathname).toFile();
        return create(file);
    }

    /**
     * Create new instance of the Archetype using instance of the {@link File}.
     *
     * @param file File
     * @return Archetype
     */
    public static Archetype create(File file) {
        Objects.requireNonNull(file);
        if (!file.exists()) {
            throw new ArchetypeException(String.format("File %s does not exist", file.getPath()));
        }
        if (file.isDirectory()) {
            return new DirectoryArchetype(file);
        }
        return new ZipArchetype(file);
    }
}
