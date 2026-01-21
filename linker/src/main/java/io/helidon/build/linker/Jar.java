/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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

package io.helidon.build.linker;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.Runtime.Version;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import io.helidon.build.common.InputStreams;
import io.helidon.build.common.logging.Log;

import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.UnsupportedVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static io.helidon.build.common.FileUtils.fileExt;
import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.OSType.CURRENT_OS;
import static io.helidon.build.linker.JavaRuntime.CURRENT_JDK;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * CDI BeansArchive aware jar wrapper. Supports creating an index if missing and adding it during copy.
 */
public final class Jar implements ResourceContainer {

    private static final Set<PosixFilePermission> POSIX_PERMS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OTHERS_READ);

    private final Path path;
    private final Version version;
    private final JarFile jar;
    private final Manifest manifest;
    private final boolean isJavaModule;
    private final boolean isMultiRelease;
    private final boolean isBeansArchive;
    private final boolean isSigned;
    private final ModuleDescriptor moduleDescriptor;
    private final Set<String> resources;

    private Jar(Path path, Version version) {
        this.path = requireFile(requireNonNull(path)); // Absolute and normalized
        this.version = requireNonNull(version);
        this.isJavaModule = fileName(path).endsWith(".jmod");

        Set<String> resources = new HashSet<>();
        boolean isMultiRelease = false;
        boolean isSigned = false;
        boolean isBeanArchive = false;
        Version moduleVersion = null;
        JarEntry moduleInfo = null;
        ModuleDescriptor moduleDescriptor = null;

        try {
            this.jar = new JarFile(path.toFile());
            this.manifest = jar.getManifest();

            // check multi-release
            if (!isJavaModule && manifest != null) {
                Object value = manifest.getMainAttributes().get(Attributes.Name.MULTI_RELEASE);
                if (value != null) {
                    isMultiRelease = "true".equalsIgnoreCase(value.toString());
                }
            }

            // process all entries
            Enumeration<JarEntry> enumeration = jar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                String entryName = entry.getName();
                if (isJavaModule) {
                    if (entryName.equals("classes/module-info.class")) {
                        moduleInfo = entry;
                    }
                } else if (isMultiRelease) {
                    if (entryName.endsWith("module-info.class")) {
                        if (entryName.startsWith("META-INF/versions/")) {
                            int beginIndex = "META-INF/versions/".length();
                            int endIndex = entryName.indexOf('/', beginIndex);
                            if (endIndex > beginIndex) {
                                Version currentVersion = Version.parse(entryName.substring(beginIndex, endIndex));
                                if (moduleVersion == null || currentVersion.compareTo(moduleVersion) > 0) {
                                    moduleVersion = currentVersion;
                                    moduleInfo = entry;
                                }
                            }
                        } else if (moduleInfo == null) {
                            moduleInfo = entry;
                        }
                    }
                } else {
                    if (entryName.startsWith("META-INF/")) {
                        if (entryName.endsWith(".SF")) {
                            isSigned = true;
                        } else if (entryName.equals("META-INF/beans.xml")) {
                            isBeanArchive = true;
                        }
                    } else if (entryName.equals("module-info.class")) {
                        moduleInfo = entry;
                    }
                }
                resources.add(entry.getName());
            }
            if (moduleInfo != null) {
                moduleDescriptor = ModuleDescriptor.read(jar.getInputStream(moduleInfo));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.resources = resources;
        this.isMultiRelease = isMultiRelease;
        this.isSigned = isSigned;
        this.isBeansArchive = isBeanArchive;
        this.moduleDescriptor = moduleDescriptor;
    }

    /**
     * Test whether the given path should be treated as a jar.
     *
     * @param path The path.
     * @return {@code true} if the path should be treated as a jar.
     */
    public static boolean isJar(Path path) {
        if (Files.isRegularFile(path)) {
            String ext = fileExt(path);
            if (ext != null) {
                switch (ext) {
                    //noinspection SpellCheckingInspection
                    case "jmod":
                    case "jar":
                    case "zip":
                        return true;
                    default:
                        return false;
                }
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
        return open(jarPath, CURRENT_JDK.version());
    }

    /**
     * Returns the given jar path as a {@link Jar}.
     *
     * @param jarPath The jar path.
     * @param version The Java version used to find versioned entries if this is
     *                a {@link #isMultiRelease() multi-release JAR}.
     * @return The {@link Jar}.
     * @throws IllegalArgumentException if the path is not treatable as a jar.
     */
    public static Jar open(Path jarPath, Version version) {
        if (!isJar(jarPath)) {
            throw new IllegalArgumentException("Not a jar: " + jarPath);
        }
        return new Jar(jarPath, version);
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
     * Returns the Java version used to find versioned entries if this is
     * a {@link #isMultiRelease() multi-release JAR}.
     *
     * @return The version.
     */
    public Version version() {
        return version;
    }

    /**
     * Returns the manifest class-path if present.
     *
     * @return The paths that exist in the file system. May be empty or contain directories.
     */
    public List<Path> classPath() {
        if (manifest != null) {
            String classPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (classPath != null) {
                Path root = requireNonNull(path.getParent());
                return Arrays.stream(classPath.split(" "))
                        .map(root::resolve)
                        .filter(Files::exists)
                        .collect(Collectors.toList());
            }
        }
        return emptyList();
    }

    /**
     * Returns the manifest, if present.
     *
     * @return The manifest or {@code null} if not present.
     */
    public Manifest manifest() {
        return manifest;
    }

    @Override
    public boolean containsResource(String path) {
        return resources.contains(path);
    }

    /**
     * Returns whether this jar is signed.
     *
     * @return {@code true} if signed.
     */
    public boolean isSigned() {
        return isSigned;
    }

    /**
     * Returns whether this jar is multi-release.
     *
     * @return {@code true} if multi-release.
     */
    public boolean isMultiRelease() {
        return isMultiRelease;
    }

    /**
     * Returns the descriptor if a {@code module-info.class} is present.
     *
     * @return The descriptor or {@code null} if not present.
     */
    public ModuleDescriptor moduleDescriptor() {
        return moduleDescriptor;
    }

    /**
     * Copy this jar into the given directory. Adds a Jandex index if required.
     *
     * @param targetDir   The targetDirectory.
     * @param ensureIndex {@code true} if an index should be added if this is a beans archive
     *                    and there is no Jandex index present.
     * @param stripDebug  {@code true} if debug information should be stripped from classes.
     * @return The normalized, absolute path to the new file.
     */
    public Path copy(Path targetDir, boolean ensureIndex, boolean stripDebug) {
        Path targetFile = requireDirectory(targetDir).resolve(path.getFileName());
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(targetFile))) {
            Indexer indexer = null;
            byte[] index = null;
            if (ensureIndex && !isJavaModule && isBeansArchive) {
                index = loadIndex();
                if (index == null) {
                    if (isSigned) {
                        Log.warn("  Cannot add Jandex index to signed jar %s", this);
                    } else {
                        indexer = new Indexer();
                        Log.info("  Creating missing index for CDI beans archive %s", this);
                    }
                }
            }

            // copy jar manually if index is built or strip debug
            if (indexer != null || stripDebug) {
                try (JarOutputStream jos = new JarOutputStream(out)) {
                    Enumeration<JarEntry> enumeration = jar.entries();
                    while (enumeration.hasMoreElements()) {
                        JarEntry entry = enumeration.nextElement();
                        String entryName = entry.getName();
                        jos.putNextEntry(copyJarEntry(entry));
                        if (!entry.isDirectory()) {
                            boolean isClassFile = entryName.endsWith(".class") && !entryName.equals("module-info.class");
                            if (isClassFile && indexer != null) {
                                try {
                                    indexer.index(jar.getInputStream(entry));
                                } catch (IOException e) {
                                    Log.warn("  Could not index class %s in %s: %s", entryName, this, e.getMessage());
                                }
                            }
                            if (isClassFile && stripDebug && !isSigned) {
                                ClassReader reader = new ClassReader(jar.getInputStream(entry));
                                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                                reader.accept(writer, ClassReader.SKIP_DEBUG);
                                jos.write(writer.toByteArray());
                            } else if (!entryName.equals("META-INF/jandex.idx")) {
                                InputStreams.transfer(jar.getInputStream(entry), jos);
                            }
                        }
                        jos.flush();
                        jos.closeEntry();
                    }

                    // (re)build index
                    if (indexer != null) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        IndexWriter writer = new IndexWriter(out);
                        writer.write(indexer.complete());
                        index = bos.toByteArray();
                    }

                    // add index
                    if (index != null) {
                        JarEntry entry = new JarEntry("META-INF/jandex.idx");
                        entry.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
                        jos.putNextEntry(entry);
                        jos.write(index);
                        jos.flush();
                        jos.closeEntry();
                    }
                }
            } else {
                // otherwise just copy the whole jar file
                InputStreams.transfer(Files.newInputStream(path), out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (CURRENT_OS.isPosix()) {
            try {
                // chmod 644
                Files.setPosixFilePermissions(targetFile, POSIX_PERMS);
            } catch (IOException e) {
                Log.warn("Unable to set %s read-only: %s", e.getMessage());
            }
        }
        return targetFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Jar jar = (Jar) o;
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

    private byte[] loadIndex() {
        JarEntry entry = jar.getJarEntry("META-INF/jandex.idx");
        if (entry != null) {
            Log.info("  Checking index in CDI beans archive %s", this);
            try (InputStream in = jar.getInputStream(entry)) {
                byte[] bytes = in.readAllBytes();
                new IndexReader(new ByteArrayInputStream(bytes)).read();
                return bytes;
            } catch (IllegalArgumentException e) {
                Log.warn("  Jandex index in %s is not valid, will re-create: %s", path, e.getMessage());
            } catch (UnsupportedVersion e) {
                Log.warn("  Jandex index in %s is an unsupported version, will re-create: %s", path, e.getMessage());
            } catch (IOException e) {
                Log.warn("  Jandex index in %s cannot be read, will re-create: %s", path, e.getMessage());
            }
        }
        return null;
    }

    private static JarEntry copyJarEntry(JarEntry entry) {
        JarEntry copy = new JarEntry(entry.getName());
        if (entry.getCreationTime() != null) {
            copy.setCreationTime(entry.getCreationTime());
        }
        if (entry.getLastModifiedTime() != null) {
            copy.setLastModifiedTime(entry.getLastModifiedTime());
        }
        if (entry.getExtra() != null) {
            copy.setExtra(entry.getExtra());
        }
        if (entry.getComment() != null) {
            copy.setComment(entry.getComment());
        }
        if (!entry.isDirectory()) {
            int method = entry.getMethod();
            if (method == JarEntry.STORED || method == ZipEntry.DEFLATED) {
                copy.setMethod(method);
            }
        }
        return copy;
    }
}
