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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for git commands.
 */
public final class GitCommands {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d\\d\\d\\d)-\\d\\d-\\d\\d");

    private GitCommands() {
    }

    /**
     * Get the root of the git repository.
     *
     * @param checkPath path within a repository
     * @return root of the repository
     */
    static Path repositoryRoot(Path checkPath) {
        // git rev-parse --show-toplevel
        // /a/b/c/repo-root
        Path path = Paths.get(singleLine(checkPath,
                                         "find git repository root",
                                         "rev-parse",
                                         "--show-toplevel"));

        if (!Files.exists(path)) {
            throw new EnforcerException("Git root path does not exist: " + path.toAbsolutePath());
        }

        return path;
    }

    /**
     * Get all files in a directory tracked by the repository.
     * This may return files that were locally deleted.
     *
     * @param root root of the repository
     * @param checkPath path to check (within the repository)
     * @return list of files tracked within the checkPath
     */
    static Set<FileRequest> gitTracked(Path root, Path checkPath) {

        Process process = startProcess(root, "log", "--pretty=%cd", "--date=short", "--name-status", "--reverse");

        Map<String, Integer> fileToYear = new HashMap<>();
        int lastYear = -1;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Matcher matcher = DATE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    lastYear = Integer.parseInt(matcher.group(1));
                    continue;
                }
                if (lastYear == -1) {
                    throw new EnforcerException("Failed to parse output, expecting date to be present");
                }

                GitOperation gitOp = gitOp(line);
                String relativePath = stripGitOp(line);

                switch (gitOp) {
                case DELETE:
                    fileToYear.remove(relativePath);
                    break;
                case RENAME:
                    rename(fileToYear, relativePath, lastYear);
                    break;
                case COPY:
                    copy(fileToYear, relativePath, lastYear);
                    break;
                case ADD:
                case MODIFY:
                default:
                    // any other type modifies the timestamp
                    fileToYear.put(relativePath, lastYear);
                    // do nothing, it is already in
                    break;
                }
            }
        } catch (IOException e) {
            throw new EnforcerException("Failed to read output when getting tracked files", e);
        }

        waitFor(process, String.valueOf(List.of()));

        List<FileRequest> files = new LinkedList<>();
        fileToYear.forEach((found, year) -> files.add(FileRequest.create(root, found, String.valueOf(year))));

        // changed files are relative to the root, we need to filter our changed files outside of check path
        String prefix = root.relativize(checkPath).toString();

        return files.stream()
                .filter(fr -> fr.relativePath().startsWith(prefix))
                .collect(Collectors.toSet());
    }

    /**
     * Files locally modified.
     * Ignores deleted files - the user of these methods must consider files that no longer exist to be locally deleted.
     *
     * @param root the root directory
     * @param checkPath directory of interest
     * @param currentYear current year
     * @return set of files in the directory of interest that were locally modified
     */
    static Set<FileRequest> locallyModified(Path root, Path checkPath, String currentYear) {

        Set<String> changedFiles = multiLine(root,
                                             "get locally modified files",
                                             s -> !s.startsWith("??") && !s.startsWith("D"),
                                             s -> {
                                                 int firstSpace = s.indexOf(' ');
                                                 if (firstSpace < 0) {
                                                     throw new EnforcerException("Cannot parse status line: " + s);
                                                 }
                                                 String fileLocation = s.substring(firstSpace).trim().replace('\\', '/');
                                                 // moved files
                                                 if (s.startsWith("R")) {
                                                     // renamed
                                                     // relative/path.java -> new/relative/path.java
                                                     int arrow = fileLocation.indexOf(" -> ");
                                                     if (arrow < 0) {
                                                         throw new EnforcerException("Cannot parse renamed status line. " + s);
                                                     }
                                                     fileLocation = fileLocation.substring(arrow + 4).trim();
                                                 }
                                                 return fileLocation;
                                             },
                                             "status", "-s");

        // changed files are relative to the root, we need to filter our changed files outside of check path
        String prefix = root.relativize(checkPath).toString();

        return changedFiles.stream()
                .filter(relativePath -> relativePath.startsWith(prefix))
                .map(relativePath -> FileRequest.create(root, relativePath, currentYear))
                .collect(Collectors.toSet());
    }

    private static void waitFor(Process process, String output) {
        try {
            int i = process.waitFor();
            if (i != 0) {
                throw new EnforcerException("Failed to find locally modified files, git exit code: " + i + ", process output: "
                                                    + output);
            }
        } catch (InterruptedException ex) {
            throw new EnforcerException("Git process was interrupted", ex);
        }

    }

    private static Process startProcess(Path path, String... command) {
        String[] allCommands = new String[command.length + 1];
        allCommands[0] = "git";
        System.arraycopy(command, 0, allCommands, 1, command.length);

        ProcessBuilder processBuilder = new ProcessBuilder(allCommands)
                .redirectErrorStream(true)
                .directory(path.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new EnforcerException("Failed to start git process to find modified year", e);
        }

        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
        return process;
    }

    private static Set<String> multiLine(Path path,
                                         String message,
                                         Predicate<String> predicate,
                                         Function<String, String> mapper,
                                         String... command) {
        Process process = startProcess(path, command);

        List<String> fullOutput = new LinkedList<>();
        Set<String> wantedOutput = new LinkedHashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fullOutput.add(line);
                String trimmed = line.trim();
                if (predicate.test(trimmed)) {
                    wantedOutput.add(mapper.apply(trimmed));
                }
            }
        } catch (IOException e) {
            throw new EnforcerException("Failed to read output to " + message, e);
        }

        waitFor(process, String.valueOf(fullOutput));

        return wantedOutput;
    }

    private static String singleLine(Path path, String message, String... command) {
        Process process = startProcess(path, command);

        List<String> output = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();

            // this should be exactly one line
            if (line == null) {
                throw new EnforcerException(
                        "Failed to " + message + ", no output for command"
                                + " \"" + command + "\" in directory "
                                + path.toAbsolutePath());
            } else {
                String trimmed = line.trim();

                output.add(trimmed);
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
                if (trimmed.isBlank() || output.size() != 1) {
                    throw new EnforcerException(
                            "Failed to " + message + ", expected single line output for command"
                                    + " \"" + command + "\" in directory "
                                    + path.toAbsolutePath() + ", but got: " + output);
                }
                return trimmed;
            }
        } catch (IOException e) {
            throw new EnforcerException("Failed to read output of git process", e);
        } finally {
            waitFor(process, String.valueOf(output));
        }
    }

    private static void rename(Map<String, Integer> fileToYear, String relativePath, int lastYear) {
        int i = relativePath.indexOf('\t');
        if (i < 0) {
            throw new EnforcerException("Failed to process renamed for path: " + relativePath);
        }
        String first = relativePath.substring(0, i).trim();
        String second = relativePath.substring(i + 1).trim();
        fileToYear.remove(first);
        fileToYear.put(second, lastYear);
    }

    private static void copy(Map<String, Integer> fileToYear, String relativePath, int lastYear) {
        int i = relativePath.indexOf('\t');
        if (i < 0) {
            throw new EnforcerException("Failed to process copy for path: " + relativePath);
        }
        String second = relativePath.substring(i + 1).trim();
        fileToYear.put(second, lastYear);
    }

    private static String stripGitOp(String line) {
        int index = line.indexOf('\t');
        if (index < 1) {
            throw new EnforcerException("Failed to strip git op for line " + line);
        }
        return line.substring(index).trim();
    }

    private static GitOperation gitOp(String line) {
        if (line.startsWith("A\t")) {
            return GitOperation.ADD;
        }
        if (line.startsWith("D\t")) {
            return GitOperation.DELETE;
        }
        if (line.startsWith("M\t")) {
            return GitOperation.MODIFY;
        }
        if (line.startsWith("C\t")) {
            return GitOperation.COPY;
        }
        if (line.startsWith("R")) {
            return GitOperation.RENAME;
        }

        throw new EnforcerException("Could not parse line " + line);
    }

    private enum GitOperation {
        ADD,
        DELETE,
        MODIFY,
        RENAME,
        COPY
    }
}
