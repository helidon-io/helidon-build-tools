/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;

import io.helidon.build.archetype.engine.v2.InputResolver.BatchResolver;
import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.archetype.engine.v2.ScriptInvoker.InvocationException;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.Nodes.model;
import static io.helidon.build.archetype.engine.v2.Nodes.modelList;
import static io.helidon.build.archetype.engine.v2.Nodes.modelMap;
import static io.helidon.build.archetype.engine.v2.Nodes.modelValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TemplateSupport}.
 */
class TemplateSupportTest {

    @Test
    void testSimpleValue() {
        Node scope = model(modelValue("foo", "bar"));
        assertThat(render("{{foo}}", scope), is("bar"));
    }

    @Test
    void testDottedKeyValue() {
        Node scope = model(modelValue("foo.bar", "foobar"));
        assertThat(render("{{foo.bar}}", scope), is("foobar"));
    }

    @Test
    void testSimpleList() {
        Node scope = model(modelList("data", modelValue("bar1"), modelValue("bar2")));
        assertThat(render("{{#data}}{{.}},{{/data}}", scope), is("bar1,bar2,"));
    }

    @Test
    void testNewLine() {
        Node scope = model(modelList("data", modelValue("bar1"), modelValue("bar2")));

        String templateMustache = "{{#data}}{{.}}\n{{/data}}";
        String templateMustache1 = "{{#data}}\n{{.}}{{/data}}";
        String templateMustache2 = "{{#data}}\n{{.}}\n{{/data}}";

        String render = render(templateMustache, scope);
        String render1 = render(templateMustache1, scope);
        String render2 = render(templateMustache2, scope);

        assertThat(render, is("bar1bar2"));
        assertThat(render1, is("bar1bar2"));
        assertThat(render2, is("bar1\nbar2\n"));

        assertThat(render, is(renderMustache(templateMustache)));
        assertThat(render1, is(renderMustache(templateMustache1)));
        assertThat(render2, is(renderMustache(templateMustache2)));
    }

    @Test
    void testSimpleMap() {
        Node scope = model(modelMap("data", modelValue("shape", "circle"), modelValue("color", "red")));
        assertThat(render("{{#data}}{{shape}}:{{color}}{{/data}}", scope), is("circle:red"));
    }

    @Test
    void testListOfMap() {
        Node scope = model(modelList("data",
                modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                modelMap(modelValue("name", "foo"), modelValue("id", "2"))));
        assertThat(render("{{#data}}{{name}}={{id}}{{^last}},{{/last}}{{/data}}", scope), is("bar=1,foo=2"));
    }

    @Test
    void testListOfListOfMap() {
        Node scope = model(modelList("data",
                modelList(
                        modelMap(modelValue("name", "bar"), modelValue("id", "1")),
                        modelMap(modelValue("name", "foo"), modelValue("id", "2"))),
                modelList(
                        modelMap(modelValue("name", "bob"), modelValue("id", "3")),
                        modelMap(modelValue("name", "alice"), modelValue("id", "4")))));

        String rendered = render("{{#data}}{{#.}}{{name}}={{id}}{{^last}},{{/last}}{{/.}}{{^last}},{{/last}}{{/data}}", scope);
        assertThat(rendered, is("bar=1,foo=2,bob=3,alice=4"));
    }

