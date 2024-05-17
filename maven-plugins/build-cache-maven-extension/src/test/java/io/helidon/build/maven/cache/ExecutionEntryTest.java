/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ExecutionEntry}.
 */
class ExecutionEntryTest {

    @Test
    void testMatch() {
        ExecutionEntry entry = new ExecutionEntry("groupId1", "artifactId1", "1.0", "goal1", "id1", null);
        assertThat(entry.match(null, null), is(true));
        assertThat(entry.match(List.of(), null), is(true));
        assertThat(entry.match(null, List.of("*")), is(false));
        assertThat(entry.match(List.of("foo*"), null), is(false));
        assertThat(entry.match(List.of("groupId1:artifactId1:1.0:goal1@id1"), null), is(true));
        assertThat(entry.match(List.of("*@id1"), null), is(true));
        assertThat(entry.match(List.of("*:*:*:*@*"), null), is(true));
        assertThat(entry.match(List.of("groupId1*"), null), is(true));
        assertThat(entry.match(null, List.of("groupId1:artifactId1:1.0:goal1@id1")), is(false));
        assertThat(entry.match(null, List.of("*@id1")), is(false));
        assertThat(entry.match(null, List.of("*:*:*:*@*")), is(false));
        assertThat(entry.match(null, List.of("groupId1*")), is(false));
    }
}
