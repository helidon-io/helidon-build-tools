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
package io.helidon.build.maven.archetype;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ScriptCompiler;
import io.helidon.build.archetype.engine.v2.Variations;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariationPlanTest {

    @Test
    void testLoadRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> VariationPlan.load((Path) null));
    }

    @Test
    void testLoadRejectsMissingPath() {
        Path missing = resource("plans.xml").resolveSibling("missing-plans.xml");
        assertThrows(IllegalStateException.class, () -> VariationPlan.load(missing));
    }

    @Test
    void testLoad() {
        List<VariationPlan> plans = VariationPlan.load(resource("plans.xml"));

        assertThat(plans, hasSize(3));
        assertThat(plans.get(0).id(), is("red"));
        assertThat(plans.get(0).externalValues(), is(Map.of("color", "red")));
        assertThat(plans.get(0).externalDefaults(), is(Map.of("artifactId", "demo-red")));
        assertThat(plans.get(0).filters(), hasSize(1));
        assertThat(plans.get(0).filters().get(0).variables(), containsInAnyOrder("color", "docker"));
        assertThat(plans.get(1).filters(), hasSize(1));
        assertThat(plans.get(1).filters().get(0).variables(), containsInAnyOrder("docker"));
        assertThat(plans.get(2).externalValues(), is(Map.of("color", "red", "docker", "false")));
        assertThat(plans.get(2).externalDefaults(), is(Map.of("artifactId", "demo-red")));
    }

    @Test
    void testLoadRejectsFragmentWithoutId() {
        XMLElement root = xml("""
                <plans xmlns="https://helidon.io/archetype-plans/1.0">
                    <fragment>
                        <values>
                            <color>red</color>
                        </values>
                    </fragment>
                    <plan id="red"/>
                </plans>
                """);

        assertThrows(IllegalStateException.class, () -> VariationPlan.load(root));
    }

    @Test
    void testLoadRejectsDuplicateFragments() {
        XMLElement root = xml("""
                <plans xmlns="https://helidon.io/archetype-plans/1.0">
                    <fragment id="color/red"/>
                    <fragment id="color/red"/>
                    <plan id="red"/>
                </plans>
                """);

        assertThrows(IllegalStateException.class, () -> VariationPlan.load(root));
    }

    @Test
    void testLoadRejectsUnknownFragmentReference() {
        XMLElement root = xml("""
                <plans xmlns="https://helidon.io/archetype-plans/1.0">
                    <plan id="red" extends="missing"/>
                </plans>
                """);

        assertThrows(IllegalStateException.class, () -> VariationPlan.load(root));
    }

    @Test
    void testLoadRejectsCircularFragmentInheritance() {
        XMLElement root = xml("""
                <plans xmlns="https://helidon.io/archetype-plans/1.0">
                    <fragment id="a" extends="b"/>
                    <fragment id="b" extends="a"/>
                    <plan id="red" extends="a"/>
                </plans>
                """);

        assertThrows(IllegalStateException.class, () -> VariationPlan.load(root));
    }

    @Test
    void testLoadPreservesInheritedValueOrder() {
        XMLElement root = xml("""
                <plans xmlns="https://helidon.io/archetype-plans/1.0">
                    <fragment id="base">
                        <values>
                            <one>1</one>
                            <two>2</two>
                        </values>
                    </fragment>
                    <fragment id="extra">
                        <values>
                            <three>3</three>
                            <four>4</four>
                        </values>
                    </fragment>
                    <plan id="ordered" extends="base, extra">
                        <values>
                            <five>5</five>
                        </values>
                    </plan>
                </plans>
                """);

        List<VariationPlan> plans = VariationPlan.load(root);

        assertThat(plans, hasSize(1));
        assertThat(plans.get(0).externalValues().keySet(), contains("one", "two", "three", "four", "five"));
    }

    @Test
    void testMergePlanVariations() {
        Path cwd = resource("script").toAbsolutePath().normalize();
        ScriptCompiler compiler = new ScriptCompiler(() -> cwd.resolve("main.xml"), cwd);
        List<VariationPlan> plans = VariationPlan.load(resource("plans.xml"));

        List<Variations> computed = new ArrayList<>();
        for (VariationPlan plan : plans) {
            computed.add(Variations.compute(
                    compiler,
                    plan.filters(),
                    plan.externalValues(),
                    plan.externalDefaults(),
                    Long.MAX_VALUE));
        }
        Variations actual = Variations.union(computed);
        Variations expected = Variations.of(
                Variations.entry(Map.of("color", "blue", "docker", "false")),
                Variations.entry(Map.of("color", "red", "docker", "false")));

        assertThat(actual, is(expected));
    }

    private static Path resource(String path) {
        return testResourcePath(VariationPlanTest.class, "variation-plans/" + path);
    }

    private static XMLElement xml(String value) {
        try {
            return XMLElement.parse(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
