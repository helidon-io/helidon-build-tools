/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import io.helidon.build.common.Strings;

/**
 * Information about the current file.
 */
public class FileRequest implements Comparable<FileRequest> {
    private final Path path;
    private final String relativePath;
    private final String fileName;
    private final String suffix;
    private final String lastModifiedYear;

    private FileRequest(Path path, String relativePath, String fileName, String suffix, String lastModifiedYear) {
        this.path = path;
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.suffix = suffix;
        this.lastModifiedYear = lastModifiedYear;
    }

    /**
     * @param rootPath root path of the repository
     * @param relativePath relative path from the rootPath
     * @param lastModifiedYear year the file was last modified
     * @return a new file request
     */
    public static FileRequest create(Path rootPath, String relativePath, String lastModifiedYear) {
        String normPath = Strings.normalizePath(relativePath);
        Path file = rootPath.resolve(normPath);
        String fileName = file.getFileName().toString();
        String fileSuffix = fileSuffix(fileName);

        return new FileRequest(file, normPath, fileName, fileSuffix, lastModifiedYear);
    }

    /**
     * Path relative to working directory.
     *
     * @param relativePath relative path (to the current directory)
     * @param lastModifiedYear year the file was last modified
     * @return a new file request
     */
    public static FileRequest create(String relativePath, String lastModifiedYear) {
        String normPath = Strings.normalizePath(relativePath);
        Path filePath = Paths.get(normPath);
        String fileName = filePath.getFileName().toString();
        String fileSuffix = fileSuffix(fileName);

        return new FileRequest(filePath, normPath, fileName, fileSuffix, lastModifiedYear);
    }

    // testing only
    static FileRequest create(String relativePath) {
        int i = relativePath.lastIndexOf('/');
        String fileName;
        if (i > -1) {
            fileName = relativePath.substring(i + 1);
        } else {
            fileName = relativePath;
        }

        return new FileRequest(null, relativePath, fileName, fileSuffix(fileName), "2021");
    }

    private static String fileSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index);
    }

    /**
     * Full path to the file.
     *
     * @return path
     */
    public Path path() {
        return path;
    }

    /**
     * Relative path string, not starting with /, using forward slash as path separator.
     *
     * @return relative path
     */
    public String relativePath() {
        return relativePath;
    }

    /**
     * File name (such as "README.md").
     *
     * @return name of the file
     */
    public String fileName() {
        return fileName;
    }

    /**
     * File suffix (such as ".md"), including the leading dot.
     *
     * @return file suffix
     */
    public String suffix() {
        return suffix;
    }

    /**
     * Last modification year of this file.
     *
     * @return year last modified
     */
    public String lastModifiedYear() {
        return lastModifiedYear;
    }

    @Override
    public int compareTo(FileRequest o) {
        return this.relativePath.compareTo(o.relativePath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileRequest that = (FileRequest) o;
        return relativePath.equals(that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath);
    }
}
