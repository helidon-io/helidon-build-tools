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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// file system utilities
final class FileSystem {
    private FileSystem() {
    }

    static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new CopyrightException("Failed to get size of file " + path.toAbsolutePath(), e);
        }
    }

    static List<String> toLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CopyrightException("Failed to read lines of file " + path.toAbsolutePath(), e);
        }
    }
}
