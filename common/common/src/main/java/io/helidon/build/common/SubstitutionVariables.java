/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Provides substitution for strings containing one or more <code>${key}</code> blocks, using one or more variable sources.
 * Literal '${' sequences can be included by backslash escaping.
 */
public class SubstitutionVariables {
    private static final String SUBSTITUTION_PREFIX = "${";
    private static final char SUBSTITUTION_SUFFIX = '}';
    private static final int MAX_RECURSION_DEPTH = 32;
    private static final char ESCAPE_CHAR = '\\';

    private final NotFoundAction notFoundAction;
    private final List<Function<String, String>> sources;

    /**
     * What to do if no source contains a value for the substitution key.
     */
    public enum NotFoundAction {


        /**
         * Throw an {@link IllegalArgumentException}.
         */
        Fail,

        /**
         * Use the value as is.
         */
        AsIs,

        /**
         * Replace {@code "${key}"} with an empty string.
         */
        Collapse
    }

    /**
     * Constructor.
     *
     * @param notFoundAction What to do if a substitution value is not found.
     * @param variableSources One or more variable sources.
     */
    @SafeVarargs
    private SubstitutionVariables(final NotFoundAction notFoundAction, final Function<String, String>... variableSources) {
        if (variableSources.length > 0) {
            this.notFoundAction = notFoundAction;
            this.sources = List.of(variableSources);
        } else {
            throw new IllegalArgumentException("At least one variable source required");
        }
    }

    /**
     * Returns an instance using the {@link NotFoundAction#Fail} action with the given map as the variable source.
     *
     * @param source The source map.
     * @return The instance.
     */
    public static SubstitutionVariables of(final Map<String, String> source) {
        return of(NotFoundAction.Fail, source);
    }

    /**
     * Returns an instance using the given action with the given map as the variable source.
     *
     * @param notFoundAction What to do if a substitution value is not found.
     * @param source The source map.
     * @return The instance.
     */
    public static SubstitutionVariables of(final NotFoundAction notFoundAction, final Map<String, String> source) {
        return new SubstitutionVariables(notFoundAction, source::get);
    }

    /**
     * Returns an instance using the {@link NotFoundAction#Fail} action with the given variable sources.
     *
     * @param sources The sources.
     * @return The instance.
     */
    @SafeVarargs
    public static SubstitutionVariables of(final Function<String, String>... sources) {
        return of(NotFoundAction.Fail, sources);
    }

    /**
     * Returns an instance using the given action with the given variable sources.
     *
     * @param notFoundAction What to do if a substitution value is not found.
     * @param sources The sources.
     * @return The instance.
     */
    @SafeVarargs
    public static SubstitutionVariables of(final NotFoundAction notFoundAction, final Function<String, String>... sources) {
        return new SubstitutionVariables(notFoundAction, sources);
    }

    /**
     * Returns a variable source using {@link #systemPropertyOrEnvVar(String)}.
     *
     * @return The source.
     */
    public static Function<String, String> systemPropertyOrEnvVarSource() {
        return SubstitutionVariables::systemPropertyOrEnvVar;
    }

    /**
     * Returns a system property or environment variable, with the properties taking precedence. If not found in either
     * location, one more attempt is made to look it up as an environment variable by mapping the name to normal env var
     * syntax: converted to uppercase with {@code "."} replaced by "{@code "_"}.
     *
     * @param propertyName The property name.
     * @return The value or {@code null} if not found.
     */
    public static String systemPropertyOrEnvVar(final String propertyName) {
        String result = System.getProperty(requireNonNull(propertyName));
        if (result == null) {
            result = System.getenv(propertyName);
            if (result == null) {
                result = System.getenv(propertyName.replace(".", "_").toUpperCase());
            }
        }
        return result;
    }

    /**
     * Recursively resolves the given value if it contains one or more substitution variables in {@code ${key}} format, or returns
     * the value if not.
     *
     * @param value The value.
     * @return The value or the substituted value, or {@code null} if substitution value not found and
     * configured with {@link NotFoundAction#Collapse}.
     * @throws IllegalArgumentException if substitution value not found and configured with
     * {@link NotFoundAction#Fail} or if a closing '}' is missing.
     */
    public String resolve(final String value) {
        return resolve(0, value, value, 0);
    }

    private String resolve(final int startIndex, final String value, final String originalValue, final int depth) {
        if (depth < MAX_RECURSION_DEPTH) {
            final int start = value.indexOf(SUBSTITUTION_PREFIX, startIndex);
            if (start >= 0) {
                if (start > 1 && value.charAt(start - 1) == ESCAPE_CHAR) {
                    final String unescapedValue = value.substring(0, start - 1) + value.substring(start);
                    return resolve(start + 1, unescapedValue, originalValue, depth + 1);
                } else {
                    final int end = value.indexOf(SUBSTITUTION_SUFFIX, start);
                    if (end >= 0) {
                        final String key = value.substring(start + 2, end);
                        String substitute = substitutionValueFor(key);
                        if (substitute == null) {
                            switch (notFoundAction) {
                                case Collapse:
                                    substitute = "";
                                    break;
                                case AsIs:
                                    return resolve(end, value, originalValue, depth + 1);
                                default:
                                    throw new IllegalArgumentException("Substitution not found for \"" + key + "\" in \""
                                                                       + value + "\"");
                            }
                        }
                        final String prefix = value.substring(0, start);
                        final String suffix = value.substring(end + 1);
                        return resolve(startIndex, prefix + substitute + suffix, originalValue, depth + 1);
                    } else {
                        throw new IllegalArgumentException("Closing '}' missing in \"" + value + "\"");
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Max recursion (" + MAX_RECURSION_DEPTH + ") depth reached "
                                               + "for \"" + originalValue + "\"");
        }
        return value;
    }

    private String substitutionValueFor(final String key) {
        for (final Function<String, String> source : sources) {
            final String result = source.apply(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
