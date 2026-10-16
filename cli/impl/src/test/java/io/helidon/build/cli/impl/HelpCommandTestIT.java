/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
import io.helidon.build.common.test.utils.WorkDirExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@code helidon --help}.
 */
@ExtendWith(WorkDirExtension.class)
class HelpCommandTestIT {

    private final Path workDir;

    HelpCommandTestIT(Path workDir) {
        this.workDir = workDir;
    }

    @Test
    void testCliHelp() {
        String output = exec("help");
        assertThat(output, containsString("Helidon command line tool"));
    }

    @Test
    void testBuildCommandHelp() {
        testHelp("build");
    }

    @Test
    void testDevCommandHelp() {
        testHelp("dev");
    }

    @Test
    void testInfoCommandHelp() {
        testHelp("info");
    }

    @Test
    void testInitCommandHelp() {
        testHelp("init");
    }

    @Test
    void testVersionCommandHelp() {
        testHelp("version");
    }

    private void testHelp(String command) {
        String cmdHelp = exec(command, "--help");
        String helpCmd = exec("help", command);
        assertThat(helpCmd, is(cmdHelp));
        assertThat(helpCmd, containsString("Usage: helidon " + command));
    }

    private String exec(String... args) {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(workDir)
                .args(args)
                .start()) {

            monitor.await();
            return monitor.output();
        }
    }
}
