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

package io.helidon.build.maven.enforcer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.ansi.AnsiTextStyle;

/**
 * File system utilities that only throw runtime exceptions.
 */
public final class FileSystem {
    private FileSystem() {
    }

    /**
     * Get the size of the file.
     *
     * @param path file to get size of
     * @return size in bytes
     * @throws io.helidon.build.maven.enforcer.EnforcerException in case of I/O issues
     */
    public static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new EnforcerException("Failed to get size of file " + path.toAbsolutePath(), e);
        }
    }

    /**
     * Read all lines of a file.
     *
     * @param path file to read
     * @return list of lines
     * @throws io.helidon.build.maven.enforcer.EnforcerException in case of I/O problems
     */
    public static List<String> toLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EnforcerException("Failed to read lines of file " + path.toAbsolutePath(), e);
        }
    }

    /**
     * Write failures to a file.
     *
     * @param path path to write
     * @param firstLine first line (such as "ENFORCER OK")
     * @param failures list of failures, each is written with "[ERROR"] prefix
     * @throws io.helidon.build.maven.enforcer.EnforcerException in case of I/O problem
     */
    public static void write(Path path, String firstLine, Map<String, List<RuleFailure>> failures) {
        List<String> lines = new LinkedList<>();
        lines.add(firstLine);
        failures.forEach((rule, ruleFails) -> {
            lines.add(rule.toUpperCase() + ":");
            ruleFails.stream()
                    .map(FileSystem::toFileLine)
                    .forEach(lines::add);
        });

        try {
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new EnforcerException("Failed to write output file " + path.toAbsolutePath(), e);
        }
    }

    private static String toFileLine(RuleFailure failure) {
        return "[ERROR] " + failure.fr().relativePath() + ":" + failure.line() + ": " + AnsiTextStyle
                .strip(RichTextRenderer.render(failure.message()));
    }
}
