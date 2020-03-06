/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.dev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;


/**
 * A file type.
 */
public enum FileType implements Predicate<Path> {
    /**
     * A Java source file.
     */
    JavaSource(".java"),

    /**
     * A Java class file.
     */
    JavaClass(".class"),

    /**
     * A Maven pom file.
     */
    MavenPom(".xml"),

    /**
     * A jar file.
     */
    Jar(".jar"),

    /**
     * Any file.
     */
    NotJavaClass(null) {
        protected boolean matchesExtension(Path path) {
            String fileName = path.getFileName().toString();
            return !fileName.endsWith(".class")
                    && !fileName.endsWith(".swp")
                    && !fileName.endsWith("~")
                    && !fileName.startsWith(".");
        }
    };

    private final String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the file extension, if applicable.
     *
     * @return The extension or {@code null} if none.
     */
    String extension() {
        return extension;
    }

    @Override
    public boolean test(Path path) {
        if (Files.isRegularFile(path)) {
            return matchesExtension(path);
        }
        return false;
    }

    protected boolean matchesExtension(Path path) {
        return path.getFileName().toString().endsWith(extension);
    }
}
