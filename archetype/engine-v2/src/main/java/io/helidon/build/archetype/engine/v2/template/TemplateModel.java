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


import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.expression.evaluator.Expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Template Model Archetype.
 */
public class TemplateModel {

    private Model model;
    private final MergingMap<String, TemplateValue> templateValues = new MergingMap<>();
    private final MergingMap<String, TemplateList> templateLists = new MergingMap<>();
    private final MergingMap<String, TemplateMap> templateMaps = new MergingMap<>();
    private final Map<String, Object> scope = new NonNullMap<>();

    TemplateModel() {
        this.model = null;
    }

    public void mergeModel(Model model) {
        if (model == null) {
            return;
        }
        if (this.model == null) {
            this.model = model;
            return;
        }

        if (evaluateCondition(model.ifProperties(), null)) {
            this.model.keyValues().addAll(model.keyValues());
            this.model.keyLists().addAll(model.keyLists());
            this.model.keyMaps().addAll(model.keyMaps());
        }
    }

    public Map<String, Object> createScope() {
        try {
            resolveModel();
        } catch (IOException ignored) {
        }
        scope.putAll(valuesToMap(templateValues));
        scope.putAll(listToMap(templateLists));
        scope.putAll(mapsToMap(templateMaps));

        return scope;
    }

    private void resolveModel() throws IOException {
        for (ModelKeyValue value : model.keyValues()) {
            templateValues.put(value.key(), new TemplateValue(value));
        }
        for (ModelKeyList list : model.keyLists()) {
            templateLists.put(list.key(), new TemplateList(list));
        }
        for (ModelKeyMap map : model.keyMaps()) {
            templateMaps.put(map.key(), new TemplateMap(map));
        }
    }

    private Map<String, Object> valuesToMap(MergingMap<String, TemplateValue> templateValues) {
        Map<String, Object> resolved = new NonNullMap<>();
        for (String key : templateValues.keySet()) {
            resolved.put(key, templateValues.get(key).value());
        }
        return resolved;
    }

    private Map<String, Object> mapsToMap(MergingMap<String, TemplateMap> templateMaps) {
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

    private Map<String, Object> listToMap(MergingMap<String, TemplateList> templateLists) {
        Map<String, Object> resolved = new NonNullMap<>();
        for (String key : templateLists.keySet()) {
            List<Object> list = mapsToList(templateLists.get(key).maps());
            list.add(valuesToList(templateLists.get(key).values()));
            list.add(listsToList(templateLists.get(key).lists()));
            resolved.put(key, list);
        }
        return resolved;
    }

    private List<Object> listsToList(List<TemplateList> templateLists) {
        List<Object> resolved = new NonNullList<>();
        for (TemplateList list : templateLists) {
            resolved.add(valuesToList(list.values()));
            resolved.add(listsToList(list.lists()));
            resolved.add(mapsToList(list.maps()));
        }
        return resolved;
    }

    private List<Object> mapsToList(List<TemplateMap> maps) {
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

    private List<String> valuesToList(List<TemplateValue> values) {
        List<String> resolved = new NonNullList<>();
        for (TemplateValue value : values) {
            resolved.add(value.value());
        }
        return resolved;
    }

    private boolean evaluateCondition(String condition, Map<String, String> variables) {
        return condition == null
                || Expression.builder().expression(condition).build().evaluate(variables);
    }

    public Model model() {
        return model;
    }

    static class NonNullList<Object> extends ArrayList<Object> {

        @Override
        public boolean add(Object object) {
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

    static class NonNullMap<String, Object> extends HashMap<String, Object> {

        @Override
        public Object put(String key, Object object) {
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
