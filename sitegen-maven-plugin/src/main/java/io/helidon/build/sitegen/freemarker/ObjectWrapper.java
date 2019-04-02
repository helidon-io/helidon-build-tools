/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen.freemarker;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.internal.RubyAttributesMapDecorator;

/**
 * A Freemarker {@code ObjectMapper} to wrap and unwrap objects to and
 * from {@code TemplateModel}.
 *
 * @author rgrecour
 */
public class ObjectWrapper extends DefaultObjectWrapper {

    /**
     * Create a new instance of {@link ObjectWrapper}.
     * @param incompatibleImprovements the freemarker version
     */
    public ObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
        this.setSimpleMapWrapper(true);
    }

    @Override
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return super.wrap(obj);
        }
        if (obj instanceof ContentNode) {
            return new ContentNodeHashModel(this, (ContentNode) obj);
        }
        if (obj instanceof RubyAttributesMapDecorator) {
            return new ContentNodeAttributesModel(this,
                    (RubyAttributesMapDecorator) obj);
        }
        if (obj instanceof Document){
            return new SimpleObjectModel(obj);
        }
        return super.wrap(obj);
    }

    @Override
    public Object unwrap(TemplateModel model) throws TemplateModelException {
        if (model instanceof ContentNodeHashModel) {
            return ((ContentNodeHashModel) model).getContentNode();
        }
        if (model instanceof SimpleObjectModel){
            return ((SimpleObjectModel) model).getWrapped();
        }
        return super.unwrap(model);
    }
}
