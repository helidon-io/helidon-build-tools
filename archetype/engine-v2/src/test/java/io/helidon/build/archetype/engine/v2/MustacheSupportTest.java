/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextValue;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MustacheSupport}.
 */
class MustacheSupportTest {

    @Test
    void testSimpleValue() {
        Block block = model(modelValue("foo", "bar")).build();
        assertThat(render("{{foo}}", block), is("bar"));
    }

    @Test
    void testDottedKeyValue() {
        Block block = model(modelValue("foo.bar", "foobar")).build();
        assertThat(render("{{foo.bar}}", block), is("foobar"));
    }

    @Test
    void testSimpleList() {
        Block block = model(modelList("data", modelValue("bar1"), modelValue("bar2"))).build();
        assertThat(render("{{#data}}{{.}},{{/data}}", block), is("bar1,bar2,"));
    }

    @Test
    void testNewLine() {
        Block block = model(modelList("data", modelValue("bar1"), modelValue("bar2"))).build();

        String templateMustache = "{{#data}}{{.}}\n{{/data}}";
        String templateMustache1 = "{{#data}}\n{{.}}{{/data}}";
        String templateMustache2 = "{{#data}}\n{{.}}\n{{/data}}";

        String render = render(templateMustache, block);
        String render1 = render(templateMustache1, block);
        String render2 = render(templateMustache2, block);

        assertThat(render, is("bar1bar2"));
        assertThat(render1, is("bar1bar2"));
        assertThat(render2, is("bar1\nbar2\n"));

        assertThat(render, is(renderMustache(templateMustache)));
        assertThat(render1, is(renderMustache(templateMustache1)));
        assertThat(render2, is(renderMustache(templateMustache2)));
    }

    @Test
    void testSimpleMap() {
        Block block = model(modelMap("data", modelValue("shape", "circle"), modelValue("color", "red"))).build();
        assertThat(render("{{#data}}{{shape}}:{{color}}{{/data}}", block), is("circle:red"));
    }

    @Test
    void testListOfMap() {
        Block block = model(modelList("data",
                modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                modelMap(modelValue("name", "foo"), modelValue("id", "2")))).build();
        assertThat(render("{{#data}}{{name}}={{id}}{{^last}},{{/last}}{{/data}}", block), is("bar=1,foo=2"));
    }

    @Test
    void testListOfListOfMap() {
        Block block = model(modelList("data",
                modelList(
                        modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                        modelMap(modelValue("name", "foo"), modelValue("id", "2"))),
                modelList(
                        modelMap(modelValue("name", "bob"), modelValue("id", "3")),
                        modelMap(modelValue("name", "alice"), modelValue("id", "4"))))).build();

        String rendered = render("{{#data}}{{#.}}{{name}}={{id}}{{^last}},{{/last}}{{/.}}{{^last}},{{/last}}{{/data}}", block);
        assertThat(rendered, is("bar=1,foo=2,bob=3,alice=4"));
    }

