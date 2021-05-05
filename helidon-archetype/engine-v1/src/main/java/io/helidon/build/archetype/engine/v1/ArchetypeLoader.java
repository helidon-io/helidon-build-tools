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
package io.helidon.build.archetype.engine.v1;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Class ArchetypeLoader. Alternative to using {@link java.net.URLClassLoader} that works
 * on Graal native.
 */
public class ArchetypeLoader implements Closeable {

    private final JarFile jarFile;
    private final File directory;

    /**
     * Create an archetype loader from a file that can be either a directory
     * or a JAR file.
     *
     * @param file The file.
     * @throws IOException If an IO error occurs.
     */
    public ArchetypeLoader(File file) throws IOException {
        if (file.isDirectory()) {
            directory = file;
            jarFile = null;
        } else {
            jarFile = new JarFile(file);
            directory = null;
        }
    }

    /**
     * Test if loading from a directory.
     *
     * @return Outcome of test.
     */
    public boolean isDirectory() {
        return directory != null;
    }

    /**
     * Test if loading from a JAR file.
     *
     * @return Outcome of test.
     */
    public boolean isJarFile() {
        return jarFile != null;
    }

    /**
     * Loads a resource as a stream. It is the caller's responsibility to close
     * the stream at the end.
     *
     * @param resource The resource.
     * @return The input stream.
     * @throws IOException If an IO error occurs.
     */
    public InputStream loadResourceAsStream(String resource) throws IOException {
        if (isDirectory()) {
            File file = directory.toPath().resolve(Path.of(resource)).toFile();
            return file.exists() ? new FileInputStream(file) : null;
        } else {
            ZipEntry entry = jarFile.getEntry(resource);
            return entry == null ? null : jarFile.getInputStream(entry);
        }
    }

    /**
     * Close underlying resources.
     *
     * @throws IOException If an IO error occurs.
     */
    @Override
    public void close() throws IOException {
        if (isJarFile()) {
            jarFile.close();
        }
    }
}
