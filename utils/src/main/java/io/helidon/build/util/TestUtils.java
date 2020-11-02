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
package io.helidon.build.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Common test utilities.
 */
public class TestUtils {

    private TestUtils() {
    }

    /**
     * Test if the debug log is enabled.
     * @return {@code true} if enabled, {@code false otherwise}
     */
    public static boolean isDebugLogEnabled() {
        return DebugLogLevel.ENABLED;
    }

    /**
     * Get the UNIX path representation (forward slashes as separator) of a given {@link Path}.
     * @param path path
     * @return String
     */
    public static String pathOf(Path path) {
        return path.toString().replace("\\", "/");
    }

    /**
     * Create a {@link Path} for a new directory under the given parent directory that does not already exist.
     * Appends {@code -$i} to the given name until a non existing entry is found.
     *
     * @param parentDirectory parent directory where to create the new directory
     * @param name   the name of the entry to create
     * @return Path
     */
    public static Path uniqueDir(Path parentDirectory, String name) {
        Path dir = parentDirectory.resolve(name);
        int i = 1;
        while (Files.exists(dir)) {
            dir = parentDirectory.resolve(name + "-" + i);
            i++;
        }
        return dir;
    }

    private static class DebugLogLevel {
        static final boolean ENABLED = resolveDebugLogLevel();
    }

    private static boolean resolveDebugLogLevel() {
        String[] cmd = System.getProperty("sun.java.command", "").split(" ");
        if (cmd.length > 0 && "org.apache.maven.surefire.booter.ForkedBooter".equals(cmd[0])) {
            if (cmd.length > 2) {
                Path dir = Paths.get(cmd[1]);
                for (int i = 2; i < cmd.length; i++) {
                    Path p = dir.resolve(cmd[i]);
                    if (!Files.isRegularFile(p)) {
                        continue;
                    }
                    try {
                        if (Files.readAllLines(p).stream()
                                .filter(line -> line.endsWith("=LOGGING_LEVEL_DEBUG"))
                                .findAny()
                                .isPresent()) {
                            return true;
                        }
                    } catch (IOException ex) {
                    }
                }
            }
        } else {
            if ("debug".equals(System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"))) {
                return true;
            }
        }
        return false;
    }
}
