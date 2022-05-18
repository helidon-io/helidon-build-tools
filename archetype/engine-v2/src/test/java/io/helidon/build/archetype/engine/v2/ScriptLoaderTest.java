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
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static io.helidon.build.archetype.engine.v2.TestHelper.walk;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link ScriptLoader}.
 */
class ScriptLoaderTest {

    @Test
    void testInputs() {
        Script script = load("loader/inputs.xml");
        int[] index = new int[]{0};
        walk(new Input.Visitor<>() {

            @Override
            public VisitResult visitText(Input.Text input, Void arg) {
                switch (++index[0]) {
                    case 1:
                        assertThat(input.id(), is("input1"));
                        assertThat(input.name(), is("Text input"));
                        assertThat(input.description(), is("A text input"));
                        assertThat(input.help(), is("Help 1"));
                        assertThat(input.isOptional(), is(true));
                        assertThat(input.defaultValue(), is(not(nullValue())));
                        assertThat(input.defaultValue().asString(), is("default#1"));
                        assertThat(input.prompt(), is("Enter 1"));
                        break;
                    case 5:
                        assertThat(input.id(), is("more-input1"));
                        assertThat(input.name(), is("More input 1"));
                        assertThat(input.description(), is("Another text input"));
                        assertThat(input.help(), is("More Help 1"));
                        assertThat(input.isOptional(), is(true));
                        assertThat(input.defaultValue(), is(not(nullValue())));
                        assertThat(input.defaultValue().asString(), is("more#1"));
                        assertThat(input.prompt(), is("More1"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                assertThat(++index[0], is(2));
                assertThat(input.id(), is("input2"));
                assertThat(input.name(), is("Boolean input"));
                assertThat(input.description(), is("A boolean input"));
                assertThat(input.help(), is("Help 2"));
                assertThat(input.isOptional(), is(false));
                assertThat(input.defaultValue().asBoolean(), is(true));
                assertThat(input.prompt(), is("Enter 2"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitEnum(Input.Enum input, Void arg) {
                assertThat(++index[0], is(3));
                assertThat(input.id(), is("input3"));
                assertThat(input.name(), is("Enum input"));
                assertThat(input.description(), is("An enum input"));
                assertThat(input.help(), is("Help 3"));
                assertThat(input.isOptional(), is(false));
                assertThat(input.options().size(), is(2));
                assertThat(input.options().get(0).value(), is("option3.1"));
                assertThat(input.options().get(0).name(), is("Option 3.1"));
                assertThat(input.options().get(0).description(), is("An option"));
                assertThat(input.options().get(1).value(), is("option3.2"));
                assertThat(input.options().get(1).name(), is("Option 3.2"));
                assertThat(input.options().get(1).description(), is("Another option"));
                assertThat(input.defaultValue(), is(not(nullValue())));
                assertThat(input.defaultValue().asString(), is("option3.1"));
                assertThat(input.prompt(), is("Enter 3"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitList(Input.List input, Void arg) {
                assertThat(++index[0], is(4));
                assertThat(input.id(), is("input4"));
                assertThat(input.name(), is("List input"));
                assertThat(input.description(), is("A list input"));
                assertThat(input.help(), is("Help 4"));
                assertThat(input.isOptional(), is(false));
                assertThat(input.options().get(0).value(), is("item4.1"));
                assertThat(input.options().get(0).name(), is("Item 4.1"));
                assertThat(input.options().get(0).description(), is("An option"));
                assertThat(input.options().get(1).value(), is("item4.2"));
                assertThat(input.options().get(1).name(), is("Item 4.2"));
                assertThat(input.options().get(1).description(), is("Another option"));
                assertThat(input.defaultValue().asList(), contains("item4.1", "item4.2"));
                assertThat(input.prompt(), is("Enter 4"));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(5));
    }

    @Test
    void testNestedInputs() {
        Script script = load("loader/nested-inputs.xml");
        int[] index = new int[]{0};
        walk(new Input.Visitor<>() {
            @Override
            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                assertThat(++index[0] <= 5, is(true));
                assertThat(input.id(), is("input" + index[0]));
                assertThat(input.name(), is("label" + index[0]));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(5));
    }

    @Test
    void testInvocations() {
        Script script = load("loader/invocations.xml");
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {

            @Override
            public VisitResult visitInvocation(Invocation invocation, Void arg) {
                return invocation.accept(new Invocation.Visitor<>() {
                    @Override
                    public VisitResult visitScriptInvocation(Invocation.ScriptInvocation invocation, Void arg) {
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
                }, arg);
            }
        }, script);
        assertThat(index[0], is(2));
    }

    @Test
    void testMethods() {
        Script script = load("loader/methods.xml");
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {

            @Override
            public VisitResult visitInvocation(Invocation invocation, Void arg) {
                return invocation.accept(new Invocation.Visitor<>() {

                    @Override
                    public VisitResult visitMethodInvocation(Invocation.MethodInvocation invocation, Void arg) {
                        switch (++index[0]) {
                            case 1:
                                assertThat(invocation.method(), is("red"));
                                break;
                            case 3:
                                assertThat(invocation.method(), is("blue"));
                                break;
                        }
                        return VisitResult.CONTINUE;
                    }
                }, arg);
            }

            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {

                    @Override
                    public VisitResult visitOutput(Output output, Void arg) {
                        return output.accept(new Output.Visitor<>() {
                            @Override
                            public VisitResult visitFile(Output.File file, Void arg) {
                                switch (++index[0]) {
                                    case 2:
                                        assertThat(file.source(), is("red.txt"));
                                        assertThat(file.target(), is("red.txt"));
                                        break;
                                    case 4:
                                        assertThat(file.source(), is("blue.txt"));
                                        assertThat(file.target(), is("blue.txt"));
                                        break;
                                }
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, script);
        assertThat(index[0], is(4));
    }

    @Test
    void testPresets() {
        Script script = load("loader/presets.xml");
        int[] index = new int[]{0};
        walk(new VisitorAdapter<>(null, null, null) {

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
                        assertThat(preset.children().isEmpty(), is(true));
                        break;
                }
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(4));
    }

    @Test
    void testVariables() {
        Script script = load("loader/variables.xml");
        int[] index = new int[]{0};
        walk(new VisitorAdapter<>(null, null, null) {

            @Override
            public VisitResult visitVariable(Variable variable, Void arg) {
                switch (++index[0]) {
                    case 1:
                        assertThat(variable.path(), is("var1"));
                        assertThat(variable.kind(), is(Variable.Kind.BOOLEAN));
                        assertThat(variable.value().asBoolean(), is(true));
                        break;
                    case 2:
                        assertThat(variable.path(), is("var2"));
                        assertThat(variable.kind(), is(Variable.Kind.TEXT));
                        assertThat(variable.value().asString(), is("text1"));
                        assertThat(variable.isTransient(), is(true));
                        break;
                    case 3:
                        assertThat(variable.path(), is("var3"));
                        assertThat(variable.kind(), is(Variable.Kind.ENUM));
                        assertThat(variable.value().asString(), is("enum1"));
                        break;
                    case 4:
                        assertThat(variable.path(), is("var4"));
                        assertThat(variable.kind(), is(Variable.Kind.LIST));
                        assertThat(variable.value().asList(), contains("list1"));
                        assertThat(variable.children().isEmpty(), is(true));
                        break;
                }
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(4));
    }

    @Test
    void testOutput() {
        Script script = load("loader/output.xml");
        int[] index = new int[]{0};
        walk(new Output.Visitor<>() {
            @Override
            public VisitResult visitTransformation(Output.Transformation transformation, Void arg) {
                assertThat(++index[0], is(1));
                assertThat(transformation.id(), is("t1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitReplace(Output.Replace replace, Void arg) {
                assertThat(++index[0], is(2));
                assertThat(replace.replacement(), is("token1"));
                assertThat(replace.regex(), is("regex1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitTemplates(Output.Templates templates, Void arg) {
                assertThat(++index[0], is(3));
                assertThat(templates.transformations(), contains("t1"));
                assertThat(templates.engine(), is("tpl-engine-1"));
                assertThat(templates.directory(), is("dir1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitInclude(Output.Include include, Void arg) {
                assertThat(++index[0], is(4));
                assertThat(include.value(), is("**/*.tpl1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitExclude(Output.Exclude exclude, Void arg) {
                assertThat(++index[0], is(6));
                assertThat(exclude.value(), is("**/*.txt"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitFiles(Output.Files files, Void arg) {
                assertThat(++index[0], is(5));
                assertThat(files.transformations(), contains("t2"));
                assertThat(files.directory(), is("dir2"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitTemplate(Output.Template template, Void arg) {
                assertThat(++index[0], is(7));
                assertThat(template.engine(), is("tpl-engine-2"));
                assertThat(template.source(), is("file1.tpl"));
                assertThat(template.target(), is("file1.txt"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitFile(Output.File file, Void arg) {
                assertThat(++index[0], is(8));
                assertThat(file.source(), is("file1.txt"));
                assertThat(file.target(), is("file2.txt"));
                return VisitResult.CONTINUE;
            }
        }, script);
        assertThat(index[0], is(8));
    }

    @Test
    void testModel() {
        Script script = load("loader/model.xml");
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
        Script script = load("loader/scoped-model.xml");
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
        Script script = load("loader/conditional.xml");
        int[] index = new int[]{0};
        Output.Visitor<Void> outputVisitor = new Output.Visitor<>() {

            @Override
            public VisitResult visitFile(Output.File file, Void arg) {
                assertThat(++index[0], is(4));
                assertThat(file.source(), is("file1.txt"));
                assertThat(file.target(), is("file2.txt"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitTemplate(Output.Template template, Void arg) {
                assertThat(++index[0], is(5));
                assertThat(template.source(), is("file1.tpl"));
                assertThat(template.target(), is("file2.txt"));
                assertThat(template.engine(), is("foo"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitFiles(Output.Files files, Void arg) {
                assertThat(++index[0], is(6));
                assertThat(files.directory(), is("colors"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitInclude(Output.Include include, Void arg) {
                switch (++index[0]) {
                    case (7):
                        assertThat(include.value(), is("red"));
                        break;
                    case (8):
                        assertThat(include.value(), is("green"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitExclude(Output.Exclude exclude, Void arg) {
                switch (++index[0]) {
                    case (9):
                        assertThat(exclude.value(), is("yellow"));
                        break;
                    case (10):
                        assertThat(exclude.value(), is("pink"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }
        };
        Model.Visitor<Void> modelVisitor = new Model.Visitor<>() {
            @Override
            public VisitResult visitValue(Model.Value value, Void arg) {
                switch (++index[0]) {
                    case (11):
                        assertThat(value.value(), is("red"));
                        break;
                    case (12):
                        assertThat(value.value(), is("green"));
                        break;
                    case (14):
                        assertThat(value.value(), is("yellow"));
                        break;
                    case (15):
                        assertThat(value.value(), is("pink"));
                        break;
                    case (17):
                        assertThat(value.key(), is("rectangle"));
                        assertThat(value.value(), is("orange"));
                        break;
                    case (18):
                        assertThat(value.key(), is("circle"));
                        assertThat(value.value(), is("lavender"));
                        break;
                    default:
                        Assertions.fail("Unexpected index: " + index[0]);
                }
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitList(Model.List list, Void arg) {
                assertThat(++index[0], is(13));
                assertThat(list.key(), is("colors1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitMap(Model.Map map, Void arg) {
                assertThat(++index[0], is(16));
                assertThat(map.key(), is("shapes1"));
                return VisitResult.CONTINUE;
            }
        };
        walk(new VisitorAdapter<>(null, outputVisitor, modelVisitor) {
            @Override
            public VisitResult visitPreset(Preset preset, Void arg) {
                assertThat(++index[0], is(1));
                assertThat(preset.path(), is("path1"));
                assertThat(preset.value().type(), is(ValueTypes.BOOLEAN));
                assertThat(preset.value().asBoolean(), is(true));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitCondition(Condition condition, Void arg) {
                if (condition.expression().eval()) {
                    return VisitResult.CONTINUE;
                }
                return VisitResult.SKIP_SUBTREE;
            }

            @Override
            public VisitResult visitStep(Step step, Void arg) {
                assertThat(++index[0], is(2));
                assertThat(step.name(), is("Step 1"));
                assertThat(step.help(), is("Help about step 1"));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitInput(Input input, Void arg) {
                return input.accept(new Input.Visitor<>() {
                    @Override
                    public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                        assertThat(++index[0], is(3));
                        assertThat(input.id(), is("input1"));
                        assertThat(input.name(), is("Input 1"));
                        return VisitResult.CONTINUE;
                    }
                }, arg);
            }
        }, script);
        assertThat(index[0], is(18));
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
