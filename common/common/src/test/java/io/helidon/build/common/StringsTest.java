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
package io.helidon.build.common;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link Strings}.
 */
public class StringsTest {

    @Test
    void testStripLeading() {
        assertThat(Strings.stripLeading("", '/'), is(""));
        assertThat(Strings.stripLeading("//////", '/'), is(""));
        assertThat(Strings.stripLeading("/./////", '/'), is("./////"));
        assertThat(Strings.stripLeading("/////./", '/'), is("./"));
        assertThat(Strings.stripLeading("foo", '/'), is("foo"));
    }

    @Test
    void testReplaceAll() {
        assertThat(Strings.replaceAll(null), is(nullValue()));
        assertThat(Strings.replaceAll("my name"), is("my name"));
        assertThat(Strings.replaceAll("my name", "\\s+", "."), is("my.name"));
        assertThat(Strings.replaceAll(" my name ", "\\s+", "."), is(".my.name."));
        assertThat(Strings.replaceAll("my name", "\\s+", ".", "my", "your"), is("your.name"));
        assertThat(Strings.replaceAll("my name", "my", "your", "your", "my"), is("my name"));
    }

    @Test
    void testReplaceWhitespaces() {
        assertThat(Strings.replaceWhitespaces(null, ""), is(nullValue()));
        assertThat(Strings.replaceWhitespaces(" ", ""), is(""));
        assertThat(Strings.replaceWhitespaces(" a  b ", ""), is("ab"));
    }
}
