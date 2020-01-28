/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.util.List;

/**
 * A project build step.
 */
public interface BuildStep {

    /**
     * Remove any output artifacts for the given component.
     * @param component The component.
     */
    default void clean(BuildComponent component) {
        component.outputDirectory().clean();
    }

    /**
     * Execute the build step, skipping any up-to-date result.
     * @param component The component.
     * @return A list of build errors, empty on success.
     */
    List<String> execute(BuildComponent component);
}
