/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.Objects;

/**
 * Archetype replace in {@link Transformation}.
 */
public class Replacement {

    private final String regex;
    private final String replacement;

    Replacement(String regex, String replacement) {
        this.regex = Objects.requireNonNull(regex, "regex is null");
        this.replacement = Objects.requireNonNull(replacement, "replacement is null");
    }

    /**
     * Get the source regular expression to match the section to be replaced.
     *
     * @return regular expression, never {@code null}
     */
    public String regex() {
        return regex;
    }

    /**
     * Get the replacement for the matches of the source regular expression.
     *
     * @return replacement, never {@code null}
     */
    public String replacement() {
        return replacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Replacement that = (Replacement) o;
        return regex.equals(that.regex)
                && replacement.equals(that.replacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regex, replacement);
    }

    @Override
    public String toString() {
        return "Replacement{"
                + "regex='" + regex + '\''
                + ", replacement='" + replacement + '\''
                + '}';
    }
}
