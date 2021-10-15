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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;

/**
 * Directory.
 */
class DirectoryArchetype implements Archetype {

    private final File directory;

    /**
     * Create a new instance.
     *
     * @param directory File
     */
    DirectoryArchetype(File directory) {
        this.directory = directory;
    }


    @Override
    public InputStream getInputStream(String path) {
        File file = getFile(path);
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ArchetypeException(String.format("File %s does not exist", path));
        }
    }

    private File getFile(String path) {
        Objects.requireNonNull(path);
        path = path.startsWith(File.separator) ? path.substring(1) : path;
        File file = directory.toPath().resolve(Path.of(path)).toFile();
        if (file.exists()) {
            return file;
        }
        throw new ArchetypeException(String.format("File %s does not exist", path));
    }

    @Override
    public Path getPath(String path) {
        File file = getFile(path);
        return directory.toPath().relativize(file.toPath());
    }

    @Override
    public ArchetypeDescriptor getDescriptor(String path) {
        Objects.requireNonNull(path);
        Path descriptorPath = directory.toPath().resolve(getPath(path));
        try {
            try (InputStream inputStream = Files.newInputStream(descriptorPath)) {
                return ArchetypeDescriptor.read(inputStream);
            }
        } catch (IOException e) {
            throw new ArchetypeException("An I/O error occurs during opening the file " + path, e);
        }
    }

    @Override
    public List<String> getPaths() {
        try (Stream<Path> stream = Files.walk(directory.toPath())) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(file -> directory.toPath().relativize(file).toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ArchetypeException("An I/O error is thrown when accessing the directory " + directory.getPath(), e);
        }
    }
}
