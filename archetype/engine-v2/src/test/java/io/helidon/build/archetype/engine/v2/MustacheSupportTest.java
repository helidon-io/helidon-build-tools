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
package io.helidon.build.archetype.engine.v2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import io.helidon.build.archetype.engine.v2.ast.Block;

import com.github.mustachejava.MustacheException;
import io.helidon.build.archetype.engine.v2.ast.Value;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.MergedModel.resolveModel;
import static io.helidon.build.archetype.engine.v2.TestHelper.model;
import static io.helidon.build.archetype.engine.v2.TestHelper.modelList;
import static io.helidon.build.archetype.engine.v2.TestHelper.modelMap;
import static io.helidon.build.archetype.engine.v2.TestHelper.modelValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MustacheSupport}.
 */
class MustacheSupportTest {

    @Test
    void testSimpleValue() {
        Block scope = model(modelValue("foo", "bar")).build();
        assertThat(render("{{foo}}", scope), is("bar"));
    }

    @Test
    void testDottedKeyValue() {
        Block scope = model(modelValue("foo.bar", "foobar")).build();
        assertThat(render("{{foo.bar}}", scope), is("foobar"));
    }

    @Test
    void testSimpleList() {
        Block scope = model(modelList("data", modelValue("bar1"), modelValue("bar2"))).build();
        assertThat(render("{{#data}}{{.}},{{/data}}", scope), is("bar1,bar2,"));
    }

    @Test
    void testSimpleMap() {
        Block scope = model(modelMap("data", modelValue("shape", "circle"), modelValue("color", "red"))).build();
        assertThat(render("{{#data}}{{shape}}:{{color}}{{/data}}", scope), is("circle:red"));
    }

    @Test
    void testListOfMap() {
        Block scope = model(modelList("data",
                modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                modelMap(modelValue("name", "foo"), modelValue("id", "2")))).build();
        assertThat(render("{{#data}}{{name}}={{id}},{{/data}}", scope), is("bar=1,foo=2,"));
    }

    @Test
    void testListOfListOfMap() {
        Block scope = model(modelList("data",
                modelList(
                        modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                        modelMap(modelValue("name", "foo"), modelValue("id", "2"))),
                modelList(
                        modelMap(modelValue("name", "bob"), modelValue("id", "3")),
                        modelMap(modelValue("name", "alice"), modelValue("id", "4"))))).build();

        String rendered = render("{{#data}}{{#.}}{{name}}={{id}},{{/.}}{{/data}}", scope);
        assertThat(rendered, is("bar=1,foo=2,bob=3,alice=4,"));
    }

    @Test
    void testListOfListOfListOfMap() {
        Block scope = model(modelList("data",
                modelList(
                        modelList(
                                modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                                modelMap(modelValue("name", "foo"), modelValue("id", "2"))),
                        modelList(
                                modelMap(modelValue("name", "bob"), modelValue("id", "3")),
                                modelMap(modelValue("name", "alice"), modelValue("id", "4")))),
                modelList(
                        modelList(
                                modelMap(modelValue("name", "roger"), modelValue("id", "5")),
                                modelMap(modelValue("name", "joe"), modelValue("id", "6"))),
                        modelList(
                                modelMap(modelValue("name", "john"), modelValue("id", "7")),
                                modelMap(modelValue("name", "jack"), modelValue("id", "8")))))).build();

        String rendered = render("{{#data}}{{#.}}{{#.}}{{name}}={{id}},{{/.}}{{/.}}{{/data}}", scope);
        assertThat(rendered, is("bar=1,foo=2,bob=3,alice=4,roger=5,joe=6,john=7,jack=8,"));
    }

    @Test
    void testMapOfList() {
        Block scope = model(modelMap("data",
                modelList("shapes", modelValue("circle"), modelValue("rectangle")),
                modelList("colors", modelValue("red"), modelValue("blue")))).build();
        String rendered = render("{{#data}}{{#shapes}}{{.}},{{/shapes}};{{#colors}}{{.}},{{/colors}}{{/data}}", scope);
        assertThat(rendered, is("circle,rectangle,;red,blue,"));
    }

    @Test
    void testMapOfMap() {
        Block scope = model(modelMap("data",
                modelMap("shapes", modelValue("circle", "red"), modelValue("rectangle", "blue")),
                modelMap("colors", modelValue("red", "circle"), modelValue("blue", "rectangle")))).build();
        String rendered = render("{{#data}}{{#shapes}}{{circle}},{{rectangle}}{{/shapes}};{{#colors}}{{red}},{{blue}}{{/colors}}{{/data}}", scope);
        assertThat(rendered, is("red,blue;circle,rectangle"));
    }

    @Test
    void testIterateOnValue() {
        Block scope = model(modelValue("data", "bar")).build();
        String rendered = render("{{#data}}{{.}}{{/data}}", scope);
        assertThat(rendered, is("bar"));
    }

