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
package io.helidon.build.common.test.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility that checks the the debug log level for tests.
 */
public abstract class TestLogLevel {

    /**
     * Indicate if the test log level is {@code debug}.
     *
     * @return {@code true} if {@code debug}, {@code false otherwise}
     */
    public static boolean isDebug() {
        return DebugLogLevel.ENABLED;
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
                                 .anyMatch(line -> line.endsWith("=LOGGING_LEVEL_DEBUG"))) {
                            return true;
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        } else {
            return "debug".equals(System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        }
        return false;
    }
}
