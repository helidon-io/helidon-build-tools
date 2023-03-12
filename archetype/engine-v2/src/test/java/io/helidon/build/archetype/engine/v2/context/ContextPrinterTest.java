/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
import io.helidon.build.common.Strings;
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

        String actual = Strings.normalizeNewLines(ContextPrinter.print(node));

        assertThat(actual, is(""
                + " +- foo\n"
                + "     : foo1 (USER)\n"
                + "     : foo2 (USER)\n"
                + " | \\- bar\n"
                + "       : bar1 (USER)\n"
                + " \\- bob\n"
                + "     : bob1 (USER)\n"
                + "   \\- alice\n"
                + "       : alice1 (USER)\n"));
    }
}
