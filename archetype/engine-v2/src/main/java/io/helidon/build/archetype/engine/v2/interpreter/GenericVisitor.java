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

/**
 * Visitor that processes the input and returns a value.
 *
 * @param <T> type of the returned value
 * @param <A> type of the additional argument
 */
public interface GenericVisitor<T, A> {

    /**
     * Process {@code InputEnumAST} element.
     *
     * @param input InputEnumAST
     * @param arg   argument
     * @return value
     */
    T visit(InputEnumAST input, A arg);

    /**
     * Process {@code InputListAST} element.
     *
     * @param input InputListAST
     * @param arg   argument
     * @return value
     */
    T visit(InputListAST input, A arg);

    /**
     * Process {@code InputBooleanAST} element.
     *
     * @param input InputBooleanAST
     * @param arg   argument
     * @return value
     */
    T visit(InputBooleanAST input, A arg);

    /**
     * Process {@code InputTextAST} element.
     *
     * @param input InputTextAST
     * @param arg   argument
     * @return value
     */
    T visit(InputTextAST input, A arg);
}
