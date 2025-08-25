/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

import io.helidon.build.common.ansi.AnsiTextStyle;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * CLI test utils.
 */
class TestUtils {

    private TestUtils() {
    }

    /**
     * Get the Helidon test version.
     *
     * @return version
     * @throws IllegalStateException if the {@code helidon.test.version} is system property not found
     */
    static String helidonTestVersion() {
        String version = System.getProperty("helidon.test.version");
        if (version == null) {
            throw new IllegalStateException("Unable to resolve helidon.test.version from test.properties");
        }
        return version;
    }

    /**
     * Returns a matcher used to assert that the message equals an expected message ignoring Ansi characters.
     *
     * @return The matcher.
     */
    static Matcher<String> equalToIgnoringStyle(String expected) {
        return new TypeSafeMatcher<>() {
            private final String strippedExpected = AnsiTextStyle.strip(expected);

            @Override
            protected boolean matchesSafely(String s) {
                return AnsiTextStyle.strip(s).equals(strippedExpected);
            }

            @Override
            public void describeMismatchSafely(String item, Description mismatchDescription) {
                mismatchDescription.appendText("was \"").appendText(AnsiTextStyle.strip(item)).appendText("\"");
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(strippedExpected);
            }
        };
    }

    /**
     * Returns a matcher used to assert that the message contains Ansi characters.
     *
     * @return The matcher.
     */
    static Matcher<String> isStyled() {
        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(String s) {
                return AnsiTextStyle.isStyled(s);
            }

            @Override
            public void describeMismatchSafely(String item, Description mismatchDescription) {
                mismatchDescription.appendText("was not styled: \"").appendText(AnsiTextStyle.strip(item)).appendText("\"");
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("String is styled");
            }
        };
    }

    /**
     * Returns a matcher used to assert that the message does not contain Ansi characters.
     *
     * @return The matcher.
     */
    static Matcher<String> isNotStyled() {
        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(String s) {
                return !AnsiTextStyle.isStyled(s);
            }

            @Override
            public void describeMismatchSafely(String item, Description mismatchDescription) {
                mismatchDescription.appendText("was styled: \"").appendText(item).appendText("\"");
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("String is not styled");
            }
        };
    }
}
