/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.cli.harness;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test {@link CommandMatcher}.
 */
class CommandMatcherTest {

    @Test
    void testClosestMatch() {
        List<String> target = List.of("dev");
        assertThat(CommandMatcher.match("ddev", target), is("dev"));
        assertThat(CommandMatcher.match("deev", target), is("dev"));
        assertThat(CommandMatcher.match("devv", target), is("dev"));
        assertThat(CommandMatcher.match("devv", target), is("dev"));
        assertThat(CommandMatcher.match("dv", target), is("dev"));
        assertThat(CommandMatcher.match("de", target), is("dev"));
        assertThat(CommandMatcher.match("ev", target), is("dev"));
        assertThat(CommandMatcher.match("d", target), is("dev"));
        assertThat(CommandMatcher.match("devel", target), is("dev"));
        assertThat(CommandMatcher.match("ved", target), is("dev"));
        assertThat(CommandMatcher.match("evd", target), is("dev"));
        assertThat(CommandMatcher.match("e", target), is(nullValue()));
        assertThat(CommandMatcher.match("vedel", target), is("dev"));
        assertThat(CommandMatcher.match("foobar", target), is(nullValue()));

        target = List.of("features");
        assertThat(CommandMatcher.match("f", target), is(nullValue()));
        assertThat(CommandMatcher.match("foo", target), is(nullValue()));
        assertThat(CommandMatcher.match("fe", target), is("features"));
        assertThat(CommandMatcher.match("fea", target), is("features"));
        assertThat(CommandMatcher.match("eat", target), is(nullValue()));

        target = List.of("pull", "push");
        assertThat(CommandMatcher.match("pul", target), is("pull"));
        assertThat(CommandMatcher.match("pus", target), is("push"));
        assertThat(CommandMatcher.match("pu", target), is("pull"));
        assertThat(CommandMatcher.match("ull", target), is("pull"));
        assertThat(CommandMatcher.match("ush", target), is("push"));
        assertThat(CommandMatcher.match("psh", target), is("push"));
        assertThat(CommandMatcher.match("pll", target), is("pull"));
        assertThat(CommandMatcher.match("pxxl", target), is("pull"));
        assertThat(CommandMatcher.match("pxxh", target), is("push"));
    }
}
