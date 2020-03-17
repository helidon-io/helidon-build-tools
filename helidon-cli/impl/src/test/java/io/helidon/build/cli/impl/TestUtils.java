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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * CLI test utils.
 */
class TestUtils {

    static final String HELIDON_PROPERTIES = "helidon.properties";

    private static Properties cliConfig;

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
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.addAll(List.of(javaPath(), "-cp", "\"" + System.getProperty("java.class.path") + "\"", Main.class.getName()));
        for (String arg : args) {
            cmdArgs.add(arg);
        }
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        Process p = pb.redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timeout waiting for process");
        }
        return new ExecResult(p.exitValue(), output);
    }

    static Properties cliConfig() {
        if (cliConfig != null) {
            return cliConfig;
        }
        try {
            InputStream sourceStream = TestUtils.class.getResourceAsStream(HELIDON_PROPERTIES);
            try (InputStreamReader isr = new InputStreamReader(sourceStream)) {
                cliConfig = new Properties();
                cliConfig.load(isr);
                return cliConfig;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
