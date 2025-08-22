/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

import java.nio.file.Path;

import io.helidon.build.cli.impl.ProcessInvocation.Monitor;
import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@code helidon --help}.
 */
class HelpCommandTest {

    static final Path CWD = TestFiles.targetDir(InitCommandTest.class).resolve("help-command-test");

    @Test
    void testCliHelp() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(CWD)
                .args("help")
                .start()) {

            monitor.await();
            assertThat(monitor.output(), containsString("Helidon command line tool"));
        }
    }

    @Test
    void testBuildCommandHelp() {
        assertHelp("build");
    }

    @Test
    void testDevCommandHelp() {
        assertHelp("dev");
    }

    @Test
    void testInfoCommandHelp() {
        assertHelp("info");
    }

    @Test
    void testInitCommandHelp() {
        assertHelp("init");
    }

    @Test
    void testVersionCommandHelp() {
        assertHelp("version");
    }

    static void assertHelp(String command) {
        String cmdHelp = exec(command, "--help");
        String helpCmd = exec("help", command);
        assertThat(helpCmd, is(cmdHelp));
        assertThat(helpCmd, containsString("Usage: helidon " + command));
    }

    static String exec(String... args) {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(CWD)
                .args(args)
                .start()) {

            monitor.await();
            return monitor.output();
        }
    }
}
