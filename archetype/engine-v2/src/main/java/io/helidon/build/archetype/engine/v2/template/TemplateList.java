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

import java.util.LinkedList;
import java.util.List;

import io.helidon.build.archetype.engine.v2.descriptor.ValueType;
import io.helidon.build.archetype.engine.v2.interpreter.ListTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.MapTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.ValueTypeAST;

/**
 * Template list used in {@link TemplateModel}.
 */
public class TemplateList implements Comparable {

    private final List<ValueTypeAST> templateValues = new LinkedList<>();
    private final List<TemplateList> templateLists = new LinkedList<>();
    private final List<TemplateMap> templateMaps = new LinkedList<>();
    private final int order;

    /**
     * TemplateList constructor.
     *
     * @param list list containing xml descriptor data
     */
    public TemplateList(ListTypeAST list) {
        this.order = list.order();
        list.children().stream()
                .filter(o -> o instanceof ValueTypeAST)
                .map(o -> (ValueTypeAST) o)
                .forEach(templateValues::add);
        list.children().stream()
                .filter(o -> o instanceof ListTypeAST)
                .map(o -> (ListTypeAST) o)
                .forEach(l -> templateLists.add(new TemplateList(l)));
        list.children().stream()
                .filter(o -> o instanceof MapTypeAST)
                .map(o -> (MapTypeAST) o)
                .forEach(m -> templateMaps.add(new TemplateMap(m)));
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
     * Get the map of {@link ValueType} merged by key for this {@link TemplateList}.
     *
     * @return values
     */
    public List<ValueTypeAST> values() {
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

    @Override
    public int compareTo(Object o) {
        if (o instanceof TemplateList) {
            return Integer.compare(this.order, ((TemplateList) o).order);
        }
        return 0;
    }
}
