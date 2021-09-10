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

import io.helidon.build.archetype.engine.v2.descriptor.ListType;
import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

class TemplateList {

    private final List<TemplateValue> templateValues = new LinkedList<>();
    private final List<TemplateList> templateLists = new LinkedList<>();
    private final List<TemplateMap> templateMaps = new LinkedList<>();
    private final int order;

    TemplateList(ListType list) throws IOException {
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

    public int order() {
        return order;
    }

    public List<TemplateValue> values() {
        return templateValues;
    }

    public List<TemplateList> lists() {
        return templateLists;
    }

    public List<TemplateMap> maps() {
        return templateMaps;
    }

}
