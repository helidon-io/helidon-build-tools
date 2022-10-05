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
package io.helidon.build.archetype.engine.v2.context;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;

/**
 * Read and write values in the context.
 *
 * @see ContextValue
 */
public interface ContextRegistry {

    /**
     * Set a value.
     *
     * @param path  context path, see {@link ContextPath}
     * @param value the value
     * @param kind  value kind
     * @return the created context value
     */
    ContextValue putValue(String path, Value value, ValueKind kind);

    /**
     * Lookup a value.
     *
     * @param path context path, see {@link ContextPath}
     * @return effective path
     * @throws NullPointerException     if path is null
     * @throws IllegalArgumentException if path is invalid
     */
    ContextValue getValue(String path);

    /**
     * Substitute the properties of the form {@code ${key}} within the given string.
     * The keys are context paths relative to the current scope.
     * Properties are substituted until the resulting string does not contain any references.
     *
     * @param value string to process
     * @return processed string
     * @throws IllegalArgumentException if the string contains any unresolved variable
     */
    String interpolate(String value);
}
