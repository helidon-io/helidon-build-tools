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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Position;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.common.xml.SimpleXMLParser;
import io.helidon.build.common.xml.SimpleXMLParser.XMLReaderException;

/**
 * Script loader.
 * XML reader for script document with caching.
 */
public class ScriptLoader {

    private static final Map<FileSystem, Map<Path, Script>> CACHE = new WeakHashMap<>();

    /**
     * Get or load the script at the given path.
     *
     * @param path path
     * @return script
     */
    public static Script load(Path path) {
        return CACHE.computeIfAbsent(path.getFileSystem(), fs -> new HashMap<>())
                    .compute(path, (p, r) -> load0(p));
    }

    /**
     * Load a script.
     *
     * @param path script path
     * @return script
     */
    static Script load0(Path path) {
        try {
            return load0(Files.newInputStream(path), path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Load a script with an input stream.
     *
     * @param is input stream
     * @return script
     */
    static Script load0(InputStream is) {
        return load0(is, null);
    }

    private static Script load0(InputStream is, Path path) {
        try {
            return new ReaderImpl().read(is, path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private ScriptLoader() {
    }

    private enum State {
        PRESET,
        INPUT,
        EXECUTABLE,
        OUTPUT,
        MODEL,
        BLOCK
    }

    private static final class Context {

        private final State state;
        private final Node.Builder<?, ?> builder;

        Context(State state, Node.Builder<?, ?> builder) {
            this.state = state;
            this.builder = builder;
        }
    }

    private static final class ReaderImpl implements SimpleXMLParser.Reader {

        private Path location;
        private Position position;
        private SimpleXMLParser parser;
        private String qName;
        private Map<String, String> attrs;
        private LinkedList<Context> stack;
        private Context ctx;
        private Script.Builder script;

        ReaderImpl() {
        }

        Script read(InputStream is, Path path) throws IOException {
            location = path;
            stack = new LinkedList<>();
            parser = SimpleXMLParser.create(is, this);
            parser.parse();
            if (script == null) {
                throw new IllegalStateException("Unable to create script");
            }
            return script.build();
        }

        @Override
        public void startElement(String qName, Map<String, String> attrs) {
            this.qName = qName;
            this.attrs = attrs;
            position = Position.of(parser.lineNumber(), parser.charNumber());
            ctx = stack.peek();
            if (ctx == null) {
                if (!"archetype-script".equals(qName)) {
                    throw new XMLReaderException(String.format(
                            "Invalid root element '%s'. {file=%s, position=%s}",
                            qName, location, position));
                }
                script = Script.builder(location, position);
                stack.push(new Context(State.EXECUTABLE, script));
            } else {
                try {
                    processElement();
                } catch (IllegalArgumentException ex) {
                    throw new XMLReaderException(String.format(
                            "Invalid element '%s'. { file=%s, position=%s }",
                            qName, location, position), ex);
                } catch (Throwable ex) {
                    throw new XMLReaderException(String.format(
                            "An unexpected error occurred. { file=%s, position=%s }",
                            location, position), ex);
                }
            }
        }

        @Override
        public void elementText(String value) {
            ctx = stack.peek();
            if (ctx != null) {
                ctx.builder.value(value);
            }
        }

        @Override
        public void endElement(String name) {
            stack.pop().builder.build();
        }

        void processElement() {
            switch (qName) {
                case "directory":
                case "help":
                    stack.push(new Context(ctx.state, new ValueBuilder(ctx.builder, qName)));
                    return;
                default:
            }
            switch (ctx.state) {
                case EXECUTABLE:
                    switch (qName) {
                        case "exec":
                        case "source":
                            addChild(ctx.state, Invocation.builder(location, position, invocationKind()));
                            break;
                        default:
                            processBlock();
                    }
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
                case OUTPUT:
                    processOutput();
                    break;
                case MODEL:
                    processModel();
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid state: %s. { element=%s }", ctx.state, qName));
            }
        }

        void processInput() {
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
                    throw new XMLReaderException(String.format(
                            "Invalid input block: %s. { element=%s }", kind, qName));
            }
            addChild(nextState, Input.builder(location, position, kind));
        }

        void processPreset() {
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case BOOLEAN:
                case TEXT:
                case ENUM:
                case LIST:
                    builder = Preset.builder(location, position, blockKind());
                    break;
                case VALUE:
                    builder = Block.builder(location, position, blockKind());
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid preset block: %s. { element=%s }", kind, qName));

            }
            addChild(ctx.state, builder);
        }

        void processOutput() {
            State nextState = State.OUTPUT;
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case INCLUDES:
                case EXCLUDES:
                    builder = Block.builder(location, position, kind);
                    break;
                case INCLUDE:
                case EXCLUDE:
                case TRANSFORMATION:
                case REPLACE:
                case FILES:
                case TEMPLATES:
                case FILE:
                case TEMPLATE:
                    builder = Output.builder(location, position, kind);
                    break;
                case MODEL:
                    nextState = State.MODEL;
                    builder = Block.builder(location, position, kind);
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid output block: %s. { element=%s }", kind, qName));

            }
            addChild(nextState, builder);
        }

        void processModel() {
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case MAP:
                case VALUE:
                case LIST:
                    builder = Model.builder(location, position, kind);
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid model block: %s. { element=%s }", kind, qName));

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
                case STEP:
                    nextState = State.EXECUTABLE;
                    builder = Step.builder(location, position, kind);
                    break;
                case INPUTS:
                    nextState = State.INPUT;
                    break;
                case PRESETS:
                    nextState = State.PRESET;
                    break;
                case OUTPUT:
                    nextState = State.OUTPUT;
                    break;
                default:
            }
            if (builder == null) {
                builder = Block.builder(location, position, kind);
            }
            addChild(nextState, builder);
        }

        void addChild(State nextState, Node.Builder<? extends Node, ?> builder) {
            builder.attributes(attrs);
            String ifExpr = attrs.get("if");
            if (ifExpr != null) {
                ctx.builder.addChild(Condition.builder(location, position).expression(ifExpr).then(builder));
            } else {
                ctx.builder.addChild(builder);
            }
            stack.push(new Context(nextState, builder));
        }

        Block.Kind blockKind() {
            return Block.Kind.valueOf(qName.toUpperCase());
        }

        Invocation.Kind invocationKind() {
            return Invocation.Kind.valueOf(qName.toUpperCase());
        }
    }

    private static final class ValueBuilder extends Node.Builder<Node, ValueBuilder> {

        private final Node.Builder<?, ?> parent;
        private final String qName;

        ValueBuilder(Node.Builder<?, ?> parent, String qName) {
            super(null, null);
            this.parent = parent;
            this.qName = qName;
        }

        @Override
        public ValueBuilder value(String value) {
            parent.attributes(qName, value);
            return this;
        }

        @Override
        protected Node doBuild() {
            return null;
        }
    }
}
