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

import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;



/**
 * Template map used in {@link TemplateModel}.
 */
public class TemplateMap {
    private final MergingMap<String, TemplateValue> templateValues = new MergingMap<>();
    private final MergingMap<String, TemplateList> templateLists = new MergingMap<>();
    private final MergingMap<String, TemplateMap> templateMaps = new MergingMap<>();
    private final int order;

    /**
     * Template map constructor.
     *
     * @param map Map containing xml descriptor data
     *
     * @throws IOException if file attribute has I/O issues
     */
    public TemplateMap(MapType map) throws IOException {
        this.order = map.order();
        for (ModelKeyValue value : map.keyValues()) {
            templateValues.put(value.key(), new TemplateValue(value));
        }
        for (ModelKeyList list : map.keyLists()) {
            templateLists.put(list.key(), new TemplateList(list));
        }
        for (ModelKeyMap keyMap : map.keyMaps()) {
            templateMaps.put(keyMap.key(), new TemplateMap(keyMap));
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
     * Get the map of {@link TemplateValue} merged by key for this {@link TemplateMap}.
     *
     * @return values
     */
    public MergingMap<String, TemplateValue> values() {
        return templateValues;
    }

    /**
     * Get the map of {@link TemplateList} merged by key for this {@link TemplateMap}.
     *
     * @return lists
     */
    public MergingMap<String, TemplateList> lists() {
        return templateLists;
    }

    /**
     * Get the map of {@link TemplateMap} merged by key for this {@link TemplateMap}.
     *
     * @return maps
     */
    public MergingMap<String, TemplateMap> maps() {
        return templateMaps;
    }
}
