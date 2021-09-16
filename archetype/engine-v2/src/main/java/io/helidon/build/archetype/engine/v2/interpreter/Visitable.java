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
 * Base interface for the objects that will be processed by the script interpreter using Visitor pattern.
 */
public interface Visitable {

    /**
     * Add additional logic to the current {@code Visitable} instance.
     *
     * @param visitor Visitor
     * @param arg     additional argument
     * @param <A>     generic type of the arguments
     */
    <A> void accept(Visitor<A> visitor, A arg);

    /**
     * Process visitor and return result of {@code T} type.
     *
     * @param visitor Visitor
     * @param arg     additional argument
     * @param <T>     generic type of the result
     * @param <A>     generic type of the arguments
     * @return
     */
    <T, A> T accept(GenericVisitor<T, A> visitor, A arg);
}
