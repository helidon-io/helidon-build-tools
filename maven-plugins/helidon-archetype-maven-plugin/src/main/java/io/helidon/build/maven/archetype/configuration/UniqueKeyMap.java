/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Map containing a unique key and accumulate values into a Set.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class UniqueKeyMap<K, V> extends HashMap<K, V> {

    private K key = null;
    private final Set<V> values = new HashSet<>();

    @Override
    public V put(K key, V value) {
        if (Objects.isNull(this.key)) {
            this.key = key;
        }
        if (!Objects.equals(key, this.key)) {
            throw new IllegalArgumentException(String.format("Unique key %s is allowed", this.key));
        }
        values.add(value);
        return value;
    }

    /**
     * Return a {@link Set} of values corresponding to the unique key.
     *
     * @return values
     */
    public Set<V> values() {
        return values;
    }
}
