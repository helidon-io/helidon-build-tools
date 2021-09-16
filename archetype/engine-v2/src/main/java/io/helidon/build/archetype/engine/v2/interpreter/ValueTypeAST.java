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

package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

/**
 * Archetype value AST node used in {@link ListTypeAST}.
 */
public class ValueTypeAST extends ASTNode implements ConditionalNode {

    private final String value;
    private final String url;
    private final String file;
    private final String template;
    private int order = 100;


    ValueTypeAST(String url, String file, String template, int order, String value, ASTNode parent, Location location) {
        super(parent, location);
        this.url = url;
        this.file = file;
        this.template = template;
        this.order = order;
        this.value = value;
    }

    /**
     * Get the value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Get the url.
     *
     * @return url
     */
    public String url() {
        return url;
    }

    /**
     * Get the file.
     *
     * @return file
     */
    public String file() {
        return file;
    }

    /**
     * Get the template.
     *
     * @return template
     */
    public String template() {
        return template;
    }

    /**
     * Get the order.
     *
     * @return oprder
     */
    public int order() {
        return order;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static ValueTypeAST create(ValueType valueFrom, ASTNode parent, Location location) {
        return new ValueTypeAST(
                valueFrom.url(),
                valueFrom.file(),
                valueFrom.template(),
                valueFrom.order(),
                valueFrom.value(),
                parent,
                location
        );
    }
}
