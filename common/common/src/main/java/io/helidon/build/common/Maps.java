/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

/**
 * Map utilities.
 */
@SuppressWarnings("unused")
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
     * @param mapper mapping function that uses as an argument Map.Entry object from the input map
     * @param <K>    key type
     * @param <V>    original value type
     * @param <X>    mapped value type
     * @return new map
     */
    public static <K, V, X> Map<K, X> mapEntryValue(Map<K, V> map, Function<Map.Entry<K, V>, X> mapper) {
        return map.entrySet()
                .stream()
                .collect(toMap(Entry::getKey, mapper, (a, b) -> b, LinkedHashMap::new));
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
        return mapValue(map, filter, mapper, LinkedHashMap::new);
    }

    /**
     * Map the given map values.
     *
     * @param map     input map
     * @param filter  key predicate
     * @param mapper  mapping function
     * @param factory factory
     * @param <K>     key type
     * @param <V>     original value type
     * @param <X>     mapped value type
     * @return new map
     */
    public static <K, V, X> Map<K, X> mapValue(Map<K, V> map,
                                               BiPredicate<K, V> filter,
                                               Function<V, X> mapper,
                                               Supplier<Map<K, X>> factory) {
        return map.entrySet()
                .stream()
                .filter(e -> filter.test(e.getKey(), e.getValue()))
                .collect(toMap(Entry::getKey, e -> mapper.apply(e.getValue()), (a, b) -> b, factory));
    }

    /**
     * Filter the given map.
     *
     * @param map    input map
     * @param filter predicate
     * @param <K>    key type
     * @param <V>    value type
     * @return new map
     */
    public static <K, V> Map<K, V> filterKey(Map<K, V> map, Predicate<K> filter) {
        return filter(map, (k, v) -> filter.test(k), LinkedHashMap::new);
    }

    /**
     * Filter the given map.
     *
     * @param map    input map
     * @param filter predicate
     * @param <K>    key type
     * @param <V>    value type
     * @return new map
     */
    public static <K, V> Map<K, V> filterValue(Map<K, V> map, Predicate<V> filter) {
        return filter(map, (k, v) -> filter.test(v), LinkedHashMap::new);
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
        return filter(map, (k, v) -> !k.equals(key), LinkedHashMap::new);
    }

    /**
     * Filter the given map.
     *
     * @param map     input map
     * @param filter  entry predicate
     * @param factory factory
     * @param <K>     key type
     * @param <V>     value type
     * @return new map
     */
    public static <K, V> Map<K, V> filter(Map<K, V> map, BiPredicate<K, V> filter, Supplier<Map<K, V>> factory) {
        return map.entrySet()
                .stream()
                .filter(e -> filter.test(e.getKey(), e.getValue()))
                .collect(toMap(Entry::getKey, Entry::getValue, (a, b) -> b, factory));
    }

    /**
     * Put a value in a map and return the map.
     *
     * @param map input map
     * @param k   key
     * @param v   value
     * @param <K> key type
     * @param <V> value type
     * @return map
     */
    public static <K, V> Map<K, V> put(Map<K, V> map, K k, V v) {
        Map<K, V> res = new HashMap<>();
        if (map != null) {
            res.putAll(map);
        }
        res.put(k, v);
        return res;
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
     * Concatenate the given maps.
     *
     * @param maps input maps
     * @param <K>  key type
     * @param <V>  value type
     * @return new map
     */
    public static <K, V> Map<K, V> putAll(List<Map<K, V>> maps) {
        Map<K, V> map = new LinkedHashMap<>();
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
            return Lists.of(map1);
        }
        maps.forEach(m -> m.putAll(map1));
        return maps;
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
            return Lists.of(Maps.of(key, value));
        }
        maps.forEach(m -> m.putIfAbsent(key, value));
        return maps;
    }

    /**
     * Create a new {@link LinkedHashMap} with the given entry.
     *
     * @param key   key
     * @param value value
     * @param <K>   key type
     * @param <V>   value type
     * @return new map
     */
    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * Create a new {@link LinkedHashMap} with the given entries.
     *
     * @param k1  key1
     * @param v1  value1
     * @param k2  key2
     * @param v2  value2
     * @param <K> key type
     * @param <V> value type
     * @return new map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * Get the given map as an {@link ArrayList} of entries.
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
     * Get the given entries as a {@link LinkedHashMap}.
     *
     * @param entries     input entries
     * @param keyMapper   key mapper
     * @param valueMapper value mapper
     * @param <T>         collection type
     * @param <K>         key type
     * @param <V>         value type
     * @return map
     */
    public static <T, K, V> Map<K, V> from(Collection<T> entries, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return from(entries, keyMapper, valueMapper, (v1, v2) -> v2);
    }

    /**
     * Get the given entries as a {@link LinkedHashMap}.
     *
     * @param entries       input entries
     * @param keyMapper     key mapper
     * @param valueMapper   value mapper
     * @param mergeFunction merge function
     * @param <T>           collection type
     * @param <K>           key type
     * @param <V>           value type
     * @return map
     */
    public static <T, K, V> Map<K, V> from(Collection<T> entries,
                                           Function<T, K> keyMapper,
                                           Function<T, V> valueMapper,
                                           BinaryOperator<V> mergeFunction) {
        return entries.stream()
                .collect(toMap(keyMapper, valueMapper, mergeFunction, LinkedHashMap::new));
    }

    /**
     * Get the given entries as a {@link LinkedHashMap}.
     *
     * @param entries input entries
     * @param <K>     key type
     * @param <V>     value type
     * @return map
     */
    public static <K, V> Map<K, V> from(Collection<Entry<K, V>> entries) {
        return from(entries, (v1, v2) -> v2);
    }

    /**
     * Get the given entries as a {@link LinkedHashMap}.
     *
     * @param entries       input entries
     * @param mergeFunction merge function
     * @param <K>           key type
     * @param <V>           value type
     * @return map
     */
    public static <K, V> Map<K, V> from(Collection<Entry<K, V>> entries, BinaryOperator<V> mergeFunction) {
        return from(entries, Entry::getKey, Entry::getValue, mergeFunction);
    }

    /**
     * Merge the maps in the given list into a {@link LinkedHashMap}.
     *
     * @param maps          maps to merge
     * @param mergeFunction merge function
     * @param <K>           key type
     * @param <V>           value type
     * @return map
     */
    public static <K, V> Map<K, V> merge(Collection<Map<K, V>> maps, BinaryOperator<V> mergeFunction) {
        return from(Lists.flatMap(maps, Map::entrySet), mergeFunction);
    }

    /**
     * Merge the maps in the given list into a {@link LinkedHashMap}.
     *
     * @param maps maps to merge
     * @param <K>  key type
     * @param <V>  value type
     * @return map
     */
    public static <K, V> Map<K, V> merge(Collection<Map<K, V>> maps) {
        Map<K, V> merged = new LinkedHashMap<>();
        for (Map<K, V> map : maps) {
            merged.putAll(map);
        }
        return merged;
    }

    /**
     * Compute non-existing keys in a map.
     *
     * @param map      map to update
     * @param mappings map of keys to mapping functions
     * @param <K>      key type
     * @param <V>      value type
     * @return map
     */
    public static <K, V> Map<K, V> computeIfAbsent(Map<K, V> map, Map<K, Function<K, V>> mappings) {
        mappings.forEach(map::computeIfAbsent);
        return map;
    }

    /**
     * Map and filter the given map where values are lists into a {@link LinkedHashMap}.
     *
     * @param map         input map
     * @param keyFilter   filter for keys
     * @param keyMapper   mapping function for keys
     * @param valueMapper mapping function for values in lists that represent values
     * @param <T>         origin key type
     * @param <U>         origin value type
     * @param <K>         mapped key type
     * @param <V>         mapped value type
     * @return new map
     */
    public static <T, U, K, V> Map<K, List<V>> mapEntry(Map<T, List<U>> map,
                                                        Predicate<T> keyFilter,
                                                        Function<T, K> keyMapper,
                                                        Function<U, V> valueMapper) {

        return map.entrySet().stream()
                .filter(e -> keyFilter.test(e.getKey()))
                .map(e -> Map.entry(keyMapper.apply(e.getKey()), Lists.map(e.getValue(), valueMapper)))
                .collect(toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    /**
     * Get a value from the given map or throw an exception.
     *
     * @param map             map
     * @param key             key to look up in the map
     * @param exceptionMapper exception mapper
     * @param <K>             key type
     * @param <V>             value type
     * @param <E>             exception type
     * @return value
     * @throws E if a value is not found
     */
    public static <K, V, E extends Exception> V getOrThrow(Map<K, V> map,
                                                           K key,
                                                           Function<K, E> exceptionMapper) throws E {
        V v = map.get(key);
        if (v == null) {
            throw exceptionMapper.apply(key);
        }
        return v;
    }

    /**
     * Test map equality using a predicate.
     *
     * @param map1      map
     * @param map2      map
     * @param predicate predicate
     * @param <K>       key type
     * @param <V>       value type
     * @return {@code true} if equal, {@code false} otherwise
     */
    public static <K, V> boolean equals(Map<K, V> map1, Map<K, V> map2, BiPredicate<V, V> predicate) {
        try {
            if (map1 == map2) {
                return true;
            }
            if (map1.size() != map2.size()) {
                return false;
            }
            for (Map.Entry<K, V> e : map1.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(map1.get(key) == null && map2.containsKey(key))) {
                        return false;
                    }
                } else {
                    V v2 = map2.get(key);
                    if (v2 == null || !predicate.test(value, v2)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Compare two maps.
     *
     * @param map1 map1
     * @param map2 map2
     * @param <K>  key type
     * @param <V>  value type
     * @return {@code -1}, {@code 0}, or {@code 1} if {@code map1} is less than, equal to, or greater than {@code map2}.
     */
    public static <K, V> int compare(Map<K, V> map1, Map<K, V> map2) {
        return Lists.compare(map1.entrySet(), map2.entrySet(), Comparator.comparing(Map.Entry::toString));
    }
}
