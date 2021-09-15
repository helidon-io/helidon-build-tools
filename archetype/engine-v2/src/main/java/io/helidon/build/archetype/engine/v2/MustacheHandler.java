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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.template.MergingMap;
import io.helidon.build.archetype.engine.v2.template.TemplateList;
import io.helidon.build.archetype.engine.v2.template.TemplateMap;
import io.helidon.build.archetype.engine.v2.template.TemplateModel;
import io.helidon.build.archetype.engine.v2.template.TemplateValue;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Custom mustache used to consume model and render provided template.
 */
public class MustacheHandler {

    /**
     * Mustache factory.
     */
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();
    private static final String MUSTACHE = "mustache";
    private static final MergingMap<String, TemplateValue> TEMPLATE_VALUES_MAP = new MergingMap<>();
    private static final MergingMap<String, TemplateList>  TEMPLATE_LISTS_MAP  = new MergingMap<>();
    private static final MergingMap<String, TemplateMap>   TEMPLATE_MAPS_MAP   = new MergingMap<>();

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
     * Perform pre-processed mustache values.
     */
    private static void bootstrapModel() {
        try {
            parseValues(TEMPLATE_VALUES_MAP);
            parseLists(TEMPLATE_LISTS_MAP);
            parseMaps(TEMPLATE_MAPS_MAP);
        } catch (Exception ignored) {
        }
    }

    /**
     * Convert the {@link TemplateModel} to an Object understandable for mustache.
     *
     * @param model model to be converted
     *
     * @return Object usually used as Mustache scope
     */
    private static Map<String, Object> createScope(TemplateModel model) {
        Map<String, Object> scope = new NonNullMap<>();
        try {
            resolveModel(model.model());
        } catch (IOException ignored) {
        }
        bootstrapModel();
        scope.putAll(valuesToMap(TEMPLATE_VALUES_MAP));
        scope.putAll(listToMap(TEMPLATE_LISTS_MAP));
        scope.putAll(mapsToMap(TEMPLATE_MAPS_MAP));

        return scope;
    }

    private static void resolveModel(Model model) throws IOException {
        for (ModelKeyValue value : model.keyValues()) {
            TEMPLATE_VALUES_MAP.put(value.key(), new TemplateValue(value));
        }
        for (ModelKeyList list : model.keyLists()) {
            TEMPLATE_LISTS_MAP.put(list.key(), new TemplateList(list));
        }
        for (ModelKeyMap map : model.keyMaps()) {
            TEMPLATE_MAPS_MAP.put(map.key(), new TemplateMap(map));
        }
    }

    private static void parseMaps(MergingMap<String, TemplateMap> maps) {
        for (String key : maps.keySet()) {
            parseValues(maps.get(key).values());
            parseLists(maps.get(key).lists());
            parseMaps(maps.get(key).maps());
        }
    }

    private static void parseMaps(List<TemplateMap> maps) {
        for (TemplateMap map : maps) {
            parseValues(map.values());
            parseLists(map.lists());
            parseMaps(map.maps());
        }
    }

    private static void parseLists(MergingMap<String, TemplateList> lists) {
        for (String key : lists.keySet()) {
            parseValues(lists.get(key).values());
            parseLists(lists.get(key).lists());
            parseMaps(lists.get(key).maps());
        }
    }

    private static void parseLists(List<TemplateList> lists) {
        for (TemplateList list : lists) {
            parseValues(list.values());
            parseLists(list.lists());
            parseMaps(list.maps());
        }
    }

    private static void parseValues(List<TemplateValue> values) {
        for (TemplateValue templateValue : values) {
            if (templateValue.template().equals(MUSTACHE)) {
                if (templateValue.value().startsWith("{{") && templateValue.value().endsWith("}}")) {
                    String keyWord = templateValue.value().substring(2, templateValue.value().length() - 2);
                    String resolved = lookForKey(values, keyWord);
                    if (resolved != null) {
                        templateValue.value(resolved);
                    }
                }
            }

        }
    }

    private static String lookForKey(List<TemplateValue> values, String keyWord) {
        for (TemplateValue templateValue : values) {
            if (templateValue.value().equals(keyWord)) {
                if (templateValue.template() != null && templateValue.template().equals(MUSTACHE)) {
                    return "not implemented yet";
                }
                return templateValue.value();
            }
        }
        return null;
    }

