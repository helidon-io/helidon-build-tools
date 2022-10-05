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
package io.helidon.build.cli.impl;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link ArchetypeInvoker}.
 */
class ArchetypeInvokerTest {

    @Test
    void testHelidonVersion() {
        assertThat(invoker("2.0.1"), is(instanceOf(ArchetypeInvoker.V1Invoker.class)));
        assertThat(invoker("0.9.0"), is(instanceOf(ArchetypeInvoker.V1Invoker.class)));
        assertThat(invoker("1.0.0"), is(instanceOf(ArchetypeInvoker.V1Invoker.class)));
        assertThat(invoker("2.5.1"), is(instanceOf(ArchetypeInvoker.V1Invoker.class)));

        assertThat(invoker("3.0.0-RC1"), is(instanceOf(ArchetypeInvoker.V2Invoker.class)));
        assertThat(invoker("3.0.0-SNAPSHOT"), is(instanceOf(ArchetypeInvoker.V2Invoker.class)));
        assertThat(invoker("3.0.0"), is(instanceOf(ArchetypeInvoker.V2Invoker.class)));
        assertThat(invoker("3.0.1"), is(instanceOf(ArchetypeInvoker.V2Invoker.class)));
    }

    private static ArchetypeInvoker invoker(String helidonVersion) {
        return ArchetypeInvoker.builder()
                               .initOptions(initOptions(helidonVersion))
                               .build();
    }

    private static InitOptions initOptions(String helidonVersion) {
        return new InitOptions(null, null, helidonVersion, null, null, null, null, null, false);
    }
}
