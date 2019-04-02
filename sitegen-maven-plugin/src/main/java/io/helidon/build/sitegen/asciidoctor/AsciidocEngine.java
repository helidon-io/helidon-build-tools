/*
 * Copyright (c) 2018-2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen.asciidoctor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.helidon.build.sitegen.AbstractBuilder;
import io.helidon.build.sitegen.Page;
import io.helidon.build.sitegen.RenderingContext;
import io.helidon.build.sitegen.RenderingException;
import io.helidon.build.sitegen.Site;
import io.helidon.config.Config;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static io.helidon.build.sitegen.Helper.checkNonNull;
import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;
import static io.helidon.build.sitegen.Helper.checkValidFile;
import static io.helidon.build.sitegen.Helper.getRelativePath;

/**
 * A facade over Asciidoctorj.
 *
 * @author rgrecour
 */
public class AsciidocEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsciidocEngine.class);
    private static final String BACKEND_PROP = "backend";
    private static final String LIBRARIES_PROP = "libraries";
    private static final String ATTRIBUTES_PROP = "attributes";
    private static final String IMAGESDIR_PROP = "imagesdir";

    /**
     * Constant for the default images directory.
     */
    public static final String DEFAULT_IMAGESDIR = "./images";

    private static volatile Asciidoctor asciidoctorInstance = null;

    private final String backend;
    private final List<String> libraries;
    private final Map<String, Object> attributes;
    private final String imagesdir;
    private final Asciidoctor asciidoctor;

    /**
     * Create a new instance of {@link AsciidocEngine}.
     * @param backend the name of the backend
     * @param libraries the asciidoctor libraries to use
     * @param attributes the asciidoctor attributes to use
     * @param imagesdir the images to use
     */
    public AsciidocEngine(String backend,
                          List<String> libraries,
                          Map<String, Object> attributes,
                          String imagesdir){
        checkNonNullNonEmpty(backend, BACKEND_PROP);
        installSLF4JBridge();
        this.backend = backend;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.libraries = libraries == null ? Collections.emptyList() : libraries;
        this.imagesdir = imagesdir == null ? DEFAULT_IMAGESDIR : imagesdir;
        if (asciidoctorInstance == null) {
            asciidoctorInstance = Asciidoctor.Factory.create();
        }
        this.asciidoctor = asciidoctorInstance;
        new AsciidocExtensionRegistry(backend).register(asciidoctor);
    }

    /**
     * Unregister asciidoctor extensions.
     */
    public void unregister(){
        this.asciidoctor.unregisterAllExtensions();
    }

    /**
     * Get the asciidoctor libraries in use.
     * @return {@code List<String>} of library name, never {@code null}
     */
    public List<String> getLibraries() {
        return libraries;
    }

    /**
     * Get the attributes in use.
     * @return {@code Map<String, Object>}, never {@code null}
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Get the images directory in use.
     * @return the images directory name if set, see {@link #ATTRIBUTES_PROP}
     */
    public String getImagesdir() {
        return imagesdir;
    }

    private static String parseSection0Title(File source) {
        try (BufferedReader br = new BufferedReader(new FileReader(source))) {
            String line;
            while ((line = br.readLine()) != null) {
                // level0
                if (line.startsWith("= ")) {
                    return line.substring(2).trim();
                }
                // level1, abort...
                if (line.startsWith("== ")) {
                    break;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Error while parsing section0 title of "
                    + source.getAbsolutePath(), ex);
        }
        return null;
    }

    /**
     * Read a document's header.
     * @param source the document to read the header from
     * @return the header as {@code Map<String, Object>}, never {@code null}
     */
    public Map<String, Object> readDocumentHeader(File source){
        checkValidFile(source, "source");
        final OptionsBuilder optionsBuilder = OptionsBuilder.options()
                .attributes(
                        AttributesBuilder
                                .attributes()
                                .attributes(attributes))
                .safe(SafeMode.UNSAFE)
                .headerFooter(false)
                .eruby("")
                .baseDir(source.getParentFile())
                .option("parse_header_only", true);
        if (backend != null) {
            optionsBuilder.backend(this.backend);
        }
        Document doc = asciidoctor.loadFile(source, optionsBuilder.asMap());
        Map<String, Object> headerMap = new HashMap<>();
        String h1 = parseSection0Title(source);
        if (h1 != null) {
            headerMap.put("h1", h1);
        }
        headerMap.putAll(doc.getAttributes());
        return headerMap;
    }

    /**
     * Render the document represented by the given {@link Page} instance.
     * @param page the {@link Page} instance representing the document to render
     * @param ctx the context representing this site processing invocation
     * @param target the file to create as a result of the rendering
     * @param extraAttributes extra asciidoctor attributes, can be {@code null}
     */
    public void render(Page page,
                       RenderingContext ctx,
                       File target,
                       Map<String, Object> extraAttributes) {

        checkNonNull(page, "page");
        checkNonNull(ctx, "ctx");

        asciidoctor.requireLibraries(libraries);

        if (extraAttributes == null) {
            extraAttributes = Collections.emptyMap();
        }

        File outputdir = ctx.getOutputdir();
        File sourcedir = ctx.getSourcedir();
        File source = new File(sourcedir, page.getSourcePath());
        checkValidFile(source, "source");

        // set attributes
        final AttributesBuilder attributesBuilder = AttributesBuilder.attributes();
        attributesBuilder.attributes(extraAttributes);
        attributesBuilder.attributes(attributes);

        // not using frontmatter
        // turn on experimental
        attributesBuilder.skipFrontMatter(true)
                         .experimental(true);

        // set imagesDir and imagesoutdir
        // imagesoutdir is needed by asciidoctorj-diagram
        if (imagesdir != null) {
            attributesBuilder.imagesDir(imagesdir);
            attributesBuilder.attribute("imagesoutdir",
                    new File(outputdir, imagesdir).getPath());
        }

        // set outdir attribute to relative to outputdir from source
        // needed by asciidoctorj-diagram
        String outdir = getRelativePath(source.getParentFile(), outputdir);
        attributesBuilder.attribute("outdir", outdir);

        // set options
        // set backend name
        // use unsafe mode
        // set headerFooter to false to use embedded transform
        // basedir needed by asciidoctorj-diagram
        final OptionsBuilder optionsBuilder = OptionsBuilder.options();
        optionsBuilder
                .attributes(attributesBuilder)
                .safe(SafeMode.UNSAFE)
                .headerFooter(false)
                .eruby("")
                .baseDir(source.getParentFile())
                .option("parse", false);
        if (backend != null) {
            optionsBuilder.backend(this.backend);
        }
        LOGGER.info("rendering {} to {}", source.getPath(), target.getPath());
        Document document = asciidoctor.loadFile(source, optionsBuilder.asMap());
        document.setAttribute("templateSession", ctx.getTemplateSession(), true);
        String output = document.convert();
        FileWriter writer;
        try {
            target.getParentFile().mkdirs();
            writer = new FileWriter(target);
            writer.write(output);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            throw new RenderingException(ex.getMessage(), ex);
        }
    }

    /**
     * A fluent builder to create {@link AsciidocEngine} instances.
     */
    public static class Builder extends AbstractBuilder<AsciidocEngine> {

        /**
         * Set the libraries to use.
         * @param libraries asciidoctor library names
         * @return the {@link Builder} instance
         */
        public Builder libraries(List<String> libraries) {
            put(LIBRARIES_PROP, libraries);
            return this;
        }

        /**
         * Set the attributes to use.
         * @param attributes asciidoctor attributes
         * @return the {@link Builder} instance
         */
        public Builder attributes(Map<String, Object> attributes) {
            put(ATTRIBUTES_PROP, attributes);
            return this;
        }

        /**
         * Set the images directory name to use.
         * @param imagesdir the asciidoctor imagesdir option
         * @return the {@link Builder} instance
         */
        public Builder imagesdir(String imagesdir) {
            put(IMAGESDIR_PROP, imagesdir);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node) {
            if (node.exists()) {
                node.get(LIBRARIES_PROP).ifExists(c
                        -> put(LIBRARIES_PROP, c.asStringList()));
                node.get(ATTRIBUTES_PROP).ifExists(c
                        -> put(ATTRIBUTES_PROP, c.detach().asMap()));
                node.get(IMAGESDIR_PROP).ifExists(c
                        -> put(IMAGESDIR_PROP, c.asString()));
            }
            return this;
        }

        @Override
        public AsciidocEngine build() {
            List<String> libraries = null;
            Map<String, Object> attributes = null;
            String imagesdir = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case (LIBRARIES_PROP):
                        libraries = asList(val, String.class);
                        break;
                    case (ATTRIBUTES_PROP):
                        attributes = asMap(val, String.class, Object.class);
                        break;
                    case (IMAGESDIR_PROP):
                        imagesdir = asType(val, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            String backendName = Site.THREADLOCAL.get();
            return new AsciidocEngine(backendName, libraries, attributes, imagesdir);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Setup the SLF4J to JUL bridge.
     */
    private static void installSLF4JBridge(){
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
