/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.logging.Log;

/**
 * Path utility class.
 */
public class PathFinder {

    private static final String PATH_ENV_VAR = "PATH";
    private static final List<String> WINDOWS_EXECUTABLE_EXTENSIONS = List.of("exe", "bin", "bat", "cmd", "ps1");
    private static final boolean IS_WINDOWS = File.pathSeparatorChar != ':';
    private static final Predicate<Path> VALID_PATH = p -> Files.exists(p) && Files.isDirectory(p);
    private static final List<Path> PATH_ENTRIES =
            Optional.ofNullable(System.getenv(PATH_ENV_VAR))
                    .map(p -> Arrays.asList(p.split(File.pathSeparator)))
                    .stream()
                    .flatMap(Collection::stream)
                    .map(Path::of)
                    .filter(VALID_PATH)
                    .collect(Collectors.toList());

    private PathFinder() {
    }

    private static Stream<Path> entries(List<Optional<Path>> entries) {
        return entries.stream()
                .flatMap(Optional::stream)
                .filter(VALID_PATH);
    }

    private static Path findCmd(Path dir, String cmd) {
        Log.debug("Searching for cmd: %s in %s", cmd, dir);
        if (IS_WINDOWS) {
            return WINDOWS_EXECUTABLE_EXTENSIONS.stream()
                    .map((ext) -> dir.resolve(cmd + "." + ext))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .orElse(null);
        } else {
            Path cmdFile = dir.resolve(cmd);
            return Files.isRegularFile(cmdFile) ? cmdFile : null;
        }
    }

    /**
     * Find file in the given path.
     *
     * @param fileName file to be found
     * @param paths    paths to search in
     * @return path to the file
     */
    public static Optional<Path> find(String fileName, List<Path> paths) {
        return paths.stream()
                .flatMap(dir -> Optional.ofNullable(findCmd(dir, fileName)).stream())
                .findFirst();
    }

    /**
     * Find file in the given path.
     *
     * @param fileName  file to be found
     * @param overrides list of path to look for first
     * @param extras    additional path
     * @return path to the file
     */
    public static Optional<Path> find(String fileName, List<Optional<Path>> overrides, List<Optional<Path>> extras) {
        return Stream.of(entries(overrides), PATH_ENTRIES.stream(), entries(extras))
                .flatMap(Function.identity())
                .flatMap(dir -> Optional.ofNullable(findCmd(dir, fileName)).stream())
                .findFirst();
    }
}
