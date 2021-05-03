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

package io.helidon.build.copyright;

import java.nio.file.Path;

/**
 * Information about the current file.
 */
public class FileRequest {
    private final Path path;
    private final String relativePath;
    private final String fileName;
    private final String suffix;

    private FileRequest(Path path, String relativePath, String fileName, String suffix) {
        this.path = path;
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.suffix = suffix;
    }

    /**
     * @param rootPath root path of the repository
     * @param relativePath relative path from the rootPath
     * @return a new file request
     */
    static FileRequest create(Path rootPath, String relativePath) {
        Path file = rootPath.resolve(relativePath);
        String fileName = file.getFileName().toString();
        String fileSuffix = fileSuffix(fileName);

        return new FileRequest(file, relativePath, fileName, fileSuffix);
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

        return new FileRequest(null, relativePath, fileName, fileSuffix(fileName));
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

    private static String fileSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index);
    }
}
