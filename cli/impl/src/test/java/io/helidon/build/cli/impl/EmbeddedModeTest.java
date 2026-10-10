/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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
import java.util.stream.Collectors;

import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogWriter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private static final LogRecorder LOG_RECORDER = LogRecorder.create();
    private LogLevel previousLogLevel;

    @BeforeAll
    static void beforeAllTests() {
        System.setProperty("jansi.passthrough", "true");
        LogWriter.addRecorder(LOG_RECORDER);
    }

    @AfterAll
    static void afterAllTests() {
        LogWriter.removeRecorder(LOG_RECORDER);
    }

    @BeforeEach
    void beforeEachTest() {
        previousLogLevel = LogLevel.get();
        LogLevel.set(LogLevel.INFO);
        LOG_RECORDER.clear();
    }

    @AfterEach
    void afterEachTest() {
        LOG_RECORDER.clear();
        LogLevel.set(previousLogLevel);
    }

    @Test
    void testValidCommand() {
        Helidon.execute("version");
        List<String> lines = loggedLines();
        assertThat(lines, is(not(empty())));
        assertThat(countLinesContainingAll("build."), is(3));
        assertThat(lines.stream().filter(line -> line.contains("build.")).findFirst().orElseThrow(), isStyled());
    }

    @Test
    void testUnknownCommand() {
        Error e = assertThrows(Error.class, () -> Helidon.execute("foo"));
        assertThat(e.getMessage(), isNotStyled());
        assertThat(e.getMessage(), startsWith("'foo' is not a valid command."));
        List<String> lines = loggedLines();
        assertThat(lines.size(), is(2));
        assertThat(lines.get(0), isStyled());
        assertThat(lines.get(1), isStyled());
        assertThat(lines.get(0), equalToIgnoringStyle("error: 'foo' is not a valid command."));
        assertThat(lines.get(1), equalToIgnoringStyle("See 'helidon --help' for more information"));
    }

    @Test
    void testInvalidCommand() {
        Error e = assertThrows(Error.class, () -> Helidon.execute("*"));
        assertThat(e.getMessage(), isNotStyled());
        assertThat(e.getMessage(), is("Invalid command name: *"));
        List<String> lines = loggedLines();
        assertThat(lines.size(), is(1));
        assertThat(lines.get(0), isStyled());
        assertThat(lines.get(0), equalToIgnoringStyle("error: Invalid command name: *"));
    }

    @Test
    void testStyledExceptionThrown() {
        Error e = assertThrows(Error.class, () -> Helidon.execute("init", "--version", "99.99", "--url", "file:///jabberwocky"));
        assertThat(e.getMessage(), isNotStyled());
        List<String> lines = loggedLines();
        assertThat(lines, is(not(empty())));
        assertThat(lines.get(lines.size() - 1), isStyled());
        assertThat(lines.get(lines.size() - 1), equalToIgnoringStyle("error: " + e.getMessage()));
    }

    @Test
    void testFormatStringInProperties() {
        System.setProperty("format", "%s");
        Helidon.execute("info", "--verbose");
        long lineCount = loggedLines().stream()
                .distinct()
                .filter(l -> l.contains("%s"))
                .filter(l -> l.contains("format") || l.contains("formatEnv"))
                .count();
        assertThat(lineCount, is(2L));
        System.clearProperty("format");
    }

    private static List<String> loggedLines() {
        return LOG_RECORDER.entries()
                .stream()
                .flatMap(String::lines)
                .collect(Collectors.toList());
    }

    private int countLinesContainingAll(String... fragments) {
        return (int) LOG_RECORDER.entries()
                .stream()
                .flatMap(String::lines)
                .filter(msg -> containsAll(msg, fragments))
                .count();
    }

    private static boolean containsAll(String msg, String... fragments) {
        for (String fragment : fragments) {
            if (!msg.contains(fragment)) {
                return false;
            }
        }
        return true;
    }
}