    @Test
    void testListOfListOfListOfMap() {
        Block block = model(modelList("data",
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

        String rendered = render("{{#data}}{{#.}}{{#.}}{{name}}={{id}},{{/.}}{{/.}}{{/data}}", block);
        assertThat(rendered, is("bar=1,foo=2,bob=3,alice=4,roger=5,joe=6,john=7,jack=8,"));
    }

    @Test
    void testMapOfList() {
        Block block = model(modelMap("data",
                modelList("shapes", modelValue("circle"), modelValue("rectangle")),
                modelList("colors", modelValue("red"), modelValue("blue")))).build();
        String rendered = render("{{#data}}{{#shapes}}{{.}},{{/shapes}};{{#colors}}{{.}},{{/colors}}{{/data}}", block);
        assertThat(rendered, is("circle,rectangle,;red,blue,"));
    }

    @Test
    void testMapOfMap() {
        Block block = model(modelMap("data",
                modelMap("shapes", modelValue("circle", "red"), modelValue("rectangle", "blue")),
                modelMap("colors", modelValue("red", "circle"), modelValue("blue", "rectangle")))).build();
        String rendered = render("{{#data}}{{#shapes}}{{circle}},{{rectangle}}{{/shapes}};{{#colors}}{{red}},{{blue}}{{/colors}}{{/data}}", block);
        assertThat(rendered, is("red,blue;circle,rectangle"));
    }

    @Test
    void testIterateOnValue() {
        Block block = model(modelValue("data", "bar")).build();
        String rendered = render("{{#data}}{{.}}{{/data}}", block);
        assertThat(rendered, is("bar"));
    }

    @Test
    void testUnknownValue() {
        Block block = model().build();
        assertThat(render("{{bar}}", block), is(""));
    }

    @Test
    void testUnknownIterable() {
        Block block = model().build();
        assertThat(render("{{#bar}}{{.}}{{/bar}}", block), is(""));
    }

    @Test
    void testListOrder() {
        Block block = model(modelList("data", modelValue("bar1", 0), modelValue("bar2", 100))).build();
        assertThat(render("{{#data}}{{.}},{{/data}}", block), is("bar2,bar1,"));
    }

    @Test
    void testMapValueOverrideByOrder() {
        Block block = model(modelMap("data", modelValue("shape", "circle", 0), modelValue("shape", "rectangle", 100))).build();
        assertThat(render("{{#data}}{{shape}}{{/data}}", block), is("rectangle"));
    }

    @Test
    void testMapValueOverride() {
        Block block = model(modelMap("data", modelValue("color", "red", 100), modelValue("color", "blue", 100))).build();
        assertThat(render("{{#data}}{{color}}{{/data}}", block), is("blue"));
    }

    @Test
    void testMapOfListMerge() {
        Block block = model(modelMap("data",
                modelList("shapes", modelValue("circle", 0), modelValue("rectangle", 1)),
                modelList("shapes", modelValue("triangle", 2)))).build();
        assertThat(render("{{#data}}{{#shapes}}{{.}},{{/shapes}}{{/data}}", block), is("triangle,rectangle,circle,"));
    }

    @Test
    void testListOfMapMerge() {
        Block block = model(
                modelList("data", modelMap(modelValue("shape", "circle"), modelValue("color", "red"))),
                modelList("data", modelMap(modelValue("shape", "rectangle"), modelValue("color", "blue")))).build();
        assertThat(render("{{#data}}{{shape}}:{{color}},{{/data}}", block), is("circle:red,rectangle:blue,"));
    }

    @Test
    void testListMerge() {
        Block block = model(
                modelList("data", modelValue("bar1"), modelValue("bar2")),
                modelList("data", modelValue("foo1"), modelValue("foo2"))).build();
        assertThat(render("{{#data}}{{.}},{{/data}}", block), is("bar1,bar2,foo1,foo2,"));
    }

    @Test
    void testMapValueWithoutKey() {
        Block block = model(modelMap("data", modelValue("circle"))).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> render("", block));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
        assertThat(ex.getCause().getMessage(), is("Cannot add a model with no key to a map"));
    }

    @Test
    void testMapAsString() {
        Block block = model(modelMap("data")).build();
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{data}}", block));
        assertThat(ex.getCause(), is(not(nullValue())));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void testListAsString() {
        Block block = model(modelList("data")).build();
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{data}}", block));
        assertThat(ex.getCause(), is(not(nullValue())));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void testExtraScope() {
        Block block = model(modelValue("color", "red")).build();
        Block extraScope = model(modelValue("shape", "circle")).build();
        assertThat(render("{{shape}}", block, extraScope), is("circle"));
    }

    @Test
    void testExtraScopeOverride() {
        Block block = model(modelValue("color", "red")).build();
        Block extraScope = model(modelValue("color", "blue")).build();
        assertThat(render("{{color}}", block, extraScope), is("blue"));
    }

    @Test
    void testConditional() {
        Block block = model(modelValue("doColors", "false")).build();
        assertThat(render("{{#doColors}}red{{/doColors}}", block), is(""));
        assertThat(render("{{^doColors}}red{{/doColors}}", block), is("red"));
        block = model(modelValue("doColors", "true")).build();
        assertThat(render("{{#doColors}}red{{/doColors}}", block), is("red"));
        assertThat(render("{{^doColors}}red{{/doColors}}", block), is(""));
    }

    @Test
    void testModelValueWithContextVariable() {
        Block block = model(modelValue("color", "${color}")).build();
        Context context = Context.create();
        context.putValue("color", Value.create("red"), ContextValue.ValueKind.EXTERNAL);
        assertThat(render("{{color}}", block, null, context), is("red"));
    }

    @Test
    void testModelValueWithContextVariables() {
        Block block = model(modelValue("colors", "${red},${blue}")).build();
        Context context = Context.create();
        context.putValue("red", Value.create("red"), ContextValue.ValueKind.EXTERNAL);
        context.putValue("blue", Value.create("blue"), ContextValue.ValueKind.EXTERNAL);
        assertThat(render("{{colors}}", block, null, context), is("red,blue"));
    }

    @Test
    void testNestedOverride() {
        Block block = model(
                modelValue("shape", "circle"),
                modelList("shapes", modelMap(modelValue("shape", "triangle")))).build();
        assertThat(render("{{#shapes}}{{shape}}{{/shapes}}", block), is("triangle"));
    }

    @Test
    void testBuiltInModel() {
        Block block = model().build();
        assertThat(render("{{current-date}}", block), is(not("")));
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

    private static String renderMustache(String template) {
        List<Container> containers = List.of(
                new Container(new Bar("bar1")),
                new Container(new Bar("bar2")));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HashMap<String, Object> scopes = new HashMap<>();
        Writer writer = new OutputStreamWriter(os);
        Mustache mustache = new DefaultMustacheFactory()
                .compile(new StringReader(template), "test-render-mustache");
        scopes.put("data", containers);
        mustache.execute(writer, scopes);
        try {
            writer.flush();
            writer.close();
            return os.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class Bar {
        final String bar;

        Bar(String bar) {
            this.bar = bar;
        }

        @Override
        public String toString() {
            return bar;
        }
    }

    private static class Container {
        public final Bar bar;

        public Container(final Bar bar) {
            this.bar = bar;
        }

        @Override
        public String toString() {
            return bar.toString();
        }
    }
}
