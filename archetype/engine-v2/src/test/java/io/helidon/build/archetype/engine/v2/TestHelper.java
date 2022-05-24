/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.BuilderInfo;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.Instance;
import io.helidon.build.common.Strings;
import io.helidon.build.common.VirtualFileSystem;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;

/**
 * Test helper.
 */
public class TestHelper {

    private static final BuilderInfo BUILDER_INFO = BuilderInfo.of(ScriptLoader.create(), Path.of("test-helper.xml"), null);
    private static final Instance<FileSystem> FS = new Instance<>(TestHelper::createTestFileSystem);

    private static FileSystem createTestFileSystem() {
        Path target = targetDir(TestHelper.class);
        Path testResources = target.resolve("test-classes");
        return VirtualFileSystem.create(testResources);
    }

    /**
     * Read the content of a file as a string.
     *
     * @param file file
     * @return string
     * @throws IOException if an IO error occurs
     */
    public static String readFile(Path file) throws IOException {
        return Strings.normalizeNewLines(Files.readString(file));
    }

    /**
     * Load a script using the {@code test-classes} directory.
     *
     * @param path path
     * @return script
     */
    public static Script load(String path) {
        return ScriptLoader.load(FS.instance().getPath(path));
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    public static void walk(Node.Visitor<Void> visitor, Script script) {
        Walker.walk(visitor, script, null);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    public static void walk(Input.Visitor<Void> visitor, Script script) {
        Walker.walk(new VisitorAdapter<>(visitor, null, null), script, null);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    public static void walk(Output.Visitor<Void> visitor, Script script) {
        Walker.walk(new VisitorAdapter<>(null, visitor, null), script, null);
    }

    /**
     * Walk the given script.
     *
     * @param visitor visitor
     * @param script  script
     */
    public static void walk(Model.Visitor<Void> visitor, Script script) {
        Walker.walk(new VisitorAdapter<>(null, null, visitor), script, null);
    }

    /**
     * Create a block builder.
     *
     * @param kind     block kind
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder block(Block.Kind kind, Block.Builder... children) {
        Block.Builder builder = Block.builder(BUILDER_INFO, kind);
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder;
    }

    /**
     * Create an output block builder.
     *
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder output(Block.Builder... children) {
        return block(Block.Kind.OUTPUT, children);
    }

    /**
     * Create a model block builder.
     *
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder model(Block.Builder... children) {
        return block(Block.Kind.MODEL, children);
    }

    /**
     * Create a model map block builder.
     *
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder modelMap(Block.Builder... children) {
        return modelBuilder(null, Block.Kind.MAP, 100, children);
    }

    /**
     * Create a model map block builder.
     *
     * @param key      model key
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder modelMap(String key, Block.Builder... children) {
        return modelBuilder(key, Block.Kind.MAP, 100, children);
    }

    /**
     * Create a model list block builder.
     *
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder modelList(Block.Builder... children) {
        return modelBuilder(null, Block.Kind.LIST, 100, children);
    }

    /**
     * Create a model list block builder.
     *
     * @param key      model key
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder modelList(String key, Block.Builder... children) {
        return modelBuilder(key, Block.Kind.LIST, 100, children);
    }

    /**
     * Create a model value block builder.
     *
     * @param value value
     * @return block builder
     */
    public static Block.Builder modelValue(String value) {
        return modelBuilder(null, Block.Kind.VALUE, 100).value(value);
    }

    /**
     * Create a model value block builder.
     *
     * @param value value
     * @param order order
     * @return block builder
     */
    public static Block.Builder modelValue(String value, int order) {
        return modelBuilder(null, Block.Kind.VALUE, order).value(value);
    }

    /**
     * Create a model value block builder.
     *
     * @param key   key
     * @param value value
     * @return block builder
     */
    public static Block.Builder modelValue(String key, String value) {
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
    public static Block.Builder modelValue(String key, String value, int order) {
        return modelBuilder(key, Block.Kind.VALUE, order).value(value);
    }

    /**
     * Create a new step block builder.
     *
     * @param name    step name
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder step(String name, Block.Builder... children) {
        Block.Builder builder = Step.builder(BUILDER_INFO)
                                    .attribute("name", Value.create(name));
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder;
    }

    /**
     * Create an input option block builder.
     *
     * @param id     input id
     * @param value    option value
     * @param children nested children
     * @return block builder
     */
    public static Block.Builder inputOption(String id, String value, Block.Builder... children) {
        Block.Builder builder = Input.builder(BUILDER_INFO, Block.Kind.OPTION)
                                     .attributes(inputAttributes(id, value));
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder;
    }

    /**
     * Create an input text block builder.
     *
     * @param id         input id
     * @param defaultValue default value
     * @return block builder
     */
    public static Block.Builder inputText(String id, String defaultValue, Block.Builder... children) {
        return inputBuilder(id, Block.Kind.TEXT, defaultValue, children);
    }

    /**
     * Create an input boolean block builder.
     *
     * @param id         input id
     * @param defaultValue default value
     * @param children     nested children
     * @return block builder
     */
    public static Block.Builder inputBoolean(String id, boolean defaultValue, Block.Builder... children) {
        return inputBuilder(id, Block.Kind.BOOLEAN, String.valueOf(defaultValue), children);
    }

    /**
     * Create an input enum block builder.
     *
     * @param id           input id
     * @param defaultValue default value
     * @param children     nested children
     * @return block builder
     */
    public static Block.Builder inputEnum(String id, String defaultValue, Block.Builder... children) {
        return inputBuilder(id, Block.Kind.ENUM, defaultValue, children);
    }

    /**
     * Create an input list block builder.
     *
     * @param id           input id`
     * @param defaultValue default value
     * @param children     nested children
     * @return block builder
     */
    public static Block.Builder inputList(String id, List<String> defaultValue, Block.Builder... children) {
        return inputBuilder(id, Block.Kind.LIST, String.join(",", defaultValue), children);
    }

    private static Block.Builder inputBuilder(String id, Block.Kind kind, String defaultValue, Block.Builder... children) {
        Block.Builder builder = Input.builder(BUILDER_INFO, kind)
                                     .attributes(inputAttributes(id, defaultValue, id));
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder;
    }

    private static Block.Builder modelBuilder(String key, Block.Kind kind, int order, Block.Builder... children) {
        Block.Builder builder = Model.builder(BUILDER_INFO, kind)
                                     .attributes(Map.of("order", DynamicValue.create(String.valueOf(order))));
        if (key != null) {
            builder.attributes(Map.of("key", DynamicValue.create(key)));
        }
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder;
    }

    private static Map<String, Value> inputAttributes(String id, String value) {
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("id", DynamicValue.create(id));
        attributes.put("value", DynamicValue.create(value));
        return attributes;
    }

    private static Map<String, Value> inputAttributes(String id, String defaultValue, String prompt) {
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("id", DynamicValue.create(id));
        attributes.put("default", DynamicValue.create(defaultValue));
        attributes.put("prompt", DynamicValue.create(prompt));
        return attributes;
    }
}
