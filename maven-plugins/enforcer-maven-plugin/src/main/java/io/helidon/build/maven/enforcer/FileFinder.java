/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.build.common.logging.Log;

/**
 * Configuration of discovery of files to check.
 */
public class FileFinder {
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

    private final String currentYear = YEAR_FORMATTER.format(ZonedDateTime.now());
    private final Path repositoryRoot;
    private final boolean useGit;
    private final boolean honorGitIgnore;

    private FileFinder(Builder builder) {
        useGit = builder.useGit;
        honorGitIgnore = builder.honorGitIgnore;
        repositoryRoot = builder.repositoryRoot;
    }

    /**
     * Builder to set up file config.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get files to check based on this file config.
     *
     * @param basePath path to use
     * @return found files
     */
    public FoundFiles findFiles(Path basePath) {
        // find repository root if not configured
        Path gitRepoDir = (repositoryRoot == null ? GitCommands.repositoryRoot(basePath) : repositoryRoot);

        List<FileMatcher> excludes = new ArrayList<>();
        if (useGit && honorGitIgnore) {
            addGitIgnore(gitRepoDir, excludes);
        }

        Set<FileRequest> foundFiles;
        Set<FileRequest> locallyModified;

        if (useGit) {
            locallyModified = GitCommands.locallyModified(gitRepoDir, basePath, currentYear);
            foundFiles = new HashSet<>(locallyModified);

            foundFiles.addAll(GitCommands.gitTracked(gitRepoDir, basePath));
        } else {
            foundFiles = findAllFiles(gitRepoDir, basePath);
            locallyModified = foundFiles;
        }

        List<FileRequest> fileRequests = foundFiles.stream()
                .filter(file -> isValid(file, excludes))
                .collect(Collectors.toList());

        Set<String> filteredLocallyModified = exclude(excludes, locallyModified);
        fileRequests.sort(FileRequest::compareTo);

        return FoundFiles.create(gitRepoDir, fileRequests, filteredLocallyModified, useGit);
    }

    @Override
    public String toString() {
        return "FileConfig{"
                + "repositoryRoot=" + repositoryRoot
                + ", useGit=" + useGit
                + ", honorGitIgnore=" + honorGitIgnore
                + '}';
    }

    private void addGitIgnore(Path gitRepoDir, List<FileMatcher> excludes) {
        Path gitIgnore = gitRepoDir.resolve(".gitignore");

        excludes.addAll(FileMatcher.create(".git/"));

        List<String> lines = FileSystem.toLines(gitIgnore)
                .stream()
                .filter(it -> !it.startsWith("#"))
                .filter(it -> !it.isBlank())
                .collect(Collectors.toList());

        for (String line : lines) {
            if (line.contains("*")) {
                if (line.startsWith("*.")) {
                    excludes.addAll(FileMatcher.create(line.substring(1)));
                } else {
                    if (line.startsWith("*")) {
                        excludes.add(new NameEndExclude(line.substring(1)));
                    } else if (line.endsWith("*")) {
                        excludes.add(new NameStartExclude(line.substring(line.length() - 1)));
                    } else {
                        Log.warn("$(YELLOW .gitignore) matches not supported: " + line);
                    }
                }
            } else {
                excludes.addAll(FileMatcher.create(line));
            }
        }
    }

    private Set<FileRequest> findAllFiles(Path gitRepoDir, Path basePath) {
        Set<FileRequest> result = new HashSet<>();

        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    result.add(FileRequest.create(gitRepoDir, gitRepoDir.relativize(file).toString(), lastModifiedYear(file)));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new EnforcerException("Failed to list files", e);
        }

        return result;
    }

    private String lastModifiedYear(Path file) {
        try {
            return YEAR_FORMATTER.format(ZonedDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(), ZoneId.of("GMT")));
        } catch (IOException e) {
            throw new EnforcerException("Failed to parse last modified year from local file: " + file, e);
        }
    }

    private Set<String> exclude(List<FileMatcher> excludes, Set<FileRequest> locallyModified) {
        return locallyModified.stream()
                .filter(it -> {
                    for (FileMatcher exclude : excludes) {
                        if (exclude.matches(it)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(FileRequest::relativePath)
                .collect(Collectors.toSet());
    }

    private boolean isValid(FileRequest file,
                            List<FileMatcher> excludes) {

        // file may have been deleted from GIT (or locally)
        if (!Files.exists(file.path())) {
            Log.debug("File " + file.relativePath() + " does not exist, ignoring.");
            return false;
        }

        for (FileMatcher exclude : excludes) {
            if (exclude.matches(file)) {
                Log.debug("Excluding " + file.relativePath());
                return false;
            }
        }

        return true;
    }

    /**
     * {@code FileConfig} builder static inner class.
     */
    public static final class Builder {
        private Path repositoryRoot;
        private boolean useGit = true;
        private boolean honorGitIgnore = true;

        private Builder() {
        }

        /**
         * Sets the {@code useGit} and returns a reference to this Builder so that the methods can be chained together.
         * @param useGit the {@code useGit} to set
         * @return a reference to this Builder
         */
        public Builder useGit(boolean useGit) {
            this.useGit = useGit;
            return this;
        }

        /**
         * Sets the {@code honorGitIgnore} and returns a reference to this Builder so that the methods can be chained together.
         * @param honorGitIgnore the {@code honorGitIgnore} to set
         * @return a reference to this Builder
         */
        public Builder honorGitIgnore(boolean honorGitIgnore) {
            this.honorGitIgnore = honorGitIgnore;
            return this;
        }

        /**
         * Sets the root of the repository, if it cannot be determined using git.
         *
         * @param repositoryRoot root of repo
         * @return updated builder
         */
        public Builder repositoryRoot(Path repositoryRoot) {
            this.repositoryRoot = repositoryRoot;
            return this;
        }

        /**
         * Returns a {@code FileConfig} built from the parameters previously set.
         *
         * @return a {@code FileConfig} built with parameters of this {@code FileConfig.Builder}
         */
        public FileFinder build() {
            return new FileFinder(this);
        }
    }

    private static final class NameEndExclude implements FileMatcher {
        private final String end;

        private NameEndExclude(String end) {
            this.end = end;
        }

        @Override
        public boolean matches(FileRequest file) {
            return file.fileName().endsWith(end);
        }
    }

    private static final class NameStartExclude implements FileMatcher {
        private final String start;

        private NameStartExclude(String start) {
            this.start = start;
        }

        @Override
        public boolean matches(FileRequest file) {
            return file.fileName().startsWith(start);
        }
    }
}
