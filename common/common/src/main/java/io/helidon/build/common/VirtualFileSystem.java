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

package io.helidon.build.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Virtual file system that provides a pseudo chroot.
 */
@SuppressWarnings("NullableProblems")
public class VirtualFileSystem extends FileSystem {

    private static final ProviderImpl PROVIDER = new ProviderImpl();

    private final Path internal;
    private final VPath root;
    private volatile boolean isOpen;

    /**
     * Create a new virtual filesystem using the given path as the root.
     *
     * @param path path
     * @return file system
     */
    public static FileSystem create(Path path) {
        return new VirtualFileSystem(path);
    }

    private VirtualFileSystem(Path internal) {
        this.internal = internal.normalize().toAbsolutePath();
        this.root = new VPath(this, "/");
        this.isOpen = true;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() {
        cleanup();
    }

    @Override
    @SuppressWarnings({"deprecation", "checkstyle:NoFinalizer"})
    protected void finalize() {
        cleanup();
    }

    @Override
    public FileSystemProvider provider() {
        return PROVIDER;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(root);
    }

    @Override
    public Path getPath(String first, String... more) {
        if (more.length == 0) {
            return new VPath(this, first);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        for (String s : more) {
            if (!s.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(s);
            }
        }
        return new VPath(this, sb.toString());
    }

    @Override
    public final boolean isReadOnly() {
        return true;
    }

    @Override
    public final UserPrincipalLookupService getUserPrincipalLookupService() {
        return internal.getFileSystem().getUserPrincipalLookupService();
    }

    @Override
    public final WatchService newWatchService() throws IOException {
        return internal.getFileSystem().newWatchService();
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndInput) {
        return internal.getFileSystem().getPathMatcher(syntaxAndInput);
    }

    @Override
    public final Iterable<FileStore> getFileStores() {
        return internal.getFileSystem().getFileStores();
    }

    @Override
    public final Set<String> supportedFileAttributeViews() {
        return internal.getFileSystem().supportedFileAttributeViews();
    }

    @Override
    public final String toString() {
        return "virtual:" + internal.toUri();
    }

    @Override
    public final String getSeparator() {
        return "/";
    }

    private boolean isNotWithinBounds(Path path) {
        return !path.normalize().toAbsolutePath().startsWith(internal);
    }

    private synchronized void cleanup() {
        if (isOpen) {
            isOpen = false;
        }
    }

    private static final class InvalidVirtualPathException extends InvalidPathException {

        InvalidVirtualPathException(VirtualFileSystem fs, String input) {
            super(input, "Not within virtual root: " + fs.internal);
        }
    }

    private static final class VPath implements Path {

        private final VirtualFileSystem fs;
        private final Path internal;

        VPath(VirtualFileSystem fs, Path path) {
            if (path.isAbsolute() && fs.isNotWithinBounds(path)) {
                throw new InvalidVirtualPathException(fs, path.toString());
            }
            this.internal = path;
            this.fs = fs;
        }

        VPath(VirtualFileSystem fs, String path) {
            if ("/".equals(path)) {
                this.internal = fs.internal;
            } else {
                Path internal = fs.internal.getFileSystem().getPath(path);
                if (internal.isAbsolute() && fs.isNotWithinBounds(internal)) {
                    throw new InvalidVirtualPathException(fs, path);
                }
                this.internal = internal;
            }
            this.fs = fs;
        }

        @Override
        public VPath getRoot() {
            return internal.isAbsolute() ? fs.root : null;
        }

        Path internalAbsolute() {
            Path path = internal.isAbsolute() ? internal : fs.internal.resolve(internal).toAbsolutePath();
            if (fs.isNotWithinBounds(path)) {
                throw new InvalidVirtualPathException(fs, path.toString());
            }
            return path;
        }

        FileSystemProvider internalProvider() {
            return internal.getFileSystem().provider();
        }


        @Override
        public VPath getFileName() {
            return new VPath(fs, internal.getFileName());
        }

        @Override
        public VPath getParent() {
            Path parent = internal.getParent();
            return parent != null ? new VPath(fs, parent) : null;
        }

        @Override
        public int getNameCount() {
            int count = internal.getNameCount();
            return internal.isAbsolute() ? count - fs.internal.getNameCount() : count;
        }

        @Override
        public VPath getName(int index) {
            Path name;
            if (internal.startsWith(fs.internal)) {
                name = internal.getName(fs.internal.getNameCount() + index);
            } else {
                name = internal.getName(index);
            }
            return new VPath(fs, name);
        }

        @Override
        public VPath subpath(int beginIndex, int endIndex) {
            if (internal.startsWith(fs.internal)) {
                int count = fs.internal.getNameCount();
                beginIndex += count;
                endIndex += count;
            }
            return new VPath(fs, internal.subpath(beginIndex, endIndex));
        }

        @Override
        public VPath toRealPath(LinkOption... options) throws IOException {
            return new VPath(fs, internalAbsolute().toRealPath(options));
        }

        @Override
        public VPath toAbsolutePath() {
            if (internal.isAbsolute()) {
                return this;
            }
            return new VPath(fs, internalAbsolute());
        }

        @Override
        public URI toUri() {
            try {
                return new URI("virtual",
                        String.format("%s!%s",
                                fs.internal.toUri(),
                                fs.internal.relativize(internalAbsolute().normalize())), null);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        public VPath relativize(Path other) {
            final VPath o = unwrap(other);
            if (fs != o.fs || isAbsolute() != o.isAbsolute()) {
                throw new IllegalArgumentException("Incorrect filesystem or path: " + other);
            }
            return new VPath(fs, internal.relativize(o.internal));
        }

        @Override
        public VirtualFileSystem getFileSystem() {
            return fs;
        }

        @Override
        public boolean isAbsolute() {
            return internal.isAbsolute();
        }

        @Override
        public VPath resolve(Path other) {
            final VPath o = unwrap(other);
            if (o.internal.isAbsolute()) {
                return o;
            }
            Path path = o.internal;
            if (path.toString().startsWith("/")) {
                return new VPath(fs, fs.internal.resolve(path.subpath(0, path.getNameCount())));
            }
            return new VPath(fs, internal.resolve(path));
        }

        @Override
        public VPath resolve(String other) {
            Path path = internal.getFileSystem().getPath(other);
            if (path.toString().startsWith("/")) {
                return new VPath(fs, fs.internal.resolve(path.subpath(0, path.getNameCount())));
            }
            return new VPath(fs, internal.resolve(path));
        }

        @Override
        public Path resolveSibling(Path other) {
            Objects.requireNonNull(other, "other");
            Path parent = getParent();
            return (parent == null) ? other : parent.resolve(other);
        }

        @Override
        public boolean startsWith(Path other) {
            return internal.startsWith(unwrap(other).internal);
        }

        @Override
        public boolean endsWith(Path other) {
            return internal.endsWith(unwrap(other).internal);
        }

        @Override
        public Path resolveSibling(String other) {
            return resolveSibling(fs.getPath(other));
        }

        @Override
        public boolean startsWith(String other) {
            return startsWith(fs.getPath(other));
        }

        @Override
        public boolean endsWith(String other) {
            return endsWith(fs.getPath(other));
        }

        @Override
        public VPath normalize() {
            return new VPath(fs, internal.normalize());
        }

        @Override
        public String toString() {
            if (fs.internal.equals(internal)) {
                return "/";
            }
            String str = internal.toString();
            return internal.isAbsolute() ? str.substring(fs.internal.toString().length() + 1) : str;
        }

        @Override
        public int hashCode() {
            return internal.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof VPath && this.internal.equals(((VPath) obj).internal);
        }

        @Override
        public int compareTo(Path other) {
            return internal.compareTo(unwrap(other).internal);
        }

        @Override
        public WatchKey register(WatchService watcher,
                                 WatchEvent.Kind<?>[] events,
                                 WatchEvent.Modifier... modifiers) throws IOException {
            return internal.register(watcher, events, modifiers);
        }

        @Override
        public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
            return internal.register(watcher, events);
        }

        @Override
        public File toFile() {
            return internalAbsolute().toFile();
        }

        @Override
        public Iterator<Path> iterator() {
            return new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return (i < getNameCount());
                }

                @Override
                public Path next() {
                    if (i < getNameCount()) {
                        Path result = getName(i);
                        i++;
                        return result;
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    throw new ReadOnlyFileSystemException();
                }
            };
        }
    }

    private static VPath unwrap(Path path) {
        Objects.requireNonNull(path, "path");
        if (!(path instanceof VPath)) {
            throw new ProviderMismatchException();
        }
        return (VPath) path;
    }

    private static final class ProviderImpl extends FileSystemProvider {

        @Override
        public String getScheme() {
            return "virtual";
        }

        @Override
        public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
            checkUri(uri);
            return new VirtualFileSystem(Path.of(uri.getPath()));
        }

        @Override
        public Path getPath(URI uri) {
            checkUri(uri);
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            int index = schemeSpecificPart.indexOf('!');
            URI virtualUri = URI.create(schemeSpecificPart.substring(0, index));
            String path = uri.getScheme();
            if (path == null || path.charAt(0) != '/') {
                throw new IllegalArgumentException("Invalid path component");
            }
            VirtualFileSystem fs = new VirtualFileSystem(Path.of(virtualUri));
            return fs.getPath(path);
        }

        @Override
        public FileSystem getFileSystem(URI uri) {
            checkUri(uri);
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            int index = schemeSpecificPart.indexOf('!');
            URI fileUri = URI.create(schemeSpecificPart.substring(0, index));
            return new VirtualFileSystem(Path.of(fileUri));
        }

        @Override
        public void checkAccess(Path path, AccessMode... modes) throws IOException {
            VPath vpath = unwrap(path);
            vpath.internalProvider().checkAccess(vpath.internalAbsolute(), modes);
        }

        @Override
        public Path readSymbolicLink(Path link) throws IOException {
            VPath vlink = unwrap(link);
            return new VPath(vlink.fs, Files.readSymbolicLink(vlink.internalAbsolute()));
        }

        @Override
        public void copy(Path src, Path target, CopyOption... options) throws IOException {
            VPath vsrc = unwrap(src);
            vsrc.internalProvider().copy(vsrc.internalAbsolute(), unwrap(target).internalAbsolute(), options);
        }

        @Override
        public void createDirectory(Path path, FileAttribute<?>... attrs) throws IOException {
            VPath vpath = unwrap(path);
            vpath.internalProvider().createDirectory(vpath.internalAbsolute(), attrs);
        }

        @Override
        public void delete(Path path) throws IOException {
            VPath vpath = unwrap(path);
            vpath.internalProvider().delete(vpath.internalAbsolute());
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().getFileAttributeView(vpath.internalAbsolute(), type, options);
        }

        @Override
        public FileStore getFileStore(Path path) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().getFileStore(vpath.fs.internal);
        }

        @Override
        public boolean isHidden(Path path) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().isHidden(vpath.internalAbsolute());
        }

