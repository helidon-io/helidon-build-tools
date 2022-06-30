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

package io.helidon.build.maven.sitegen.asciidoctor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.helidon.build.common.Instance;
import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.Context;
import io.helidon.build.maven.sitegen.models.Page;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;

import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.Strings.normalizePath;
import static io.helidon.build.common.Strings.requireValid;
import static java.util.Objects.requireNonNull;

/**
 * A facade over Asciidoctorj.
 */
public class AsciidocEngine {

    /**
     * Constant for the default images directory.
     */
    public static final String DEFAULT_IMAGESDIR = "./images";

    private final String backend;
    private final List<String> libraries;
    private final Map<String, Object> attributes;
    private final String imagesdir;
    private final Instance<Asciidoctor> asciidoctorInst;
    private final AsciidocLogHandler logHandler;
    private final AsciidocPageRenderer pageRenderer;
    private volatile String sourcePath;
    private volatile AsciidocConverter converter;

    private AsciidocEngine(Builder builder) {
        backend = requireValid(builder.backend, "backend is invalid!");
        attributes = builder.attributes;
        libraries = builder.libraries;
        imagesdir = builder.imagesDir;
        pageRenderer = new AsciidocPageRenderer(this);
        logHandler = new AsciidocLogHandler(this::frames);
        asciidoctorInst = new Instance<>(this::initAsciidoctor);
    }

    private Asciidoctor initAsciidoctor() {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        AsciidocLogHandler.init();
        asciidoctor.registerLogHandler(logHandler);
        asciidoctor.requireLibraries(libraries);
        AsciidocExtensionRegistry.create(backend).register(asciidoctor);
        return asciidoctor;
    }

    private Collection<String> frames() {
        return converter != null ? converter.frames() : List.of(sourcePath + ":0");
    }

    /**
     * Set the converter.
     *
     * @param converter converter
     */
    void converter(AsciidocConverter converter) {
        this.converter = converter;
    }

    /**
     * Get the page renderer.
     *
     * @return page renderer
     */
    public AsciidocPageRenderer pageRenderer() {
        return pageRenderer;
    }

    /**
     * Get the asciidoctor libraries in use.
     *
     * @return list library name, never {@code null}
     */
    public List<String> libraries() {
        return libraries;
    }

    /**
     * Get the attributes.
     *
     * @return attributes, never {@code null}
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * Get the image directory.
     *
     * @return image directory
     */
    public String imagesDir() {
        return imagesdir;
    }

    /**
     * Render the document represented by the given {@link Page} instance.
     *
     * @param page   the {@link Page} instance representing the document to render
     * @param ctx    the context representing this site processing invocation
     * @param target the file to create as a result of the rendering
     */
    public void render(Page page, Context ctx, Path target) {
        requireNonNull(page, "page is null!");
        requireNonNull(ctx, "ctx is null!");

        Map<String, Object> extraAttributes = Map.of("page", page);

        Path outputDir = ctx.outputDir();
        Path sourceDir = ctx.sourceDir();
        Path source = requireFile(sourceDir.resolve(page.source()));

        // set attributes
        AttributesBuilder attrsBuilder = Attributes.builder()
                                                   .attributes(attributes)
                                                   .skipFrontMatter(true)
                                                   .experimental(true)
                                                   .attributes(extraAttributes);

        // set imagesDir and 'imagesoutdir'
        // 'imagesoutdir' is needed by asciidoctorj-diagram
        if (imagesdir != null) {
            attrsBuilder.imagesDir(imagesdir);
            attrsBuilder.attribute("imagesoutdir", outputDir.resolve(imagesdir).toString());
        }

        // set outdir attribute to relative to outputDir from source
        // needed by asciidoctorj-diagram
        String outDir = relativePath(source, VirtualFileSystem.unwrap(outputDir));
        attrsBuilder.attribute("outdir", outDir);

        // set options
        // set headerFooter to false in order to use embedded transform
        // basedir needed by asciidoctorj-diagram
        Options options = Options.builder()
                                 .attributes(attrsBuilder.build())
                                 .sourcemap(true)
                                 .safe(SafeMode.UNSAFE)
                                 .headerFooter(false)
                                 .eruby("")
                                 .baseDir(source.getParent().toFile())
                                 .option("parse", false)
                                 .backend(backend)
                                 .build();

        sourcePath = sourceDir.relativize(source).toString();
        Log.info("rendering %s to %s", sourcePath, outputDir.relativize(target));

        try (Asciidoctor asciidoctor = asciidoctorInst.instance()) {
            Document document = asciidoctor.loadFile(source.toFile(), options);
            try {
                String output = document.convert();
                Files.createDirectories(target.getParent());
                Files.writeString(target, output);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    /**
     * A builder of {@link AsciidocEngine}.
     */
    @SuppressWarnings("unused")
    public static class Builder implements Supplier<AsciidocEngine> {

        private final List<String> libraries = new ArrayList<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private String imagesDir = DEFAULT_IMAGESDIR;
        private String backend;

        private Builder() {
        }

        /**
         * Set the backend name.
         *
         * @param backend backend name
         * @return this builder
         */
        public Builder backend(String backend) {
            this.backend = backend;
            return this;
        }

        /**
         * Add libraries.
         *
         * @param libraries asciidoctor library names
         * @return this builder
         */
        public Builder libraries(List<String> libraries) {
            if (libraries != null) {
                this.libraries.addAll(libraries);
            }
            return this;
        }

        /**
         * Add a library.
         *
         * @param library asciidoctor library name
         * @return this builder
         */
        public Builder library(String library) {
            if (library != null) {
                this.libraries.add(library);
            }
            return this;
        }

        /**
         * Add attributes.
         *
         * @param attributes asciidoctor attributes
         * @return this builder
         */
        public Builder attributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }

        /**
         * Add an attribute.
         *
         * @param key   attribute key
         * @param value attribute value
         * @return this builder
         */
        public Builder attribute(String key, Object value) {
            if (key != null && value != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        /**
         * Set the image directory.
         *
         * @param imagesDir the asciidoctor {@code "imagesdir"} option
         * @return this builder
         */
        public Builder imagesDir(String imagesDir) {
            this.imagesDir = imagesDir;
            return this;
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            libraries.addAll(config.get("libraries")
                                   .asList(String.class)
                                   .orElseGet(List::of));

            attributes.putAll(config.get("attributes")
                                    .asMap()
                                    .orElseGet(Map::of));

            imagesDir = config.get("images-dir")
                              .asString()
                              .orElse(null);
            return this;
        }

        /**
         * Build the new instance.
         *
         * @return new instance.
         */
        public AsciidocEngine build() {
            return new AsciidocEngine(this);
        }

        @Override
        public AsciidocEngine get() {
            return build();
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config  config
     * @param backend backend name
     * @return new instance
     */
    public static AsciidocEngine create(String backend, Config config) {
        return builder()
                .backend(backend)
                .config(config)
                .build();
    }

    /**
     * Create a new instance.
     *
     * @param backend backend name
     * @return new instance
     */
    public static AsciidocEngine create(String backend) {
        return builder()
                .backend(backend)
                .build();
    }

    /**
     * Create a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static String relativePath(Path sourceDir, Path source) {
        return normalizePath(sourceDir.relativize(source).toString());
    }
}
