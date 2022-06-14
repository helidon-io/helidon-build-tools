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

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link CopyOnWriteContextEdge}.
 */
class CopyOnWriteContextEdgeTest {

    @Test
    void testFirstWrite() {
        ContextNode root = ContextNode.create(CopyOnWriteContextEdge::create);
        ContextValue value = root.putValue("foo", Value.create("foo1"), ValueKind.USER);
        assertThat(value.asString(), is("foo1"));
        assertThat(value.scope().parent(), is(root));
    }

    @Test
    void testSimpleCopy() {
        ContextNode root = ContextNode.create(CopyOnWriteContextEdge::create);

        ContextValue value;

        value = root.putValue("foo", Value.create("foo1"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo1"));
        assertThat(value.scope().parent(), is(root));

        value = root.putValue("foo", Value.create("foo2"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo2"));
        assertThat(value.scope().parent(), is(not(root)));

        value = value.scope().getValue("foo");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo2"));
        assertThat(value.scope().parent(), is(not(root)));

        value = root.getValue("foo");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo1"));
        assertThat(value.scope().parent(), is(root));
    }

    @Test
    void testDeepCopy() {
        ContextNode root = ContextNode.create(CopyOnWriteContextEdge::create);

        ContextValue value;

        // populate the tree
        value = root.putValue("foo", Value.create("foo1"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo1"));
        assertThat(value.scope().parent(), is(root));

        value = root.putValue("foo.bar", Value.create("bar1"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));
        assertThat(value.scope().parent().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().parent(), is(root));

        value = root.putValue("bob", Value.create("bob1"), ValueKind.USER);
        assertThat(value.asString(), is("bob1"));
        assertThat(value.scope().parent(), is(root));

        value = root.putValue("bob.alice", Value.create("alice1"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("alice1"));
        assertThat(value.scope().parent().parent(), is(not(nullValue())));
        assertThat(value.scope().parent().parent(), is(root));

        value = root.putValue("foo", Value.create("foo2"), ValueKind.USER);
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo2"));
        assertThat(value.scope().parent(), is(not(root)));

        ContextScope copyScope = value.scope();
        assertThat(copyScope.parent(), is(not(root)));
        assertThat(copyScope.parent0(), is(root));

        // absolute query using altered root
        value = copyScope.getValue("~foo");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("foo2"));

        // local query
        value = copyScope.getValue("bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));

        // absolute query using altered root
        value = copyScope.getValue("~foo.bar");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("bar1"));

        // make sure that all branches are copied
        value = copyScope.getValue("~bob.alice");
        assertThat(value, is(not(nullValue())));
        assertThat(value.asString(), is("alice1"));

        // check variations
        value = root.getValue("foo");
        assertThat(value, is(not(nullValue())));

        ContextEdge edge = ((ContextNode) value.scope()).edge();
        assertThat(edge, is(instanceOf(CopyOnWriteContextEdge.class)));
        List<CopyOnWriteContextEdge> variations = ((CopyOnWriteContextEdge) edge).variations();
        assertThat(variations.size(), is(2));
        assertThat(variations.get(0), is(edge));
        assertThat(variations.get(1).value(), is(not(nullValue())));
        assertThat(variations.get(1).value().asString(), is("foo2"));
        assertThat(variations.get(1).variations(), is(variations));
    }

    @Test
    void testVisitEdges() {
        ContextNode node = ContextNode.create(CopyOnWriteContextEdge::create);
        node.putValue("foo", Value.create("foo1"), ValueKind.USER);
        node.putValue("foo.bar", Value.create("bar1"), ValueKind.USER);
        node.putValue("bob", Value.create("bob1"), ValueKind.USER);
        node.putValue("bob.alice", Value.create("alice1"), ValueKind.USER);
        node.putValue("foo", Value.create("foo2"), ValueKind.USER);

        int[] index = new int[]{0};
        node.visitEdges(edge -> {
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
                case 5:
                    assertThat(path, is("foo.bar"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("bar1"));
                    break;
                case 4:
                    assertThat(path, is("foo"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("foo2"));
                    break;
                case 6:
                    assertThat(path, is("bob"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("bob1"));
                    break;
                case 7:
                    assertThat(path, is("bob.alice"));
                    assertThat(edge.value(), is(not(nullValue())));
                    assertThat(edge.value().asString(), is("alice1"));
                    break;
                default:
                    Assertions.fail("Unexpected index: " + index[0]);
            }
        });
        assertThat(index[0], is(7));
    }
}
