/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link LineReader}.
 */
class LineReaderTest {

    @Test
    void testDrainAfterLineAtBufferBoundary() {
        String first = "x".repeat(1023);
        List<String> lines = new ArrayList<>();
        LineReader reader = reader(first + "\nBUILD SUCCESS\n", lines);

        reader.drain();

        assertThat(lines, is(List.of(first, "BUILD SUCCESS")));
    }

    @Test
    void testDrainLineAcrossBuffers() {
        String first = "x".repeat(1019);
        List<String> lines = new ArrayList<>();
        LineReader reader = reader(first + "\nBUILD SUCCESS\n", lines);

        reader.drain();

        assertThat(lines, is(List.of(first, "BUILD SUCCESS")));
    }

    @Test
    void testDrainRemainingAfterTick() throws IOException {
        List<String> lines = new ArrayList<>();
        LineReader reader = reader("BUILD SUCCESS", lines);

        assertThat(reader.tick(), is(true));
        assertThat(lines.isEmpty(), is(true));
        reader.drain();

        assertThat(lines, is(List.of("BUILD SUCCESS")));
    }

    private static LineReader reader(String input, List<String> lines) {
        return new LineReader(new ByteArrayInputStream(input.getBytes(UTF_8)), lines::add, () -> { });
    }
}
