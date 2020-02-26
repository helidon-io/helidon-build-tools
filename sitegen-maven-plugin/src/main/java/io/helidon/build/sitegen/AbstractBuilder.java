/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base builder class.
 * @param <T> the type built by the builder
 */
public abstract class AbstractBuilder<T> {

    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * Store an attribute.
     * @param key the attribute name
     * @param val the attribute value
     */
    protected void put(String key, Object val){
        attributes.put(key, val);
    }

    /**
     * Get all stored attributes.
     * @return a {@code Collection} of attributes as {@code Entry<String, Object>}
     */
    protected Collection<Entry<String, Object>> values(){
        return attributes.entrySet();
    }

    /**
     * Convert an object (a builder attribute) into the given type.
     * @param <T> the target type
     * @param object the object to convert
     * @param clazz the class representing the target type
     * @return the object as the given type
     * @throws IllegalStateException if a cast error occurs
     */
    @SuppressWarnings("unchecked")
    protected static <T> T asType(Object object, Class<T> clazz)
            throws IllegalStateException {

        try {
            return (T) object;
        } catch (ClassCastException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Convert an object (a builder attribute) into a {@code Map}.
     * @param <U> the target key type
     * @param <V> the target value type
     * @param object the object to convert
     * @param keyClass the class representing the target key type
     * @param valueClass the class representing the target value type
     * @return the object as the given {@code Map} type
     * @throws IllegalStateException if a cast error occurs
     */
    @SuppressWarnings("unchecked")
    protected static <U, V> Map<U, V> asMap(Object object,
                                          Class<U> keyClass,
                                          Class<V> valueClass)
            throws IllegalStateException {

        try {
            return (Map<U, V>) object;
        } catch (ClassCastException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Convert an object (a builder attribute) into a {@code List}.
     * @param <U> the target item type
     * @param object the object to convert
     * @param eltClass the class representing the target item type
     * @return the object as the given {@code List} type
     * @throws IllegalStateException if a cast error occurs
     */
    @SuppressWarnings("unchecked")
    protected static <U> List<U> asList(Object object, Class<U> eltClass)
            throws IllegalStateException {

        try {
            return (List<U>) object;
        } catch (ClassCastException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Build the object instance.
     * @return the created instance
     */
    public abstract T build();
}
