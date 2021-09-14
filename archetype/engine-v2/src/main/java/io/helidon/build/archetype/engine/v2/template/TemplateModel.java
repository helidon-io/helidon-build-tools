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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.descriptor.ListType;
import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;
import io.helidon.build.archetype.engine.v2.expression.evaluator.Expression;

/**
 * Template Model Archetype.
 */
public class TemplateModel {

    private Model model;

    TemplateModel() {
        this.model = null;
    }

    /**
     * Merge a new model to the unique model.
     *
     * @param model model to be merged
     */
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

    private void sortModel() {
        sortValues(model.keyValues());
        sortLists(model.keyLists());
        sortMaps(model.keyMaps());
    }

    private void sortMaps(LinkedList<? extends MapType> maps) {
        maps.sort(Comparator.comparingInt(MapType::order));
        for (MapType map : maps) {
            sortValues(map.keyValues());
            sortLists(map.keyLists());
            sortMaps(map.keyMaps());
        }
    }

    private void sortLists(LinkedList<? extends ListType> lists) {
        lists.sort(Comparator.comparingInt(ListType::order));
        for (ListType list : lists) {
            sortValues(list.values());
            sortLists(list.lists());
            sortMaps(list.maps());
        }
    }

    private void sortValues(LinkedList<? extends ValueType> values) {
        values.sort(Comparator.comparingInt(ValueType::order));
    }

    private boolean evaluateCondition(String condition, Map<String, String> variables) {
        return condition == null
                || Expression.builder().expression(condition).build().evaluate(variables);
    }

    /**
     * Get the unique model descriptor.
     *
     * @return model descriptor
     */
    public Model model() {
        sortModel();
        return model;
    }

}
