/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link InputTree}.
 */
class InputTreeTest {

    @Test
    void testSomething() {
        InputTree tree = create("e2e");
        tree.print();
        //   InputPermutations collector = create(Path.of("/Users/batsatt/dev/helidon/archetypes-v2")); // TODO REMOVE
        assertThat(tree, is(not(nullValue())));
    }

    private InputTree create(String testDir) {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/" + testDir);
        return create(sourceDir);
    }

    private InputTree create(Path sourceDir) {
        return InputTree.builder()
                        .archetypePath(sourceDir)
                        .build();
    }
}
