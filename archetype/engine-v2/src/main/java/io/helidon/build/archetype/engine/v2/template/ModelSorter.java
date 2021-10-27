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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.interpreter.ValueTypeAST;

/**
 * Class used to sort {@link TemplateModel} by order.
 */
public class ModelSorter {

    private ModelSorter() {
    }

    /**
     * Sort {@link TemplateModel} element by order.
     *
     * @param values    Model values
     * @param lists     Model lists
     * @param maps      Model maps
     */
    public static void sortModelByOrder(MergingMap<String, ValueTypeAST> values,
                                        MergingMap<String, TemplateList> lists,
                                        MergingMap<String, TemplateMap> maps) {
        sortValuesOrder(values);
        sortListsOrder(lists);
        sortMapsOrder(maps);
    }

    private static void sortMapsOrder(MergingMap<String, TemplateMap> maps) {
        List<Map.Entry<String, TemplateMap>> map = new ArrayList<>(maps.entrySet());
        map.sort(Map.Entry.comparingByValue());
        maps.clear();
        for (Map.Entry<String, TemplateMap> entry : map) {
            maps.put(entry.getKey(), entry.getValue());
        }
        for (String key : maps.keySet()) {
            sortValuesOrder(maps.get(key).values());
            sortListsOrder(maps.get(key).lists());
            sortMapsOrder(maps.get(key).maps());
        }
    }

    private static void sortListsOrder(MergingMap<String, TemplateList> lists) {
        List<Map.Entry<String, TemplateList>> list = new ArrayList<>(lists.entrySet());
        list.sort(Map.Entry.comparingByValue());
        lists.clear();
        for (Map.Entry<String, TemplateList> entry : list) {
            lists.put(entry.getKey(), entry.getValue());
        }
        for (String key : lists.keySet()) {
            sortValuesOrder(lists.get(key).values());
            sortListsOrder(lists.get(key).lists());
            sortMapsOrder(lists.get(key).maps());
        }
    }

    private static void sortValuesOrder(MergingMap<String, ValueTypeAST> values) {
        List<Map.Entry<String, ValueTypeAST>> list = new ArrayList<>(values.entrySet());
        list.sort(Map.Entry.comparingByValue());
        values.clear();
        for (Map.Entry<String, ValueTypeAST> entry : list) {
            values.put(entry.getKey(), entry.getValue());
        }
    }

    private static void sortMapsOrder(List<TemplateMap> maps) {
        maps.sort(Comparator.comparingInt(TemplateMap::order));
        for (TemplateMap map : maps) {
            sortValuesOrder(map.values());
            sortListsOrder(map.lists());
            sortMapsOrder(map.maps());
        }
    }

    private static void sortListsOrder(List<TemplateList> lists) {
        lists.sort(Comparator.comparingInt(TemplateList::order));
        for (TemplateList list : lists) {
            sortValuesOrder(list.values());
            sortListsOrder(list.lists());
            sortMapsOrder(list.maps());
        }
    }

    private static void sortValuesOrder(List<ValueTypeAST> values) {
        values.sort(Comparator.comparingInt(ValueTypeAST::order));
    }
}
