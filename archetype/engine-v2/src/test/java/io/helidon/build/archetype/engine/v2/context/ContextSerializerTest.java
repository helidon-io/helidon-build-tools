/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2.context;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;

/**
 * Tests {@link ContextSerializer}.
 */
class ContextSerializerTest {

    @Test
    void testSerialize() {
        Map<String, String> expectedResult = Map.of(
                "foo", "foo",
                "foo1", "",
                "foo2", "some_var_default",
                "foo3", "bar1_value");
        Context context = Context.builder()
                                 .externalDefaults(Map.of(
                                         "some_var", "some_var_default",
                                         "bar1", "bar1_default_value"))
                                 .externalValues(Map.of(
                                         "foo", "foo",
                                         "bar", "${foo}",
                                         "foo1", "${non_exist_var}",
                                         "foo2", "${some_var}",
                                         "bar1", "bar1_value",
                                         "foo3", "${bar1}"))
                                 .build();

        Map<String, String> result = ContextSerializer.serialize(context,
                edge -> edge.node().path().startsWith("foo"),
                Function.identity(),
                ",");

        assertThat(result.entrySet(), everyItem(isIn(expectedResult.entrySet())));
    }
}
