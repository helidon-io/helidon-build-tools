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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Noop;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Position;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Statement;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.common.xml.SimpleXMLParser;
import io.helidon.build.common.xml.SimpleXMLParser.XMLReaderException;

/**
 * Script loader.
 * XML reader for script document with caching.
 */
public class ScriptLoader {

    private static final WeakHashMap<FileSystem, Map<Path, Script>> CACHE = new WeakHashMap<>();

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
        VALUE,
        BLOCK
    }

    private static final class Context {

        private final State state;
        private final Node.Builder<?, ?> builder;
        private final boolean build;

        Context(State state, Node.Builder<?, ?> builder, boolean build) {
            this.state = state;
            this.builder = builder;
            this.build = build;
        }
    }

    private static final class ReaderImpl implements SimpleXMLParser.Reader {

        private Path location;
        private Position position;
        private SimpleXMLParser parser;
        private String qName;
        private Map<String, String> attrs;
        private Deque<Context> stack;
        private Context ctx;
        private Script.Builder script;

        ReaderImpl() {
        }

        Script read(InputStream is, Path path) throws IOException {
            location = path;
            stack = new ArrayDeque<>();
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
                stack.push(new Context(State.EXECUTABLE, script, true));
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
            if (ctx != null && ctx.state == State.VALUE) {
                ctx.builder.value(value);
            }
        }

        @Override
        public void endElement(String name) {
            Context ctx = stack.pop();
            if (ctx.build) {
                ctx.builder.build();
            }
        }

        void processElement() {
            if (Noop.Kind.NAMES.contains(qName)) {
                statement(State.VALUE, Noop.builder(location, position, noopKind()));
                return;
            }
            switch (ctx.state) {
                case EXECUTABLE:
                    switch (qName) {
                        case "exec":
                        case "source":
                            statement(ctx.state, Invocation.builder(location, position, invocationKind()));
                            break;
                        default:
                            processBlock();
                    }
                    break;
                case BLOCK:
                    processBlock();
                    break;
                case INPUT:
                    processInput();
                    break;
                case PRESET:
                    statement(State.VALUE, Preset.builder(location, position, presetKind()));
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid state: %s. { element=%s }", ctx.state, qName));
            }
        }

        void processInput() {
            State nextState;
            Block.Kind blockKind = blockKind();
            switch (blockKind) {
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
                            "Invalid input block: %s. { element=%s }", blockKind, qName));
            }
            statement(nextState, Input.builder(location, position, blockKind));
        }

        void processBlock() {
            State nextState = State.BLOCK;
            Block.Builder builder = null;
            Block.Kind blockKind = blockKind();
            switch (blockKind) {
                case SCRIPT:
                    nextState = State.EXECUTABLE;
                    break;
                case STEP:
                    nextState = State.EXECUTABLE;
                    builder = Step.builder(location, position, blockKind);
                    break;
                case INPUTS:
                    nextState = State.INPUT;
                    break;
                case PRESETS:
                    nextState = State.PRESET;
                    break;
                case TRANSFORMATION:
                case FILES:
                case TEMPLATES:
                case FILE:
                case TEMPLATE:
                    builder = Output.builder(location, position, blockKind);
                    break;
                case MAP:
                case VALUE:
                case LIST:
                    builder = Model.builder(location, position, blockKind);
                    break;
                default:
            }
            if (builder == null) {
                builder = Block.builder(location, position, blockKind);
            }
            statement(nextState, builder);
        }

        void statement(State nextState, Statement.Builder<? extends Statement, ?> stmt) {
            stmt.attributes(attrs);
            String ifExpr = attrs.get("if");
            if (ifExpr != null) {
                ctx.builder.statement(Condition.builder(location, position).expression(ifExpr).then(stmt));
            } else {
                ctx.builder.statement(stmt);
            }
            stack.push(new Context(nextState, stmt, true));
        }

        Preset.Kind presetKind() {
            return Preset.Kind.valueOf(qName.toUpperCase());
        }

        Block.Kind blockKind() {
            return Block.Kind.valueOf(qName.toUpperCase());
        }

        Invocation.Kind invocationKind() {
            return Invocation.Kind.valueOf(qName.toUpperCase());
        }

        Noop.Kind noopKind() {
            return Noop.Kind.valueOf(qName.toUpperCase());
        }
    }
}
