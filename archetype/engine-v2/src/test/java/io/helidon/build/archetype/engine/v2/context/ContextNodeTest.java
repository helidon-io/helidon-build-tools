/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2.context;

import io.helidon.build.archetype.engine.v2.context.ContextScope.Visibility;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link ContextNode}.
 */
class ContextNodeTest {

    @Test
    void testSimpleGetOrCreate() {
        ContextNode root = ContextNode.create();
        ContextNode scope = root.getOrCreate("foo", Visibility.GLOBAL);
        assertThat(scope, is(not(nullValue())));
        assertThat(scope.id(), is("foo"));
        assertThat(scope.visibility(), is(Visibility.GLOBAL));
        assertThat(scope.parent(), is(root));
        assertThat(scope.root(), is(root));
    }

    @Test
    void testPutNonExisting() {
        ContextNode root = ContextNode.create();
        ContextValue value = root.putValue("foo.bar", Value.create("bar1"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));
        assertThat(value.scope(), is(not(nullValue())));
        assertThat(value.scope().path(true), is("foo.bar"));
        assertThat(value.scope().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().path(true), is("foo"));
        assertThat(value.scope().parent().visibility(), is(Visibility.UNSET));
        assertThat(value.scope().parent().parent(), is(root));
    }

    @Test
    void testPutExisting() {
        ContextNode root = ContextNode.create();

        // pre-create foo as global
        ContextNode scope = root.getOrCreate("foo", Visibility.GLOBAL);
        assertThat(scope, is(not(nullValue())));
        assertThat(scope.id(), is("foo"));
        assertThat(scope.visibility(), is(Visibility.GLOBAL));

        ContextValue value = root.putValue("foo.bar.bob", Value.create("bob1"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bob1"));
        assertThat(value.scope(), is(not(nullValue())));
        assertThat(value.scope().path(true), is("foo.bar.bob"));
        assertThat(value.scope().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().path(true), is("foo.bar"));
        assertThat(value.scope().parent().visibility(), is(Visibility.UNSET));
        assertThat(value.scope().parent().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().parent().path(true), is("foo"));
        assertThat(value.scope().parent().parent().visibility(), is(Visibility.GLOBAL));
        assertThat(value.scope().parent().parent().parent(), is(root));
    }

    @Test
    void testGetValueOrCreateInvalidPath() {
        ContextNode root = ContextNode.create();
        assertThrows(NullPointerException.class, () -> root.getOrCreate(null, true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate(".", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("~", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("..", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate(".foo..", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("~foo", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("foo.bar", true));
    }

    @Test
    void testSimpleGetValue() {
        ContextNode root = ContextNode.create();
        root.putValue("foo", Value.create("foo1"), ValueKind.USER);

        ContextValue value;

        value = root.getValue("foo");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo1"));

        value = root.getValue("~foo");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo1"));
    }

    @Test
    void testGetLocal1() {
        ContextNode scope = ContextNode.create();
        scope.putValue("foo.bar", Value.create("bar1"), ValueKind.USER);

        ContextValue value;

        value = scope.getValue("foo.bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));

        value = value.scope().getValue("~bar");
        assertThat(value, is(nullValue()));
    }

    @Test
    void testGetLocal2() {
        ContextNode scope = ContextNode.create();
        scope.getOrCreate("foo", Visibility.GLOBAL)
             .getOrCreate("bar", Visibility.LOCAL)
             .putValue("bob", Value.create("bob1"), ValueKind.USER);

        ContextValue value;

        value = scope.getValue("bob");
        assertThat(value, is(nullValue()));

        value = scope.getValue("foo.bar.bob");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bob1"));

        value = scope.getValue("bar.bob");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bob1"));

        value = scope.getValue("~bar.bob");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bob1"));
    }

    @Test
    void testGetGlobalImplicit1() {
        ContextNode scope = ContextNode.create();

        // pre-create foo as global
        scope.getOrCreate("foo", Visibility.GLOBAL)
             .putValue("bar", Value.create("bar1"), ValueKind.USER);

        ContextValue value;

        value = scope.getValue("bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));

        value = value.scope().getValue("~bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));
    }

    @Test
    void testGetGlobalImplicit2() {
        ContextNode scope = ContextNode.create();

        // pre-create foo and bar as global
        scope.getOrCreate("foo", Visibility.GLOBAL)
             .getOrCreate("bar", Visibility.GLOBAL)
             .putValue("bob", Value.create("bob1"), ValueKind.USER);

        ContextValue value;

        value = scope.getValue("bob");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bob1"));

        value = value.scope().getValue("~bob");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bob1"));
    }

    @Test
    void testGetGlobal() {
        ContextNode scope = ContextNode.create();

        // pre-create foo and bar as global
        scope.getOrCreate("foo", Visibility.GLOBAL)
             .getOrCreate("bar", Visibility.GLOBAL);

        scope.putValue("bar", Value.create("bar1"), ValueKind.USER);

        ContextValue value;

        value = scope.getValue("bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));

        value = value.scope().getValue("~bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));
    }

    @Test
    void testPutExistingReadOnly() {
        ContextNode scope = ContextNode.create();
        scope.putValue("foo", Value.create("foo"), ValueKind.EXTERNAL);
        assertThrows(IllegalStateException.class, () -> scope.putValue("foo", Value.create("bar"), ValueKind.EXTERNAL));
    }

    @Test
    void testPutCurrent() {
        ContextNode scope = ContextNode.create();

        // pre-create foo and bar as global
        scope.getOrCreate("foo", Visibility.GLOBAL)
             .getOrCreate("bar", Visibility.GLOBAL)
             .putValue("", Value.create("bar1"), ValueKind.USER);

        ContextValue value;

        value = scope.getValue("bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));

        value = value.scope().getValue("~bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));
    }

    @Test
    void testPutInvalidPath() {
        ContextNode scope = ContextNode.create();
        assertThrows(IllegalArgumentException.class, () -> scope.putValue(".foo", Value.create("foo"), ValueKind.EXTERNAL));
    }

    @Test
    void testPathGlobalImplicit() {
        ContextNode scope = ContextNode.create();
        scope = scope.getOrCreate("foo", Visibility.GLOBAL)
                     .getOrCreate("bar", Visibility.LOCAL);
        assertThat(scope.path(), is("bar"));
        assertThat(scope.path(true), is("foo.bar"));
    }

    @Test
    void testPathGlobal() {
        ContextNode scope = ContextNode.create();
        scope = scope.getOrCreate("foo", Visibility.GLOBAL)
                     .getOrCreate("bar", Visibility.GLOBAL);
        assertThat(scope.path(), is("bar"));
        assertThat(scope.path(true), is("foo.bar"));
    }

    @Test
    void testLocal() {
        ContextNode scope = ContextNode.create();
        scope = scope.getOrCreate("global", Visibility.GLOBAL)
                     .getOrCreate("foo", Visibility.LOCAL)
                     .getOrCreate("bar", Visibility.LOCAL);

        assertThat(scope.path(), is("foo.bar"));
        assertThat(scope.path(true), is("global.foo.bar"));
    }

    @Test
    void testVisitValues() {
        ContextNode scope = ContextNode.create();
        scope.putValue("foo", Value.create("foo1"), ValueKind.USER);
        scope.putValue("foo.bar", Value.create("bar1"), ValueKind.USER);
        scope.putValue("bob", Value.create("bob1"), ValueKind.USER);
        scope.putValue("bob.alice", Value.create("alice1"), ValueKind.USER);
        int[] index = new int[]{0};
        scope.visitEdges(edge -> {
            String path = edge.node().path(true);
            switch (++index[0]) {
                case 1:
                    assertThat(path, is(""));
                    assertThat(edge.value(), is(nullValue()));
                    break;
                case 2:
                    assertThat(path, is("foo"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("foo1"));
                    break;
                case 3:
                    assertThat(path, is("foo.bar"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("bar1"));
                    break;
                case 4:
                    assertThat(path, is("bob"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("bob1"));
                    break;
                case 5:
                    assertThat(path, is("bob.alice"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("alice1"));
                    break;
                default:
                    Assertions.fail("Unexpected index: " + index[0]);
            }
        });
        assertThat(index[0], is(5));
    }
}
