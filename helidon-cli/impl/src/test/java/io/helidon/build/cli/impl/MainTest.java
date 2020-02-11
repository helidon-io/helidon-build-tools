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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * CLI main class test.
 */
public class MainTest {

    static final String CLI_USAGE = resourceAsString("cli-usage.txt");
    static final String BUILD_CMD_HELP = resourceAsString("build-cmd-help.txt");
    static final String DEV_CMD_HELP = resourceAsString("dev-cmd-help.txt");
    static final String FEATURES_CMD_HELP = resourceAsString("features-cmd-help.txt");
    static final String INFO_CMD_HELP = resourceAsString("info-cmd-help.txt");
    static final String INIT_CMD_HELP = resourceAsString("init-cmd-help.txt");
    static final String VERSION_CMD_HELP = resourceAsString("version-cmd-help.txt");

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
        InputStream is = MainTest.class.getResourceAsStream(name);
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
        cmdArgs.addAll(List.of(javaPath(), "-cp", System.getProperty("java.class.path"), Main.class.getName()));
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

    @Test
    public void testUsage() throws IOException, InterruptedException {
        ExecResult res = exec("--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(CLI_USAGE)));

        res = exec("help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(CLI_USAGE)));

        res = exec();
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(CLI_USAGE)));
    }

    @Test
    public void testHelp() throws IOException, InterruptedException {
        ExecResult res = exec("build" ,"--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(BUILD_CMD_HELP)));

        res = exec("help" ,"build");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(BUILD_CMD_HELP)));

        res = exec("dev" ,"--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(DEV_CMD_HELP)));

        res = exec("help" ,"dev");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(DEV_CMD_HELP)));

        res = exec("features" ,"--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(FEATURES_CMD_HELP)));

        res = exec("help", "features");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(FEATURES_CMD_HELP)));

        res = exec("info" ,"--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(INFO_CMD_HELP)));

        res = exec("help", "info");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(INFO_CMD_HELP)));

        res = exec("init" ,"--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(INIT_CMD_HELP)));

        res = exec("help", "init");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(INIT_CMD_HELP)));

        res = exec("version" ,"--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(VERSION_CMD_HELP)));

        res = exec("help", "version");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(VERSION_CMD_HELP)));
    }
}
