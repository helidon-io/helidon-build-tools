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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static io.helidon.build.common.FileUtils.lastModifiedTime;

/**
 * File changes detection utility.
 */
public abstract class FileChanges {

    private FileChanges() {
    }

    /**
     * Change detection type.
     */
    public enum DetectionType {
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
     * @param directory  The directory.
     * @param baseTime   The time to check against. If {@code null}, uses {@code FileUtils.fromMillis(0)}.
     * @param dirFilter  A filter for directories to visit.
     * @param fileFilter A filter for which files to check.
     * @param type       The type.
     * @return The time, if changed.
     */
    public static Optional<FileTime> changedSince(Path directory,
                                                  FileTime baseTime,
                                                  Predicate<Path> dirFilter,
                                                  Predicate<Path> fileFilter,
                                                  DetectionType type) {

        final FileTime base = baseTime == null ? FileTime.fromMillis(0) : baseTime;
        final AtomicReference<FileTime> checkTime = new AtomicReference<>(base);
        final AtomicReference<FileTime> changeTime = new AtomicReference<>();
        final boolean checkAllFiles = type == DetectionType.LATEST;
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
}
