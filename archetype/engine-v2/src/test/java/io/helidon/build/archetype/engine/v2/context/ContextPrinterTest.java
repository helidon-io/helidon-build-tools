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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link ContextPrinter}.
 */
class ContextPrinterTest {

    @Test
    void testPrint() {
        ContextNode node = ContextNode.create(CopyOnWriteContextEdge::create);
        node.putValue("foo", Value.create("foo1"), ValueKind.USER);
        node.putValue("foo.bar", Value.create("bar1"), ValueKind.USER);
        node.putValue("bob", Value.create("bob1"), ValueKind.USER);
        node.putValue("bob.alice", Value.create("alice1"), ValueKind.USER);
        node.putValue("foo", Value.create("foo2"), ValueKind.USER);

        String actual = ContextPrinter.print(node);
        assertThat(actual, is(""
                + " +- foo:foo1(USER)\n"
                + " | \\- bar:bar1(USER)\n"
                + " +- foo:foo2(USER)\n"
                + " | \\- bar:bar1(USER)\n"
                + " \\- bob:bob1(USER)\n"
                + "   \\- alice:alice1(USER)\n"));
    }
}
