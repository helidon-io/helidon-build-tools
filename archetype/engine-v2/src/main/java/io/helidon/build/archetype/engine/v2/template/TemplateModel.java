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

package io.helidon.build.archetype.engine.v2.template;

import io.helidon.build.archetype.engine.v2.descriptor.Model;

/**
 * Template Model Archetype.
 */
public class TemplateModel {

    private Model model;

    /**
     * Template default constructor.
     */
    public TemplateModel() {
        this.model = null;
    }

    /**
     * Merge a new model to the unique model.
     *
     * @param model model to be merged
     */
    public void mergeModel(Model model) {
        if (model == null) {
            return;
        }
        if (this.model == null) {
            this.model = model;
            return;
        }

        this.model.keyValues().addAll(model.keyValues());
        this.model.keyLists().addAll(model.keyLists());
        this.model.keyMaps().addAll(model.keyMaps());
    }

    /**
     * Get the unique model descriptor.
     *
     * @return model descriptor
     */
    public Model model() {
        return model;
    }

}
