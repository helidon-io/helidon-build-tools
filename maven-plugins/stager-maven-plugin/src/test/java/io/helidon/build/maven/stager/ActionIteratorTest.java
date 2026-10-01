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
package io.helidon.build.maven.stager;

import java.util.List;
import java.util.Map;

import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests {@link ActionIterator}.
 */
class ActionIteratorTest {

    @Test
    void testCartesianProduct() {
        var iterator = new ActionIterator(new Variables(XMLElement.read("""
                <variables>
                    <variable name="letter"><value>a</value><value>b</value></variable>
                    <variable name="number"><value>1</value><value>2</value><value>3</value></variable>
                </variables>
                """)));
        var iterations = iterator.forVariables(Map.of());
        assertThat(iterations, is(List.of(
                Map.of("letter", "a", "number", "1"),
                Map.of("letter", "a", "number", "2"),
                Map.of("letter", "a", "number", "3"),
                Map.of("letter", "b", "number", "1"),
                Map.of("letter", "b", "number", "2"),
                Map.of("letter", "b", "number", "3"))));
    }

    @Test
    void testInheritedVariablesPrecedence() {
        var iterator = new ActionIterator(new Variables(XMLElement.read("""
                <variables>
                    <variable name="version"><value>iterator</value></variable>
                    <variable name="coordinates"><value version="later" classifier="tests"/></variable>
                </variables>
                """)));

        var iterations = iterator.forVariables(Map.of("version", "inherited", "repository", "central"));
        assertThat(iterations, is(List.of(
                Map.of("version", "later", "repository", "central", "classifier", "tests"))));
    }

    @Test
    void testEmptyVariable() {
        var iterator = new ActionIterator(new Variables(XMLElement.read("""
                <variables>
                    <variable name="version"/>
                </variables>
                """)));

        var iterations = iterator.forVariables(Map.of("inherited", "value"));
        assertThat(iterations, is(empty()));
    }

    @Test
    void testZeroDimensions() {
        var iterator = new ActionIterator(new Variables(XMLElement.read("<variables/>")));
        var inherited = Map.of("inherited", "value");

        var iterations = iterator.forVariables(inherited);
        assertThat(iterations, is(List.of(inherited)));
        assertNotSame(inherited, iterations.get(0));
    }
}
