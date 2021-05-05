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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.SubstitutionVariables.NotFoundAction.AsIs;
import static io.helidon.build.common.SubstitutionVariables.NotFoundAction.Collapse;
import static io.helidon.build.common.SubstitutionVariables.NotFoundAction.Fail;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *  Unit test for class {@link SubstitutionVariables}.
 */
class SubstitutionVariablesTest {

    @Test
    public void missingClosingBrace() {
        SubstitutionVariables variables = SubstitutionVariables.of(emptyMap());
        assertThrows(IllegalArgumentException.class, () -> variables.resolve("roses are ${color and violets are blue"));
    }

    @Test
    public void notFoundCollapseStrategy() {
        SubstitutionVariables variables = SubstitutionVariables.of(Collapse, emptyMap());
        assertThat(variables, is(not(nullValue())));
        assertThat(variables.resolve("roses are ${color}"), is("roses are "));
    }

    @Test
    public void notFoundAsIsStrategy() {
        SubstitutionVariables variables = SubstitutionVariables.of(AsIs, emptyMap());
        assertThat(variables, is(not(nullValue())));
        assertThat(variables.resolve("roses are ${color}"), is("roses are ${color}"));
    }

    @Test
    public void notFoundFailStrategy() {
        SubstitutionVariables variables = SubstitutionVariables.of(Fail, emptyMap());
        assertThat(variables, is(not(nullValue())));
        assertThrows(IllegalArgumentException.class, () -> variables.resolve("roses are ${color}"));
    }

    @Test
    public void maxRecursion() {
        assertMaxRecursion(AsIs);
        assertMaxRecursion(Collapse);
        assertMaxRecursion(Fail);
    }

    @Test
    public void singleSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of("color", "blue"));
        assertThat(variables.resolve("the sky is ${color}"), is("the sky is blue"));
    }

    @Test
    public void escapedSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of("color", "red"));
        assertThat(variables.resolve("vars start with '\\${'"), is("vars start with '${'"));

        assertThat(variables.resolve("vars start with '\\${' and roses are ${color}"),
                   is("vars start with '${' and roses are red"));
    }

    @Test
    public void multipleSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of("color", "blue",
                                                                          "weather", "clear"));
        assertThat(variables.resolve("the sky is ${color} and ${weather}"), is("the sky is blue and clear"));
    }

    @Test
    public void recursiveSubstitution() {
        SubstitutionVariables v1 = SubstitutionVariables.of(Map.of("skyDescription", "${skyConditions}",
                                                                   "skyConditions", "${skyColor} and ${weather}",
                                                                   "skyColor", "grey",
                                                                   "weather", "stormy"));
        assertThat(v1.resolve("the sky is ${skyDescription}"), is("the sky is grey and stormy"));

        SubstitutionVariables v2 = SubstitutionVariables.of(AsIs, Map.of("skyDescription", "${skyConditions}",
                                                                         "skyConditions", "${skyColor} and ${weather}"));

        assertThat(v2.resolve("the sky is ${skyDescription}"), is("the sky is ${skyColor} and ${weather}"));

        SubstitutionVariables v3 = SubstitutionVariables.of(Collapse, Map.of("skyDescription", "${skyConditions}",
                                                                             "skyConditions", "${skyColor} and ${weather}"));

        assertThat(v3.resolve("the sky is ${skyDescription}"), is("the sky is  and "));
    }

    private static void assertMaxRecursion(final SubstitutionVariables.NotFoundAction action) {
        String value = "roses are ${color}";
        SubstitutionVariables v1 = SubstitutionVariables.of(action, Map.of("color", "${color}"));
        assertThrows(IllegalArgumentException.class, () -> v1.resolve(value));

        Map<String, String> vars = new HashMap<>();
        vars.put("color", "${c0}");
        IntStream.range(0, 32).forEach(index -> vars.put("c" + index, "${c" + (index + 1) + "}"));
        SubstitutionVariables v2 = SubstitutionVariables.of(action, vars);
        assertThrows(IllegalArgumentException.class, () -> v2.resolve(value));
    }
}
