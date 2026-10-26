/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
import java.util.concurrent.atomic.AtomicReference;
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
    void missingClosingBrace() {
        SubstitutionVariables variables = SubstitutionVariables.of(emptyMap());
        assertThrows(IllegalArgumentException.class, () -> variables.resolve("roses are ${color and violets are blue"));
        assertThrows(IllegalArgumentException.class, () -> variables.resolve("${outer:${inner}"));
    }

    @Test
    void notFoundCollapseStrategy() {
        SubstitutionVariables variables = SubstitutionVariables.of(Collapse, emptyMap());
        assertThat(variables, is(not(nullValue())));
        assertThat(variables.resolve("roses are ${color}"), is("roses are "));
    }

    @Test
    void notFoundAsIsStrategy() {
        SubstitutionVariables variables = SubstitutionVariables.of(AsIs, emptyMap());
        assertThat(variables, is(not(nullValue())));
        assertThat(variables.resolve("roses are ${color}"), is("roses are ${color}"));
        assertThat(variables.resolve("value=${unknown:${nested}}"), is("value=${unknown:${nested}}"));
    }

    @Test
    void notFoundFailStrategy() {
        SubstitutionVariables variables = SubstitutionVariables.of(Fail, emptyMap());
        assertThat(variables, is(not(nullValue())));
        assertThrows(IllegalArgumentException.class, () -> variables.resolve("roses are ${color}"));
    }

    @Test
    void maxRecursion() {
        assertMaxRecursion(AsIs);
        assertMaxRecursion(Collapse);
        assertMaxRecursion(Fail);
    }

    @Test
    void singleSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of("color", "blue"));
        assertThat(variables.resolve("the sky is ${color}"), is("the sky is blue"));
    }

    @Test
    void balancedNestedSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of(
                "outer:${inner}", "nested",
                "outer:{ordinary", "ordinary"));

        assertThat(variables.resolve("${outer:${inner}}"), is("nested"));
        assertThat(variables.resolve("${outer:{ordinary}"), is("ordinary"));
    }

    @Test
    void sourceReceivesCompleteRawExpression() {
        AtomicReference<String> actual = new AtomicReference<>();
        SubstitutionVariables variables = SubstitutionVariables.of(expression -> {
            actual.set(expression);
            return "resolved";
        });

        assertThat(variables.resolve("value=${path:C:/${workDir}/file}"), is("value=resolved"));
        assertThat(actual.get(), is("path:C:/${workDir}/file"));
    }

    @Test
    void handlerResolvesNestedSubstitution() {
        SubstitutionVariables nested = SubstitutionVariables.of(Map.of("root", "base"));
        SubstitutionVariables variables = SubstitutionVariables.of(expression -> {
            int separator = expression.indexOf(':');
            if (separator > 0 && expression.substring(0, separator).equals("path")) {
                return nested.resolve(expression.substring(separator + 1)).replace('/', '|');
            }
            return null;
        });

        assertThat(variables.resolve("${path:${root}/nested/file}"), is("base|nested|file"));
    }

    @Test
    void escapedSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of("color", "red"));
        assertThat(variables.resolve("\\${color}"), is("${color}"));
        assertThat(variables.resolve("vars start with '\\${'"), is("vars start with '${'"));

        assertThat(variables.resolve("vars start with '\\${' and roses are ${color}"),
                   is("vars start with '${' and roses are red"));
        assertThat(variables.resolve("literal \\${color} and resolved ${color}"),
                   is("literal ${color} and resolved red"));
    }

    @Test
    void multipleSubstitution() {
        SubstitutionVariables variables = SubstitutionVariables.of(Map.of("color", "blue",
                                                                          "weather", "clear"));
        assertThat(variables.resolve("the sky is ${color} and ${weather}"), is("the sky is blue and clear"));
    }

    @Test
    void recursiveSubstitution() {
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

    @Test
    void substitutionReturnedFromHandlerIsResolved() {
        SubstitutionVariables variables = SubstitutionVariables.of(expression -> {
            if (expression.equals("first")) {
                return "${second}";
            }
            if (expression.equals("second")) {
                return "resolved";
            }
            return null;
        });

        assertThat(variables.resolve("value=${first}"), is("value=resolved"));
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
