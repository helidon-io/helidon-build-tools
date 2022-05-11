/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.archetype.v2.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.json.JsonObject;

import io.helidon.build.archetype.engine.v2.ast.Script;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.v2.json.JsonFactory.jsonDiff;
import static io.helidon.build.archetype.v2.json.JsonFactory.readJson;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static javax.json.JsonValue.EMPTY_JSON_ARRAY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ScriptDeserializer}.
 */
class ScriptDeserializerTest {

    @Test
    void testDeserialize() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path jsonFile = targetDir.resolve("test-classes/deserializer/script.json");
        Path expected = targetDir.resolve("test-classes/expected/deserializer.json");
        Script script = ScriptDeserializer.deserialize(Files.newInputStream(jsonFile));

        JsonObject archetypeJson = ScriptSerializer.serialize(script);
        System.out.println(JsonFactory.toPrettyString(archetypeJson));
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }
}
