/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.util.List;

import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogRecorder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.TestUtils.equalToIgnoringStyle;
import static io.helidon.build.cli.impl.TestUtils.isNotStyled;
import static io.helidon.build.cli.impl.TestUtils.isStyled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for embedded mode.
 */
@SuppressWarnings("unchecked")
class EmbeddedModeTest {

    @BeforeAll
    static void beforeAllTests() {
        System.setProperty("jansi.passthrough", "true");
    }

    @Test
    void testValidCommand() {
        try (LogRecorder recorder = new LogRecorder(LogLevel.DEBUG).start()) {
            Helidon.execute("version");

            List<String> logEntries = recorder.entries();
            assertThat(logEntries, is(not(empty())));
            assertThat(logEntries.get(0), isStyled());
            assertThat(logEntries, hasItems(
                    containsString("build."),
                    containsString("build."),
                    containsString("build.")));
        }
    }

    @Test
    void testUnknownCommand() {
        try (LogRecorder recorder = new LogRecorder(LogLevel.DEBUG).start()) {
            Error e = assertThrows(Error.class, () -> Helidon.execute("foo"));

            assertThat(e.getMessage(), isNotStyled());
            assertThat(e.getMessage(), startsWith("'foo' is not a valid command."));

            List<String> logLines = recorder.lines();
            assertThat(logLines, contains(
                    allOf(isStyled(), equalToIgnoringStyle("error: 'foo' is not a valid command.")),
                    allOf(isStyled(), equalToIgnoringStyle("See 'helidon --help' for more information"))));
        }
    }

    @Test
    void testInvalidCommand() {
        try (LogRecorder recorder = new LogRecorder(LogLevel.DEBUG).start()) {
            Error e = assertThrows(Error.class, () -> Helidon.execute("*"));

            assertThat(e.getMessage(), isNotStyled());
            assertThat(e.getMessage(), is("Invalid command name: *"));

            List<String> logLines = recorder.lines();
            assertThat(logLines, contains(allOf(isStyled(), equalToIgnoringStyle("error: Invalid command name: *"))));
        }
    }

    @Test
    void testStyledExceptionThrown() {
        try (LogRecorder recorder = new LogRecorder(LogLevel.DEBUG).start()) {
            Error e = assertThrows(Error.class,
                    () -> Helidon.execute("init", "--version", "99.99", "--url", "file:///jabberwocky"));

            assertThat(e.getMessage(), isNotStyled());
            assertThat(e.getMessage(), is("Helidon version 99.99 not found."));

            List<String> logEntries = recorder.entries();

            assertThat(logEntries, is(not(empty())));
            for (int i = 0; i < logEntries.size() - 1; i++) {
                assertThat(logEntries.get(i), isStyled());
            }
            assertThat(logEntries.get(logEntries.size() - 1),
                    allOf(isStyled(), equalToIgnoringStyle("Helidon version 99.99 not found.")));
        }
    }

    @Test
    void testFormatStringInProperties() {
        try (LogRecorder recorder = new LogRecorder(LogLevel.DEBUG).start()) {
            System.setProperty("format", "%s");

            Helidon.execute("info", "--verbose");

            List<String> logEntries = recorder.entries();
            assertThat(logEntries, hasItems(
                    allOf(containsString("%s"), containsString("format"), containsString("formatEnv")),
                    allOf(containsString("%s"), containsString("format"), containsString("formatEnv"))));
        } finally {
            System.clearProperty("format");
        }
    }
}