    @Test
    void testListOfListOfListOfMap() {
        Node scope = model(modelList("data",
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
                                modelMap(modelValue("name", "jack"), modelValue("id", "8"))))));

        String rendered = render("{{#data}}{{#.}}{{#.}}{{name}}={{id}},{{/.}}{{/.}}{{/data}}", scope);
        assertThat(rendered, is("bar=1,foo=2,bob=3,alice=4,roger=5,joe=6,john=7,jack=8,"));
    }

    @Test
    void testMapOfList() {
        Node scope = model(modelMap("data",
                modelList("shapes", modelValue("circle"), modelValue("rectangle")),
                modelList("colors", modelValue("red"), modelValue("blue"))));
        String rendered = render("{{#data}}{{#shapes}}{{.}},{{/shapes}};{{#colors}}{{.}},{{/colors}}{{/data}}", scope);
        assertThat(rendered, is("circle,rectangle,;red,blue,"));
    }

    @Test
    void testMapOfMap() {
        Node scope = model(modelMap("data",
                modelMap("shapes", modelValue("circle", "red"), modelValue("rectangle", "blue")),
                modelMap("colors", modelValue("red", "circle"), modelValue("blue", "rectangle"))));
        String rendered = render(
                "{{#data}}{{#shapes}}{{circle}},{{rectangle}}{{/shapes}};{{#colors}}{{red}},{{blue}}{{/colors}}{{/data}}",
                scope);
        assertThat(rendered, is("red,blue;circle,rectangle"));
    }

    @Test
    void testIterateOnValue() {
        Node scope = model(modelValue("data", "bar"));
        String rendered = render("{{#data}}{{.}}{{/data}}", scope);
        assertThat(rendered, is("bar"));
    }

    @Test
    void testUnknownValue() {
        Node scope = Node.builder(Kind.MODEL);
        assertThat(render("{{bar}}", scope), is(""));
    }

    @Test
    void testUnknownIterable() {
        Node scope = Node.builder(Kind.MODEL);
        assertThat(render("{{#bar}}{{.}}{{/bar}}", scope), is(""));
    }

    @Test
    void testListOrder() {
        Node scope = model(modelList("data", modelValue("bar1", 0), modelValue("bar2", 100)));
        assertThat(render("{{#data}}{{.}},{{/data}}", scope), is("bar2,bar1,"));
    }

    @Test
    void testMapValueOverrideByOrder() {
        Node scope = model(modelMap("data", modelValue("shape", "circle", 0), modelValue("shape", "rectangle", 100)));
        assertThat(render("{{#data}}{{shape}}{{/data}}", scope), is("rectangle"));
    }

    @Test
    void testMapValueOverride() {
        Node scope = model(modelMap("data", modelValue("color", "red", 100), modelValue("color", "blue", 100)));
        assertThat(render("{{#data}}{{color}}{{/data}}", scope), is("red"));
    }

    @Test
    void testMapOfListMerge() {
        Node scope = model(modelMap("data",
                modelList("shapes", modelValue("circle", 0), modelValue("rectangle", 1)),
                modelList("shapes", modelValue("triangle", 2))));
        assertThat(render("{{#data}}{{#shapes}}{{.}},{{/shapes}}{{/data}}", scope), is("triangle,rectangle,circle,"));
    }

    @Test
    void testListOfMapMerge() {
        Node scope = model(
                modelList("data", modelMap(modelValue("shape", "circle"), modelValue("color", "red"))),
                modelList("data", modelMap(modelValue("shape", "rectangle"), modelValue("color", "blue"))));
        assertThat(render("{{#data}}{{shape}}:{{color}},{{/data}}", scope), is("circle:red,rectangle:blue,"));
    }

    @Test
    void testListMerge() {
        Node scope = model(
                modelList("data", modelValue("bar1"), modelValue("bar2")),
                modelList("data", modelValue("foo1"), modelValue("foo2")));
        assertThat(render("{{#data}}{{.}},{{/data}}", scope), is("bar1,bar2,foo1,foo2,"));
    }

    @Test
    void testMapValueWithoutKey() {
        Node scope = model(modelMap("data", modelValue("circle")));
        InvocationException ex = assertThrows(InvocationException.class, () -> render("", scope));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
        assertThat(ex.getCause().getMessage(), is("Cannot add a model with no key to a map"));
    }

    @Test
    void testMapAsString() {
        Node scope = model(modelMap("data"));
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{data}}", scope));
        assertThat(ex.getCause(), is(not(nullValue())));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void testListAsString() {
        Node scope = model(modelList("data"));
        MustacheException ex = assertThrows(MustacheException.class, () -> render("{{data}}", scope));
        assertThat(ex.getCause(), is(not(nullValue())));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void testExtraScope() {
        Node scope = model(modelValue("color", "red"));
        Node extraScope = model(modelValue("shape", "circle"));
        assertThat(render("{{shape}}", scope, extraScope), is("circle"));
    }

    @Test
    void testExtraScopeOverride() {
        Node scope = model(modelValue("color", "red"));
        Node extraScope = model(modelValue("color", "blue"));
        assertThat(render("{{color}}", scope, extraScope), is("blue"));
    }

    @Test
    void testConditional() {
        Node scope = model(modelValue("doColors", "false"));
        assertThat(render("{{#doColors}}red{{/doColors}}", scope), is(""));
        assertThat(render("{{^doColors}}red{{/doColors}}", scope), is("red"));

        scope = model(modelValue("doColors", "true"));
        assertThat(render("{{#doColors}}red{{/doColors}}", scope), is("red"));
        assertThat(render("{{^doColors}}red{{/doColors}}", scope), is(""));
    }

    @Test
    void testModelValueWithContextVariable() {
        Node scope = model(modelValue("color", "${color}"));
        Context context = new Context()
                .externalValues(Map.of("color", "red"));
        assertThat(render("{{color}}", scope, null, context), is("red"));
    }

    @Test
    void testModelValueWithContextVariables() {
        Node scope = model(modelValue("colors", "${red},${blue}"));
        Context context = new Context()
                .externalValues(Map.of(
                        "red", "red",
                        "blue", "blue"));
        assertThat(render("{{colors}}", scope, null, context), is("red,blue"));
    }

    @Test
    void testNestedOverride() {
        Node scope = model(
                modelValue("shape", "circle"),
                modelList("shapes", modelMap(modelValue("shape", "triangle"))));
        assertThat(render("{{#shapes}}{{shape}}{{/shapes}}", scope), is("triangle"));
    }

    @Test
    void testBuiltInModel() {
        Node scope = model();
        assertThat(render("{{current-date}}", scope), is(not("")));
    }

    static String render(String template, Node scope) {
        return render(template, scope, null, new Context());
    }

    static String render(String template, Node scope, Node extraScope) {
        return render(template, scope, extraScope, new Context());
    }

    static String render(String template, Node scope, Node extraScope, Context context) {
        InputStream is = new ByteArrayInputStream(template.getBytes(UTF_8));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TemplateSupport support = new TemplateSupport(resolveModel(scope, context), context);
        support.render(is, "test", UTF_8, os, extraScope);
        return os.toString(UTF_8);
    }

    static TemplateModel resolveModel(Node scope, Context context) {
        TemplateModel model = new TemplateModel(context);
        ScriptInvoker.invoke(scope, context, new BatchResolver(context), model);
        return model;
    }

    static String renderMustache(String template) {
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

    static class Bar {
        final String bar;

        Bar(String bar) {
            this.bar = bar;
        }

        @Override
        public String toString() {
            return bar;
        }
    }

    static class Container {
        final Bar bar;

        Container(final Bar bar) {
            this.bar = bar;
        }

        @Override
        public String toString() {
            return bar.toString();
        }
    }
}
