/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import io.helidon.build.util.Log;
import io.helidon.build.util.StreamUtils;
import io.helidon.linker.util.Constants;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.UnsupportedVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.fileName;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * CDI BeansArchive aware jar wrapper. Supports creating an index if missing and adding it during copy.
 */
public final class Jar implements ResourceContainer {
    private static final String JMOD_SUFFIX = ".jmod";
    private static final Set<String> SUPPORTED_SUFFIXES = Set.of(".jar", ".zip", JMOD_SUFFIX);
    private static final String BEANS_RESOURCE_PATH = "META-INF/beans.xml";
    private static final String JANDEX_INDEX_RESOURCE_PATH = "META-INF/jandex.idx";
    private static final String CLASS_FILE_SUFFIX = ".class";
    private static final String JMOD_CLASSES_PREFIX = "classes/";
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String SIGNATURE_PREFIX = "META-INF/";
    private static final String SIGNATURE_SUFFIX = ".SF";
    private final Path path;
    private final boolean isJmod;
    private final JarFile jar;
    private final Manifest manifest;
    private final boolean isMultiRelease;
    private final boolean isBeansArchive;
    private final boolean isSigned;
    private final ModuleDescriptor descriptor;
    private final AtomicReference<Set<String>> resources;
    private Index index;
    private boolean builtIndex;

    /**
     * An entry in a jar file.
     */
    public final class Entry extends JarEntry {

        private Entry(JarEntry entry) {
            super(entry);
        }

        /**
         * Returns entry path.
         *
         * @return The path.
         */
        public String path() {
            return getName();
        }

