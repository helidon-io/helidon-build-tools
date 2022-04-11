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

import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Script;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static io.helidon.build.archetype.engine.v2.TestHelper.walk;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link Walker}.
 */
class WalkerTest {

    @Test
    void testMethodsOverride() {
        Script script = load("walker/method-override.xml");
        int[] index = new int[]{0};
        walk(new Output.Visitor<>() {
            @Override
            public VisitResult visitFile(Output.File file, Void arg) {
                switch (++index[0]) {
                    case 1:
                        assertThat(file.source(), is("foo1.txt"));
                        assertThat(file.target(), is("foo1.txt"));
                        break;
                    case 2:
                    case 3:
                        assertThat(file.source(), is("foo2.txt"));
                        assertThat(file.target(), is("foo2.txt"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(3));
    }
}
