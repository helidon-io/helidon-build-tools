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
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Preset;

import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load0;
import static io.helidon.build.archetype.engine.v2.TestHelper.walk;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link ScriptLoader}.
 */
class ScriptLoaderTest {

    @Test
    void testInputs() {
        Script script = load0("loader/inputs.xml");
        int[] index = new int[]{0};
        walk(new Input.Visitor<>() {

            @Override
            public VisitResult visitText(Input.Text input, Void arg) {
                assertThat(++index[0], is(1));
                assertThat(input.name(), is("input1"));
                assertThat(input.label(), is("Text input"));
                assertThat(input.help(), is("Help 1"));
                assertThat(input.isOptional(), is(true));
                assertThat(input.defaultValue(), is(not(nullValue())));
                assertThat(input.defaultValue().asString(), is("default#1"));
                assertThat(input.prompt(), is("Enter 1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                assertThat(++index[0], is(2));
                assertThat(input.name(), is("input2"));
                assertThat(input.label(), is("Boolean input"));
                assertThat(input.help(), is("Help 2"));
                assertThat(input.isOptional(), is(false));
                assertThat(input.defaultValue().asBoolean(), is(true));
                assertThat(input.prompt(), is("Enter 2"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitEnum(Input.Enum input, Void arg) {
                assertThat(++index[0], is(3));
                assertThat(input.name(), is("input3"));
                assertThat(input.label(), is("Enum input"));
                assertThat(input.help(), is("Help 3"));
                assertThat(input.isOptional(), is(false));
                assertThat(input.options().size(), is(2));
                assertThat(input.options().get(0).value(), is("option3.1"));
                assertThat(input.options().get(0).label(), is("Option 3.1"));
                assertThat(input.options().get(1).value(), is("option3.2"));
                assertThat(input.options().get(1).label(), is("Option 3.2"));
                assertThat(input.defaultValue(), is(not(nullValue())));
                assertThat(input.defaultValue().asString(), is("option3.1"));
                assertThat(input.prompt(), is("Enter 3"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitList(Input.List input, Void arg) {
                assertThat(++index[0], is(4));
                assertThat(input.name(), is("input4"));
                assertThat(input.label(), is("List input"));
                assertThat(input.help(), is("Help 4"));
                assertThat(input.isOptional(), is(false));
                assertThat(input.options().get(0).value(), is("item4.1"));
                assertThat(input.options().get(0).label(), is("Item 4.1"));
                assertThat(input.options().get(1).value(), is("item4.2"));
                assertThat(input.options().get(1).label(), is("Item 4.2"));
                assertThat(input.defaultValue().asList(), contains("item4.1", "item4.2"));
                assertThat(input.prompt(), is("Enter 4"));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(4));
    }

    @Test
    void testNestedInputs() {
        Script script = load0("loader/nested-inputs.xml");
        int[] index = new int[]{0};
        walk(new Input.Visitor<>() {
            @Override
            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                assertThat(++index[0] <= 5, is(true));
                assertThat(input.name(), is("input" + index[0]));
                assertThat(input.label(), is("label" + index[0]));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(5));
    }

    @Test
    void testInvocations() {
        Script script = load0("loader/invocations.xml");
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {

            @Override
            public VisitResult visitInvocation(Invocation invocation, Void arg) {
                switch (++index[0]) {
                    case 1:
                        assertThat(invocation.kind(), is(Invocation.Kind.SOURCE));
                        assertThat(invocation.src(), is("./dir1/script1.xml"));
                        break;
                    case 2:
                        assertThat(invocation.kind(), is(Invocation.Kind.EXEC));
                        assertThat(invocation.src(), is("./dir2/script2.xml"));
                        break;
                }
                // stop here to avoid io exceptions on the fake files
                return VisitResult.SKIP_SUBTREE;
            }
        }, script);
        assertThat(index[0], is(2));
    }

    @Test
    void testPresets() {
        Script script = load0("loader/presets.xml");
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {

            @Override
            public VisitResult visitPreset(Preset preset, Void arg) {
                switch (++index[0]) {
                    case 1:
                        assertThat(preset.path(), is("preset1"));
                        assertThat(preset.kind(), is(Preset.Kind.BOOLEAN));
                        assertThat(preset.value().asBoolean(), is(true));
                        break;
                    case 2:
                        assertThat(preset.path(), is("preset2"));
                        assertThat(preset.kind(), is(Preset.Kind.TEXT));
                        assertThat(preset.value().asString(), is("text1"));
                        break;
                    case 3:
                        assertThat(preset.path(), is("preset3"));
                        assertThat(preset.kind(), is(Preset.Kind.ENUM));
                        assertThat(preset.value().asString(), is("enum1"));
                        break;
                    case 4:
                        assertThat(preset.path(), is("preset4"));
                        assertThat(preset.kind(), is(Preset.Kind.LIST));
                        assertThat(preset.value().asList(), contains("list1"));
                        break;
                }
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(4));
    }

    @Test
    void testOutput() {
        Script script = load0("loader/output.xml");
        int[] index = new int[]{0};
        walk(new Output.Visitor<>() {
            @Override
            public VisitResult visitTransformation(Output.Transformation transformation, Void arg) {
                assertThat(++index[0], is(1));
                assertThat(transformation.id(), is("t1"));
                assertThat(transformation.operations().size(), is(1));
                assertThat(transformation.operations().get(0).replacement(), is("token1"));
                assertThat(transformation.operations().get(0).regex(), is("regex1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitTemplates(Output.Templates templates, Void arg) {
                assertThat(++index[0], is(2));
                assertThat(templates.transformations(), contains("t1"));
                assertThat(templates.engine(), is("tpl-engine-1"));
                assertThat(templates.directory(), is("dir1"));
                assertThat(templates.includes(), contains("**/*.tpl1"));
                assertThat(templates.excludes(), is(empty()));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitFiles(Output.Files files, Void arg) {
                assertThat(++index[0], is(3));
                assertThat(files.transformations(), contains("t2"));
                assertThat(files.directory(), is("dir2"));
                assertThat(files.excludes(), contains("**/*.txt"));
                assertThat(files.includes(), is(empty()));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitTemplate(Output.Template template, Void arg) {
                assertThat(++index[0], is(4));
                assertThat(template.engine(), is("tpl-engine-2"));
                assertThat(template.source(), is("file1.tpl"));
                assertThat(template.target(), is("file1.txt"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitFile(Output.File file, Void arg) {
                assertThat(++index[0], is(5));
                assertThat(file.source(), is("file1.txt"));
                assertThat(file.target(), is("file2.txt"));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(5));
    }

    @Test
    void testModel() {
        Script script = load0("loader/model.xml");
        int[] index = new int[]{0};
        walk(new Model.Visitor<>() {
            @Override
            public VisitResult visitMap(Model.Map map, Void arg) {
                switch (++index[0]) {
                    case (1):
                        assertThat(map.key(), is("key1"));
                        break;
                    case (12):
                        assertThat(map.key(), is(nullValue()));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitList(Model.List list, Void arg) {
                switch (++index[0]) {
                    case (3):
                        assertThat(list.key(), is("key1.2"));
                        break;
                    case (7):
                        assertThat(list.key(), is("key3"));
                        break;
                    case (9):
                        assertThat(list.key(), is(nullValue()));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitValue(Model.Value value, Void arg) {
                switch (++index[0]) {
                    case (2):
                        assertThat(value.key(), is("key1.1"));
                        assertThat(value.value(), is("value1.1"));
                        break;
                    case (4):
                        assertThat(value.key(), is(nullValue()));
                        assertThat(value.value(), is("value1.2a"));
                        break;
                    case (5):
                        assertThat(value.key(), is(nullValue()));
                        assertThat(value.value(), is("value1.2b"));
                        break;
                    case (6):
                        assertThat(value.key(), is("key2"));
                        assertThat(value.order(), is(50));
                        assertThat(value.value(), is("value2"));
                        break;
                    case (8):
                        assertThat(value.key(), is(nullValue()));
                        assertThat(value.value(), is("value3.1"));
                        break;
                    case (10):
                        assertThat(value.key(), is(nullValue()));
                        assertThat(value.value(), is("value3.2-a"));
                        break;
                    case (11):
                        assertThat(value.key(), is(nullValue()));
                        assertThat(value.value(), is("value3.2-b"));
                        break;
                    case (13):
                        assertThat(value.key(), is("key3.3-a"));
                        assertThat(value.value(), is("value3.3-a"));
                        break;
                    case (14):
                        assertThat(value.key(), is("key3.3-b"));
                        assertThat(value.value(), is("value3.3-b"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(14));
    }

    @Test
    void testScopedModel() {
        Script script = load0("loader/scoped-model.xml");
        int[] index = new int[]{0};
        walk(new Model.Visitor<>() {
            @Override
            public VisitResult visitValue(Model.Value value, Void arg) {
                switch (++index[0]) {
                    case 1:
                        assertThat(value.key(), is("key1"));
                        assertThat(value.value(), is("value1"));
                        break;
                    case 2:
                        assertThat(value.key(), is("key2"));
                        assertThat(value.value(), is("value2"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(2));
    }

    @Test
    void testConditional() {
        Script script = load0("loader/conditional.xml");
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {

            @Override
            public VisitResult visitCondition(Condition condition, Void arg) {
                if (condition.expression().eval()) {
                    return VisitResult.CONTINUE;
                }
                return VisitResult.SKIP_SUBTREE;
            }

            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {

                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        assertThat(step.label(), is("Step 1"));
                        assertThat(step.help(), is("Help about step 1"));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                assertThat(++index[0], is(2));
                                assertThat(input.name(), is("input1"));
                                assertThat(input.label(), is("Input 1"));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }

                    @Override
                    public VisitResult visitOutput(Output output, Void arg) {
                        return output.accept(new Output.Visitor<>() {
                            @Override
                            public VisitResult visitFile(Output.File file, Void arg) {
                                assertThat(++index[0], is(3));
                                assertThat(file.source(), is("file1.txt"));
                                assertThat(file.target(), is("file2.txt"));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, script);
        assertThat(index[0], is(3));
    }

    @Test
    void testExec() {
        Path target = TestFiles.targetDir(ScriptLoader.class);
        Path testResources = target.resolve("test-classes");
        Script script = ScriptLoader.load(testResources.resolve("loader/exec.xml"));
        int[] index = new int[]{0};
        walk(new Model.Visitor<>() {
            @Override
            public VisitResult visitValue(Model.Value value, Void arg) {
                assertThat(++index[0], is(1));
                assertThat(value.key(), is("foo"));
                assertThat(value.value(), is("bar"));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(1));
    }

    @Test
    void testNestedOutput() {
        Path target = TestFiles.targetDir(ScriptLoader.class);
        Path testResources = target.resolve("test-classes");
        Script script = ScriptLoader.load(testResources.resolve("loader/nested-output.xml"));
        List<String> colors = new LinkedList<>();
        walk(new Model.Visitor<>() {
            @Override
            public VisitResult visitValue(Model.Value value, Void arg) {
                colors.add(value.value());
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(colors, contains("yellow", "green", "red", "blue"));
    }
}
