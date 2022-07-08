/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.freemarker;

import java.util.Objects;

import io.helidon.build.maven.sitegen.Model;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A Freemarker template model to resolve {@link Model}.
 */
final class SimpleHashModel implements TemplateHashModel {

    private final ObjectWrapper objectWrapper;
    private final Model model;

    /**
     * Create a new instance.
     *
     * @param objectWrapper the object wrapper to use
     * @param model         model instance
     */
    SimpleHashModel(ObjectWrapper objectWrapper, Model model) {
        this.objectWrapper = Objects.requireNonNull(objectWrapper);
        this.model = Objects.requireNonNull(model);
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        return objectWrapper.wrap(model.get(key));
    }

    /**
     * Get the wrapped instance.
     *
     * @return model
     */
    public Model wrapped() {
        return model;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
