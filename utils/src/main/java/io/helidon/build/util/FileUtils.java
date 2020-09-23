/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.util.Objects.requireNonNull;

/**
 * File utilities.
 */
public final class FileUtils {

    /**
     * The working directory.
     */
    public static final Path WORKING_DIR = requiredDirectoryFromProperty("user.dir", false);

    /**
     * The user home directory.
     */
    public static final Path USER_HOME_DIR = requiredDirectoryFromProperty("user.home", false);

    private static final String JAVA_BINARY_NAME = Constants.OS.javaExecutable();
    private static final String JAVA_HOME_VAR = "JAVA_HOME";
    private static final String PATH_VAR = "PATH";
    private static final String BIN_DIR_NAME = "bin";

    /**
     * Returns a directory path from the given system property name, creating it if required.
     *
     * @param systemPropertyName The property name.
     * @param createIfRequired {@code true} If the directory should be created if it does not exist.
     * @return The directory.
     */
    public static Path requiredDirectoryFromProperty(String systemPropertyName, boolean createIfRequired) {
        final String path = Requirements.requireNonNull(System.getProperty(systemPropertyName),
                "Required system property %s not set", systemPropertyName);
        return requiredDirectory(path, createIfRequired);
    }

    /**
     * Returns a directory path from the given path, creating it if required.
     *
     * @param path The path.
     * @param createIfRequired {@code true} If the directory should be created if it does not exist.
     * @return The directory.
     */
    public static Path requiredDirectory(String path, boolean createIfRequired) {
        final Path dir = Path.of(requireNonNull(path, "valid path required"));
        return createIfRequired ? ensureDirectory(dir) : assertDir(dir);
    }

    /**
     * Returns the relative path from the working directory for the given path, if possible.
     *
     * @param path The path.
     * @return The relative path or the original if it is not within the working directory.
     */
    public static Path fromWorking(Path path) {
        try {
            Path relativePath = WORKING_DIR.relativize(path);
            if (relativePath.getName(0).toString().equals("..")) {
                return path;
            } else {
                return relativePath;
            }
        } catch (IllegalArgumentException e) {
            return path;
        }
    }

