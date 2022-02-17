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
import io.helidon.build.common.ansi.AnsiTextStyle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
        logged.install();
    }

    @AfterEach
    public void afterEach() {
        logged.uninstall();
    }

    static void invoke(String... args) {
        List<String> arguments = new ArrayList<>();
        arguments.add(EMBEDDED_ARG);
        arguments.addAll(Arrays.asList(args));
        Helidon.main(arguments.toArray(new String[0]));
    }

    @Test
    void testValidCommand() {
        invoke("version");
        assertThat(logged.lines(), is(not(empty())));
        assertThat(logged.countLinesContainingAll("build."), is(3));
    }

    @Test
    void testUnknownCommand() {
        Error e = assertThrows(Error.class, () -> invoke("foo"));
        assertThat(e.getMessage(), containsString("'foo' is not a valid command"));
        List<String> lines = logged.lines();
        assertThat(lines, is(not(empty())));
        assertThat(lines.get(0), is(""));
        assertThat(AnsiTextStyle.strip(lines.get(1)), is("error: 'foo' is not a valid command."));
        assertThat(AnsiTextStyle.strip(lines.get(2)), is("See 'helidon --help' for more information"));
    }
}
