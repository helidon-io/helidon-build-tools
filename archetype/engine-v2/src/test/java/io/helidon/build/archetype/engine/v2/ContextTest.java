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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.Context.ValueKind;
import io.helidon.build.archetype.engine.v2.Context.Visibility;
import io.helidon.build.common.Strings;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link Context}.
 */
class ContextTest {

    @Test
    void testParsePath() {
        String[] segments;

        segments = Context.Key.parse("");
        assertThat(segments.length, is(0));

        segments = Context.Key.parse("..");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining(".."));

        segments = Context.Key.parse("....");
        assertThat(segments.length, is(2));
        assertThat(segments, arrayContaining("..", ".."));

        segments = Context.Key.parse("foo");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining("foo"));

        segments = Context.Key.parse("foo-bar");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining("foo-bar"));

        segments = Context.Key.parse("foo.bar");
        assertThat(segments.length, is(2));
        assertThat(segments, arrayContaining("foo", "bar"));

        segments = Context.Key.parse("~foo");
        assertThat(segments.length, is(2));
        assertThat(segments, arrayContaining("~", "foo"));

        segments = Context.Key.parse("~foo.bar");
        assertThat(segments.length, is(3));
        assertThat(segments, arrayContaining("~", "foo", "bar"));

        segments = Context.Key.parse("~..foo.bar");
        assertThat(segments.length, is(3));
        assertThat(segments, arrayContaining("~", "foo", "bar"));

        segments = Context.Key.parse("foo..");
        assertThat(segments.length, is(0));

        segments = Context.Key.parse("..foo..");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining(".."));

        segments = Context.Key.parse("....foo....");
        assertThat(segments.length, is(3));
        assertThat(segments, arrayContaining("..", "..", ".."));
    }

    @Test
    void testInvalidPath() {
        assertThrows(NullPointerException.class, () -> Context.Key.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("."));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("..."));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse(".foo"));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("foo."));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse(".foo."));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse(".foo.."));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse(".foo..bar"));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse(".foo.bar"));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse(".foo......."));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("-"));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("foo-"));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("-foo"));
        assertThrows(IllegalArgumentException.class, () -> Context.Key.parse("foo--bar"));
    }

    @Test
    void testPopScope() {
        Context context = new Context();
        context.pushScope(s -> s.getOrCreate("foo", false, false))
                .value(Value.of("foo1"), ValueKind.EXTERNAL);
        context.pushScope(s -> s.getOrCreate("bar", false, false))
                .value(Value.of("bar1"), ValueKind.EXTERNAL);

        context.popScope();
        Value<?> value;

        value = context.scope().get("bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));

        value = context.scope().value();
        assertThat(value.asString().orElse(null), is("foo1"));

        value = context.scope().get("~foo").value();
        assertThat(value.asString().orElse(null), is("foo1"));

        value = context.scope().get("..foo").value();
        assertThat(value.asString().orElse(null), is("foo1"));

        value = context.scope().get("~foo.bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));

        value = context.scope().get("~..foo.bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));
    }

    @Test
    void testExternalValuesSubstitution() {
        Context context = new Context()
                .externalDefaults(Map.of(
                        "some_var", "some_var_default",
                        "bar1", "bar1_default_value"))
                .externalValues(Map.of(
                        "foo", "foo",
                        "bar", "${foo}",
                        "foo1", "${non_exist_var}",
                        "foo2", "${some_var}",
                        "bar1", "bar1_value",
                        "foo3", "${bar1}"));

        Context.ScopeValue<?> value;

        value = context.scope().get("bar").value();
        assertThat(value.asString().orElse(null), is("foo"));

        value = context.scope().get("foo1").value();
        assertThat(value.asString().orElse(null), is(""));

        value = context.scope().get("foo2").value();
        assertThat(value.asString().orElse(null), is("some_var_default"));

        value = context.scope().get("foo3").value();
        assertThat(value.asString().orElse(null), is("bar1_value"));
    }

    @Test
    void testToMap() {
        Map<String, String> expectedResult = Map.of(
                "foo", "foo",
                "foo1", "",
                "foo2", "some_var_default",
                "foo3", "bar1_value");
        Context context = new Context()
                .externalDefaults(Map.of(
                        "some_var", "some_var_default",
                        "bar1", "bar1_default_value"))
                .externalValues(Map.of(
                        "foo", "foo",
                        "bar", "${foo}",
                        "foo1", "${non_exist_var}",
                        "foo2", "${some_var}",
                        "bar1", "bar1_value",
                        "foo3", "${bar1}"));

        Map<String, String> result = context.toMap();
        assertThat(result.entrySet(), everyItem(isIn(expectedResult.entrySet())));
    }

    @Test
    void testParentForRootNode() {
        Context.Scope root = new Context.Scope();
        Context.ScopeValue<?> value = root.get("..foo").value();
        assertThat(value.isEmpty(), is(true));
    }

    @Test
    void testSimpleGetOrCreate() {
        Context.Scope root = new Context.Scope();
        Context.Scope scope = root.getOrCreate("foo", false, Visibility.GLOBAL);
        assertThat(scope, is(not(nullValue())));
        assertThat(scope.id(), is("foo"));
        assertThat(scope.visibility(), is(Visibility.GLOBAL));
        assertThat(scope.parent(), is(root));
    }

    @Test
    void testPutNonExisting() {
        Context.Scope root = new Context.Scope();
        Context.ScopeValue<?> value = root.getOrCreate("foo.bar").value(Value.of("bar1"), ValueKind.USER);
        assertThat(value.asString().orElse(null), is("bar1"));
        assertThat(value.scope(), is(not(nullValue())));
        assertThat(value.scope().internalKey(), is("foo.bar"));
        assertThat(value.scope().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().internalKey(), is("foo"));
        assertThat(value.scope().parent().visibility(), is(Visibility.UNSET));
        assertThat(value.scope().parent().parent(), is(root));
    }

    @Test
    void testPutExisting() {
        Context.Scope root = new Context.Scope();

        // pre-create foo as global
        Context.Scope scope = root.getOrCreate("foo", false, Visibility.GLOBAL);
        assertThat(scope, is(not(nullValue())));
        assertThat(scope.id(), is("foo"));
        assertThat(scope.visibility(), is(Visibility.GLOBAL));

        scope = root.getOrCreate("foo.bar.bob");
        Context.ScopeValue<?> value = scope.value(Value.of("bob1"), ValueKind.USER);

        assertThat(value.asString().orElse(null), is("bob1"));
        assertThat(value.scope().internalKey(), is("foo.bar.bob"));
        assertThat(value.scope().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().internalKey(), is("foo.bar"));
        assertThat(value.scope().parent().visibility(), is(Visibility.UNSET));
        assertThat(value.scope().parent().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().parent().internalKey(), is("foo"));
        assertThat(value.scope().parent().parent().visibility(), is(Visibility.GLOBAL));
        assertThat(value.scope().parent().parent().parent(), is(root));
    }

    @Test
    void testGetOrCreateInvalidPath() {
        Context.Scope root = new Context.Scope();
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate(".", false, Visibility.GLOBAL));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("~", false, Visibility.GLOBAL));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("..", false, Visibility.GLOBAL));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate(".foo..", false, Visibility.GLOBAL));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("~foo", false, Visibility.GLOBAL));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("foo.bar", false, Visibility.GLOBAL));
    }

    @Test
    void testSimpleGetValue() {
        Context.Scope root = new Context.Scope();
        root.getOrCreate("foo").value(Value.of("foo1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = root.get("foo").value();
        assertThat(value.asString().orElse(null), is("foo1"));

        value = root.get("~foo").value();
        assertThat(value.asString().orElse(null), is("foo1"));
    }

    @Test
    void testGetLocal1() {
        Context.Scope scope = new Context.Scope();
        scope.getOrCreate("foo.bar").value(Value.of("bar1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = scope.get("foo.bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));

        value = value.scope().get("~bar").value();
        assertThat(value.isEmpty(), is(true));
    }

    @Test
    void testGetLocal2() {
        Context.Scope scope = new Context.Scope();
        scope.getOrCreate("foo", false, Visibility.GLOBAL)
                .getOrCreate("bar", false, Visibility.LOCAL)
                .getOrCreate("bob")
                .value(Value.of("bob1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = scope.get("bob").value();
        assertThat(value.isEmpty(), is(true));

        value = scope.get("foo.bar.bob").value();
        assertThat(value.asString().orElse(null), is("bob1"));

        value = scope.get("bar.bob").value();
        assertThat(value.asString().orElse(null), is("bob1"));

        value = scope.get("~bar.bob").value();
        assertThat(value.asString().orElse(null), is("bob1"));
    }

    @Test
    void testGetGlobalImplicit1() {
        Context.Scope scope = new Context.Scope();

        // pre-create foo as global
        scope.getOrCreate("foo", false, Visibility.GLOBAL)
                .getOrCreate("bar")
                .value(Value.of("bar1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = scope.get("bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));

        value = value.scope().get("~bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));
    }

    @Test
    void testGetGlobalImplicit2() {
        Context.Scope scope = new Context.Scope();

        // pre-create foo and bar as global
        scope.getOrCreate("foo", false, Visibility.GLOBAL)
                .getOrCreate("bar", false, Visibility.GLOBAL)
                .getOrCreate("bob")
                .value(Value.of("bob1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = scope.get("bob").value();
        assertThat(value.asString().orElse(null), is("bob1"));

        value = value.scope().get("~bob").value();
        assertThat(value.asString().orElse(null), is("bob1"));
    }

    @Test
    void testGetGlobal() {
        Context context = new Context();

        // pre-create foo and bar as global
        context.pushScope(s -> s.getOrCreate("foo", false, true));
        context.pushScope(s -> s.getOrCreate("bar", false, true));

        context.scope().value(Value.of("bar1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = context.scope().get("bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));

        value = value.scope().get("~bar").value();
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString().orElse(null), is("bar1"));
    }

    @Test
    void testPutExistingReadOnly() {
        Context context = new Context()
                .externalValues(Map.of("foo", "foo"));
        assertThrows(IllegalStateException.class,
                () -> context.scope().get("foo").value(Value.of("bar"), ValueKind.EXTERNAL));
    }

    @Test
    void testGlobalMismatch() {
        // test external values "specialized"
        // external values are always created as direct children of the root scope
        // If there are multiple global inputs in the input hierarchy, it creates duplicates
        // When calling getOrCreate() global nodes can be "specialized" to avoid duplicates
        // "specialized" means moved down in the tree
        Context.Scope root = new Context.Scope();

        // pre-create foo and bar as global
        Context.Scope foo = root.getOrCreate("foo", false, Visibility.GLOBAL);
        Context.Scope bar = foo.getOrCreate("bar", false, Visibility.GLOBAL);

        // pre-create foo.bob as global
        foo.getOrCreate("bob", false, Visibility.UNSET)
                .value(Value.of("bob1"), ValueKind.USER);

        // query bar.bob as global
        Context.ScopeValue<?> value = bar.getOrCreate("bob", false, Visibility.GLOBAL).value();

        assertThat(value.asString().orElse(null), is("bob1"));
        assertThat(foo.get("bob").value(), is(value));
    }

    @Test
    void testGlobalPop() {
        // https://github.com/helidon-io/helidon-build-tools/issues/1069
        try {
            Context context = new Context();
            context.pushScope(s -> s.getOrCreate("flavor", false, true)).value(Value.of("se"), ValueKind.USER);
            context.pushScope(s -> s.getOrCreate("app-type", false, true)).value(Value.of("quickstart"), ValueKind.USER);
            context.scope().getOrCreate("media", false, false).value(Value.of(List.of("json")), ValueKind.USER);
            context.popScope();
            context.popScope();
            context.pushScope(s -> s.getOrCreate("media", false, false));
            assertThat(context.scope().value().asList().orElse(List.of()), is(List.of("json")));
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void testGlobalMismatchDeep() {
        Context.Scope root = new Context.Scope();

        // pre-create foo and bar as global
        Context.Scope foo = root.getOrCreate("foo", false, Visibility.GLOBAL);
        Context.Scope bar = foo.getOrCreate("bar", false, Visibility.GLOBAL);

        // pre-create foo.bob.alice as global
        foo.getOrCreate("bob", false, Visibility.GLOBAL)
                .getOrCreate("alice", false, Visibility.GLOBAL)
                .value(Value.of("alice1"), ValueKind.USER);

        // create bar.bob.alice as global
        Context.ScopeValue<?> value = bar.getOrCreate("bob", false, Visibility.GLOBAL)
                .getOrCreate("alice", false, Visibility.GLOBAL)
                .value();

        assertThat(value.asString().orElse(null), is("alice1"));
    }

    @Test
    void testGlobalLookup() {
        Context.Scope root = new Context.Scope();

        // pre-create foo and bar as global
        Context.Scope foo = root.getOrCreate("foo", false, Visibility.GLOBAL);
        Context.Scope bar = foo.getOrCreate("bar", false, Visibility.GLOBAL);

        // pre-create foo.bob.alice as global
        foo.getOrCreate("bob", false, Visibility.GLOBAL)
                .getOrCreate("alice", false, Visibility.GLOBAL)
                .value(Value.of("alice1"), ValueKind.USER);

        // lookup "alice" via bar
        Context.ScopeValue<?> value = bar.get("alice").value();
        assertThat(value.asString().orElse(null), is("alice1"));
    }

    @Test
    void testPutCurrent() {
        Context.Scope scope = new Context.Scope();

        // pre-create foo and bar as global
        scope.getOrCreate("foo", false, Visibility.GLOBAL)
                .getOrCreate("bar", false, Visibility.GLOBAL)
                .value(Value.of("bar1"), ValueKind.USER);

        Context.ScopeValue<?> value;

        value = scope.get("bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));

        value = value.scope().get("~bar").value();
        assertThat(value.asString().orElse(null), is("bar1"));
    }

    @Test
    void testPutInvalidPath() {
        Context.Scope scope = new Context.Scope();
        assertThrows(IllegalArgumentException.class, () -> scope.getOrCreate(".foo"));
    }

    @Test
    void testPathGlobalImplicit() {
        Context.Scope scope = new Context.Scope();
        scope = scope.getOrCreate("foo", false, Visibility.GLOBAL)
                .getOrCreate("bar", false, Visibility.LOCAL);
        assertThat(scope.key(), is("bar"));
        assertThat(scope.internalKey(), is("foo.bar"));
    }

    @Test
    void testPathGlobal() {
        Context.Scope scope = new Context.Scope();
        scope = scope.getOrCreate("foo", false, Visibility.GLOBAL)
                .getOrCreate("bar", false, Visibility.GLOBAL);
        assertThat(scope.key(), is("bar"));
        assertThat(scope.internalKey(), is("foo.bar"));
    }

    @Test
    void testLocal() {
        Context.Scope scope = new Context.Scope();
        scope = scope.getOrCreate("global", false, Visibility.GLOBAL)
                .getOrCreate("foo", false, Visibility.LOCAL)
                .getOrCreate("bar", false, Visibility.LOCAL);

        assertThat(scope.key(), is("foo.bar"));
        assertThat(scope.internalKey(), is("global.foo.bar"));
    }

    @Test
    void testVisitValues() {
        Context.Scope scope = new Context.Scope();
        scope.getOrCreate("foo").value(Value.of("foo1"), ValueKind.USER);
        scope.getOrCreate("foo.bar").value(Value.of("bar1"), ValueKind.USER);
        scope.getOrCreate("bob").value(Value.of("bob1"), ValueKind.USER);
        scope.getOrCreate("bob.alice").value(Value.of("alice1"), ValueKind.USER);

        List<Context.Scope> nodes = new ArrayList<>();
        scope.visit(nodes::add);
        Iterator<Context.Scope> it = nodes.iterator();

        scope = it.next();

        assertThat(scope.internalKey(), is(""));
        assertThat(scope.value().isEmpty(), is(true));

        scope = it.next();
        assertThat(scope.internalKey(), is("foo"));
        assertThat(scope.value().asString().orElse(null), is("foo1"));

        scope = it.next();
        assertThat(scope.internalKey(), is("foo.bar"));
        assertThat(scope.value().asString().orElse(null), is("bar1"));

        scope = it.next();
        assertThat(scope.internalKey(), is("bob"));
        assertThat(scope.value().asString().orElse(null), is("bob1"));

        scope = it.next();
        assertThat(scope.internalKey(), is("bob.alice"));
        assertThat(scope.value().asString().orElse(null), is("alice1"));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testPrint() {
        Context.Scope scope = new Context.Scope();
        scope.getOrCreate("foo").value(Value.of("foo1"), ValueKind.USER);
        scope.getOrCreate("foo.bar").value(Value.of("bar1"), ValueKind.USER);
        scope.getOrCreate("bob").value(Value.of("bob1"), ValueKind.USER);
        scope.getOrCreate("bob.alice").value(Value.of("alice1"), ValueKind.USER);

        String actual = Strings.normalizeNewLines(scope.print());

        assertThat(actual, is(""
                              + " +- foo\n"
                              + "     : foo1 (USER)\n"
                              + " | \\- bar\n"
                              + "       : bar1 (USER)\n"
                              + " \\- bob\n"
                              + "     : bob1 (USER)\n"
                              + "   \\- alice\n"
                              + "       : alice1 (USER)\n"));
    }
}