        /**
         * Returns a stream to access the data for this entry.
         *
         * @return The stream.
         */
        public InputStream data() {
            try {
                return jar.getInputStream(this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Test whether or not the given path should be treated as a jar.
     *
     * @param path The path.
     * @return {@code true} if the path should be treated as a jar.
     */
    public static boolean isJar(Path path) {
        if (Files.isRegularFile(path)) {
            final String name = fileName(path);
            final int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                return SUPPORTED_SUFFIXES.contains(name.substring(lastDot));
            }
        }
        return false;
    }

    /**
     * Returns the given jar path as a {@link Jar}.
     *
     * @param jarPath The jar path.
     * @return The {@link Jar}.
     * @throws IllegalArgumentException if the path is not treatable as a jar.
     */
    public static Jar open(Path jarPath) {
        if (!isJar(jarPath)) {
            throw new IllegalArgumentException("Not a jar: " + jarPath);
        }
        return new Jar(jarPath);
    }

    private Jar(Path path) {
        this.path = assertFile(path); // Absolute and normalized
        this.isJmod = fileName(path).endsWith(JMOD_SUFFIX);
        try {
            this.jar = new JarFile(path.toFile());
            this.manifest = jar.getManifest();
            this.isMultiRelease = !isJmod && isMultiRelease(manifest);
            this.isSigned = !isJmod && hasSignatureFile();
            this.isBeansArchive = !isJmod && hasEntry(BEANS_RESOURCE_PATH);
            final Entry moduleInfo = findEntry(isJmod ? JMOD_CLASSES_PREFIX + MODULE_INFO_CLASS : MODULE_INFO_CLASS);
            if (moduleInfo != null) {
                this.descriptor = ModuleDescriptor.read(moduleInfo.data());
            } else {
                this.descriptor = null;
            }
            this.resources = new AtomicReference<>();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the file name of this jar.
     *
     * @return The name.
     */
    public String name() {
        return fileName(path);
    }

    /**
     * Returns the path to this jar.
     *
     * @return The path.
     */
    public Path path() {
        return path;
    }

    /**
     * Returns whether or not this is a {@code .jmod} file.
     *
     * @return {@code true} if {@link .jmod}.
     */
    public boolean isJmod() {
        return isJmod;
    }

    /**
     * Returns the manifest class-path if present.
     *
     * @return The paths that exist in the file system. May be empty or contain directories.
     */
    public List<Path> classPath() {
        if (manifest != null) {
            final Object classPath = manifest.getMainAttributes().get(Attributes.Name.CLASS_PATH);
            if (classPath != null) {
                final Path root = requireNonNull(path().getParent());
                return Arrays.stream(((String) classPath).split(" "))
                             .map(root::resolve)
                             .filter(file -> Files.exists(file))
                             .collect(Collectors.toList());
            }
        }
        return emptyList();
    }

    /**
     * Returns the entries in this jar.
     *
     * @return The entries.
     */
    public Stream<Entry> entries() {
        final Iterator<JarEntry> iterator = jar.entries().asIterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                            .map(Entry::new);
    }

    @Override
    public boolean containsResource(String resourcePath) {
        Set<String> paths = resources.get();
        if (paths == null) {
            synchronized (resources) {
                paths = entries().map(JarEntry::getName).collect(Collectors.toSet());
                resources.set(paths);
            }
        }
        return paths.contains(resourcePath);
    }

    /**
     * Returns whether or not this jar is signed.
     *
     * @return {@code true} if signed.
     */
    public boolean isSigned() {
        return isSigned;
    }

    /**
     * Returns whether or not this jar is multi-release.
     *
     * @return {@code true} if multi-release.
     */
    public boolean isMultiRelease() {
        return isMultiRelease;
    }

    /**
     * Returns whether or not this jar is a CDI beans archive.
     *
     * @return {@code true} if a beans archive.
     */
    public boolean isBeansArchive() {
        return isBeansArchive;
    }

    /**
     * Returns whether or not this jar is a CDI beans archive containing a Jandex index.
     *
     * @return {@code true} if a beans archive containing an index.
     */
    public boolean hasIndex() {
        return isBeansArchive && index != null;
    }

    /**
     * Returns whether or not this jar contains a {@code module-info.class).
     *
     * @return {@code true} if a {@code module-info.class) is present.
     */
    public boolean hasModuleDescriptor() {
        return descriptor != null;
    }

    /**
     * Returns the descriptor if a {@code module-info.class) is present.
     *
     * @return The descriptor or {@code null} if not present.
     */
    public ModuleDescriptor moduleDescriptor() {
        return descriptor;
    }

    /**
     * Copy this jar into the given directory. Adds a Jandex index if required.
     *
     * @param targetDir The targetDirectory.
     * @param ensureIndex {@code true} if an index should be added if this is a beans archive
     * and their is no Jandex index present.
     * @param stripDebug {@code true} if debug information should be stripped from classes.
     * @return The normalized, absolute path to the new file.
     */
    public Path copyToDirectory(Path targetDir, boolean ensureIndex, boolean stripDebug) {
        final Path fileName = path.getFileName();
        final Path targetFile = assertDir(targetDir).resolve(fileName);
        if (ensureIndex) {
            ensureIndex();
        }
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(targetFile))) {

            // Add the index if we built it, and/or strip debug information if required; otherwise just copy the whole jar file

            if (builtIndex) {
                copy(out, true, stripDebug);
            } else if (stripDebug) {
                copy(out, false, true);
            } else {
                StreamUtils.transfer(Files.newInputStream(path), out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (Constants.OS.isPosix()) {
            try {
                Files.setPosixFilePermissions(targetFile, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.OTHERS_READ
                ));
            } catch (IOException e) {
                Log.warn("Unable to set %s read-only: %s", e.getMessage());
            }
        }
        return targetFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Jar jar = (Jar) o;
        return path.equals(jar.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return isSigned ? name() + " (signed)" : name();
    }

    private void ensureIndex() {
        if (isBeansArchive) {
            if (hasEntry(JANDEX_INDEX_RESOURCE_PATH)) {
                index = loadIndex();
            }
            if (index == null) {
                if (isSigned) {
                    Log.warn("Cannot add Jandex index to signed jar %s", name());
                } else {
                    index = buildIndex();
                    builtIndex = true;
                }
            }
        }
    }

    private Index loadIndex() {
        Log.info("  checking index in CDI beans archive %s", this);
        try (InputStream in = getEntry(JANDEX_INDEX_RESOURCE_PATH).data()) {
            return new IndexReader(in).read();
        } catch (IllegalArgumentException e) {
            Log.warn("  Jandex index in %s is not valid, will re-create: %s", path, e.getMessage());
        } catch (UnsupportedVersion e) {
            Log.warn("  Jandex index in %s is an unsupported version, will re-create: %s", path, e.getMessage());
        } catch (IOException e) {
            Log.warn("  Jandex index in %s cannot be read, will re-create: %s", path, e.getMessage());
        }
        return null;
    }

    private Index buildIndex() {
        Log.info("  creating missing index for CDI beans archive %s", this);
        final Indexer indexer = new Indexer();
        classEntries().forEach(entry -> {
            try {
                indexer.index(entry.data());
            } catch (IOException e) {
                Log.warn("  could not index class %s in %s: %s", entry.path(), this, e.getMessage());
            }
        });
        return indexer.complete();
    }

    private boolean hasSignatureFile() {
        return entries().anyMatch(e -> {
            final String path = e.path();
            return path.startsWith(SIGNATURE_PREFIX) && path.endsWith(SIGNATURE_SUFFIX);
        });
    }

    private boolean hasEntry(String path) {
        return entries().anyMatch(entry -> entry.path().equals(path));
    }

    private Entry findEntry(String path) {
        return entries().filter(entry -> entry.path().equals(path))
                        .findFirst().orElse(null);
    }

    private Entry getEntry(String path) {
        return entries().filter(entry -> entry.path().equals(path))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Could not get '" + path + "' entry."));
    }

    private Stream<Entry> classEntries() {
        return entries().filter(Jar::isNormalClassFile);
    }

    private void copy(OutputStream out, boolean addIndex, boolean stripDebug) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(out)) {

            if (addIndex) {
                addIndex(jar);
            }

            // Copy all entries, filtering out any previous index (that could not be read)

            entries().filter(e -> !e.path().equals(JANDEX_INDEX_RESOURCE_PATH))
                     .forEach(entry -> {
                         try {
                             jar.putNextEntry(newJarEntry(entry));
                             if (!entry.isDirectory()) {
                                 StreamUtils.transfer(data(entry, stripDebug), jar);
                             }
                             jar.flush();
                             jar.closeEntry();
                         } catch (IOException e) {
                             throw new UncheckedIOException(e);
                         }
                     });
        }
    }

    private InputStream data(Entry entry, boolean stripDebug) throws IOException {
        if (stripDebug && isNormalClassFile(entry) && !isSigned) {
            ClassReader reader = new ClassReader(entry.data());
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            reader.accept(writer, ClassReader.SKIP_DEBUG);
            return new ByteArrayInputStream(writer.toByteArray());
        } else {
            return entry.data();
        }
    }

    private static boolean isNormalClassFile(Entry entry) {
        final String name = entry.path();
        return name.endsWith(CLASS_FILE_SUFFIX) && !name.equals(MODULE_INFO_CLASS);
    }

    private static JarEntry newJarEntry(Entry entry) {
        final JarEntry result = new JarEntry(entry.getName());
        if (result.getCreationTime() != null) {
            result.setCreationTime(entry.getCreationTime());
        }
        if (result.getLastModifiedTime() != null) {
            result.setLastModifiedTime(entry.getLastModifiedTime());
        }
        if (entry.getExtra() != null) {
            result.setExtra(entry.getExtra());
        }
        if (result.getComment() != null) {
            result.setComment(entry.getComment());
        }
        if (!entry.isDirectory()) {
            final int method = entry.getMethod();
            if (method == JarEntry.STORED || method == ZipEntry.DEFLATED) {
                result.setMethod(method);
            }
        }
        return result;
    }

    private void addIndex(JarOutputStream jar) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final IndexWriter writer = new IndexWriter(out);
        try {
            writer.write(index);
            final ByteArrayInputStream data = new ByteArrayInputStream(out.toByteArray());
            final JarEntry entry = new JarEntry(JANDEX_INDEX_RESOURCE_PATH);
            entry.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
            jar.putNextEntry(entry);
            StreamUtils.transfer(data, jar);
            jar.flush();
            jar.closeEntry();
        } catch (IOException e) {
            Log.warn("Unable to add index: %s", e);
        }
    }


    private static boolean isMultiRelease(Manifest manifest) {
        return manifest != null && "true".equalsIgnoreCase(mainAttribute(manifest, Attributes.Name.MULTI_RELEASE));
    }

    private static String mainAttribute(Manifest manifest, Attributes.Name name) {
        if (manifest != null) {
            final Object value = manifest.getMainAttributes().get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

}
