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
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.helidon.build.common.maven.url.MavenFileResolver.IS_WINDOWS;

/**
 * File system provider to support {@code mvn://} URIs.
 */
public final class MavenFileSystemProvider extends FileSystemProvider {

    private final Map<Path, FileSystem> filesystems = new HashMap<>();
    private final MavenFileResolver resolver;

    /**
     * Create a new provider instance.
     */
    public MavenFileSystemProvider() {
        try {
            this.resolver = new MavenFileResolver();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public String getScheme() {
        return "mvn";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        MavenURLParser parser = new MavenURLParser(uri.toString());
        String type = parser.type();
        if(!("jar".equals(type) || "zip".equals(type))) {
            throw new IllegalArgumentException("Unsupported artifact type: " + type);
        }
        Path path = resolver.resolveArtifact(uri);
        synchronized (filesystems) {
            ensureFile(path);
            Path realPath = path.toRealPath();
            if (filesystems.containsKey(realPath)) {
                throw new FileSystemAlreadyExistsException();
            }
            String uriPrefix = "jar:file:";
            if (IS_WINDOWS) {
                uriPrefix += "/";
            }
            URI realUri = URI.create(uriPrefix + realPath.toString().replace("\\", "/"));
            FileSystem fileSystem;
            try {
                fileSystem = FileSystems.newFileSystem(realUri, env);
            } catch (FileSystemAlreadyExistsException ex) {
                fileSystem = FileSystems.getFileSystem(realUri);
            }
            filesystems.put(realPath, fileSystem);
            return fileSystem;
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        synchronized (filesystems) {
            FileSystem fs = null;
            try {
                fs = filesystems.get(resolver.resolveArtifact(uri).toRealPath());
            } catch (IOException x) {
                // ignore
            }
            if (fs == null) {
                throw new FileSystemNotFoundException();
            }
            return fs;
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Path getPath(URI uri) {
        String spec = uri.getSchemeSpecificPart();
        int index = spec.indexOf("!/");
        if (index == -1) {
            throw new IllegalArgumentException("URI does not contain path info: " + uri);
        }
        return getFileSystem(uri).getPath(spec.substring(index + 1));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {

        return path.getFileSystem().provider().newByteChannel(path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
            throws IOException {
        return dir.getFileSystem().provider().newDirectoryStream(dir, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return path.getFileSystem().provider().isSameFile(path, path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return path.getFileSystem().provider().isHidden(path);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return path.getFileSystem().provider().getFileStore(path);
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        path.getFileSystem().provider().checkAccess(path, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return path.getFileSystem().provider().getFileAttributeView(path, type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {

        return path.getFileSystem().provider().readAttributes(path, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return path.getFileSystem().provider().readAttributes(path, attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    private void ensureFile(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        if (!attrs.isRegularFile()) {
            throw new UnsupportedOperationException();
        }
    }
}
