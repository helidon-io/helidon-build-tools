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
 * Types of the {@link FlowState}.
 */
public enum FlowStateEnum {
    /**
     * Initial state when script interpreter has not started its work yet.
     */
    INITIAL,
    /**
     * State when script interpreter cannot continue its work because unresolved input elements exist.
     */
    WAITING,
    /**
     * State when flow does not have unresolved input elements.
     */
    READY,
    /**
     * State when script interpreter have done its work and the {@link Flow.Result} is ready.
     */
    DONE;
}