    /**
     * Ensure that the given path is an existing directory, creating it if required.
     *
     * @param path The path.
     * @param attrs The attributes.
     * @return The normalized, absolute directory path.
     */
    public static Path ensureDirectory(Path path, FileAttribute<?>... attrs) {
        if (Files.exists(requireNonNull(path))) {
            return assertDir(path);
        } else {
            try {
                return Files.createDirectories(path, attrs);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Copies the source directory to the destination.
     *
     * @param source The source directory.
     * @param destination The destination directory. Must not exist.
     * @return The absolute, normalized destination directory.
     * @throws IllegalArgumentException If the destination exists.
     */
    @SuppressWarnings("CaughtExceptionImmediatelyRethrown")
    public static Path copyDirectory(Path source, Path destination) {
        assertDoesNotExist(destination);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                try {
                    final Path dst = destination.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        Files.createDirectory(dst);
                    } else {
                        Files.copy(src, dst, COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return destination.toAbsolutePath().normalize();
    }

    /**
     * List all files in the given directory that match the given filter. Does not recurse.
     *
     * @param directory The directory.
     * @param fileNameFilter The filter.
     * @return The normalized, absolute file paths.
     */
    public static List<Path> listFiles(Path directory, Predicate<String> fileNameFilter) {
        return listFiles(directory, fileNameFilter, 1);
    }

    /**
     * List all files in the given directory that match the given filter. Does not recurse.
     *
     * @param directory The directory.
     * @param pathFilter The filter.
     * @return The normalized, absolute file paths.
     */
    public static List<Path> listFiles(Path directory, BiPredicate<Path, BasicFileAttributes> pathFilter) {
        return listFiles(directory, pathFilter, 1);
    }

    /**
     * List all files in the given directory that match the given filter, recursively if maxDepth > 1.
     *
     * @param directory The directory.
     * @param fileNameFilter The filter.
     * @param maxDepth The maximum recursion depth.
     * @return The normalized, absolute file paths.
     */
    public static List<Path> listFiles(Path directory, Predicate<String> fileNameFilter, int maxDepth) {
        return listFiles(directory, (path, attrs) -> fileNameFilter.test(path.getFileName().toString()), maxDepth);
    }

    /**
     * List all files in the given directory that match the given filter, recursively if maxDepth > 1.
     *
     * @param directory The directory.
     * @param pathFilter The filter.
     * @param maxDepth The maximum recursion depth.
     * @return The normalized, absolute file paths.
     */
    public static List<Path> listFiles(Path directory, BiPredicate<Path, BasicFileAttributes> pathFilter, int maxDepth) {
        try {
            return Files.find(assertDir(directory), maxDepth, pathFilter).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * List all files and directories in the given directory. Does not recurse.
     *
     * @param directory The directory.
     * @return The normalized, absolute file paths.
     */
    public static List<Path> list(Path directory) {
        return list(directory, 1);
    }

    /**
     * List all files and directories in the given directory, recursively if maxDepth > 1.
     *
     * @param directory The directory.
     * @param maxDepth The maximum recursion depth.
     * @return The normalized, absolute file paths.
     */
    public static List<Path> list(Path directory, final int maxDepth) {
        try {
            return Files.find(assertDir(directory), maxDepth, (path, attrs) -> true)
                        .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Assert that the given path exists and is a directory.
     *
     * @param directory The directory.
     * @return The normalized, absolute directory path.
     * @throws IllegalArgumentException If the path does not exist or is not a directory.
     */
    public static Path assertDir(Path directory) {
        final Path result = assertExists(directory);
        if (Files.isDirectory(result)) {
            return result;
        } else {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
    }

    /**
     * Assert that the given path exists and is a file.
     *
     * @param file The file.
     * @return The normalized, absolute file path.
     * @throws IllegalArgumentException If the path does not exist or is not a file.
     */
    public static Path assertFile(Path file) {
        final Path result = assertExists(file);
        if (Files.isRegularFile(result)) {
            return result;
        } else {
            throw new IllegalArgumentException(file + " is not a file");
        }
    }

    /**
     * Assert that the given path exists.
     *
     * @param path The path.
     * @return The normalized, absolute path.
     * @throws IllegalArgumentException If the path does not exist.
     */
    public static Path assertExists(Path path) {
        if (Files.exists(requireNonNull(path))) {
            return path.toAbsolutePath().normalize();
        } else {
            throw new IllegalArgumentException(path + " does not exist");
        }
    }

    /**
     * Assert that the given path does not exist.
     *
     * @param path The path.
     * @return The normalized, absolute path.
     * @throws IllegalArgumentException If the path exists.
     */
    public static Path assertDoesNotExist(Path path) {
        if (Files.exists(requireNonNull(path))) {
            throw new IllegalArgumentException(path + " exists");
        } else {
            return path.toAbsolutePath().normalize();
        }
    }

    /**
     * Deletes the given file or directory if it exists.
     *
     * @param fileOrDirectory The file or directory.
     * @return The file or directory.
     * @throws IOException If an error occurs.
     */
    public static Path delete(Path fileOrDirectory) throws IOException {
        if (Files.exists(fileOrDirectory)) {
            if (Files.isRegularFile(fileOrDirectory)) {
                Files.delete(fileOrDirectory);
            } else {
                deleteDirectory(fileOrDirectory);
            }
        }
        return fileOrDirectory;
    }

    /**
     * Deletes the given directory if it exists.
     *
     * @param directory The directory.
     * @return The directory.
     * @throws IOException If an error occurs.
     */
    public static Path deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            if (Files.isDirectory(directory)) {
                try (Stream<Path> stream = Files.walk(directory)) {
                    stream.sorted(Comparator.reverseOrder())
                          .forEach(file -> {
                              try {
                                  Files.delete(file);
                              } catch (IOException e) {
                                  throw new UncheckedIOException(e);
                              }
                          });
                }
            } else {
                throw new IllegalArgumentException(directory + " is not a directory");
            }
        }
        return directory;
    }

    /**
     * Deletes the content of the given directory, if any.
     *
     * @param directory The directory.
     * @return The directory.
     * @throws IOException If an error occurs.
     */
    public static Path deleteDirectoryContent(Path directory) throws IOException {
        if (Files.exists(directory)) {
            if (Files.isDirectory(directory)) {
                try (Stream<Path> stream = Files.walk(directory)) {
                    stream.sorted(Comparator.reverseOrder())
                          .filter(file -> !file.equals(directory))
                          .forEach(file -> {
                              try {
                                  Files.delete(file);
                              } catch (IOException e) {
                                  throw new UncheckedIOException(e);
                              }
                          });
                }
            } else {
                throw new IllegalArgumentException(directory + " is not a directory");
            }
        }
        return directory;
    }

    /**
     * Returns the total size of all files in the given path, including subdirectories.
     *
     * @param path The path. May be a file or directory.
     * @return The size, in bytes.
     * @throws UncheckedIOException If an error occurs.
     */
    public static long sizeOf(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            } else {
                final AtomicLong size = new AtomicLong();
                Files.walkFileTree(path, new FileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        size.addAndGet(attrs.size());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
                return size.get();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the last modified time of the given file, in seconds.
     *
     * @param file The file.
     * @return The last modified time.
     */
    public static long lastModifiedSeconds(Path file) {
        return lastModifiedTime(file).to(TimeUnit.SECONDS);
    }

    /**
     * Returns the last modified time of the given file, in millis.
     *
     * @param file The file.
     * @return The last modified time.
     */
    public static long lastModifiedMillis(Path file) {
        return lastModifiedTime(file).to(TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the last modified time of the given file.
     *
     * @param file The file.
     * @return The last modified time.
     */
    public static FileTime lastModifiedTime(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Tests whether or not the given file has a modified time that is newer than the base time.
     *
     * @param file The file.
     * @param baseTime The base time. May be {@code null}.
     * @return {@code true} if base time is {@code null} or change time is newer.
     */
    public static Optional<FileTime> newerThan(Path file, FileTime baseTime) {
        final FileTime modTime = lastModifiedTime(file);
        if (newerThan(modTime, baseTime)) {
            return Optional.of(modTime);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Tests whether or not the given file has a modified time that is older than the base time.
     *
     * @param file The file.
     * @param baseTime The base time. May be {@code null}.
     * @return {@code true} if base time is {@code null} or change time is older.
     */
    public static Optional<FileTime> olderThan(Path file, FileTime baseTime) {
        final FileTime modTime = lastModifiedTime(file);
        if (olderThan(modTime, baseTime)) {
            return Optional.of(modTime);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Tests whether or not the given change time is newer than a base time.
     *
     * @param changeTime The time.
     * @param baseTime The base time. May be {@code null}.
     * @return {@code true} if base time is {@code null} or change time is newer.
     */
    public static boolean newerThan(FileTime changeTime, FileTime baseTime) {
        return baseTime == null || changeTime.compareTo(baseTime) > 0;
    }

    /**
     * Tests whether or not the given change time is older than a base time.
     *
     * @param changeTime The time.
     * @param baseTime The base time. May be {@code null}.
     * @return {@code true} if base time is {@code null} or change time is older.
     */
    public static boolean olderThan(FileTime changeTime, FileTime baseTime) {
        return baseTime == null || changeTime.compareTo(baseTime) < 0;
    }

    /**
     * Returns the file name of the given file, as a string.
     *
     * @param file The file.
     * @return The name.
     */
    public static String fileName(Path file) {
        return requireNonNull(file.getFileName()).toString();
    }

    /**
     * Find an executable in the {@code PATH} environment variable, if present.
     *
     * @param executableName The executable name.
     * @return The path.
     */
    public static Optional<Path> findExecutableInPath(String executableName) {
        return Arrays.stream(requireNonNull(System.getenv(PATH_VAR)).split(File.pathSeparator))
                     .map(dir -> Paths.get(dir))
                     .map(path -> path.resolve(executableName))
                     .filter(path -> !Constants.OS.isPosix() || Files.isExecutable(path))
                     .findFirst();
    }

    /**
     * Returns the path to the java executable, searching the {@code PATH} var first, then checking {@code JAVA_HOME}.
     *
     * @return The path.
     */
    public static Optional<Path> javaExecutable() {
        final Optional<Path> path = javaExecutableInPath();
        if (path.isPresent()) {
            return path;
        } else {
            return javaExecutableInJavaHome();
        }
    }

    /**
     * Normalize a given path.
     *
     * @param path path to normalize
     * @return normalized path
     */
    public static String normalize(Path path) {
        return path.toString().replaceAll("\\\\", "/");
    }

    /**
     * Returns the path to the java executable, searching the {@code PATH} var first, then checking {@code JAVA_HOME}.
     *
     * @return The path.
     * @throws IllegalStateException if not found.
     */
    public static Path assertJavaExecutable() {
        return javaExecutable().orElseThrow(() -> new IllegalStateException(JAVA_BINARY_NAME + " not found. Please add it to "
                                                                            + "your PATH or set the JAVA_HOME or variable."));
    }

    /**
     * Returns the path to the java executable using the {@code PATH} var.
     *
     * @return The path.
     */
    public static Optional<Path> javaExecutableInPath() {
        return findExecutableInPath(JAVA_BINARY_NAME);
    }

    /**
     * Returns the path to the java executable using the {@code JAVA_HOME} var if present and valid.
     *
     * @return The path.
     */
    public static Optional<Path> javaExecutableInJavaHome() {
        final String javaHomePath = System.getenv(JAVA_HOME_VAR);
        if (javaHomePath != null) {
            final Path javaHome = Paths.get(javaHomePath);
            final Path binary = javaHome.resolve(BIN_DIR_NAME).resolve(JAVA_BINARY_NAME);
            if (!Constants.OS.isPosix() || Files.isExecutable(binary)) {
                return Optional.of(binary);
            } else {
                throw new IllegalStateException(JAVA_BINARY_NAME + " not found in JAVA_HOME path: " + javaHomePath);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates the given file (with no content) if it does not already exist.
     *
     * @param file The file.
     * @return The file.
     */
    public static Path ensureFile(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return file;
    }

    /**
     * Ensure that the given file exists, and update the modified time if it does.
     *
     * @param file The file.
     * @return The file.
     */
    public static Path touch(Path file) {
        if (Files.exists(file)) {
            final long currentTime = System.currentTimeMillis();
            final long lastModified = lastModifiedSeconds(file);
            final long lastModifiedPlusOneSecond = lastModified + 1000;
            final long newTime = Math.max(currentTime, lastModifiedPlusOneSecond);
            try {
                Files.setLastModifiedTime(file, FileTime.fromMillis(newTime));
                return file;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return ensureFile(file);
        }
    }

    /**
     * Change detection type.
     */
    public enum ChangeDetectionType {
        /**
         * Return the first newer modification time.
         */
        FIRST,
        /**
         * Return the latest newer modification time.
         */
        LATEST
    }

    /**
     * Checks whether any matching file in the given directory has a modified time more recent than the given time.
     *
     * @param directory The directory.
     * @param baseTime The time to check against. If {@code null}, uses {@code FileUtils.fromMillis(0)}.
     * @param dirFilter A filter for directories to visit.
     * @param fileFilter A filter for which files to check.
     * @param type The type.
     * @return The time, if changed.
     */
    public static Optional<FileTime> changedSince(Path directory,
                                                  FileTime baseTime,
                                                  Predicate<Path> dirFilter,
                                                  Predicate<Path> fileFilter,
                                                  ChangeDetectionType type) {

        final FileTime base = baseTime == null ? FileTime.fromMillis(0) : baseTime;
        final AtomicReference<FileTime> checkTime = new AtomicReference<>(base);
        final AtomicReference<FileTime> changeTime = new AtomicReference<>();
        final boolean checkAllFiles = type == ChangeDetectionType.LATEST;
        Log.debug("Checking if project has files newer than last check time %s", checkTime.get());
        try {
            Files.walkFileTree(directory, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return dirFilter.test(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (fileFilter.test(file)) {
                        final FileTime fileTime = lastModifiedTime(file);
                        if (fileTime.compareTo(checkTime.get()) > 0) {
                            Log.debug("%s @ %s is newer than last check time %s", file, fileTime, checkTime.get());
                            changeTime.set(fileTime);
                            if (checkAllFiles) {
                                checkTime.set(fileTime);
                            } else {
                                return FileVisitResult.TERMINATE;
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    changeTime.set(null);
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            return Optional.ofNullable(changeTime.get());

        } catch (Exception e) {
            Log.warn(e.getMessage());
        }

        return Optional.of(FileTime.fromMillis(System.currentTimeMillis())); // Force it if we get here
    }

    private FileUtils() {
    }
}
