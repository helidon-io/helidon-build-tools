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
    void testGetOrCreate1() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreate("foo", Visibility.GLOBAL);
        assertThat(foo, is(not(nullValue())));
        assertThat(foo.id(), is("foo"));
        assertThat(foo.parent(), is(root));
        assertThat(foo.root(), is(root));
    }

    @Test
    void testGetOrCreate2() {
        ContextScope root = ContextScope.create();
        ContextScope bar = root.getOrCreate("foo.bar", Visibility.GLOBAL);
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.id(), is("bar"));
        assertThat(bar.parent(), is(not(nullValue())));
        assertThat(bar.parent().id(), is("foo"));
        assertThat(bar.parent().parent(), is(root));
        assertThat(bar.root(), is(root));
    }

    @Test
    void testGetOrCreate3() {
        ContextScope root = ContextScope.create();
        ContextScope bar = root.getOrCreate(".foo.bar", Visibility.GLOBAL);
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.id(), is("bar"));
        assertThat(bar.parent(), is(not(nullValue())));
        assertThat(bar.parent().id(), is("foo"));
        assertThat(bar.parent().parent(), is(root));
        assertThat(bar.root(), is(root));
    }

    @Test
    void testGetOrCreate4() {
        ContextScope root = ContextScope.create();
        ContextScope bob = root.getOrCreate("foo.bar.bob", Visibility.GLOBAL);
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
    void testGetOrCreate5() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreate(".foo", Visibility.GLOBAL);
        assertThat(foo, is(not(nullValue())));
        assertThat(foo.id(), is("foo"));
        assertThat(foo.parent(), is(root));
        assertThat(foo.root(), is(root));
    }

    @Test
    void testGetOrCreate6() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreate("foo", Visibility.GLOBAL);
        ContextScope bar = foo.getOrCreate("..bar", Visibility.GLOBAL);
        assertThat(bar, is(not(nullValue())));
        assertThat(bar.id(), is("bar"));
        assertThat(bar.parent(), is(root));
        assertThat(bar.root(), is(root));
    }

    @Test
    void testGetOrCreate7() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreate(".foo", Visibility.GLOBAL);
        ContextScope bar = foo.getOrCreate(".bar", Visibility.GLOBAL);
        ContextScope bob = bar.getOrCreate("bob", Visibility.GLOBAL);
        assertThat(bob, is(not(nullValue())));
        assertThat(bob.id(), is("bob"));
        assertThat(bob.parent(), is(root));
        assertThat(bob.root(), is(root));
    }

    @Test
    void testGetOrCreate8() {
        ContextScope root = ContextScope.create();
        ContextScope foo = root.getOrCreate(".foo", Visibility.GLOBAL);
        ContextScope bob = foo.getOrCreate(".bar..bob", Visibility.GLOBAL);
        assertThat(bob, is(not(nullValue())));
        assertThat(bob.id(), is("bob"));
        assertThat(bob.parent(), is(foo));
        assertThat(bob.root(), is(root));
    }

    @Test
    void testGetOrCreateInvalidPath() {
        ContextScope root = ContextScope.create();
        assertThrows(NullPointerException.class, () -> root.getOrCreate(null, true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate(".", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("..", true));
        assertThrows(IllegalArgumentException.class, () -> root.getOrCreate("foo.bar..", true));
    }
}
