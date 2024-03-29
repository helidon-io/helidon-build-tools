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

import freemarker.template.TemplateModel;

/**
 * A simple {@code TemplateModel} to pass objects from templates.
 */
final class SimpleObjectModel implements TemplateModel {

    private final Object wrapped;

    /**
     * Create a new instance of {@link SimpleObjectModel}.
     *
     * @param wrapped the object to wrap
     */
    SimpleObjectModel(Object wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Get the wrapped object.
     *
     * @return the wrapped object
     */
    public Object wrapped() {
        return wrapped;
    }
}
