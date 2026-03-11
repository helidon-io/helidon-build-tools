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
package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link Variations}.
 */
class VariationsTest {

    @Test
    void testVariationsList1() {
        Variations expected = loadVariations("variations/expected/list1.xml");
        Variations actual = variations("variations", "list1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsList2() {
        Variations expected = loadVariations("variations/expected/list2.xml");
        Variations actual = variations("variations", "list2.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsList3() {
        Variations expected = loadVariations("variations/expected/list3.xml");
        Variations actual = variations("variations", "list3.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsEnum1() {
        Variations expected = loadVariations("variations/expected/enum1.xml");
        Variations actual = variations("variations", "enum1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsEnum2() {
        Variations expected = loadVariations("variations/expected/enum2.xml");
        Variations actual = variations("variations", "enum2.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsEnum3() {
        Variations expected = loadVariations("variations/expected/enum3.xml");
        Variations actual = variations("variations", "enum3.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsBoolean1() {
        Variations expected = loadVariations("variations/expected/boolean1.xml");
        Variations actual = variations("variations", "boolean1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsBoolean2() {
        Variations expected = loadVariations("variations/expected/boolean2.xml");
        Variations actual = variations("variations", "boolean2.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsBoolean3() {
        Variations expected = loadVariations("variations/expected/boolean3.xml");
        Variations actual = variations("variations", "boolean3.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsBoolean4() {
        Variations expected = loadVariations("variations/expected/boolean4.xml");
        Variations actual = variations("variations", "boolean4.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsText1() {
        Variations expected = Variations.of(Map.of("name", "Foo"), Set.of("name"));
        Variations actual = variations("variations", "text1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationEntriesText1() {
        Variations expected = Variations.of(Map.of("name", "Foo"), Set.of("name"));
        Variations actual = variations("variations", "text1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(), is(expected.toString()));
        assertThat(actual.exhaustive(), is(false));
        assertThat(actual.unboundedInputs(), contains("name"));
    }

    @Test
    void testVariationEntriesText1WithExternalValueAreExhaustive() {
        Variations expected = Variations.of(Map.of("name", "Bar"), Set.of());
        Variations actual = variations("variations", "text1.xml", List.of(), Map.of("name", "Bar"), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(), is(expected.toString()));
        assertThat(actual.exhaustive(), is(true));
        assertThat(actual.unboundedInputs(), is(Set.of()));
    }

    @Test
    void testVariationEntriesTextWithExternalDefaultRetainDependentBranches() {
        Variations expected = Variations.of(
                Variations.entry(Map.of("name", "Bar", "flag", "false"), Set.of("name")),
                Variations.entry(Map.of("name", "Bar", "flag", "true"), Set.of("name")));
        Variations actual = variations("variations", "text-external-default.xml", List.of(),
                Map.of(), Map.of("name", "Bar"), Long.MAX_VALUE);
        assertThat(actual, is(expected));
        assertThat(actual.exhaustive(), is(false));
        assertThat(actual.unboundedInputs(), contains("name"));
    }

    @Test
    void testVariationsText2() {
        Variations expected = Variations.of(Map.of("name", "<?>"), Set.of("name"));
        Variations actual = variations("variations", "text2.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationEntriesText2() {
        Variations expected = Variations.of(Map.of("name", "<?>"), Set.of("name"));
        Variations actual = variations("variations", "text2.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(), is(expected.toString()));
    }

    @Test
    void testVariationEntriesBoolean1AreExhaustive() {
        Variations actual = variations("variations", "boolean1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.exhaustive(), is(true));
        assertThat(actual.unboundedInputs(), is(Set.of()));
        assertThat(actual.stream().allMatch(Variations.Entry::exhaustive), is(true));
    }

    @Test
    void testVariationsOfCreatesExhaustiveEntry() {
        Variations actual = Variations.of(Map.of("name", "Foo"), Set.of());
        assertThat(actual.iterator().next().exhaustive(), is(true));
        assertThat(actual.toString(), is(Variations.of(Map.of("name", "Foo"), Set.of()).toString()));
    }

    @Test
    void testVariationEntryFactoryCreatesExhaustiveEntry() {
        Variations.Entry actual = Variations.entry(Map.of("name", "Foo"));
        assertThat(actual.exhaustive(), is(true));
        assertThat(actual, is(Variations.of(Map.of("name", "Foo"), Set.of()).iterator().next()));
    }

    @Test
    void testVariationEntryToStringWithSeparator() {
        Variations.Entry actual = Variations.of(Map.of("name", "Foo"), Set.of("name")).iterator().next();
        assertThat(actual.toString(" "), is("name=Foo unbounded=[name]"));
        assertThat(actual.toString(false, " "), is("name=Foo"));
    }

    @Test
    void testVariationsToStringWithSeparator() {
        Variations actual = variations("variations", "boolean1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(" | "), is("colors=false | colors=true"));
    }

    @Test
    void testVariationsUnionMergesEquivalentEntries() {
        Variations actual = Variations.union(List.of(
                Variations.of(
                        Variations.entry(Map.of("name", "Foo"), Set.of("name")),
                        Variations.entry(Map.of("name", "Bar"), Set.of())),
                Variations.of(
                        Variations.entry(Map.of("name", "Foo"), Set.of()),
                        Variations.entry(Map.of("name", "Baz"), Set.of()))));

        Variations expected = Variations.of(
                Variations.entry(Map.of("name", "Foo"), Set.of("name")),
                Variations.entry(Map.of("name", "Bar"), Set.of()),
                Variations.entry(Map.of("name", "Baz"), Set.of()));

        assertThat(actual, is(expected));
        assertThat(actual.unboundedInputs(), contains("name"));
    }

    @Test
    void testVariationsUnionRetainsDistinctResolvedValues() {
        Variations actual = Variations.union(List.of(
                Variations.of(Variations.entry(Map.of("name", "Foo", "preset", "red"), Set.of())),
                Variations.of(Variations.entry(Map.of("name", "Foo", "preset", "blue"), Set.of()))));

        Variations expected = Variations.of(
                Variations.entry(Map.of("name", "Foo", "preset", "red"), Set.of()),
                Variations.entry(Map.of("name", "Foo", "preset", "blue"), Set.of()));

        assertThat(actual, is(expected));
    }

    @Test
    void testVariationsSubstitutions() {
        Variations expected = loadVariations("variations/expected/substitutions.xml");
        Variations actual = variations("variations", "substitutions.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsConditionals() {
        Variations expected = loadVariations("variations/expected/conditionals.xml");
        Variations actual = variations("variations", "conditionals.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsNestedConditionalOptionPruning() {
        Variations expected = loadVariations("variations/expected/nested-conditional-option.xml");
        Variations actual = variations("variations", "nested-conditional-option.xml",
                List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsPruneInactiveBranchBeforePresetValidation() {
        Variations expected = Variations.of(Map.of("app-type", "oci", "flavor", "mp"), Set.of());
        Variations actual = variations("variations", "branch-pruning.xml",
                List.of(), Map.of("flavor", "mp", "app-type", "oci"), Map.of(), Long.MAX_VALUE);
        assertThat(actual, is(expected));
    }

    @Test
    void testVariationsJoinOrder1() {
        Variations expected = loadVariations("variations/expected/join-order1.xml");
        Variations actual = variations("variations", "join-order1.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    @Test
    void testVariationsFailWhenProjectedCountExceedsMax() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> variations("variations", "boolean1.xml", List.of(), Map.of(), Map.of(), 1));
        assertThat(ex.getMessage(),
                is("Projected variation count 2 exceeds the configured limit of 1"));
    }

    @Test
    void testVariationsE2e() {
        Variations actual = variations("e2e", "main.xml", List.of(), Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.size(), is(65604));
    }

    @Test
    void testVariationsFilters() {
        Variations expected = loadVariations("variations/expected/filtered.xml");
        List<Expression> filters = filters("variations/filters.xml");
        Variations actual = variations("e2e", "main.xml", filters, Map.of(), Map.of(), Long.MAX_VALUE);
        assertThat(actual.toString(false), is(expected.toString(false)));
    }

    static Variations variations(String path,
                                 String entrypoint,
                                 List<Expression> filters,
                                 Map<String, String> externalValues,
                                 Map<String, String> externalDefaults,
                                 long max) {

        Path targetDir = targetDir(VariationsTest.class);
        try (FileSystem fs = VirtualFileSystem.create(targetDir.resolve("test-classes"))) {
            Path cwd = fs.getPath(path);
            Path source = cwd.resolve(entrypoint).toAbsolutePath().normalize();
            ScriptCompiler compiler = new ScriptCompiler(() -> source, cwd);
            return Variations.compute(compiler, filters, externalValues, externalDefaults, max);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    static List<Expression> filters(String path) {
        List<Expression> excludes = new ArrayList<>();
        XMLElement root = loadXml(path);
        for (XMLElement elt : root.traverse(it -> it.name().equals("exclude"))) {
            Expression exclude = Expression.TRUE;
            for (XMLElement n = elt; n.parent() != null; n = n.parent()) {
                exclude = exclude.and(Expression.create(n.attribute("if")));
            }
            excludes.add(exclude);
        }
        return excludes;
    }

    static Variations loadVariations(String path) {
        List<Variations.Entry> result = new ArrayList<>();
        for (XMLElement e : loadXml(path).children("variation")) {
            Map<String, String> map = new java.util.LinkedHashMap<>();
            for (XMLElement entry : e.children()) {
                map.put(entry.name(), entry.value());
            }
            result.add(Variations.entry(map));
        }
        return Variations.of(result);
    }

    static XMLElement loadXml(String path) {
        Path targetDir = targetDir(VariationsTest.class);
        Path testClasses = targetDir.resolve("test-classes");
        try (InputStream is = Files.newInputStream(testClasses.resolve(path))) {
            return XMLElement.parse(is);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }
}