    @Test
    void testUnknownValue() {
        Block scope = model().build();
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{bar}}", scope));
        assertThat(ex.getMessage(), startsWith("Failed to get value for bar"));
    }

    @Test
    void testUnknownIterable() {
        Block scope = model().build();
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{#bar}}{{.}}{{/bar}}", scope));
        assertThat(ex.getMessage(), startsWith("Unresolved model value: 'bar'"));
    }

    @Test
    void testListOrder() {
        Block scope = model(modelList("data", modelValue("bar1", 0), modelValue("bar2", 100))).build();
        assertThat(render("{{#data}}{{.}},{{/data}}", scope), is("bar2,bar1,"));
    }

    @Test
    void testMapValueOverrideByOrder() {
        Block scope = model(modelMap("data", modelValue("shape", "circle", 0), modelValue("shape", "rectangle", 100))).build();
        assertThat(render("{{#data}}{{shape}}{{/data}}", scope), is("rectangle"));
    }

    @Test
    void testMapValueOverride() {
        Block scope = model(modelMap("data", modelValue("color", "red", 100), modelValue("color", "blue", 100))).build();
        assertThat(render("{{#data}}{{color}}{{/data}}", scope), is("red"));
    }

    @Test
    void testMapOfListMerge() {
        Block scope = model(modelMap("data",
                modelList("shapes", modelValue("circle", 0), modelValue("rectangle", 1)),
                modelList("shapes", modelValue("triangle", 2)))).build();
        assertThat(render("{{#data}}{{#shapes}}{{.}},{{/shapes}}{{/data}}", scope), is("triangle,rectangle,circle,"));
    }

    @Test
    void testListOfMapMerge() {
        Block scope = model(
                modelList("data", modelMap(modelValue("shape", "circle"), modelValue("color", "red"))),
                modelList("data", modelMap(modelValue("shape", "rectangle"), modelValue("color", "blue")))).build();
        assertThat(render("{{#data}}{{shape}}:{{color}},{{/data}}", scope), is("circle:red,rectangle:blue,"));
    }

    @Test
    void testListMerge() {
        Block scope = model(
                modelList("data", modelValue("bar1"), modelValue("bar2")),
                modelList("data", modelValue("foo1"), modelValue("foo2"))).build();
        assertThat(render("{{#data}}{{.}},{{/data}}", scope), is("bar1,bar2,foo1,foo2,"));
    }

    @Test
    void testMapValueWithoutKey() {
        Block scope = model(modelMap("data", modelValue("circle"))).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> render("", scope));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
        assertThat(ex.getCause().getMessage(), is("Cannot add a model with no key to a map"));
    }

    @Test
    void testMapAsString() {
        Block scope = model(modelMap("data")).build();
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{data}}", scope));
        assertThat(ex.getCause(), is(not(nullValue())));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void testListAsString() {
        Block scope = model(modelList("data")).build();
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{data}}", scope));
        assertThat(ex.getCause(), is(not(nullValue())));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void testExtraScope() {
        Block scope = model(modelValue("color", "red")).build();
        Block extraScope = model(modelValue("shape", "circle")).build();
        assertThat(render("{{shape}}", scope, extraScope), is("circle"));
    }

    @Test
    void testExtraScopeOverride() {
        Block scope = model(modelValue("color", "red")).build();
        Block extraScope = model(modelValue("color", "blue")).build();
        assertThat(render("{{color}}", scope, extraScope), is("blue"));
    }

    @Test
    void testConditional() {
        Block scope = model(modelValue("doColors", "false")).build();
        assertThat(render("{{#doColors}}red{{/doColors}}", scope), is(""));
        assertThat(render("{{^doColors}}red{{/doColors}}", scope), is("red"));
        scope = model(modelValue("doColors", "true")).build();
        assertThat(render("{{#doColors}}red{{/doColors}}", scope), is("red"));
        assertThat(render("{{^doColors}}red{{/doColors}}", scope), is(""));
    }

    @Test
    void testModelValueWithContextVariable() {
        Block scope = model(modelValue("color", "${color}")).build();
        Context context = Context.create();
        context.put("color", Value.create("red"));
        assertThat(render("{{color}}", scope, null, context), is("red"));
    }

    @Test
    void testModelValueWithContextVariables() {
        Block scope = model(modelValue("colors", "${red},${blue}")).build();
        Context context = Context.create();
        context.put("red", Value.create("red"));
        context.put("blue", Value.create("blue"));
        assertThat(render("{{colors}}", scope, null, context), is("red,blue"));
    }

    private static String render(String template, Block scope) {
        return render(template, scope, null, Context.create());
    }

    private static String render(String template, Block scope, Block extraScope) {
        return render(template, scope, extraScope, Context.create());
    }

    private static String render(String template, Block scope, Block extraScope, Context context) {
        InputStream is = new ByteArrayInputStream(template.getBytes(UTF_8));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MustacheSupport support = new MustacheSupport(resolveModel(scope, context), context);
        support.render(is, "test", UTF_8, os, extraScope);
        return os.toString(UTF_8);
    }
}
