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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.common.VirtualFileSystem;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link InputPermutations}.
 */
class InputPermutationsTest {

    @Test
    void testSomething() {
        InputPermutations collector = create("e2e");
        assertThat(collector, is(not(nullValue())));
    }

    private InputPermutations create(String testDir) {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/" + testDir);
        FileSystem fs = VirtualFileSystem.create(sourceDir);
        Path cwd = fs.getPath("/");
        Context context = Context.create(cwd, Map.of(), Map.of());
        Script script = ScriptLoader.load(cwd.resolve("main.xml"));
        return InputPermutations.create(script, context);
    }
}
