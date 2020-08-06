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
package io.helidon.build.cli.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;

import static io.helidon.build.cli.impl.BaseCommand.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.util.Style.strip;
import static io.helidon.build.util.Constants.EOL;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLI test utils.
 */
class TestUtils {

    private TestUtils() {
    }

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

    static String resourceAsString(String name) {
        InputStream is = TestUtils.class.getResourceAsStream(name);
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static String exec(String... args) throws Exception {
        return execWithDirAndInput(null, null, args);
    }

    static String execWithDirAndInput(File wd, File input, String... args) throws Exception {
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        List<String> cmdArgs = new ArrayList<>(List.of(javaPath(), "-cp", "\"" + classPath + "\""));
        String version = System.getProperty(HELIDON_VERSION_PROPERTY);
        if (version != null) {
            cmdArgs.add("-D" + HELIDON_VERSION_PROPERTY + "=" + version);
        }
        cmdArgs.add(Main.class.getName());
        cmdArgs.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);

        if (wd != null) {
            pb.directory(wd);
        }

        ProcessMonitor monitor = ProcessMonitor.builder()
                                               .processBuilder(pb)
                                               .stdIn(input)
                                               .stdOut(Log::info)
                                               .stdErr(Log::error)
                                               .capture(true)
                                               .build()
                                               .start()
                                               .waitForCompletion(120, TimeUnit.SECONDS);
        String output = String.join(EOL, monitor.output());
        return strip(output);
    }

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
}
