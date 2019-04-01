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

package io.helidon.sitegen.freemarker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.ContentNode;

/**
 * A Freemarker template model to resolve {@link ContentNode}.
 *
 * This provides properties style references on {@link ContentNode} objects
 * inside Freemarker templates.
 *
 * @author rgrecour
 */
public class ContentNodeHashModel implements TemplateHashModel {

    private final ObjectWrapper objectWrapper;
    private final ContentNode contentNode;

    /**
     * Create a new instance of {@link ContentNodeHashModel}.
     * @param objectWrapper the object wrapper to use
     * @param node the {@link ContentNode} to expose as {@link TemplateHashModel}
     */
    public ContentNodeHashModel(ObjectWrapper objectWrapper, ContentNode node) {
        Objects.requireNonNull(objectWrapper);
        this.objectWrapper = objectWrapper;
        Objects.requireNonNull(node);
        this.contentNode = node;
    }

    /**
     * Get the wrapped content node.
     *
     * @return the wrapped instance
     */
    public ContentNode getContentNode() {
        return contentNode;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {

        // hardcode this as a shorthand to the current model
        if ("this".equals(key)) {
            return this;
        }

        // derive getter name from key
        String getterName = "get";
        getterName += Character.toUpperCase(key.charAt(0));
        getterName += key.substring(1);

        // find getter method
        Method getterMethod = null;
        for (Method m : contentNode.getClass().getMethods()) {
            if (getterName.equals(m.getName())) {
                if (m.getParameterCount() == 0) {
                    getterMethod = m;
                    break;
                }
            }
        }

        // invoke getter if found
        if (getterMethod != null) {
            try {
                return objectWrapper.wrap(getterMethod.invoke(contentNode));
            } catch (InvocationTargetException
                    | SecurityException
                    | IllegalAccessException
                    | IllegalArgumentException ex) {
                throw new TemplateModelException(String.format(
                        "Error during getter invocation: node=%s, methodname=%s",
                        contentNode,
                        getterMethod.getName()),
                        ex);
            }
        }

        // return method model if method name found for key
        if (SimpleMethodModel.hasMethodWithName(contentNode, key)) {
            return new SimpleMethodModel(objectWrapper, contentNode, key);
        }
        return null;
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
