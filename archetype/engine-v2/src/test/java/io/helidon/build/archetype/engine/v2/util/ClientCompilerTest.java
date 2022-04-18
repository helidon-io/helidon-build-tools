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
package io.helidon.build.archetype.engine.v2.util;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static io.helidon.build.archetype.engine.v2.TestHelper.walk;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * Tests {@link ClientCompiler}.
 */
class ClientCompilerTest {

    @Test
    void testEmptyScript() {
        Script script = load("compiler/empty-script/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {
                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitAny(Block block, Void arg) {
                        switch (block.kind()) {
                            case SCRIPT:
                                break;
                            case INPUTS:
                                assertThat(++index[0], is(2));
                                break;
                            default:
                                fail(String.format("Unexpected block: %s, index=%d", block, index[0]));
                        }
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitEnum(Input.Enum input, Void arg) {
                                assertThat(++index[0], is(3));
                                assertThat(input.name(), is("enum1"));
                                assertThat(input.label(), is("Enum1"));
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitOption(Input.Option option, Void arg) {
                                switch (++index[0]) {
                                    case 4:
                                        assertThat(option.value(), is("enum1-value1"));
                                        assertThat(option.label(), is("Enum1Value1"));
                                        break;
                                    case 5:
                                        assertThat(option.value(), is("enum1-value2"));
                                        assertThat(option.label(), is("Enum1Value2"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(5));
    }

    @Test
    void testFiltering() {
        Script script = load("compiler/filtering/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {
                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitAny(Block block, Void arg) {
                        switch (block.kind()) {
                            case SCRIPT:
                                break;
                            case INPUTS:
                                assertThat(++index[0], is(2));
                                break;
                            default:
                                fail(String.format("Unexpected block: %s, index=%d", block, index[0]));
                        }
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                assertThat(++index[0], is(3));
                                assertThat(input.name(), is("boolean1"));
                                assertThat(input.label(), is("Boolean1"));
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(3));
    }

    @Test
    void testInlined1() {
        Script script = load("compiler/inlined1/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {
                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitAny(Block block, Void arg) {
                        switch (block.kind()) {
                            case SCRIPT:
                                break;
                            case INPUTS:
                                assertThat(++index[0], is(2));
                                break;
                            default:
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                        }
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                assertThat(++index[0], is(3));
                                assertThat(input.name(), is("boolean1"));
                                assertThat(input.label(), is("Boolean1"));
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(3));
    }

    @Test
    void testInlined2() {
        Script script = load("compiler/inlined2/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {
                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitAny(Block block, Void arg) {
                        switch (block.kind()) {
                            case SCRIPT:
                                break;
                            case INPUTS:
                                switch (++index[0]) {
                                    case 2:
                                    case 4:
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                break;
                            default:
                                fail(String.format("Unexpected block: %s, index=%d", block, index[0]));
                        }
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                switch (++index[0]) {
                                    case 3:
                                        assertThat(input.name(), is("boolean1"));
                                        assertThat(input.label(), is("Boolean1"));
                                        break;
                                    case 5:
                                        assertThat(input.name(), is("boolean2"));
                                        assertThat(input.label(), is("Boolean2"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(5));
    }

    @Test
    void testInlined3() {
        Script script = load("compiler/inlined3/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {
                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitAny(Block block, Void arg) {
                        switch (block.kind()) {
                            case SCRIPT:
                                break;
                            case INPUTS:
                                switch (++index[0]) {
                                    case 2:
                                    case 4:
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                break;
                            default:
                                fail(String.format("Unexpected block: %s, index=%d", block, index[0]));
                        }
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                switch (++index[0]) {
                                    case 3:
                                        assertThat(input.name(), is("boolean1"));
                                        assertThat(input.label(), is("Boolean1"));
                                        break;
                                    case 5:
                                        assertThat(input.name(), is("boolean2"));
                                        assertThat(input.label(), is("Boolean2"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(5));
    }

    @Test
    void testInlined4() {
        Script script = load("compiler/inlined4/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {
                    @Override
                    public VisitResult visitStep(Step step, Void arg) {
                        assertThat(++index[0], is(1));
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {

                            @Override
                            public VisitResult visitEnum(Input.Enum input, Void arg) {
                                switch (++index[0]) {
                                    case 2:
                                        assertThat(input.name(), is("fruit"));
                                        assertThat(input.label(), is("Fruit"));
                                        break;
                                    case 4:
                                        assertThat(input.name(), is("berry-type"));
                                        assertThat(input.label(), is("Berry type"));
                                        break;
                                    case 10:
                                        assertThat(input.name(), is("tropical-type"));
                                        assertThat(input.label(), is("Tropical type"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitOption(Input.Option option, Void arg) {
                                switch (++index[0]) {
                                    case 3:
                                        assertThat(option.value(), is("berries"));
                                        assertThat(option.label(), is("Berries"));
                                        break;
                                    case 5:
                                        assertThat(option.value(), is("raspberry"));
                                        assertThat(option.label(), is("Raspberry"));
                                        break;
                                    case 7:
                                        assertThat(option.value(), is("strawberry"));
                                        assertThat(option.label(), is("Strawberry"));
                                        break;
                                    case 9:
                                        assertThat(option.value(), is("tropical"));
                                        assertThat(option.label(), is("Tropical"));
                                        break;
                                    case 11:
                                        assertThat(option.value(), is("mango"));
                                        assertThat(option.label(), is("Mango"));
                                        break;
                                    case 14:
                                        assertThat(option.value(), is("banana"));
                                        assertThat(option.label(), is("Banana"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                switch (++index[0]) {
                                    case 6:
                                        assertThat(input.name(), is("organic"));
                                        assertThat(input.label(), is("Organic"));
                                        break;
                                    case 8:
                                        assertThat(input.name(), is("frozen"));
                                        assertThat(input.label(), is("Frozen"));
                                        break;
                                    case 12:
                                        assertThat(input.name(), is("fare-trade"));
                                        assertThat(input.label(), is("Fare trade"));
                                        break;
                                    case 15:
                                        assertThat(input.name(), is("plantain"));
                                        assertThat(input.label(), is("Plantain"));
                                        break;
                                    case 17:
                                        assertThat(input.name(), is("frosting"));
                                        assertThat(input.label(), is("Frosting"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitText(Input.Text input, Void arg) {
                                switch (++index[0]) {
                                    case 13:
                                    case 16:
                                        assertThat(input.name(), is("comment"));
                                        assertThat(input.label(), is("Comment"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(17));
    }

    @Test
    void testDeclarations() {
        Script script = load("compiler/declarations/main.xml");
        Script compiledScript = ClientCompiler.compile(script, false);
        int[] index = new int[]{0};
        walk(new Node.Visitor<>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<>() {

                    @Override
                    public VisitResult visitAny(Block block, Void arg) {
                        switch (block.kind()) {
                            case SCRIPT:
                            case INVOKE:
                            case METHOD:
                            case INPUTS:
                                break;
                            default:
                                fail(String.format("Unexpected block: %s, index=%d", block, index[0]));
                        }
                        return VisitResult.CONTINUE;
                    }

                    @Override
                    public VisitResult visitInput(Input input, Void arg) {
                        return input.accept(new Input.Visitor<>() {
                            @Override
                            public VisitResult visitBoolean(Input.Boolean input, Void arg) {
                                switch (++index[0]) {
                                    case 1:
                                        assertThat(input.name(), is("boolean1"));
                                        assertThat(input.label(), is("Boolean1"));
                                        break;
                                    case 2:
                                    case 6:
                                        assertThat(input.name(), is("boolean2"));
                                        assertThat(input.label(), is("Boolean2"));
                                        break;
                                    case 3:
                                    case 7:
                                    case 10:
                                        assertThat(input.name(), is("boolean3"));
                                        assertThat(input.label(), is("Boolean3"));
                                        break;
                                    case 4:
                                    case 5:
                                    case 8:
                                    case 9:
                                    case 11:
                                    case 12:
                                        assertThat(input.name(), is("boolean4"));
                                        assertThat(input.label(), is("Boolean4"));
                                        break;
                                    default:
                                        fail("Unexpected index: " + index[0]);
                                }
                                return VisitResult.CONTINUE;
                            }

                            @Override
                            public VisitResult visitAny(Input input, Void arg) {
                                fail(String.format("Unexpected input: %s, index=%d", block, index[0]));
                                return VisitResult.CONTINUE;
                            }
                        }, arg);
                    }
                }, arg);
            }
        }, compiledScript);
        assertThat(index[0], is(12));
    }
}
