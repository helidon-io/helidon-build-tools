/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import io.helidon.build.sitegen.AbstractBuilder;
import io.helidon.build.sitegen.RenderingContext;
import io.helidon.build.sitegen.RenderingException;
import io.helidon.build.sitegen.Site;
import io.helidon.config.Config;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import org.asciidoctor.ast.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.helidon.build.sitegen.Helper.checkNonNull;
import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;

/**
 * A facade over freemarker.
 */
public class FreemarkerEngine {

    private static final String BACKEND_PROP = "backend";
    private static final String DIRECTIVES_PROP = "directives";
    private static final String MODEL_PROP = "model";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Logger LOGGER = LoggerFactory.getLogger(FreemarkerEngine.class);
    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_23;
    private static final ObjectWrapper OBJECT_WRAPPER = new ObjectWrapper(FREEMARKER_VERSION);

    private final String backend;
    private final Map<String, String> directives;
    private final Map<String, String> model;
    private final Configuration freemarker;

    /**
     * Create a new instance of {@link FreemarkerEngine}.
     * @param backend the backend name
     * @param directives custom directives to register
     * @param model some model attributes to set for each rendering invocation
     */
    public FreemarkerEngine(String backend,
                            Map<String, String> directives,
                            Map<String, String> model) {
        checkNonNullNonEmpty(backend, BACKEND_PROP);
        this.backend = backend;
        this.directives = directives == null ? Collections.emptyMap() : directives;
        this.model = model == null ? Collections.emptyMap() : model;
        freemarker = new Configuration(FREEMARKER_VERSION);
        freemarker.setTemplateLoader(new TemplateLoader());
        freemarker.setDefaultEncoding(DEFAULT_ENCODING);
        freemarker.setObjectWrapper(OBJECT_WRAPPER);
        freemarker.setTemplateExceptionHandler(
                TemplateExceptionHandler.RETHROW_HANDLER);
        freemarker.setLogTemplateExceptions(false);
    }

    /**
     * Get the custom directives in-use.
     * @return {@code Map<String, String>}, never {@code null}
     */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /**
     * Get the custom model in-use.
     * @return {@code Map<String, String>}, never {@code null}
     */
    public Map<String, String> getModel() {
        return model;
    }

    /**
     * Render a template to a file.
     *
     * @param template the relative path of the template to render
     * @param targetPath the relative target path of the file to create
     * @param model the model for the template to use
     * @param ctx the processing context
     * @return the created file
     * @throws RenderingException if an error occurred
     */
    public File renderFile(String template,
                           String targetPath,
                           Map<String, Object> model,
                           RenderingContext ctx)
            throws RenderingException {

        String rendered = renderString(template, model, ctx.getTemplateSession());
        File target = new File(ctx.getOutputdir(), targetPath);
        target.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(target)) {
            writer.write(rendered);
        } catch (IOException ex) {
            throw new RenderingException(
                    "error while writing rendered output to file", ex);
        }
        return target;
    }

    /**
     * Render a template.
     *
     * @param template the relative path of the template to render
     * @param node the asciidoctor node to use as model for the template
     * @return the rendered output
     * @throws RenderingException if an error occurred
     */
    public String renderString(String template, ContentNode node)
            throws RenderingException {

        Object session = node.getDocument().getAttribute("templateSession");
        checkNonNull(session, "document attribute 'templateSession'");
        if (!(session instanceof TemplateSession)) {
            throw new IllegalStateException(
                    "document attribute 'templateSession' is not valid");
        }
        // TODO extract page, pages, templateSession
        // and set them as variables
        return renderString(template, node, (TemplateSession) session);
    }

    /**
     * Render a template to a string.
     *
     * @param template the relative path of the template to render
     * @param model the model for the template to use
     * @return the rendered output
     * @throws RenderingException if an error occurred
     */
    public String renderString(String template, Object model)
            throws RenderingException {
        return renderString(template, model, null);
    }

    /**
     * Render a template to a string.
     *
     * @param template the relative path of the template to render
     * @param model the model for the template to use
     * @param session the session to share the global variable across invocations
     * @return the rendered output
     * @throws RenderingException if an error occurred
     */
    public String renderString(String template, Object model, TemplateSession session)
            throws RenderingException {

        String templatePath = backend + "/" + template;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Template tpl = freemarker.getTemplate(templatePath);
            OutputStreamWriter writer = new OutputStreamWriter(baos);
            LOGGER.debug("Applying template: {}", templatePath);
            Environment env = tpl.createProcessingEnvironment(model,
                    writer);
            if (session != null) {
                for (Entry<String, TemplateDirectiveModel> directive
                        : session.getDirectives().entrySet()) {
                    env.setVariable(directive.getKey(), directive.getValue());
                }
            }
            env.setVariable("helper", new Helper(OBJECT_WRAPPER));
            env.setVariable("passthroughfix", new PassthroughFixDirective());
            env.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            env.setLogTemplateExceptions(false);
            env.process();
            return baos.toString(DEFAULT_ENCODING);
        } catch (TemplateNotFoundException ex) {
            LOGGER.warn("Unable to find template: {}", templatePath);
            return "";
        } catch (TemplateException | IOException ex) {
            throw new RenderingException(
                    "An error occurred during rendering of " + templatePath, ex);
        }
    }

    /**
     * A fluent builder to create {@link FreemarkerEngine} instances.
     */
    public static class Builder extends AbstractBuilder<FreemarkerEngine> {

        /**
         * Set some custom directives.
         * @param directives the directives to set
         * @return the {@link Builder} instance
         */
        public Builder directives(Map<String, String> directives) {
            put(DIRECTIVES_PROP, directives);
            return this;
        }

        /**
         * Set some custom model.
         * @param model the model to set
         * @return the {@link Builder} instance
         */
        public Builder model(Map<String, String> model) {
            put(MODEL_PROP, model);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node) {
            if (node.exists()) {
                node.get(DIRECTIVES_PROP).ifExists(n
                        -> put(DIRECTIVES_PROP, n.detach().asMap()));
                node.get(MODEL_PROP).ifExists(n
                        -> put(MODEL_PROP, n.detach().asMap()));
            }
            return this;
        }

        @Override
        public FreemarkerEngine build() {
            Map<String, String> directives = null;
            Map<String, String> model = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case (DIRECTIVES_PROP):
                        directives = asMap(val, String.class, String.class);
                        break;
                    case (MODEL_PROP):
                        model = asMap(val, String.class, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            String backendName = Site.THREADLOCAL.get();
            return new FreemarkerEngine(backendName, directives, model);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