    private static void parseValues(MergingMap<String, TemplateValue> values) {
        for (String key : values.keySet()) {
            if (values.get(key).template() != null && values.get(key).template().equals(MUSTACHE)) {
                TemplateValue value = values.get(key);
                if (value.value().startsWith("{{") && value.value().endsWith("}}")) {
                    String keyWord = value.value().substring(2, value.value().length() - 2);
                    String resolved = lookForKey(values, keyWord);
                    if (resolved != null) {
                        value.value(resolved);
                    }
                }
            }

        }
    }

    private static String lookForKey(MergingMap<String, TemplateValue> values, String keyWord) {
        for (String key : values.keySet()) {
            if (key.equals(keyWord)) {
                if (values.get(key).template() != null && values.get(key).template().equals(MUSTACHE)) {
                    return "not implemented yet";
                }
                return values.get(key).value();
            }
        }
        return null;
    }


    private static Map<String, Object> valuesToMap(MergingMap<String, TemplateValue> templateValues) {
        Map<String, Object> resolved = new NonNullMap<>();
        for (String key : templateValues.keySet()) {
            resolved.put(key, templateValues.get(key).value());
        }
        return resolved;
    }

    private static Map<String, Object> mapsToMap(MergingMap<String, TemplateMap> templateMaps) {
        Map<String, Object> resolved = new NonNullMap<>();
        for (String key : templateMaps.keySet()) {
            Map<String, Object> m = new NonNullMap<>();
            m.putAll(valuesToMap(templateMaps.get(key).values()));
            m.putAll(listToMap(templateMaps.get(key).lists()));
            m.putAll(mapsToMap(templateMaps.get(key).maps()));
            resolved.put(key, m);
        }
        return resolved;
    }

    private static Map<String, Object> listToMap(MergingMap<String, TemplateList> templateLists) {
        Map<String, Object> resolved = new NonNullMap<>();
        for (String key : templateLists.keySet()) {
            List<Object> list = mapsToList(templateLists.get(key).maps());
            list.add(valuesToList(templateLists.get(key).values()));
            list.add(listsToList(templateLists.get(key).lists()));
            resolved.put(key, list);
        }
        return resolved;
    }

    private static List<Object> listsToList(List<TemplateList> templateLists) {
        List<Object> resolved = new NonNullList<>();
        for (TemplateList list : templateLists) {
            resolved.add(valuesToList(list.values()));
            resolved.add(listsToList(list.lists()));
            resolved.add(mapsToList(list.maps()));
        }
        return resolved;
    }

    private static List<Object> mapsToList(List<TemplateMap> maps) {
        List<Object> resolved = new NonNullList<>();
        for (TemplateMap map : maps) {
            Map<String, Object> m = new NonNullMap<>();
            m.putAll(valuesToMap(map.values()));
            m.putAll(listToMap(map.lists()));
            m.putAll(mapsToMap(map.maps()));
            resolved.add(m);
        }
        return resolved;
    }

    private static List<String> valuesToList(List<TemplateValue> values) {
        List<String> resolved = new NonNullList<>();
        for (TemplateValue value : values) {
            resolved.add(value.value());
        }
        return resolved;
    }

    private static void cleanHandler() {
        TEMPLATE_VALUES_MAP.clear();
        TEMPLATE_LISTS_MAP.clear();
        TEMPLATE_MAPS_MAP.clear();
    }

    /**
     * List with non null values.
     *
     * @param <O> any object.
     */
    static class NonNullList<O> extends ArrayList<O> {

        @Override
        public boolean add(O object) {
            if (object == null) {
                return false;
            }
            if (object instanceof Map) {
                if (((Map<?, ?>) object).isEmpty()) {
                    return false;
                }
            }
            if (object instanceof List) {
                if (((List<?>) object).isEmpty()) {
                    return false;
                }
            }
            return super.add(object);
        }
    }

    /**
     * Map containing non null or not empty values for {@link List} or {@link Map}.
     *
     * @param <K> key
     * @param <V> value
     */
    static class NonNullMap<K, V> extends TreeMap<K, V> {

        @Override
        public V put(K key, V object) {
            if (object instanceof Map) {
                if (((Map<?, ?>) object).isEmpty()) {
                    return null;
                }
            }
            if (object instanceof List) {
                if (((List<?>) object).isEmpty()) {
                    return null;
                }
            }
            return super.put(key, object);
        }
    }
}
