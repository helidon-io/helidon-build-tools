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

import io.helidon.build.maven.sitegen.Model;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.jruby.internal.RubyAttributesMapDecorator;

/**
 * A Freemarker {@code ObjectMapper} to wrap and unwrap objects to and
 * from {@code TemplateModel}.
 */
final class ObjectWrapper extends DefaultObjectWrapper {

    /**
     * Create a new instance.
     *
     * @param incompatibleImprovements the freemarker version
     */
    ObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
        this.setSimpleMapWrapper(true);
    }

    @Override
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return super.wrap(null);
        }
        if (obj instanceof ContentNode) {
            return new ContentNodeHashModel(this, (ContentNode) obj);
        }
        if (obj instanceof RubyAttributesMapDecorator) {
            return new ContentNodeAttributesModel(this, (RubyAttributesMapDecorator) obj);
        }
        if (obj instanceof Document) {
            return new SimpleObjectModel(obj);
        }
        if (obj instanceof Model) {
            return new SimpleHashModel(this, (Model) obj);
        }
        return super.wrap(obj);
    }

    @Override
    public Object unwrap(TemplateModel model) throws TemplateModelException {
        if (model instanceof ContentNodeHashModel) {
            return ((ContentNodeHashModel) model).getContentNode();
        }
        if (model instanceof SimpleObjectModel) {
            return ((SimpleObjectModel) model).wrapped();
        }
        if (model instanceof SimpleHashModel) {
            return ((SimpleHashModel) model).wrapped();
        }
        return super.unwrap(model);
    }
}
