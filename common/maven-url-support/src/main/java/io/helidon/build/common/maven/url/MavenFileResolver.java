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

package io.helidon.build.common.maven.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static java.nio.file.FileSystems.newFileSystem;

/**
 * Maven file resolver.
 */
final class MavenFileResolver {

    /**
     * System property to control the maven local repository location.
     */
    static final String LOCAL_REPO_PROPERTY = "io.helidon.mvn.local.repository";

    private static final Map<String, String> FS_ENV = Map.of("create", "true");
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "unknown")
                                                    .toLowerCase(Locale.ENGLISH)
                                                    .contains("win");

    private final Path localRepo;

    MavenFileResolver() throws IOException {
        String repoPath = System.getProperty(LOCAL_REPO_PROPERTY);
        if (repoPath == null) {
            throw new IOException(String.format("System property %s is not set.", LOCAL_REPO_PROPERTY));
        }
        localRepo = Path.of(repoPath);
        if (!(Files.exists(localRepo) && Files.isDirectory(localRepo))) {
            throw new IOException(String.format("Directory %s does not exist.", localRepo));
        }
    }

    /**
     * Get or create the file system for the given archive.
     *
     * @param artifact archive
     * @return file system
     * @throws IOException if an IO error occurs
     */
    FileSystem fileSystem(Path artifact) throws IOException {
        String uriPrefix = "jar:file:";
        if (IS_WINDOWS) {
            uriPrefix += "/";
        }
        URI uri = URI.create(uriPrefix + artifact.toRealPath().toString().replace("\\", "/"));
        try {
            return newFileSystem(uri, FS_ENV, this.getClass().getClassLoader());
        } catch (FileSystemAlreadyExistsException ex) {
            return FileSystems.getFileSystem(uri);
        }
    }

    /**
     * Get the input stream of the given file inside the artifact archive.
     *
     * @param artifact archive
     * @param path     path inside the archive
     * @return input stream
     * @throws IOException if an IO error occurs
     */
    InputStream inputStream(Path artifact, String path) throws IOException {
        FileSystem fileSystem = fileSystem(artifact);
        return Files.newInputStream(fileSystem.getPath(path));
    }

    /**
     * Resolve the artifact in the local repository.
     *
     * @param uri uri
     * @return artifact
     * @throws IOException if an IO error occurs
     */
    Path resolveArtifact(URI uri) throws IOException {
        return resolveArtifact(new MavenURLParser(uri.toString()));
    }

    /**
     * Resolve the artifact in the local repository.
     *
     * @param parser parser
     * @return artifact
     * @throws IOException if an IO error occurs
     */
    Path resolveArtifact(MavenURLParser parser) throws IOException {
        Path artifact = parser.resolveArtifact(localRepo);
        if (!Files.exists(artifact)) {
            throw new IOException(String.format("File %s does not exist.", artifact));
        }
        return artifact;
    }
}
