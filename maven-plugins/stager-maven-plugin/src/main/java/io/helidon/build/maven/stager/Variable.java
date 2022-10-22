/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import io.helidon.build.common.Strings;

/**
 * Variable model.
 */
class Variable implements StagingElement {

    static final String ELEMENT_NAME = "variable";

    private final String name;
    private final VariableValue value;

    Variable(String name, VariableValue value) {
        this.name = Strings.requireValid(name, "name is required");
        if (value == null) {
            throw new IllegalArgumentException("value is required");
        }
        this.value = value;
    }

    /**
     * Get the variable name.
     *
     * @return name
     */
    String name() {
        return name;
    }

    /**
     * Get the variable value.
     *
     * @return value
     */
    VariableValue value() {
        return value;
    }

    @Override
    public String elementName() {
        return ELEMENT_NAME;
    }
}
