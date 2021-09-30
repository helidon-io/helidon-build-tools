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

import java.util.Objects;

/**
 * Archetype value with key attribute used in {@link Model} and {@link MapType}.
 */
public class ModelKeyValue extends ValueType {

    private final String key;

    /**
     * ModelKeyValue constructor.
     *
     * @param key           key attribute
     * @param url           url attribute
     * @param file          file attribute
     * @param template      template attribute
     * @param order         order attribute
     * @param ifProperties  if attribute
     */
    public ModelKeyValue(String key,
                         String url,
                         String file,
                         String template,
                         int order,
                         String ifProperties) {
        super(url, file, template, order, ifProperties);
        this.key = key;
    }

    /**
     * Get the key of the map.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ModelKeyValue m = (ModelKeyValue) o;
        return key.equals(m.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), key);
    }

    @Override
    public String toString() {
        return "ModelKeyValue{"
                + ", value=" + value()
                + ", url=" + url()
                + ", file=" + file()
                + ", template=" + template()
                + ", order=" + order()
                + '}';
    }
}
