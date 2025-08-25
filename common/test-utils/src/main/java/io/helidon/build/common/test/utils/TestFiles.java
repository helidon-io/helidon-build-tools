/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common.test.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test files utility.
 */
public abstract class TestFiles {

    private TestFiles() {
    }

    /**
     * Returns the target directory for the given test class.
     *
     * @param testClass The test class.
     * @return The directory.
     */
    public static Path targetDir(Class<?> testClass) {
        try {
            final Path codeSource = Paths.get(testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            return codeSource.getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a path to a resource file or directory under "target/test-classes/" for the given test class.
     *
     * @param testClass    The test class.
     * @param resourcePath The resource path.
     * @return The path.
     */
    public static Path testResourcePath(Class<?> testClass, String resourcePath) {
        Path targetDir = targetDir(testClass);
        return targetDir.resolve("test-classes/" + resourcePath);
    }

    /**
     * Return the content of a resource file for the given test class.
     *
     * @param testClass    The test class.
     * @param resourcePath The resource path.
     * @return The resource content
     */
    public static String testResourceString(Class<?> testClass, String resourcePath) {
        try {
            return Files.readString(testResourcePath(testClass, resourcePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the UNIX path representation (forward slashes as separator) of a given {@link Path}.
     *
     * @param path path
     * @return String
     */
    public static String pathOf(Path path) {
        return path.toString().replace("\\", "/");
    }
}
