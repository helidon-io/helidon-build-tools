/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.InputResolver.BatchResolver;
import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.common.VirtualFileSystem;

import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.Nodes.file;
import static io.helidon.build.archetype.engine.v2.Nodes.method;
import static io.helidon.build.archetype.engine.v2.Nodes.output;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Tests {@link ScriptInvoker}.
 */
class ScriptInvokerTest {

    @Test
    void testMethodsOverride() {
        Node node = load("invoker/method-override.xml");
        List<Node> nodes = new ArrayList<>();
        Context context = new Context().pushCwd(node.script().path().getParent());
        ScriptInvoker.invoke(node, context, nodes::add);
        Iterator<Node> it = nodes.iterator();
        assertThat(it.next(), is(node));
        assertThat(it.next(), is(method("foo")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(file("foo1.txt", "foo1.txt")));

        assertThat(it.next(), isScript(node.script().path().getParent().resolve("foo.xml")));
        assertThat(it.next(), is(method("foo")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(file("foo2.txt", "foo2.txt")));

        assertThat(it.next(), isScript(node.script().path().getParent().resolve("bar.xml")));
        assertThat(it.next(), is(method("foo")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(file("foo2.txt", "foo2.txt")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testPresets() {
        Node node = load("invoker/presets.xml");
        Context context = new Context();
        context.pushCwd(node.script().path().getParent());

        ScriptInvoker.invoke(node, context, new BatchResolver(context));

        Context.Scope scope = context.scope();
        Value<?> preset1 = scope.get("preset1").value();
        assertThat(preset1.asBoolean().orElse(false), Matchers.is(true));

        Value<?> preset2 = scope.get("preset2").value();
        assertThat(preset2.asString().orElse(null), Matchers.is("text1"));

        Value<?> preset3 = scope.get("preset3").value();
        assertThat(preset3.asString().orElse(null), Matchers.is("enum1"));

        Value<?> preset4 = scope.get("preset4").value();
        assertThat(preset4.asList().orElse(List.of()), Matchers.is(List.of("list1")));
    }

    @Test
    void testConditional() {
        Node node = load("invoker/conditional.xml");
        Context context = new Context()
                .externalValues(Map.of(
                        "doModel", "true",
                        "doColors", "true",
                        "doRed", "true",
                        "doGreen", "false",
                        "doBlue", "true",
                        "doShapes", "false"));

        List<String> values = modelValues(node, context);
        assertThat(values, contains("red", "blue"));
    }

    @Test
    void testExecCwd() {
        Node node = load("invoker/cwd/exec.xml");

        List<String> values = modelValues(node);
        assertThat(values, contains("red"));
    }

    @Test
    void testSourceCwd() {
        Node node = load("invoker/cwd/source.xml");

        List<String> values = modelValues(node);
        assertThat(values, contains("green"));
    }

    @Test
    void testConditionalCwd() {
        Node node = load("invoker/cwd/conditional.xml");
        Path cwd = node.script().path().getParent();
        Context context = new Context()
                .pushCwd(cwd)
                .externalValues(Map.of(
                        "source", "true",
                        "exec", "true"));

        List<String> values = modelValues(node, context);
        assertThat(values, contains("red", "green"));

        context = new Context()
                .pushCwd(cwd)
                .externalValues(Map.of(
                        "source", "true",
                        "exec", "false"));

        values = modelValues(node, context);
        assertThat(values, contains("green"));

        context = new Context()
                .pushCwd(cwd)
                .externalValues(Map.of(
                        "source", "false",
                        "exec", "true"));

        values = modelValues(node, context);
        assertThat(values, contains("red"));

        context = new Context()
                .pushCwd(cwd)
                .externalValues(Map.of(
                        "source", "false",
                        "exec", "false"));

        values = modelValues(node, context);
        assertThat(values.size(), Matchers.is(0));
    }

    @Test
    void testOutput() {
        Node node = load("invoker/output.xml");
        Context context = new Context()
                .pushCwd(node.script().path().getParent())
                .externalValues(Map.of(
                        "bool", "true",
                        "enum", "option"));

        List<String> values = modelValues(node, context);
        Iterator<String> it = values.iterator();

        assertThat(values.size(), Matchers.is(12));
        assertThat(it.next(), Matchers.is("script1"));
        assertThat(it.next(), Matchers.is("script2"));
        assertThat(it.next(), Matchers.is("step1"));
        assertThat(it.next(), Matchers.is("step2"));
        assertThat(it.next(), Matchers.is("inputs1"));
        assertThat(it.next(), Matchers.is("inputs2"));
        assertThat(it.next(), Matchers.is("boolean1"));
        assertThat(it.next(), Matchers.is("boolean2"));
        assertThat(it.next(), Matchers.is("enum1"));
        assertThat(it.next(), Matchers.is("enum2"));
        assertThat(it.next(), Matchers.is("method1"));
        assertThat(it.next(), Matchers.is("method2"));
    }

    @Test
    void testCall() {
        Node node = load("invoker/call.xml");
        List<String> values = modelValues(node);

        assertThat(values, contains("blue"));
    }

    static List<String> modelValues(Node node) {
        return modelValues(node, new Context().pushCwd(node.script().path().getParent()));
    }

    static List<String> modelValues(Node node, Context context) {
        List<String> values = new ArrayList<>();
        ScriptInvoker.invoke(node, context, new BatchResolver(context), n -> {
            if (n.kind() == Kind.MODEL_VALUE) {
                values.add(n.value().getString());
            }
            return true;
        });
        return values;
    }

    static Node load(String path) {
        Path testClasses = targetDir(ScriptInvokerTest.class).resolve("test-classes");
        try (FileSystem fs = VirtualFileSystem.create(testClasses)) {
            return Script.load(fs.getPath(path));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }

    static ScriptMatcher isScript(Path path) {
        return new ScriptMatcher(path);
    }

    static final class ScriptMatcher extends TypeSafeMatcher<Node> {

        final Path path;

        ScriptMatcher(Path path) {
            this.path = path;
        }

        @Override
        protected boolean matchesSafely(Node node) {
            return node.kind() == Kind.SCRIPT && node.script().path().equals(path);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(path.toString());
        }
    }
}
