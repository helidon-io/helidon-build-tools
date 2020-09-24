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

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.TestUtils.exec;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * {@code helidon help} test.
 */
public class HelpCommandTest {

    @Test
    void testCliHelp() throws Exception {
        String output = exec("help");
        assertThat(output, containsString("Helidon Project command line tool"));
    }

    @Test
    public void testBuildCommandHelp() throws Exception {
        assertCommandHelp("build");
    }

    @Test
    public void testDevCommandHelp() throws Exception {
        assertCommandHelp("dev");
    }

    @Test
    public void testInfoCommandHelp() throws Exception {
        assertCommandHelp("info");
    }

    @Test
    public void testInitCommandHelp() throws Exception {
        assertCommandHelp("init");
    }

    @Test
    public void testVersionCommandHelp() throws Exception {
        assertCommandHelp("version");
    }

    private static String assertCommandHelp(String command) throws Exception {
        String commandHelp = exec(command, "--help");
        String helpCommand = exec("help", command);
        assertThat(helpCommand, is(commandHelp));
        assertThat(helpCommand, containsString("Usage:\thelidon " + command));
        return helpCommand;
    }
}