        @Override
        public boolean isSameFile(Path path, Path other) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().isSameFile(vpath.internalAbsolute(), unwrap(other).internalAbsolute());
        }

        @Override
        public void move(Path src, Path target, CopyOption... options) throws IOException {
            VPath vsrc = unwrap(src);
            vsrc.internalProvider().move(vsrc.internalAbsolute(), unwrap(target).internalAbsolute(), options);
        }

        @Override
        public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
                                                                  Set<? extends OpenOption> options,
                                                                  ExecutorService exec,
                                                                  FileAttribute<?>... attrs) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().newAsynchronousFileChannel(vpath.internalAbsolute(), options, exec, attrs);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                                                  FileAttribute<?>... attrs) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().newByteChannel(vpath.internalAbsolute(), options, attrs);
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path path,
                                                        DirectoryStream.Filter<? super Path> filter) throws IOException {
            VPath vpath = unwrap(path);
            Iterator<Path> it = vpath.internalProvider().newDirectoryStream(vpath.internalAbsolute(), filter).iterator();
            Stream<Path> stream = StreamSupport.stream(spliteratorUnknownSize(it, Spliterator.ORDERED), false);
            return new DirectoryStream<>() {
                @Override
                public Iterator<Path> iterator() {
                    return stream.map(p -> (Path) new VPath(vpath.fs, p))
                                 .iterator();
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public FileChannel newFileChannel(Path path,
                                          Set<? extends OpenOption> options,
                                          FileAttribute<?>... attrs) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().newFileChannel(vpath.internalAbsolute(), options, attrs);
        }

        @Override
        public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().newInputStream(vpath.internalAbsolute(), options);
        }

        @Override
        public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().newOutputStream(vpath.internalAbsolute(), options);
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(Path path,
                                                                Class<A> type,
                                                                LinkOption... options) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().readAttributes(vpath.internalAbsolute(), type, options);
        }

        @Override
        public Map<String, Object> readAttributes(Path path,
                                                  String attribute,
                                                  LinkOption... options) throws IOException {
            VPath vpath = unwrap(path);
            return vpath.internalProvider().readAttributes(vpath.internalAbsolute(), attribute, options);
        }

        @Override
        public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
            VPath vpath = unwrap(path);
            vpath.internalProvider().setAttribute(vpath.internalAbsolute(), attribute, value, options);
        }

        private void checkUri(URI uri) {
            if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
                throw new IllegalArgumentException("URI does not match this provider");
            }
            if (uri.getAuthority() != null) {
                throw new IllegalArgumentException("Authority component present");
            }
            if (uri.getQuery() != null) {
                throw new IllegalArgumentException("Query component present");
            }
            if (uri.getFragment() != null) {
                throw new IllegalArgumentException("Fragment component present");
            }
        }
    }
}
