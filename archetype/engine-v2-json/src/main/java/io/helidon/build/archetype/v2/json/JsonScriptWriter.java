/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.stream.JsonGenerator;

import io.helidon.build.archetype.engine.v2.Expression;
import io.helidon.build.archetype.engine.v2.Node;
import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.archetype.engine.v2.Script;
import io.helidon.build.archetype.engine.v2.Value;

/**
 * JSON writer.
 */
public class JsonScriptWriter implements Script.Writer {

    private final Map<Expression, String> expressions = new HashMap<>();
    private final JsonGenerator generator;

    /**
     * Create a new instance.
     *
     * @param writer writer
     * @param pretty pretty
     */
    public JsonScriptWriter(Writer writer, boolean pretty) {
        this.generator = JsonFactory.createGenerator(writer, pretty);
    }

    @Override
    public void close() {
        generator.close();
    }

    @Override
    public void writeScript(Node script) {
        generator.writeStartObject();
        generator.writeStartObject("expressions");
        for (Node node : script.traverse()) {
            if (node.kind() == Kind.CONDITION) {
                writeExpression(node.expression());
            }
        }
        generator.writeEnd();
        generator.writeStartObject("methods");
        script.script().methods().forEach((k, v) -> {
            generator.writeStartArray(k);
            writeDirectives(v);
            generator.writeEnd();
        });
        generator.writeEnd();
        generator.writeStartArray("children");
        writeDirectives(script);
        generator.writeEnd();
        generator.writeEnd();
    }

    private void writeDirectives(Node block) {
        AtomicInteger stepId = new AtomicInteger();
        block.visit(new Node.Visitor() {
            @Override
            public boolean visit(Node node) {
                if (node != block && node.kind() != Kind.CONDITION) {
                    generator.writeStartObject();
                    generator.write("kind", node.kind().token());
                    if (node.kind() == Kind.STEP) {
                        // add a unique id
                        generator.write("id", String.valueOf(stepId.incrementAndGet()));
                    }
                    node.attributes().forEach((k, v) -> {
                        if (k.equals("default") && node.kind() == Kind.INPUT_BOOLEAN) {
                            // TODO attributes should be pre-typed
                            write(k, v.asBoolean());
                        } else {
                            write(k, v);
                        }
                    });
                    if (node.parent().kind() == Kind.CONDITION) {
                        generator.write("if", expressions.get(node.parent().expression()));
                    }
                    writeValue(node);
                    if (!node.children().isEmpty()) {
                        generator.writeStartArray("children");
                    }
                }
                return true;
            }

            @Override
            public void postVisit(Node node) {
                if (node != block && node.kind() != Kind.CONDITION) {
                    if (!node.children().isEmpty()) {
                        generator.writeEnd();
                    }
                    generator.writeEnd();
                }
            }
        });
    }

    private void writeValue(Node node) {
        switch (node.kind()) {
            case PRESET_BOOLEAN:
            case VARIABLE_BOOLEAN:
                write("value", node.value().asBoolean().or(() -> Value.FALSE));
                break;
            case PRESET_ENUM:
            case VARIABLE_ENUM:
            case PRESET_TEXT:
            case VARIABLE_TEXT:
                write("value", node.value().asString().or(() -> Value.EMPTY_STRING));
                break;
            case PRESET_LIST:
            case VARIABLE_LIST:
                write("value", node.value().asList().or(() -> Value.EMPTY_LIST));
                break;
            default:
                if (node.value().isPresent()) {
                    write("value", node.value());
                }
        }
    }

    private void writeExpression(Expression expr) {
        String id = expressions.computeIfAbsent(expr, k -> String.valueOf(expressions.size() + 1));
        generator.writeStartArray(id);
        for (Expression.Token token : expr.tokens()) {
            generator.writeStartObject();
            if (token.isOperator()) {
                generator.write("kind", "operator");
                generator.write("value", token.operator().symbol());
            } else if (token.isVariable()) {
                generator.write("kind", "variable");
                generator.write("value", token.variable());
            } else if (token.isOperand()) {
                generator.write("kind", "literal");
                write("value", token.operand());
            }
            generator.writeEnd();
        }
        generator.writeEnd();
    }

    private void write(String key, Value<?> value) {
        generator.writeKey(key);
        write(value);
    }

    private void write(Value<?> value) {
        switch (value.type()) {
            case DYNAMIC:
            case STRING:
                generator.write(value.getString());
                break;
            case INTEGER:
                generator.write(value.getInt());
                break;
            case BOOLEAN:
                generator.write(value.getBoolean());
                break;
            case LIST:
                generator.writeStartArray();
                value.getList().forEach(generator::write);
                generator.writeEnd();
                break;
            default:
                generator.writeNull();
        }
    }
}
