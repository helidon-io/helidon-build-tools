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
package io.helidon.build.common.test.utils;

import java.io.File;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.LogMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * Tests {@link LogMatchers}.
 */
class LogMatchersTest {

    @Test
    void testMatchesExactMultilineOutput() {
        String expected = "first line\nsecond line\n";

        assertThat(expected, matches(expected));
        assertThat("prefix\n" + expected, not(matches(expected)));
        assertThat(expected + "suffix", not(matches(expected)));
    }

    @Test
    void testDefaultTokensAsLiteralText() {
        String literal = "literal * and regex ^$.+?()[]{}|\\E";

        assertThat(literal, matches(literal));
        assertThat("literal value and regex ^$.+?()[]{}|\\E", not(matches(literal)));
        assertThat("\\*", matches("\\*"));
        assertThat("*", not(matches("\\*")));
    }

    @Test
    void testInlineWildcard() {
        Matcher<String> matcher = matches("start ~.* middle ~.* end");

        assertThat("start  middle  end", matcher);
        assertThat("start one middle two end", matcher);
        assertThat("start one\ntwo middle three end", not(matcher));
        assertThat("start one middle two end\n", matches("start ~.* middle ~.* end\n"));
    }

    @Test
    void testEllipsisOneOrMultipleLines() {
        Matcher<String> matcher = matches("start\n~...\nend\n");

        assertThat("start\nend\n", not(matcher));
        assertThat("start\n\nend\n", matcher);
        assertThat("start\none\nend\n", matcher);
        assertThat("start\none\ntwo\nend\n", matcher);
    }

    @Test
    void testInvalidEllipsis() {
        Matcher<String> matcher = matches("start\n  \t~...  \nend");

        assertThat("start\nend", not(matcher));
        assertThat("start\n\nend", matcher);
        assertThat("start\nanything\nend", matcher);
        assertThat("start\nprefix ~... suffix\nend", matches("start\nprefix ~... suffix\nend"));
        assertThat("start\nanything\nend", not(matches("start\nprefix ~... suffix\nend")));
    }

    @Test
    void testCustomTokens() {
        Matcher<String> matcher = matches("#...", "#.*", "start\n#...\nvalue=#.*; literals=~... and ~.*");

        assertThat("start\none\ntwo\nvalue=anything; literals=~... and ~.*", matcher);
        assertThat("start\nvalue=anything; literals=~... and ~.*", not(matcher));
    }

    @Test
    void testDefaultPathSubstitution() {
        assertThat("dir" + File.separator + "nested" + File.separator + "file",
                   matches("${path:dir/nested/file}"));
        assertThat("dir" + File.separator + "nested" + File.separator + "file",
                   matches("${path:dir${file.separator}nested/file}"));
        assertThat("dir" + File.separator + "." + File.separator + "nested" + File.separator + ".." + File.separator + "file",
                   matches("${path:dir/./nested/../file}"));
    }

    @Test
    void testFileSeparatorCompatibility() {
        assertThat("dir" + File.separator + "nested" + File.separator + "file",
                   matches("dir${file.separator}nested${file.separator}file"));
    }

    @Test
    void testUnknownAndEscapedParameterizedSubstitutions() {
        assertThat("value=${unknown:dir/file}", matches("value=${unknown:dir/file}"));
        assertThat("value=${path:dir/file}", matches("value=\\${path:dir/file}"));
        assertThat("value=\\${path:dir/file}", not(matches("value=\\${path:dir/file}")));
    }

    @Test
    void testCustomSubstitutionHandler() {
        assertThat("C:\\work\\file", matches(LogMatchersTest::windowsPath, "${path:C:/work/file}"));
        assertThat("one\\two | three\\four | " + File.separator,
                   matches(LogMatchersTest::windowsPath,
                           "${path:one/two} | ${path:three/four} | ${file.separator}"));
    }

    @Test
    void testCustomTokensAndSubstitutionHandler() {
        Matcher<String> matcher = matches("#...", "#.*", LogMatchersTest::windowsPath,
                                          "start\n#...\npath=${path:C:/work/}#.*");

        assertThat("start\none\npath=C:\\work\\file", matcher);
    }

    @Test
    void testNormalizeLineEndings() {
        assertThat("one\r\ntwo\r", matches("one\ntwo\n"));
        assertThat("one", not(matches("one\n")));
        assertThat("one\n", not(matches("one")));
    }

    private static String windowsPath(String expression) {
        int separator = expression.indexOf(':');
        if (separator < 0 || !expression.substring(0, separator).equals("path")) {
            return null;
        }
        return expression.substring(separator + 1).replace('/', '\\');
    }
}
