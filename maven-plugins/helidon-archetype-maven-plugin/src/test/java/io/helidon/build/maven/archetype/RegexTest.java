/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import io.helidon.build.archetype.engine.v2.Node;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.helidon.build.archetype.engine.v2.Nodes.regex;
import static io.helidon.build.archetype.engine.v2.Nodes.validation;
import static io.helidon.build.archetype.engine.v2.Nodes.validations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link Regex}.
 */
class RegexTest {

    @Test
    void testCompatibleSingleRegex() {
        Node validations = validations(
                validation("validation", "description", regex("/abc/")));

        List<String> errors = Regex.validate(validations);
        assertThat(errors.size(), is(0));
    }

    @Test
    void testCompatibleMultiRegex() {
        Node validations = validations(
                validation("validation", "description",
                        regex("/abc/"),
                        regex("/\\([^\\)]+\\)/g"),
                        regex("/^(?=(?:.*[A-Z]){2})(?=(?:.*[a-z]){2})(?=.*\\d).{5,15}$/")),
                validation("validation1", "description1",
                        regex("/a/"),
                        regex("/b/")));

        List<String> errors = Regex.validate(validations);
        assertThat(errors.size(), is(0));
    }

    @Test
    void testIncompatibleSingleRegex() {
        Node validations = validations(
                validation("validation", "description", regex("[abc]")));

        List<String> errors = Regex.validate(validations);
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(errorMessage("[abc]", "validation")));
    }

    @Test
    void testIncompatibleMultiRegex() {
        Node validations = validations(
                validation("validation", "description",
                        regex("[abc]"),
                        regex("foo"),
                        regex("/^(?=(?:.*[A-Z]){2})(?=(?:.*[a-z]){2})(?=.*\\d).{5,15}$/")),
                validation("validation1", "description1",
                        regex("/a/"),
                        regex("bar")));

        List<String> errors = Regex.validate(validations);
        assertThat(errors.size(), is(3));
        assertThat(errors.get(0), containsString(errorMessage("[abc]", "validation")));
        assertThat(errors.get(1), containsString(errorMessage("foo", "validation")));
        assertThat(errors.get(2), containsString(errorMessage("bar", "validation1")));
    }

    String errorMessage(String regex, String id) {
        return String.format("Regular expression '%s' at validation '%s' is not JavaScript compatible",
                regex, id);
    }
}
