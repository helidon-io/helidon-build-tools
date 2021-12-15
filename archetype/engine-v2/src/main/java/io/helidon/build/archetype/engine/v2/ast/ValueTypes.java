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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.List;

import io.helidon.build.common.GenericType;

/**
 * All built-in value types.
 */
public final class ValueTypes {

    /**
     * String.
     */
    public static final GenericType<String> STRING = GenericType.create(String.class);

    /**
     * Boolean.
     */
    public static final GenericType<Boolean> BOOLEAN = GenericType.create(Boolean.class);

    /**
     * Integer.
     */
    public static final GenericType<Integer> INT = GenericType.create(Integer.class);

    /**
     * List of strings.
     */
    public static final GenericType<List<String>> STRING_LIST = new GenericType<>() {
    };

    private ValueTypes() {
    }
}
