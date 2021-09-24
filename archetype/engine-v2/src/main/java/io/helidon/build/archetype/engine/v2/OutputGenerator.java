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

import io.helidon.build.archetype.engine.v2.descriptor.ListType;
import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;
import io.helidon.build.archetype.engine.v2.interpreter.ASTNode;
import io.helidon.build.archetype.engine.v2.interpreter.Flow;
import io.helidon.build.archetype.engine.v2.interpreter.ListTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.MapTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyListAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyMapAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyValueAST;
import io.helidon.build.archetype.engine.v2.interpreter.ValueTypeAST;
import io.helidon.build.archetype.engine.v2.template.TemplateModel;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OutputGenerator {

    OutputGenerator() {

    }

    public TemplateModel createUniqueModel(List<ASTNode> outputNodes) {
        Objects.requireNonNull(outputNodes, "outputNodes is null");
        TemplateModel templateModel = new TemplateModel();

        List<ModelAST> models = outputNodes.stream()
                .filter(o -> o instanceof ModelAST)
                .map(o -> (ModelAST) o)
                .collect(Collectors.toList());

        for (ModelAST node : models) {
            templateModel.mergeModel(convertASTModel(node));
        }

        return templateModel;
    }

    public void generate() {

    }

    private Model convertASTModel(ModelAST model) {
        Model modelDescriptor = new Model("true");

        modelDescriptor.keyValues().addAll(convertASTKeyValues(model.children().stream()
                .filter(v ->  v instanceof ModelKeyValueAST)
                .map(v -> (ModelKeyValueAST) v)
                .collect(Collectors.toList()))
        );

        modelDescriptor.keyLists().addAll(convertASTKeyLists(model.children().stream()
                .filter(v ->  v instanceof ModelKeyListAST)
                .map(v -> (ModelKeyListAST) v)
                .collect(Collectors.toList()))
        );

        modelDescriptor.keyMaps().addAll(convertASTKeyMaps(model.children().stream()
                .filter(v ->  v instanceof ModelKeyMapAST)
                .map(v -> (ModelKeyMapAST) v)
                .collect(Collectors.toList()))
        );

        return modelDescriptor;
    }

    private Collection<? extends ModelKeyMap> convertASTKeyMaps(List<ModelKeyMapAST> astMaps) {
        LinkedList<ModelKeyMap> maps = new LinkedList<>();

        for (ModelKeyMapAST map : astMaps) {
            ModelKeyMap keyMap = new ModelKeyMap(map.key(), map.order(), "true");

            keyMap.keyValues().addAll(convertASTKeyValues(map.children().stream()
                    .filter(v ->  v instanceof ModelKeyValueAST)
                    .map(v -> (ModelKeyValueAST) v)
                    .collect(Collectors.toList()))
            );

            keyMap.keyLists().addAll(convertASTKeyLists(map.children().stream()
                    .filter(v ->  v instanceof ModelKeyListAST)
                    .map(v -> (ModelKeyListAST) v)
                    .collect(Collectors.toList()))
            );

            keyMap.keyMaps().addAll(convertASTKeyMaps(map.children().stream()
                    .filter(v ->  v instanceof ModelKeyMapAST)
                    .map(v -> (ModelKeyMapAST) v)
                    .collect(Collectors.toList()))
            );

            maps.add(keyMap);
        }

        return maps;
    }

    private Collection<? extends ModelKeyList> convertASTKeyLists(List<ModelKeyListAST> astLists) {
        LinkedList<ModelKeyList> lists = new LinkedList<>();

        for (ModelKeyListAST list : astLists) {
            ModelKeyList keyList = new ModelKeyList(list.key(), list.order(), "true");

            keyList.values().addAll(convertASTValues(list.children().stream()
                    .filter(v -> v instanceof ValueTypeAST)
                    .map(v -> (ValueTypeAST) v)
                    .collect(Collectors.toList()))
            );

            keyList.lists().addAll(convertASTLists(list.children().stream()
                    .filter(v -> v instanceof ListTypeAST)
                    .map(v -> (ListTypeAST) v)
                    .collect(Collectors.toList()))
            );

            keyList.maps().addAll(convertASTMaps(list.children().stream()
                    .filter(v -> v instanceof MapTypeAST)
                    .map(v -> (MapTypeAST) v)
                    .collect(Collectors.toList()))
            );

            lists.add(keyList);
        }

        return lists;
    }

    private Collection<? extends ModelKeyValue> convertASTKeyValues(List<ModelKeyValueAST> astValues) {
        LinkedList<ModelKeyValue> values = new LinkedList<>();

        for (ModelKeyValueAST value : astValues) {
            ModelKeyValue keyValue = new ModelKeyValue(
                    value.key(),
                    value.url(),
                    value.file(),
                    value.template(),
                    value.order(),
                    "true");
            keyValue.value(value.value());
            values.add(keyValue);
        }
        return values;
    }

    private Collection<? extends ValueType> convertASTValues(List<ValueTypeAST> astValues) {
        LinkedList<ValueType> values = new LinkedList<>();

        for (ValueTypeAST value : astValues) {
            ValueType valueType = new ValueType(
                    value.url(),
                    value.file(),
                    value.template(),
                    value.order(),
                    "true");
            valueType.value(value.value());
            values.add(valueType);
        }
        return values;
    }

    private Collection<? extends ListType> convertASTLists(List<ListTypeAST> astList) {
        LinkedList<ListType> lists = new LinkedList<>();

        for (ListTypeAST list : astList) {
            ListType listType = new ListType(list.order(), "true");

            listType.values().addAll(convertASTValues(list.children().stream()
                    .filter(l -> l instanceof ValueTypeAST)
                    .map(v -> (ValueTypeAST) v)
                    .collect(Collectors.toList()))
            );

            listType.lists().addAll(convertASTLists(list.children().stream()
                    .filter(l -> l instanceof ListTypeAST)
                    .map(v -> (ListTypeAST) v)
                    .collect(Collectors.toList()))

            );

            listType.maps().addAll(convertASTMaps(list.children().stream()
                    .filter(v -> v instanceof MapTypeAST)
                    .map(v -> (MapTypeAST) v)
                    .collect(Collectors.toList()))
            );

            lists.add(listType);
        }

        return lists;
    }

    private Collection<? extends MapType> convertASTMaps(List<MapTypeAST> astMap) {
        LinkedList<MapType> maps = new LinkedList<>();

        for (MapTypeAST map : astMap) {
            MapType mapType = new MapType(map.order(), "true");

            mapType.keyValues().addAll(convertASTKeyValues(map.children().stream()
                    .filter(l -> l instanceof ModelKeyValueAST)
                    .map(v -> (ModelKeyValueAST) v)
                    .collect(Collectors.toList()))
            );

            mapType.keyLists().addAll(convertASTKeyLists(map.children().stream()
                    .filter(l -> l instanceof ModelKeyListAST)
                    .map(v -> (ModelKeyListAST) v)
                    .collect(Collectors.toList()))

            );

            mapType.keyMaps().addAll(convertASTKeyMaps(map.children().stream()
                    .filter(v -> v instanceof ModelKeyMapAST)
                    .map(v -> (ModelKeyMapAST) v)
                    .collect(Collectors.toList()))
            );

            maps.add(mapType);
        }

        return maps;
    }

}
