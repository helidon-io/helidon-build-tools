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

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.common.Strings;
import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.common.test.utils.TestFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test helper.
 */
class TestHelper {

    /**
     * Create a unique directory.
     *
     * @param parent parent
     * @param name   name
     * @return directory
     */
    static Path uniqueDir(Path parent, String name) {
        Path newDir = parent.resolve(name);
        for (int i = 1; Files.exists(newDir); i++) {
            newDir = parent.resolve(name + "-" + i);
        }
        return newDir;
    }

    /**
     * Read the content of a file as a string.
     *
     * @param file file
     * @return string
     * @throws IOException if an IO error occurs
     */
    static String readFile(Path file) throws IOException {
        return Strings.normalizeNewLines(Files.readString(file));
    }

    /**
     * Create a new archetype engine.
     *
     * @param path archetype directory path under {@code test-classes}.
     * @return engine
     */
    static ArchetypeEngineV2 engine(String path) {
        Path target = TestFiles.targetDir(TestHelper.class);
        FileSystem fileSystem = VirtualFileSystem.create(target.resolve("test-classes/" + path));
        return new ArchetypeEngineV2(fileSystem);
    }

    /**
     * Load a script using the {@code test-classes} directory.
     *
     * @param path path
     * @return script
     */
    static Script load(String path) {
        Path target = TestFiles.targetDir(TestHelper.class);
        Path testResources = target.resolve("test-classes");
        return ScriptLoader.load(testResources.resolve(path));
    }

    /**
     * Load a script using class-loader resource.
     *
     * @param path resource path
     * @return script
     */
    static Script load0(String path) {
        InputStream is = TestHelper.class.getClassLoader().getResourceAsStream(path);
        assertThat(is, is(notNullValue()));
        return ScriptLoader.load0(is);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    static void walk(Node.Visitor<Void> visitor, Script script) {
        Walker.walk(visitor, script, null);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    static void walk(Input.Visitor<Void> visitor, Script script) {
        Walker.walk(new VisitorAdapter<>(visitor, null, null), script, null);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    static void walk(Output.Visitor<Void> visitor, Script script) {
        Walker.walk(new VisitorAdapter<>(null, visitor, null), script, null);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    static void walk(Model.Visitor<Void> visitor, Script script) {
        Walker.walk(new VisitorAdapter<>(null, null, visitor), script, null);
    }

    /**
     * Create a block builder.
     *
     * @param kind       block kind
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder block(Block.Kind kind, Block.Builder... statements) {
        Block.Builder builder = Block.builder(null, null, kind);
        for (Block.Builder statement : statements) {
            builder.addChild(statement);
        }
        return builder;
    }

    /**
     * Create an output block builder.
     *
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder output(Block.Builder... statements) {
        return block(Block.Kind.OUTPUT, statements);
    }

    /**
     * Create a model block builder.
     *
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder model(Block.Builder... statements) {
        return block(Block.Kind.MODEL, statements);
    }

    /**
     * Create a model map block builder.
     *
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder modelMap(Block.Builder... statements) {
        return modelBuilder(null, Block.Kind.MAP, 100, statements);
    }

    /**
     * Create a model map block builder.
     *
     * @param key        model key
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder modelMap(String key, Block.Builder... statements) {
        return modelBuilder(key, Block.Kind.MAP, 100, statements);
    }

    /**
     * Create a model list block builder.
     *
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder modelList(Block.Builder... statements) {
        return modelBuilder(null, Block.Kind.LIST, 100, statements);
    }

    /**
     * Create a model list block builder.
     *
     * @param key        model key
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder modelList(String key, Block.Builder... statements) {
        return modelBuilder(key, Block.Kind.LIST, 100, statements);
    }

    /**
     * Create a model value block builder.
     *
     * @param value value
     * @return block builder
     */
    static Block.Builder modelValue(String value) {
        return modelBuilder(null, Block.Kind.VALUE, 100).value(value);
    }

    /**
     * Create a model value block builder.
     *
     * @param value value
     * @param order order
     * @return block builder
     */
    static Block.Builder modelValue(String value, int order) {
        return modelBuilder(null, Block.Kind.VALUE, order).value(value);
    }

    /**
     * Create a model value block builder.
     *
     * @param key   key
     * @param value value
     * @return block builder
     */
    static Block.Builder modelValue(String key, String value) {
        return modelBuilder(key, Block.Kind.VALUE, 100).value(value);
    }

    /**
     * Create a model value block builder.
     *
     * @param key   key
     * @param value value
     * @param order order
     * @return block builder
     */
    static Block.Builder modelValue(String key, String value, int order) {
        return modelBuilder(key, Block.Kind.VALUE, order).value(value);
    }

    /**
     * Create an input option block builder.
     *
     * @param name       option name
     * @param value      option value
     * @param statements nested statements
     * @return block builder
     */
    static Block.Builder inputOption(String name, String value, Block.Builder... statements) {
        Block.Builder builder = Input.builder(null, null, Block.Kind.OPTION)
                                     .attributes(inputAttributes(name, value));
        for (Block.Builder statement : statements) {
            builder.addChild(statement);
        }
        return builder;
    }

    /**
     * Create an input text block builder.
     *
     * @param name         option name
     * @param defaultValue default value
     * @return block builder
     */
    static Block inputText(String name, String defaultValue, Block.Builder... statements) {
        return inputBuilder(name, Block.Kind.TEXT, defaultValue, statements);
    }

    /**
     * Create an input boolean block builder.
     *
     * @param name         option name
     * @param defaultValue default value
     * @param statements   nested statements
     * @return block builder
     */
    static Block inputBoolean(String name, boolean defaultValue, Block.Builder... statements) {
        return inputBuilder(name, Block.Kind.BOOLEAN, String.valueOf(defaultValue), statements);
    }

    /**
     * Create an input enum block builder.
     *
     * @param name         option name
     * @param defaultValue default value
     * @param statements   nested statements
     * @return block builder
     */
    static Block inputEnum(String name, String defaultValue, Block.Builder... statements) {
        return inputBuilder(name, Block.Kind.ENUM, defaultValue, statements);
    }

    /**
     * Create an input list block builder.
     *
     * @param name         option name
     * @param defaultValue default value
     * @param statements   nested statements
     * @return block builder
     */
    static Block inputList(String name, List<String> defaultValue, Block.Builder... statements) {
        return inputBuilder(name, Block.Kind.LIST, String.join(",", defaultValue), statements);
    }

    private static Block inputBuilder(String name, Block.Kind kind, String defaultValue, Block.Builder... statements) {
        Block.Builder builder = Input.builder(null, null, kind)
                                     .attributes(inputAttributes(name, defaultValue, name));
        for (Block.Builder statement : statements) {
            builder.addChild(statement);
        }
        return builder.build();
    }

    private static Block.Builder modelBuilder(String key, Block.Kind kind, int order, Block.Builder... statements) {
        Block.Builder builder = Model.builder(null, null, kind)
                                     .attributes(Map.of("order", String.valueOf(order)));
        if (key != null) {
            builder.attributes(Map.of("key", key));
        }
        for (Block.Builder statement : statements) {
            builder.addChild(statement);
        }
        return builder;
    }

    private static Map<String, String> inputAttributes(String name, String value) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", name);
        attributes.put("value", value);
        return attributes;
    }

    private static Map<String, String> inputAttributes(String name, String defaultValue, String prompt) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", name);
        attributes.put("default", defaultValue);
        attributes.put("prompt", prompt);
        return attributes;
    }
}
