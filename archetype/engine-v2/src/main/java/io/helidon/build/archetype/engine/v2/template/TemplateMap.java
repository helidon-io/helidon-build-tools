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

import io.helidon.build.archetype.engine.v2.descriptor.ValueType;
import io.helidon.build.archetype.engine.v2.interpreter.MapTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyListAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyMapAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyValueAST;
import io.helidon.build.archetype.engine.v2.interpreter.ValueTypeAST;

/**
 * Template map used in {@link TemplateModel}.
 */
public class TemplateMap implements Comparable {

    private final MergingMap<String, ValueTypeAST> templateValues  = new MergingMap<>();
    private final MergingMap<String, TemplateList> templateLists   = new MergingMap<>();
    private final MergingMap<String, TemplateMap>  templateMaps    = new MergingMap<>();
    private final int order;

    /**
     * Template map constructor.
     *
     * @param map Map containing xml descriptor data
     */
    public TemplateMap(MapTypeAST map) {
        this.order = map.order();
        map.children().stream()
                .filter(o -> o instanceof ModelKeyValueAST)
                .map(o -> (ModelKeyValueAST) o)
                .forEach(v -> templateValues.put(v.key(), v));
        map.children().stream()
                .filter(o -> o instanceof ModelKeyListAST)
                .map(o -> (ModelKeyListAST) o)
                .forEach(l -> templateLists.put(l.key(), new TemplateList(l)));
        map.children().stream()
                .filter(o -> o instanceof ModelKeyMapAST)
                .map(o -> (ModelKeyMapAST) o)
                .forEach(m -> templateMaps.put(m.key(), new TemplateMap(m)));
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
     * Get the map of {@link ValueType} merged by key for this {@link TemplateMap}.
     *
     * @return values
     */
    public MergingMap<String, ValueTypeAST> values() {
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

    @Override
    public int compareTo(Object o) {
        if (o instanceof TemplateMap) {
            return Integer.compare(this.order, ((TemplateMap) o).order);
        }
        return 0;
    }
}
