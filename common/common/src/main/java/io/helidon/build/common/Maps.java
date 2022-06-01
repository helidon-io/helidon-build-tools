/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Map utilities.
 */
public class Maps {

    private Maps() {
    }

    /**
     * Utility to convert a {@code Properties} into a {@code Map<String, String>}.
     *
     * @param properties The properties.
     * @return A map instance.
     */
    public static Map<String, String> fromProperties(Properties properties) {
        Objects.requireNonNull(properties);
        Map<String, String> map = new HashMap<>();
        properties.stringPropertyNames().forEach(k -> map.put(k, properties.getProperty(k)));
        return map;
    }

    /**
     * Utility to convert a {@code Map<String, String>} into a {@code Properties}.
     *
     * @param map The map.
     * @return Properties instance.
     */
    public static Properties toProperties(Map<String, String> map) {
        Objects.requireNonNull(map);
        Properties properties = new Properties();
        map.forEach(properties::setProperty);
        return properties;
    }

    /**
     * Utility to reverse a map.
     *
     * @param map map to reverse
     * @param <T> key type
     * @param <U> value type
     * @return reversed map
     */
    public static <T, U> Map<U, Set<T>> reverse(Map<T, U> map) {
        Map<U, Set<T>> reversed = new HashMap<>();
        for (Entry<T, U> entry : map.entrySet()) {
            reversed.computeIfAbsent(entry.getValue(), v -> new HashSet<>())
                    .add(entry.getKey());
        }
        return reversed;
    }

    /**
     * Sort the given map by values.
     *
     * @param map map
     * @param cmp comparator
     * @param <K> key type
     * @param <V> value type
     * @return sorted map
     */
    public static <K, V> Map<K, V> sortByValue(Map<K, V> map, Comparator<V> cmp) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue(cmp));
        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Map the given map values.
     *
     * @param map    input map
     * @param mapper mapping function
     * @param <K>    key type
     * @param <V>    original value type
     * @param <X>    mapped value type
     * @return new map
     */
    public static <K, V, X> Map<K, X> mapValue(Map<K, V> map, Function<V, X> mapper) {
        return mapValue(map, (k, v) -> true, mapper);
    }

    /**
     * Map the given map values.
     *
     * @param map    input map
     * @param filter key predicate
     * @param mapper mapping function
     * @param <K>    key type
     * @param <V>    original value type
     * @param <X>    mapped value type
     * @return new map
     */
    public static <K, V, X> Map<K, X> mapValue(Map<K, V> map, BiPredicate<K, V> filter, Function<V, X> mapper) {
        return map.entrySet()
                  .stream()
                  .filter(e -> filter.test(e.getKey(), e.getValue()))
                  .collect(toMap(Entry::getKey, e -> mapper.apply(e.getValue())));
    }

    /**
     * Filter the given map.
     *
     * @param map    input map
     * @param filter key predicate
     * @param <K>    key type
     * @param <V>    value type
     * @return new map
     */
    public static <K, V> Map<K, V> filter(Map<K, V> map, BiPredicate<K, V> filter) {
        return map.entrySet()
                  .stream()
                  .filter(e -> filter.test(e.getKey(), e.getValue()))
                  .collect(toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Filter the given map.
     *
     * @param map input map
     * @param key key to remove
     * @param <K> key type
     * @param <V> value type
     * @return new map
     */
    public static <K, V> Map<K, V> filter(Map<K, V> map, K key) {
        return filter(map, (BiPredicate<K, V>) (k, v) -> !k.equals(key));
    }

    /**
     * Convert the given list of maps into a map of map keyed by the given key.
     *
     * @param maps input maps
     * @param key  key
     * @param <K>  key type
     * @param <V>  value type
     * @return new map
     */
    public static <K, V> Map<V, List<Map<K, V>>> keyedBy(List<Map<K, V>> maps, K key) {
        return maps.stream().collect(toMap(m -> m.get(key), m -> List.of(filter(m, key)), Lists::addAll));
    }

    /**
     * Concatenate the given maps.
     *
     * @param map1 input map 1
     * @param map2 input map 2
     * @param <K>  key type
     * @param <V>  value type
     * @return new map
     */
    public static <K, V> Map<K, V> putAll(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> map = new HashMap<>();
        map.putAll(map1);
        map.putAll(map2);
        return map;
    }

    /**
     * Add the given entry if absent in the maps.
     *
     * @param map   input map
     * @param key   input key
     * @param value input value
     * @param <K>   key type
     * @param <V>   value type
     * @return new map
     */
    public static <K, V> Map<K, V> putIfAbsent(Map<K, V> map, K key, V value) {
        Map<K, V> copy = new HashMap<>(map);
        copy.putIfAbsent(key, value);
        return copy;
    }

    /**
     * Concatenate the given maps.
     *
     * @param maps input maps
     * @param <K>  key type
     * @param <V>  value type
     * @return new map
     */
    public static <K, V> Map<K, V> putAll(List<Map<K, V>> maps) {
        Map<K, V> map = new HashMap<>();
        maps.forEach(map::putAll);
        return map;
    }

    /**
     * Concatenate the given maps.
     *
     * @param maps input maps
     * @param map1 input map 1
     * @param <K>  key type
     * @param <V>  value type
     * @return new map
     */
    public static <K, V> List<Map<K, V>> putAll(List<Map<K, V>> maps, Map<K, V> map1) {
        if (maps.isEmpty()) {
            return List.of(map1);
        }
        return Lists.mapElement(maps, m -> putAll(m, map1));
    }

    /**
     * Add the given entry if absent in the maps.
     *
     * @param maps  input maps
     * @param key   input key
     * @param value input value
     * @param <K>   key type
     * @param <V>   value type
     * @return new map
     */
    public static <K, V> List<Map<K, V>> putIfAbsent(List<Map<K, V>> maps, K key, V value) {
        if (maps.isEmpty()) {
            return List.of(Map.of(key, value));
        }
        return Lists.mapElement(maps, m -> putIfAbsent(m, key, value));
    }

    /**
     * Get the given map as a list of entries.
     *
     * @param map input map
     * @param <K> key type
     * @param <V> value type
     * @return list of entries
     */
    public static <K, V> List<Entry<K, V>> entries(Map<K, V> map) {
        return new ArrayList<>(map.entrySet());
    }

    /**
     * Get the given entries as a map.
     *
     * @param entries input entries
     * @param <K>     key type
     * @param <V>     value type
     * @return map
     */
    public static <K, V> Map<K, V> fromEntries(Collection<Entry<K, V>> entries) {
        return fromEntries(entries.stream());
    }

    /**
     * Get the given entries as a map.
     *
     * @param entries input entries
     * @param <K>     key type
     * @param <V>     value type
     * @return map
     */
    public static <K, V> Map<K, V> fromEntries(Stream<Entry<K, V>> entries) {
        return entries.collect(toMap(Entry::getKey, Entry::getValue));
    }
}
