/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.FileSystem;
import java.nio.file.Path;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.common.VirtualFileSystem;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.v2.json.JsonFactory.jsonDiff;
import static io.helidon.build.archetype.v2.json.JsonFactory.readJson;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static javax.json.JsonValue.EMPTY_JSON_ARRAY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link ScriptSerializer}.
 */
class ScriptSerializerTest {

    @Test
    void testEmptyScript() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/empty-script");
        Path expected = targetDir.resolve("test-classes/expected/empty-script.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testInlined1() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/inlined1");
        Path expected = targetDir.resolve("test-classes/expected/inlined1.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testInlined2() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/inlined2");
        Path expected = targetDir.resolve("test-classes/expected/inlined2.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testInlined3() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/inlined3");
        Path expected = targetDir.resolve("test-classes/expected/inlined3.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testInlined4() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/inlined4");
        Path expected = targetDir.resolve("test-classes/expected/inlined4.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testEmptyMethodNotCompiled() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/empty-method");
        Path expected = targetDir.resolve("test-classes/expected/empty-method.json");

        JsonObject archetypeJson = ScriptSerializer.serialize0(loadScript(sourceDir), true);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testEmptyMethodCompiled() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/empty-method");
        Path expected = targetDir.resolve("test-classes/expected/empty.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs, true);
        System.out.println(JsonFactory.toPrettyString(archetypeJson));
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testFiltering1() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/filtering1");
        Path expected = targetDir.resolve("test-classes/expected/filtering1.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testFiltering2() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/filtering2");
        Path expected = targetDir.resolve("test-classes/expected/empty.json");
        FileSystem fs = VirtualFileSystem.create(sourceDir);

        JsonObject archetypeJson = ScriptSerializer.serialize(fs);
        assertThat(jsonDiff(archetypeJson, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testConvertExpression() throws IOException {
        int id = 1;
        JsonObjectBuilder builder = JsonFactory.createObjectBuilder();
        //noinspection UnusedAssignment
        builder.add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains 'foo'"))
               .add(String.valueOf(id++), convertExpression("!(['', 'adc', 'def'] contains 'foo' == false && false)"))
               .add(String.valueOf(id++), convertExpression("!false"))
               .add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains 'foo' == false && true || !false"))
               .add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains 'foo' == false && true || !true"))
               .add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains 'def'"))
               .add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains 'foo' == true && false"))
               .add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains 'foo' == false && true"))
               .add(String.valueOf(id++), convertExpression("'aaa' == 'aaa' && ['', 'adc', 'def'] contains ''"))
               .add(String.valueOf(id++), convertExpression("true && \"bar\" == 'foo1' || true"))
               .add(String.valueOf(id++), convertExpression("true && \"bar\" == 'foo1' || false"))
               .add(String.valueOf(id++), convertExpression("('def' != 'def1') && false == true"))
               .add(String.valueOf(id++), convertExpression("('def' != 'def1') && false"))
               .add(String.valueOf(id++), convertExpression("('def' != 'def1') && true"))
               .add(String.valueOf(id++), convertExpression("'def' != 'def1'"))
               .add(String.valueOf(id++), convertExpression("'def' == 'def'"))
               .add(String.valueOf(id++), convertExpression("'def' != 'def'"))
               .add(String.valueOf(id++), convertExpression("true==((true|| false)&&true)"))
               .add(String.valueOf(id++), convertExpression("false==((true|| false)&&true)"))
               .add(String.valueOf(id++), convertExpression("false==((true|| false)&&false)"))
               .add(String.valueOf(id++), convertExpression("true == 'def'"))
               .add(String.valueOf(id++), convertExpression("'true' || 'def'"))
               .add(String.valueOf(id++), convertExpression("['', 'adc', 'def'] contains ['', 'adc', 'def']"))
               .add(String.valueOf(id++), convertExpression("true == ${def}"));
        JsonObject jsonObject = builder.build();

        Path targetDir = targetDir(this.getClass());
        Path expected = targetDir.resolve("test-classes/expected/expressions.json");
        assertThat(jsonDiff(jsonObject, readJson(expected)), is(EMPTY_JSON_ARRAY));
    }

    @Test
    void testVariables() {
        Path targetDir = targetDir(this.getClass());
        Path scriptPath = targetDir.resolve("test-classes/variables.xml");
        JsonObject jsonObject = ScriptSerializer.serialize(ScriptLoader.load(scriptPath), false);
        JsonArray children = jsonObject.getJsonArray("children");
        assertThat(children, is(not(nullValue())));
        assertThat(children.size(), is(1));

        JsonObject variablesNode = children.getJsonObject(0);
        assertThat(variablesNode, is(not(nullValue())));

        JsonArray variables = variablesNode.getJsonArray("children");
        assertThat(variables, is(not(nullValue())));
        assertThat(variables.size(), is(6));

        JsonObject var1 = variables.getJsonObject(0);
        assertThat(var1, is(not(nullValue())));
        assertThat(var1.getString("path"), is("var1"));
        assertThat(var1.isNull("value"), is(true));

        JsonObject var2 = variables.getJsonObject(1);
        assertThat(var2, is(not(nullValue())));
        assertThat(var2.getString("path"), is("var2"));
        assertThat(var2.getString("value"), is("some-value"));

        JsonObject var3 = variables.getJsonObject(2);
        assertThat(var3, is(not(nullValue())));
        assertThat(var3.getString("path"), is("var3"));
        assertThat(var3.getBoolean("value"), is(true));

        JsonObject var4 = variables.getJsonObject(3);
        assertThat(var4, is(not(nullValue())));
        assertThat(var4.getString("path"), is("var4"));
        assertThat(var4.getString("value"), is("some-constant"));

        JsonObject var5 = variables.getJsonObject(4);
        assertThat(var5, is(not(nullValue())));
        assertThat(var5.getString("path"), is("var5"));
        assertThat(var5.getJsonArray("value").size(), is(0));

        JsonObject var6 = variables.getJsonObject(5);
        assertThat(var6, is(not(nullValue())));
        assertThat(var6.getString("path"), is("var6"));
        assertThat(var6.getJsonArray("value").getString(0), is("item1"));
    }

    private static Script loadScript(Path sourceDir) {
        FileSystem fs = VirtualFileSystem.create(sourceDir);
        Path scriptPath = fs.getPath("/").resolve("main.xml");
        return ScriptLoader.load(scriptPath);
    }

    private static JsonArray convertExpression(String expression) {
        return ScriptSerializer.serialize(Expression.parse(expression));
    }
}
