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
package io.helidon.build.archetype.engine.v2;

import io.helidon.build.archetype.engine.v2.ContextScope.Visibility;
import io.helidon.build.archetype.engine.v2.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link ContextScope}.
 */
class ContextScopeTest {

    @Test
    void testGetValueOrCreateScope1() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreateScope("foo", Visibility.GLOBAL);
        assertThat(foo, is(not(nullValue())));
        assertThat(foo.id(), is("foo"));
        assertThat(foo.parent(), is(root));
        assertThat(foo.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope2() {
        ContextScope root = ContextScope.create();
        ContextScope bar = root.getOrCreateScope("foo.bar", Visibility.GLOBAL);
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.id(), is("bar"));
        assertThat(bar.parent(), is(not(nullValue())));
        assertThat(bar.parent().id(), is("foo"));
        assertThat(bar.parent().parent(), is(root));
        assertThat(bar.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope3() {
        ContextScope root = ContextScope.create();
        ContextScope bar = root.getOrCreateScope(".foo.bar", Visibility.GLOBAL);
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.id(), is("bar"));
        assertThat(bar.parent(), is(not(nullValue())));
        assertThat(bar.parent().id(), is("foo"));
        assertThat(bar.parent().parent(), is(root));
        assertThat(bar.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope4() {
        ContextScope root = ContextScope.create();
        ContextScope bob = root.getOrCreateScope("foo.bar.bob", Visibility.GLOBAL);
        assertThat(bob, is(not(nullValue())));
        assertThat(bob.id(), is("bob"));
        assertThat(bob.parent(), is(not(nullValue())));
        assertThat(bob.parent().id(), is("bar"));
        assertThat(bob.parent().parent(), is(not(nullValue())));
        assertThat(bob.parent().parent().id(), is("foo"));
        assertThat(bob.parent().parent().parent(), is(root));
        assertThat(bob.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope5() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreateScope(".foo", Visibility.GLOBAL);
        assertThat(foo, is(not(nullValue())));
        assertThat(foo.id(), is("foo"));
        assertThat(foo.parent(), is(root));
        assertThat(foo.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope6() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreateScope("foo", Visibility.GLOBAL);
        ContextScope bar = foo.getOrCreateScope("..bar", Visibility.GLOBAL);
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.id(), is("bar"));
        assertThat(bar.parent(), is(root));
        assertThat(bar.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope7() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreateScope(".foo", Visibility.GLOBAL);
        ContextScope bar = foo.getOrCreateScope(".bar", Visibility.GLOBAL);
        ContextScope bob = bar.getOrCreateScope("bob", Visibility.GLOBAL);
        assertThat(bob, is(not(nullValue())));
        assertThat(bob.id(), is("bob"));
        assertThat(bob.parent(), is(root));
        assertThat(bob.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScope8() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreateScope(".foo", Visibility.GLOBAL);
        ContextScope bob = foo.getOrCreateScope(".bar..bob", Visibility.GLOBAL);
        assertThat(bob, is(not(nullValue())));
        assertThat(bob.id(), is("bob"));
        assertThat(bob.parent(), is(foo));
        assertThat(bob.root(), is(root));
    }

    @Test
    void testGetValueOrCreateScopeInvalidPath() {
        ContextScope root = ContextScope.create();
        assertThrows(NullPointerException.class, () -> root.getOrCreateScope(null, true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreateScope(".", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreateScope("..", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreateScope(".foo..", true));
    }

    @Test
    void testGetValue1() {
        ContextScope root = ContextScope.create();
        root.putValue("foo", Value.create("foo"), ValueKind.USER);
        Value foo = root.getValue("foo");
        assertThat(foo, is(not(nullValue())));
        assertThat(foo.asString(), is("foo"));
    }

    @Test
    void testGetValue2() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("foo", Visibility.LOCAL);
        scope.putValue("bar", Value.create("bar"), ValueKind.USER);
        Value bar = root.getValue("foo.bar");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bar"));
    }

    @Test
    void testGetValue3() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("foo", Visibility.GLOBAL);
        scope.putValue("bar", Value.create("bar"), ValueKind.USER);
        Value bar = scope.getValue(".bar");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bar"));
    }

    @Test
    void testGetValue4() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("foo", Visibility.GLOBAL);
        scope.putValue("bar", Value.create("bar"), ValueKind.USER);
        Value bar = root.getValue("bar");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bar"));
    }

    @Test
    void testGetValue5() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("foo", Visibility.GLOBAL);
        scope = scope.getOrCreateScope("bar", Visibility.GLOBAL);
        scope.putValue("bob", Value.create("bob"), ValueKind.USER);
        Value bar = root.getValue("bob");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bob"));
    }

    @Test
    void testGetValue6() {
        ContextScope root = ContextScope.create();
        ContextScope fooScope = root.getOrCreateScope("foo", Visibility.GLOBAL);
        ContextScope barScope = fooScope.getOrCreateScope("bar", Visibility.LOCAL);
        barScope.putValue("bob", Value.create("bob"), ValueKind.USER);

        Value bar;

        bar = root.getValue("bob");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bob"));

        bar = fooScope.getValue("bob");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bob"));

        bar = barScope.getValue("bob");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bob"));

        bar = barScope.getValue(".bob");
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.asString(), is("bob"));
    }

    @Test
    void testPutValue1() {
        ContextScope root = ContextScope.create();
        root.putValue("foo", Value.create("foo"), ValueKind.EXTERNAL);
        assertThrows(IllegalStateException.class, () -> root.putValue("foo", Value.create("bar"), ValueKind.EXTERNAL));
    }

    @Test
    void testPutValue2() {
        ContextScope root = ContextScope.create();
        assertThrows(IllegalArgumentException.class, () -> root.putValue(".foo", Value.create("foo"), ValueKind.EXTERNAL));
        assertThrows(IllegalArgumentException.class, () -> root.putValue("foo.bar", Value.create("foo"), ValueKind.EXTERNAL));
        assertThrows(IllegalArgumentException.class, () -> root.putValue("foo..bar", Value.create("foo"), ValueKind.EXTERNAL));
    }

    @Test
    void testPath1() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("foo", Visibility.GLOBAL)
                                 .getOrCreateScope(".bar", Visibility.LOCAL);
        assertThat(scope.path(), is("bar"));
    }

    @Test
    void testPath2() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("foo", Visibility.GLOBAL)
                                 .getOrCreateScope(".bar", Visibility.GLOBAL);
        assertThat(scope.path(), is("bar"));
    }

    @Test
    void testPath3() {
        ContextScope root = ContextScope.create();
        ContextScope scope = root.getOrCreateScope("global", Visibility.GLOBAL)
                                 .getOrCreateScope(".foo", Visibility.LOCAL)
                                 .getOrCreateScope(".bar", Visibility.LOCAL);
        assertThat(scope.path(), is("foo.bar"));
    }
}
