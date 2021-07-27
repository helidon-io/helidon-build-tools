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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.LinkedList;
import java.util.Objects;

public class MapType extends Conditional {

    private final LinkedList<ModelKeyValue> keyValues = new LinkedList<>();
    private final LinkedList<ModelKeyList> keyLists = new LinkedList<>();
    private final LinkedList<ModelKeyMap> keyMaps = new LinkedList<>();
    private int order = 100;

    MapType(int order, String ifProperties) {
        super(ifProperties);
        this.order = order;
    }

    public LinkedList<ModelKeyValue> keyValues() {
        return keyValues;
    }

    public LinkedList<ModelKeyList> keyLists() {
        return keyLists;
    }

    public LinkedList<ModelKeyMap> keyMaps() {
        return keyMaps;
    }

    public int order() {
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MapType m = (MapType) o;
        return keyValues.equals(m.keyValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), keyValues, keyLists, keyMaps);
    }

    @Override
    public String toString() {
        return "ListType{"
                + "values=" + keyValues()
                + "if=" + ifProperties()
                + '}';
    }
}
