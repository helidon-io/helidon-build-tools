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
package io.helidon.build.common;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link PropertyEvaluator}.
 */
class PropertyEvaluatorTest {

    @Test
    public void testResolveProperties() {
        Map<String, String> props = Map.of("foo", "bar", "bar", "foo");
        assertThat(PropertyEvaluator.evaluate("${foo}", props), is("bar"));
        assertThat(PropertyEvaluator.evaluate("${xxx}", props), is(""));
        assertThat(PropertyEvaluator.evaluate("-${foo}-", props), is("-bar-"));
        assertThat(PropertyEvaluator.evaluate("$${foo}}", props), is("$bar}"));
        assertThat(PropertyEvaluator.evaluate("${foo}-${bar}", props), is("bar-foo"));
        assertThat(PropertyEvaluator.evaluate("foo", props), is("foo"));
        assertThat(PropertyEvaluator.evaluate("$foo", props), is("$foo"));
        assertThat(PropertyEvaluator.evaluate("${foo", props), is("${foo"));
        assertThat(PropertyEvaluator.evaluate("${ foo}", props), is(""));
        assertThat(PropertyEvaluator.evaluate("${foo }", props), is(""));
    }
}
