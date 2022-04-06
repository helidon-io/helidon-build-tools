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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.JsonValue;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Location;
import io.helidon.build.archetype.engine.v2.ast.Method;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.v2.json.SimpleJSONParser.JSONReaderException;
import io.helidon.build.archetype.v2.json.SimpleJSONParser.KeyInfo;
import io.helidon.build.common.Maps;

/**
 * Script de-serializer.
 */
public final class ScriptDeserializer {

    private ScriptDeserializer() {
    }

    /**
     * De-serialize the given JSON file.
     *
     * @param is input stream
     * @return Script
     */
    public static Script deserialize(InputStream is) {
        return new ReaderImpl(is).read();
    }

    private enum State {
        START,
        PRESET,
        INPUT,
        EXPRESSIONS,
        EXPRESSION,
        TOKEN,
        METHOD,
        EXECUTABLE,
        BLOCK
    }

    private static final class Context {

        private final State state;
        private final Node.Builder<?, ?> builder;
        private final List<Expression.Token> tokens;

        Context(State state) {
            this.state = state;
            this.tokens = Collections.emptyList();
            this.builder = null;
        }

        Context(State state, Node.Builder<?, ?> builder) {
            this.state = state;
            this.builder = builder;
            this.tokens = Collections.emptyList();
        }

        Context(State state, List<Expression.Token> tokens) {
            this.state = state;
            this.tokens = tokens;
            this.builder = null;
        }
    }

    private static final class ReaderImpl implements SimpleJSONParser.Reader {

        private final ScriptLoader loader = ScriptLoader.create();
        private final Path path = loader.unknownPath();
        private final Map<String, Expression> expressions = new HashMap<>();
        private final Deque<Context> stack = new ArrayDeque<>();
        private final SimpleJSONParser parser;

        private String qName;
        private Map<String, Value> attrs;
        private Context ctx;
        private Location location;
        private Script.Builder scriptBuilder;

        ReaderImpl(InputStream is) {
            this.parser = SimpleJSONParser.create(is, this);
        }

        Script read() {
            parser.parse();
            if (scriptBuilder == null) {
                throw new IllegalStateException("Unable to create script");
            }
            Script script = scriptBuilder.build();
            loader.add(script);
            return script;
        }

        @Override
        public KeyInfo keyInfo(String key) {
            switch (key) {
                case "methods":
                case "expressions":
                    return KeyInfo.OBJECT;
                case "children":
                    return KeyInfo.CHILDREN;
                case "kind":
                    return KeyInfo.NAME;
                default:
                    return KeyInfo.UNKNOWN;
            }
        }

        @Override
        public void startElement(String qName, Map<String, JsonValue> attrs) {
            this.qName = qName;
            location = Location.of(path, parser.lineNumber(), parser.charNumber());
            ctx = stack.peek();
            if (ctx == null) {
                scriptBuilder = Script.builder(loader, path);
                stack.push(new Context(State.START, scriptBuilder));
            } else {
                this.attrs = Maps.mapValue(attrs, JsonFactory::readValue);
                try {
                    processElement();
                } catch (IllegalArgumentException ex) {
                    throw new JSONReaderException(String.format(
                            "Invalid element '%s'. { location=%s }",
                            qName, location), ex);
                } catch (Throwable ex) {
                    throw new JSONReaderException(String.format(
                            "An unexpected error occurred. { location=%s }",
                            location), ex);
                }
            }
        }

        @Override
        public void endElement(String qName) {
            ctx = stack.pop();
            if (ctx.builder != null) {
                ctx.builder.build();
            } else if (ctx.state == State.EXPRESSION) {
                expressions.put(qName, Expression.create(ctx.tokens));
            }
        }

        void processElement() {
            switch (ctx.state) {
                case START:
                case EXECUTABLE:
                    if (ctx.state == State.START) {
                        if (processExpressions()) {
                            break;
                        }
                    }
                    if (!processExec()) {
                        processBlock();
                    }
                    break;
                case EXPRESSIONS:
                    processExpression();
                    break;
                case TOKEN:
                    processToken();
                    break;
                case METHOD:
                    processMethod();
                    break;
                case BLOCK:
                    processBlock();
                    break;
                case PRESET:
                    processPreset();
                    break;
                case INPUT:
                    processInput();
                    break;
                default:
                    throw new JSONReaderException(String.format(
                            "Invalid state: %s. { element=%s }", ctx.state, qName));
            }
        }

