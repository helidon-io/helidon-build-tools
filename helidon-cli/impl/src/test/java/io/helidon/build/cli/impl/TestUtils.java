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

import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.util.Log;
import io.helidon.build.util.RequirementsFailure;

import static io.helidon.build.cli.impl.BaseCommand.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.test.StripAnsi.stripAnsi;
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

    static class ExecResult {

        final int code;
        final String output;

        ExecResult(int code, String output) {
            this.code = code;
            this.output = output;
        }
    }

    static ExecResult exec(String... args) throws IOException, InterruptedException {
        return execWithDirAndInput(null, null, args);
    }

    static ExecResult execWithDirAndInput(File wd, File input, String... args) throws IOException, InterruptedException {
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
        if (input != null) {
            pb.redirectInput(input);
        }
        Process p = pb.redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timeout waiting for process");
        }
        System.out.println(output);
        return new ExecResult(p.exitValue(), stripAnsi(output));
    }

    static void assertPackageExist(Path projectPath, String packageName) {
        assertTrue(Files.exists(projectPath));
        Path path = projectPath.resolve("src/main/java");
        assertTrue(Files.exists(path));
        String[] dirs = packageName.split("\\.");
        for (String dir : dirs) {
            path = path.resolve(dir);
            assertTrue(Files.exists(path));
        }
    }

    static boolean apptypeArchetypeFound(Flavor flavor, String helidonVersion, String apptype) {
        boolean found;
        try {
            AppTypeBrowser browser = new AppTypeBrowser(flavor, helidonVersion);
            found = browser.appTypes().contains(apptype);
            if (!found) {
                Log.warn("Unable to find archetype %s for flavor %s and version %s", apptype, flavor, helidonVersion);
            }
        } catch (RequirementsFailure e) {
            Log.warn("IGNORING '%s'. Fix this once we have a Helidon release with new archetypes!", e.getMessage());
            found = false;
        }
        return found;
    }
}
