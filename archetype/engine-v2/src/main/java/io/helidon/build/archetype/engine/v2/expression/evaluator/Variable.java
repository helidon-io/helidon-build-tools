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

package io.helidon.build.archetype.engine.v2.expression.evaluator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable.
 */
final class Variable implements AbstractSyntaxTree {

    private static final Pattern VAR_PATTERN = Pattern.compile("^\\$\\{(?<varName>[\\w.-]+)}");
    private final String name;
    private Literal<?> value;

    /**
     * Create a new variable.
     *
     * @param name a raw name of the variable.
     */
    Variable(String name) {
        Matcher matcher = VAR_PATTERN.matcher(name);
        if (matcher.find()) {
            this.name = matcher.group("varName");
        } else {
            throw new ParserException("incorrect name of the variable " + name);
        }
    }

    /**
     * Get name of the variable.
     *
     * @return name of the variable.
     */
    public String name() {
        return name;
    }

    /**
     * Set the value of the variable.
     *
     * @param value Literal that represents value of the variable.
     */
    public void value(Literal<?> value) {
        this.value = value;
    }

    /**
     * Get value.
     *
     * @return Value
     */
    public Literal<?> value() {
        return value;
    }

    @Override
    public String toString() {
        return "Variable{"
                + "value=" + value
                + ", name='" + name + '\''
                + '}';
    }
}