        void processMethod() {
            addChild(State.EXECUTABLE, Method.builder(loader, path, location)
                                             .attribute("name", Value.create(qName)));
        }

        boolean processExec() {
            if ("call".equals(qName)) {
                addChild(ctx.state, Invocation.builder(loader, path, location, Invocation.Kind.CALL));
                return true;
            }
            return false;
        }

        boolean processExpressions() {
            if ("expressions".equals(qName)) {
                stack.push(new Context(State.EXPRESSIONS));
                return true;
            }
            return false;
        }

        void processExpression() {
            stack.push(new Context(State.EXPRESSION, new LinkedList<>()));
        }

        void processToken() {
            Value value = attrs.get("value");
            Expression.Token token;
            switch (qName) {
                case "literal":
                    token = Expression.Token.create(value);
                    break;
                case "operator":
                    token = Expression.Token.create(Expression.Operator.valueOf(value.asString()));
                    break;
                case "variable":
                    token = Expression.Token.create(value.asString());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported token kind: " + qName);
            }
            ctx.tokens.add(token);
            stack.push(new Context(State.TOKEN));
        }

        void processInput() {
            if (processExec()) {
                return;
            }
            State nextState;
            Block.Kind kind = blockKind();
            switch (kind) {
                case OPTION:
                case BOOLEAN:
                case TEXT:
                    nextState = State.EXECUTABLE;
                    break;
                case LIST:
                case ENUM:
                    nextState = State.INPUT;
                    break;
                case OUTPUT:
                    processBlock();
                    return;
                default:
                    throw new JSONReaderException(String.format(
                            "Invalid input block: %s. { element=%s }", kind, qName));
            }
            addChild(nextState, Input.builder(loader, path, location, kind));
        }

        void processPreset() {
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case BOOLEAN:
                case TEXT:
                case ENUM:
                case LIST:
                    builder = Preset.builder(loader, path, location, blockKind());
                    break;
                case VALUE:
                    builder = Block.builder(loader, path, location, blockKind());
                    break;
                default:
                    throw new JSONReaderException(String.format(
                            "Invalid preset block: %s. { element=%s }", kind, qName));

            }
            addChild(ctx.state, builder);
        }

        void processBlock() {
            State nextState = State.BLOCK;
            Block.Builder builder = null;
            Block.Kind kind = blockKind();
            switch (kind) {
                case SCRIPT:
                    nextState = State.EXECUTABLE;
                    break;
                case METHODS:
                    nextState = State.METHOD;
                    break;
                case METHOD:
                    nextState = State.EXECUTABLE;
                    builder = Method.builder(loader, path, location);
                    break;
                case STEP:
                    nextState = State.EXECUTABLE;
                    builder = Step.builder(loader, path, location);
                    break;
                case INPUTS:
                    nextState = State.INPUT;
                    break;
                case PRESETS:
                    nextState = State.PRESET;
                    break;
                default:
            }
            if (builder == null) {
                builder = Block.builder(loader, path, location, kind);
            }
            addChild(nextState, builder);
        }

        void addChild(State nextState, Node.Builder<? extends Node, ?> builder) {
            builder.attributes(attrs);
            Value ifExprId = attrs.get("if");
            if (ifExprId != null) {
                Expression expr = expressions.get(ifExprId.asString());
                if (expr == null) {
                    throw new IllegalStateException("Unresolved expression: " + ifExprId);
                }
                ctx.builder.addChild(Condition.builder(loader, path, location)
                                              .expression(expr)
                                              .then(builder));
            } else {
                ctx.builder.addChild(builder);
            }
            stack.push(new Context(nextState, builder));
        }

        Block.Kind blockKind() {
            return Block.Kind.valueOf(qName.toUpperCase());
        }
    }
}
