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

public interface ContextValue {

    /**
     * Flags this value as declared externally prior to any flow invocation.
     *  E.g. passed-in with query parameter or CLI option.
     *
     * @return external
     */
    boolean external();

    /**
     * Flags this value as set by a <context> directive.
     *
     * @return read-only
     */
    boolean readOnly();
}
