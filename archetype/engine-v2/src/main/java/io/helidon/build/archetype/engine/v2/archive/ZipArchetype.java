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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;

/**
 * Zip file.
 */
public class ZipArchetype implements Archetype, Closeable {

    private final Path zipPath;
    private final FileSystem fileSystem;

    /**
     * Create a new instance.
     *
     * @param file File
     */
    public ZipArchetype(File file) {
        try {
            //check if the file has a ZIP format
            new ZipFile(file);
            zipPath = file.toPath();
            fileSystem = FileSystems.newFileSystem(zipPath, null);
        } catch (IOException e) {
            throw new ArchetypeException("Cannot create new ZipArchetype instance with file " + file.getPath(), e);
        }
    }

    @Override
    public Path getFile(String path) {
        Objects.requireNonNull(path);
        Path filePath = fileSystem.getPath(path);
        if (Files.isRegularFile(filePath)) {
            return filePath;
        }
        throw new ArchetypeException(String.format("File %s does not exist in the archive %s", path, zipPath));
    }

    @Override
    public ArchetypeDescriptor getDescriptor(String path) {
        Objects.requireNonNull(path);
        Path descriptorPath = getFile(path);
        try {
            InputStream inputStream = Files.newInputStream(descriptorPath);
            return ArchetypeDescriptor.read(inputStream);
        } catch (IOException e) {
            throw new ArchetypeException("An I/O error occurs during opening the file " + path, e);
        }
    }

    @Override
    public List<String> getPaths() {
        List<String> result = new ArrayList<>();
        try {
            for (Path rootDirectory : fileSystem.getRootDirectories()) {
                Files.walk(rootDirectory).forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        result.add(path.toString());
                    }
                });
            }
        } catch (IOException e) {
            throw new ArchetypeException("An I/O error is thrown when accessing the " + zipPath, e);
        }

        return result;
    }

    /**
     * Close underlying {@code FileSystem}.
     *
     * @throws IOException If an error occurs.
     */
    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
