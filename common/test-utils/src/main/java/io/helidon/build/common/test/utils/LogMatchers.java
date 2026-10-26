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
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.build.common.SubstitutionVariables;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static io.helidon.build.common.Strings.normalizeNewLines;
import static io.helidon.build.common.SubstitutionVariables.NotFoundAction.AsIs;
import static java.util.Objects.requireNonNull;

/**
 * Hamcrest log matchers.
 */
public final class LogMatchers {

    private static final String DEFAULT_ELLIPSIS = "~...";
    private static final String DEFAULT_WILDCARD = "~.*";
    private static final String SINGLE_LINE_REGEX = "[^\\n]*";
    private static final Map<String, String> DEFAULT_PROPERTIES = Map.of("file.separator", File.separator);
    private static final SubstitutionVariables DEFAULT_PROPERTY_SUBSTITUTIONS =
            SubstitutionVariables.of(AsIs, DEFAULT_PROPERTIES);

    private LogMatchers() {
    }

    /**
     * Matches complete log output against the given template.
     *
     * @param expected expected output template
     * @return matcher
     */
    public static Matcher<String> matches(String expected) {
        return matches(DEFAULT_ELLIPSIS, DEFAULT_WILDCARD, LogMatchers::defaultSubstitution, expected);
    }

    /**
     * Matches complete log output against the given template using the given substitution handler.
     *
     * @param handler handler receiving the complete unresolved expression between <code>${</code> and <code>}</code>
     * @param expected expected output template
     * @return matcher
     */
    public static Matcher<String> matches(Function<String, String> handler, String expected) {
        return matches(DEFAULT_ELLIPSIS, DEFAULT_WILDCARD, handler, expected);
    }

    /**
     * Matches complete log output against the given template using the given tokens.
     *
     * @param ellipsis standalone-line token matching content across lines
     * @param wildcard inline token matching zero or more characters within a line
     * @param expected expected output template
     * @return matcher
     */
    public static Matcher<String> matches(String ellipsis, String wildcard, String expected) {
        return matches(ellipsis, wildcard, LogMatchers::defaultSubstitution, expected);
    }

    /**
     * Matches complete log output against the given template using the given tokens and substitution handler.
     *
     * @param ellipsis standalone-line token matching content across lines
     * @param wildcard inline token matching zero or more characters within a line
     * @param handler handler receiving the complete unresolved expression between <code>${</code> and <code>}</code>
     * @param expected expected output template
     * @return matcher
     */
    public static Matcher<String> matches(String ellipsis,
                                          String wildcard,
                                          Function<String, String> handler,
                                          String expected) {
        return new LogMatcher(ellipsis, wildcard, handler, expected);
    }

    private static String defaultSubstitution(String expr) {
        int separator = expr.indexOf(':');
        if (separator < 0 || !expr.substring(0, separator).equals("path")) {
            return null;
        }
        return DEFAULT_PROPERTY_SUBSTITUTIONS.resolve(expr.substring(separator + 1)).replace("/", File.separator);
    }

    private static final class LogMatcher extends TypeSafeMatcher<String> {

        private final String expected;
        private final String regex;

        private LogMatcher(String ellipsis, String wildcard, Function<String, String> handler, String expected) {
            this.expected = requireNonNull(expected);
            String ellipsisToken = requireNonNull(ellipsis);
            String wildcardToken = requireNonNull(wildcard);
            SubstitutionVariables substitutions =
                    SubstitutionVariables.of(AsIs, DEFAULT_PROPERTIES::get, requireNonNull(handler));
            if (ellipsisToken.isEmpty() || wildcardToken.isEmpty()) {
                throw new IllegalArgumentException("tokens must not be empty");
            }
            String wildcardRegex = Pattern.quote(wildcardToken);
            String regex = expected.lines()
                    .map(it -> {
                        if (it.strip().equals(ellipsisToken)) {
                            return ".*";
                        }
                        return Arrays.stream(it.split(wildcardRegex, -1))
                                .map(substitutions::resolve)
                                .map(Pattern::quote)
                                .collect(Collectors.joining(SINGLE_LINE_REGEX));
                    })
                    .collect(Collectors.joining("\n"));
            this.regex = expected.endsWith("\n") ? regex + "\n" : regex;
        }

        @Override
        protected boolean matchesSafely(String actual) {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
            return pattern.matcher(normalize(actual)).matches();
        }

        @Override
        protected void describeMismatchSafely(String actual, Description mismatchDescription) {
            mismatchDescription.appendText("was ").appendValue(normalize(actual));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expected);
        }

        private static String normalize(String value) {
            return normalizeNewLines(value).replace('\r', '\n');
        }
    }
}
