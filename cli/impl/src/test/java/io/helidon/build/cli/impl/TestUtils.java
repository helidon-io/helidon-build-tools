/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.common.Log.Level;
import io.helidon.build.common.LogFormatter;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;

import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.ansi.AnsiTextStyle.strip;
import static java.io.File.pathSeparator;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLI test utils.
 */
class TestUtils {

    private static final String EOL = System.lineSeparator();

    private TestUtils() {
    }

    /**
     * Get the path of the java executable to use.
     *
     * @return absolute path, never {@code null}
     */
    static String javaPath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File javaHomeBin = new File(javaHome, "bin");
            if (javaHomeBin.exists() && javaHomeBin.isDirectory()) {
                File javaBin = new File(javaHomeBin, "java");
                if (javaBin.exists() && javaBin.isFile()) {
                    return javaBin.getAbsolutePath();
                }
            }
        }
        return "java";
    }

    /**
     * Get the content of the given resource on the class-path.
     *
     * @param name resource name
     * @return resource content
     */
    @SuppressWarnings("unused")
    static String resourceAsString(String name) {
        InputStream is = TestUtils.class.getResourceAsStream(name);
        if (is != null) {
            try {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        } else {
            return null;
        }
    }

    /**
     * Execute the given CLI command
     *
     * @param args command arguments
     * @return output stripped of ANSI colors
     * @throws Exception if an error occurs
     */
    static String exec(String... args) throws Exception {
        return execWithDirAndInput(null, null, args);
    }

    /**
     * Execute the given CLI command.
     *
     * @param wd    working directory
     * @param input file to pass as standard input to the process
     * @param args  command arguments
     * @return output stripped of ANSI colors
     * @throws Exception if an error occurs
     */
    static String execWithDirAndInput(File wd, File input, String... args) throws Exception {
        List<String> cmdArgs = new ArrayList<>(List.of(javaPath(), "-cp", "\"" + classpath() + "\""));
        String version = System.getProperty(HELIDON_VERSION_PROPERTY);
        if (version != null) {
            cmdArgs.add("-D" + HELIDON_VERSION_PROPERTY + "=" + version);
        }
        cmdArgs.add(Helidon.class.getName());
        cmdArgs.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);

        if (wd != null) {
            pb.directory(wd);
        }

        ProcessMonitor monitor = ProcessMonitor.builder()
                                               .processBuilder(pb)
                                               .stdIn(input)
                                               .stdOut(PrintStreams.apply(STDOUT, LogFormatter.of(Level.INFO)))
                                               .stdErr(PrintStreams.apply(STDERR, LogFormatter.of(Level.ERROR)))
                                               .capture(true)
                                               .build()
                                               .start()
                                               .waitForCompletion(10, TimeUnit.MINUTES);
        String output = String.join(EOL, monitor.output());
        return strip(output);
    }

    /**
     * The java class-path to use.
     *
     * @return class-path string
     */
    static String classpath() {
        List<String> classPath = new LinkedList<>();
        classPath.addAll(Arrays.asList(System.getProperty("surefire.test.class.path", "").split(pathSeparator)));
        classPath.addAll(Arrays.asList(System.getProperty("java.class.path", "").split(pathSeparator)));
        classPath.addAll(Arrays.asList(System.getProperty("jdk.module.path", "").split(pathSeparator)));
        return classPath.stream()
                        .distinct()
                        .collect(Collectors.joining(pathSeparator));
    }

    /**
     * Assert that the root directory of the Java package exists under {@code src/main/java} of the given project directory.
     *
     * @param projectPath project directory
     * @param packageName Java package name
     */
    static void assertPackageExists(Path projectPath, String packageName) {
        assertTrue(Files.exists(projectPath));
        Path path = projectPath.resolve("src/main/java");
        assertTrue(Files.exists(path));
        String[] dirs = packageName.split("\\.");
        for (String dir : dirs) {
            path = path.resolve(dir);
            assertTrue(Files.exists(path));
        }
    }

    /**
     * Get the Helidon test version.
     *
     * @return version
     * @throws IllegalStateException if the {@code helidon.test.version} is system property not found
     */
    static String helidonTestVersion() {
        String version = System.getProperty("helidon.test.version");
        if (version == null) {
            throw new IllegalStateException("Unable to resolve helidon.test.version from test.properties");
        }
        return version;
    }
}
