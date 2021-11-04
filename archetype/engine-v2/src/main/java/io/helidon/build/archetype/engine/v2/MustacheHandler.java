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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.helidon.build.archetype.engine.v2.interpreter.ModelAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyListAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyMapAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyValueAST;
import io.helidon.build.archetype.engine.v2.interpreter.ValueTypeAST;
import io.helidon.build.archetype.engine.v2.template.MergingMap;
import io.helidon.build.archetype.engine.v2.template.ModelSorter;
import io.helidon.build.archetype.engine.v2.template.ModelTransformer;
import io.helidon.build.archetype.engine.v2.template.MustacheResolver;
import io.helidon.build.archetype.engine.v2.template.TemplateList;
import io.helidon.build.archetype.engine.v2.template.TemplateMap;
import io.helidon.build.archetype.engine.v2.template.TemplateModel;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * MustacheHandler render provided mustache template with the unique {@link TemplateModel}.
 */
public class MustacheHandler {

    /**
     * Mustache factory.
     */
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();
    private static final MergingMap<String, ValueTypeAST> TEMPLATE_VALUES_MAP = new MergingMap<>();
    private static final MergingMap<String, TemplateList> TEMPLATE_LISTS_MAP  = new MergingMap<>();
    private static final MergingMap<String, TemplateMap>  TEMPLATE_MAPS_MAP   = new MergingMap<>();
    private static final Set<String> TEMPLATE_FILES = new HashSet<>();

    /**
     * Mustache string.
     */
    public static final String MUSTACHE = "mustache";

    private MustacheHandler() {
    }

    /**
     * Render a mustache template.
     *
     * @param is     input stream for the template to render
     * @param name   name of the template
     * @param target target file to create
     * @param scope  the scope for the template
     * @throws IOException if an IO error occurs
     */
    public static void renderMustacheTemplate(InputStream is, String name, Path target, Object scope)
            throws IOException {

        Mustache m = MUSTACHE_FACTORY.compile(new InputStreamReader(is), name);
        Files.createDirectories(target.getParent());
        if (scope instanceof TemplateModel) {
            scope = createScope((TemplateModel) scope);
        }
        try (Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            m.execute(writer, scope).flush();
        }
        cleanHandler();
    }

    /**
     * Render a mustache template.
     *
     * @param is            input stream for the template to render
     * @param name          name of the template
     * @param target        output stream to write data
     * @param scope         the scope for the template
     * @throws IOException  if an IO error occurs
     */
    public static void renderMustacheTemplate(InputStream is, String name, OutputStream target, Object scope)
            throws IOException {

        Mustache m = MUSTACHE_FACTORY.compile(new InputStreamReader(is), name);
        if (scope instanceof TemplateModel) {
            scope = createScope((TemplateModel) scope);
        }
        try (Writer writer = new OutputStreamWriter(target, StandardCharsets.UTF_8)) {
            m.execute(writer, scope).flush();
        }
        cleanHandler();
    }

    /**
     * Convert the {@link TemplateModel} to an Object usable by mustache.
     *
     * @param model     Model to be converted
     *
     * @return          Object usually used as Mustache scope
     */
    private static Map<String, Object> createScope(TemplateModel model) throws IOException {
        resolveModel(model.model());
        ModelSorter.sortModelByOrder(TEMPLATE_VALUES_MAP, TEMPLATE_LISTS_MAP, TEMPLATE_MAPS_MAP);
        MustacheResolver.render(TEMPLATE_VALUES_MAP, TEMPLATE_LISTS_MAP, TEMPLATE_MAPS_MAP, TEMPLATE_FILES);
        Map<String, Object> scope = ModelTransformer.transform(TEMPLATE_VALUES_MAP, TEMPLATE_LISTS_MAP, TEMPLATE_MAPS_MAP);

        if (!TEMPLATE_FILES.isEmpty()) {
            MustacheResolver.renderTemplateFiles(TEMPLATE_VALUES_MAP, TEMPLATE_LISTS_MAP, TEMPLATE_MAPS_MAP,
                    TEMPLATE_FILES, scope);
            scope = ModelTransformer.transform(TEMPLATE_VALUES_MAP, TEMPLATE_LISTS_MAP, TEMPLATE_MAPS_MAP);
        }
        return scope;
    }

    private static void resolveModel(ModelAST model) {
        model.children().stream()
                .filter(o -> o instanceof ModelKeyValueAST)
                .map(o -> (ModelKeyValueAST) o)
                .forEach(v -> TEMPLATE_VALUES_MAP.put(v.key(), v));
        model.children().stream()
                .filter(o -> o instanceof ModelKeyListAST)
                .map(o -> (ModelKeyListAST) o)
                .forEach(l -> TEMPLATE_LISTS_MAP.put(l.key(), new TemplateList(l)));
        model.children().stream()
                .filter(o -> o instanceof ModelKeyMapAST)
                .map(o -> (ModelKeyMapAST) o)
                .forEach(m -> TEMPLATE_MAPS_MAP.put(m.key(), new TemplateMap(m)));
    }

    private static void cleanHandler() {
        TEMPLATE_VALUES_MAP.clear();
        TEMPLATE_LISTS_MAP.clear();
        TEMPLATE_MAPS_MAP.clear();
        TEMPLATE_FILES.clear();
    }
}
