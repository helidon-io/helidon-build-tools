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

package io.helidon.build.archetype.engine.v2.template;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

import com.github.mustachejava.Mustache;

/**
 * Class used for transforming {@link TemplateModel} into an Object usable by {@link Mustache}.
 */
public class ModelTransformer {

    private ModelTransformer() {
    }

    /**
     * Convert {@link TemplateModel} element into an Object usable by {@link Mustache}.
     *
     * @param values    Model values
     * @param lists     Model lists
     * @param maps      Model maps
     * @return          scope
     */
    public static Map<String, Object> transform(MergingMap<String, ValueType> values,
                                                MergingMap<String, TemplateList> lists,
                                                MergingMap<String, TemplateMap> maps) {
        Map<String, Object> scope = new NonNullMap<>();
        scope.putAll(valuesToMap(values));
        scope.putAll(listToMap(lists));
        scope.putAll(mapsToMap(maps));
        return scope;
    }

    private static Map<String, Object> valuesToMap(MergingMap<String, ValueType> templateValues) {
        Map<String, Object> resolved = new NonNullMap<>();
        for (String key : templateValues.keySet()) {
            ValueType value = templateValues.get(key);
            if (value.value() != null && value.file() != null && value.template() != null) {
                resolved.put(key, value.value());
                continue;
            }
            if (value.value() != null) {
                resolved.put(key, value.value());
                continue;
            }
            if (value.file() != null && value.template() == null) {
                try (InputStream is = new FileInputStream(value.file())) {
                    resolved.put(key, new String(is.readAllBytes()));
                } catch (IOException ignored) {
                }
            }
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

    private static List<String> valuesToList(List<ValueType> values) {
        List<String> resolved = new NonNullList<>();
        for (ValueType value : values) {
            if (value.value() != null && value.file() != null && value.template() != null) {
                resolved.add(value.value());
                continue;
            }
            if (value.value() != null) {
                resolved.add(value.value());
            }
            if (value.file() != null) {
                try (InputStream is = new FileInputStream(value.file())) {
                    resolved.add(new String(is.readAllBytes()));
                } catch (IOException ignored) {
                }
            }
        }
        return resolved;
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
     * Map containing non null or not empty values for {@link List} and {@link Map}.
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
