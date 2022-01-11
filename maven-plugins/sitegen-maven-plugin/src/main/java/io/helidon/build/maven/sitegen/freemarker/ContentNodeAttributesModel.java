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

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.jruby.internal.RubyAttributesMapDecorator;

/**
 * A Freemarker template model to resolve {@link org.asciidoctor.ast.ContentNode} attributes.
 *
 * This provides normal freemarker dotted notation access to the map as well
 * as invoking methods.
 */
public class ContentNodeAttributesModel implements TemplateHashModel {

    private final RubyAttributesMapDecorator rubyMap;
    private final ObjectWrapper objectWrapper;

    /**
     * Create a new instance of {@link ContentNodeAttributesModel}.
     * @param objectWrapper the {@link ObjectWrapper} to use wrapping java objects.
     * @param rubyMap the {@link RubyAttributesMapDecorator} containing the attributes
     */
    public ContentNodeAttributesModel(ObjectWrapper objectWrapper,
                                      RubyAttributesMapDecorator rubyMap) {
        Objects.requireNonNull(rubyMap);
        this.rubyMap = rubyMap;
        Objects.requireNonNull(objectWrapper);
        this.objectWrapper = objectWrapper;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (rubyMap.containsKey(key)) {
            return objectWrapper.wrap(rubyMap.get(key));
        }
        // return method model if method name found for key
        if (SimpleMethodModel.hasMethodWithName(rubyMap, key)) {
            return new SimpleMethodModel(objectWrapper, rubyMap, key);
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return rubyMap.isEmpty();
    }
}
