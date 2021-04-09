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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// git utility class
final class Git {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d\\d\\d\\d)-\\d\\d-\\d\\d");

    private Git() {
    }

    static Path repositoryRoot(Path checkPath) {
        ProcessBuilder processBuilder = new ProcessBuilder("git",
                                                           "rev-parse",
                                                           "--show-toplevel")
                .redirectErrorStream(true)
                .directory(checkPath.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new CopyrightException("Failed to start git process to find modified year", e);
        }

        // git rev-parse --show-toplevel
        // /a/b/c/repo-root

        // no input from our side
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }

        List<String> output = new LinkedList<>();
        Path path;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();

            // this should be exactly one line
            if (line == null) {
                throw new CopyrightException(
                        "Failed to find git repository root, no output for command"
                                + " \"git rev-parse --show-toplevel\" in directory "
                                + checkPath.toAbsolutePath());
            } else {
                String trimmed = line.trim();

                path = Paths.get(trimmed);
                if (!Files.exists(path)) {
                    output.add(line);
                    while ((line = reader.readLine()) != null) {
                        output.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new CopyrightException("Failed to read output of git process", e);
        }

        try {
            int i = process.waitFor();
            if (i != 0) {
                throw new CopyrightException("Failed to find locally modified files, git exit code: " + i + ", process output: "
                                                     + output);
            }
        } catch (InterruptedException ex) {
            throw new CopyrightException("Git process was interrupted", ex);
        }

        if (output.isEmpty()) {
            return path;
        }
        throw new CopyrightException("Git root directory " + path.toAbsolutePath() + " does not exist. Full output: " + output);
    }

    static Optional<String> yearModified(Path root, String relativePath, List<String> messages) {
        ProcessBuilder processBuilder = new ProcessBuilder("git",
                                                           "log",
                                                           "-1",
                                                           "--pretty=%cd",
                                                           "--date=short",
                                                           relativePath)
                .redirectErrorStream(true)
                .directory(root.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new CopyrightException("Failed to start git process to find modified year", e);
        }

        // git log -1 --pretty="%cd" --date=short pom.xml
        // 2021-03-30

        // no input from our side
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }

        String year = null;

        List<String> output = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();

            // this should be exactly one line
            if (line == null) {
                messages.add("Failed to read timestamp from git for " + relativePath);
            } else {
                String trimmed = line.trim();
                Matcher matcher = DATE_PATTERN.matcher(trimmed);
                if (matcher.matches()) {
                    year = matcher.group(1);
                } else {
                    messages.add("Failed to read timestamp from git for " + relativePath + ", got " + line);
                    output.add(line);
                    while ((line = reader.readLine()) != null) {
                        output.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new CopyrightException("Failed to read output of git process", e);
        }

        try {
            int i = process.waitFor();
            if (i != 0) {
                throw new CopyrightException("Failed to find locally modified files, git exit code: " + i + ", process output: "
                                                     + output);
            }
        } catch (InterruptedException ex) {
            throw new CopyrightException("Git process was interrupted", ex);
        }

        return Optional.ofNullable(year);
    }

    static Set<String> gitTracked(Path root, Path checkPath) {
        ProcessBuilder processBuilder = new ProcessBuilder("git",
                                                           "ls-tree",
                                                           "-r",
                                                           "HEAD",
                                                           "--name-only")
                .redirectErrorStream(true)
                .directory(root.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new CopyrightException("Failed to start git process to get modified files", e);
        }

        // no input from our side
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }

        Set<String> changedFiles = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) {
                    // commit line (would contain id and message, but now it is empty thanks to formatting
                    continue;
                }
                changedFiles.add(trimmed.replace('\\', '/'));
            }
        } catch (IOException e) {
            throw new CopyrightException("Failed to read output of git process", e);
        }

        try {
            int i = process.waitFor();
            if (i != 0) {
                throw new CopyrightException("Failed to read modified files, git exit code: "
                                                     + i + ", process output: " + changedFiles);
            }
        } catch (InterruptedException ex) {
            throw new CopyrightException("Process to get modified files was interrupted", ex);
        }

        // changed files are relative to the root, we need to filter our changed files outside of check path
        String prefix = root.relativize(checkPath).toString();

        return changedFiles.stream()
                .filter(relativePath -> relativePath.startsWith(prefix))
                .collect(Collectors.toSet());
    }

    /**
     * Files modified in commits from a branch.
     *
     * @param root the root directory
     * @param checkPath directory of interest
     * @param branchName name of master branch
     * @return set of files in the directory of interest that were modified since the master branch
     */
    static Set<String> gitModified(Path root, Path checkPath, String branchName) {
        ProcessBuilder processBuilder = new ProcessBuilder("git",
                                                           "log",
                                                           "--no-merges",
                                                           "--pretty=",
                                                           "--name-only",
                                                           branchName + "..")
                .redirectErrorStream(true)
                .directory(root.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new CopyrightException("Failed to start git process to get modified files", e);
        }

        // no input from our side
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }

        Set<String> changedFiles = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) {
                    // commit line (would contain id and message, but now it is empty thanks to formatting
                    continue;
                }
                changedFiles.add(trimmed.replace('\\', '/'));
            }
        } catch (IOException e) {
            throw new CopyrightException("Failed to read output of git process", e);
        }

        try {
            int i = process.waitFor();
            if (i != 0) {
                throw new CopyrightException("Failed to read modified files, git exit code: "
                                                     + i + ", process output: " + changedFiles);
            }
        } catch (InterruptedException ex) {
            throw new CopyrightException("Process to get modified files was interrupted", ex);
        }

        // changed files are relative to the root, we need to filter our changed files outside of check path
        String prefix = root.relativize(checkPath).toString();

        return changedFiles.stream()
                .filter(relativePath -> relativePath.startsWith(prefix))
                .collect(Collectors.toSet());
    }

    static Set<String> locallyModified(Path root, Path checkPath) {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "status", "-s")
                .redirectErrorStream(true)
                .directory(root.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new CopyrightException("Failed to start git process to find locally modified files", e);
        }

        // no input from our side
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }

        Set<String> changedFiles = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("AM")) {
                    changedFiles.add(trimmed.substring(2).trim().replace('\\', '/'));
                } else if (trimmed.startsWith("M") || trimmed.startsWith("A")) {
                    changedFiles.add(trimmed.substring(1).trim().replace('\\', '/'));
                }
            }
        } catch (IOException e) {
            throw new CopyrightException("Failed to read output of git command", e);
        }

        try {
            int i = process.waitFor();
            if (i != 0) {
                throw new CopyrightException("Failed to find locally modified files, git exit code: " + i + ", process output: "
                                                     + changedFiles);
            }
        } catch (InterruptedException ex) {
            throw new CopyrightException("Git process was interrupted", ex);
        }

        // changed files are relative to the root, we need to filter our changed files outside of check path
        String prefix = root.relativize(checkPath).toString();

        return changedFiles.stream()
                .filter(relativePath -> relativePath.startsWith(prefix))
                .collect(Collectors.toSet());
    }
}
