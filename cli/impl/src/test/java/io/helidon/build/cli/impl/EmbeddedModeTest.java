/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.helidon.build.common.CapturingLogWriter;
import io.helidon.build.common.Log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.TestUtils.containsStringIgnoringStyle;
import static io.helidon.build.cli.impl.TestUtils.equalToIgnoringStyle;
import static io.helidon.build.cli.impl.TestUtils.isNotStyled;
import static io.helidon.build.cli.impl.TestUtils.isStyled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for embedded mode.
 */
class EmbeddedModeTest {
    private static final String EMBEDDED_ARG = "-Dembedded.mode=true";

    private CapturingLogWriter logged;

    @BeforeEach
    public void beforeEach() {
        logged = CapturingLogWriter.create();
        logged.level(Log.Level.INFO);
        logged.install();
    }

    @AfterEach
    public void afterEach() {
        logged.uninstall();
    }

    static void invoke(String... args) {
        List<String> arguments = new ArrayList<>();
        arguments.add(EMBEDDED_ARG); // Must be first to handle invalid command names
        arguments.addAll(Arrays.asList(args));
        Helidon.main(arguments.toArray(new String[0]));
    }

    @Test
    void testValidCommand() {
        invoke("version");
        assertThat(logged.lines(), is(not(empty())));
        assertThat(logged.countLinesContainingAll("build."), is(3));
        assertThat(logged.lines().get(0), isStyled());
    }

    @Test
    void testUnknownCommand() {
        Error e = assertThrows(Error.class, () -> invoke("foo"));
        assertThat(e.getMessage(), isNotStyled());
        assertThat(e.getMessage(), startsWith("'foo' is not a valid command."));
        List<String> lines = logged.lines();
        assertThat(lines.size(), is(2));
        assertThat(lines.get(0), isStyled());
        assertThat(lines.get(1), isStyled());
        assertThat(lines.get(0), equalToIgnoringStyle("error: 'foo' is not a valid command."));
        assertThat(lines.get(1), equalToIgnoringStyle("See 'helidon --help' for more information"));
    }

    @Test
    void testInvalidCommand() {
        Error e = assertThrows(Error.class, () -> invoke("*"));
        assertThat(e.getMessage(), isNotStyled());
        assertThat(e.getMessage(), is("Invalid command name: *"));
        List<String> lines = logged.lines();
        assertThat(lines.size(), is(1));
        assertThat(lines.get(0), isStyled());
        assertThat(lines.get(0), equalToIgnoringStyle("error: Invalid command name: *"));
    }

    @Test
    void testStyledExceptionThrown() {
        Error e = assertThrows(Error.class, () -> invoke("init", "--version", "99.99", "--url", "file:///jabberwocky"));
        assertThat(e.getMessage(), isNotStyled());
        assertThat(e.getMessage(), is("Helidon version lookup failed."));
        List<String> lines = logged.lines();
        assertThat(lines.size(), is(3));
        assertThat(lines.get(0), isNotStyled());
        assertThat(lines.get(1), isStyled());
        assertThat(lines.get(2), isStyled());
        assertThat(lines.get(0), is("Updating metadata for Helidon version 99.99"));
        assertThat(lines.get(1), containsStringIgnoringStyle("cli-data.zip (No such file or directory)"));
        assertThat(lines.get(2), equalToIgnoringStyle("Helidon version lookup failed."));
    }
}
