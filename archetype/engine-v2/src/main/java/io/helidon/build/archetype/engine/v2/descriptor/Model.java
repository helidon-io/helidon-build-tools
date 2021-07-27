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

/**
 * Archetype model.
 */
public class Model extends Conditional {

    private final LinkedList<ModelKeyValue> keyValues = new LinkedList<>();
    private final LinkedList<ModelKeyList> keyLists = new LinkedList<>();
    private final LinkedList<ModelKeyMap> keyMaps = new LinkedList<>();

    Model(String ifProperties) {
        super(ifProperties);
    }

    /**
     * Get the model values with key.
     *
     * @return values
     */
    public LinkedList<ModelKeyValue> keyValues() {
        return keyValues;
    }

    /**
     * Get the model lists with key.
     *
     * @return lists
     */
    public LinkedList<ModelKeyList> keyLists() {
        return keyLists;
    }

    /**
     * Get the model maps with key.
     *
     * @return maps
     */
    public LinkedList<ModelKeyMap> keyMaps() {
        return keyMaps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Model m = (Model) o;
        return keyValues.equals(m.keyValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), keyValues);
    }

    @Override
    public String toString() {
        return "Model{"
                + "keyValues=" + keyValues()
                + "keyLists=" + keyLists()
                + "keyMaps=" + keyMaps()
                + "if=" + ifProperties()
                + '}';
    }

}
