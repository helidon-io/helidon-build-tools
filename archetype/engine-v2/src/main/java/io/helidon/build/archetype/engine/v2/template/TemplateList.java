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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.archetype.engine.v2.descriptor.ListType;
import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

/**
 * Template list used in {@link TemplateModel}.
 */
public class TemplateList {

    private final List<TemplateValue> templateValues = new LinkedList<>();
    private final List<TemplateList> templateLists = new LinkedList<>();
    private final List<TemplateMap> templateMaps = new LinkedList<>();
    private final int order;

    /**
     * TemplateList constructor.
     *
     * @param list list containing xml descriptor data
     *
     * @throws IOException if file attribute has I/O issues
     */
    public TemplateList(ListType list) throws IOException {
        this.order = list.order();
        for (ValueType value : list.values()) {
            templateValues.add(new TemplateValue(value));
        }
        for (MapType map : list.maps()) {
            templateMaps.add(new TemplateMap(map));
        }
        for (ListType listType : list.lists()) {
            templateLists.add(new TemplateList(listType));
        }
    }

    /**
     * Get its order.
     *
     * @return order
     */
    public int order() {
        return order;
    }

    /**
     * Get the map of {@link TemplateValue} merged by key for this {@link TemplateList}.
     *
     * @return values
     */
    public List<TemplateValue> values() {
        return templateValues;
    }

    /**
     * Get the map of {@link TemplateList} merged by key for this {@link TemplateList}.
     *
     * @return values
     */
    public List<TemplateList> lists() {
        return templateLists;
    }

    /**
     * Get the map of {@link TemplateMap} merged by key for this {@link TemplateList}.
     *
     * @return values
     */
    public List<TemplateMap> maps() {
        return templateMaps;
    }

}
