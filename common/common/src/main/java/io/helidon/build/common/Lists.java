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
package io.helidon.build.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * List utilities.
 */
@SuppressWarnings("unused")
public class Lists {

    private Lists() {
    }

    /**
     * Filter the elements of the given list.
     *
     * @param list      input list
     * @param predicate predicate function
     * @param <T>       output element type
     * @return new list
     */
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        return list == null ? List.of() : list.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Filter the elements of the given list.
     *
     * @param list  input list
     * @param clazz type predicate
     * @param <T>   input element type
     * @param <V>   output element type
     * @return new list
     */
    public static <T, V> List<V> filter(Collection<T> list, Class<V> clazz) {
        return list == null ? List.of() : list.stream()
                                              .filter(clazz::isInstance)
                                              .map(clazz::cast)
                                              .collect(Collectors.toList());
    }

    /**
     * Map the elements of the given list.
     *
     * @param list     input list
     * @param function mapping function
     * @param <T>      output element type
     * @param <V>      input element type
     * @return new list
     */
    public static <T, V> List<T> map(Collection<V> list, Function<V, T> function) {
        return list == null ? List.of() : list.stream().map(function).collect(Collectors.toList());
    }

    /**
     * Map the elements of the given list.
     *
     * @param stream   input stream
     * @param function mapping function
     * @param <T>      output element type
     * @param <V>      input element type
     * @return new list
     */
    public static <T, V> List<T> map(Stream<V> stream, Function<V, T> function) {
        return stream == null ? List.of() : stream.map(function).collect(Collectors.toList());
    }

    /**
     * Flat-map the elements of the given list.
     *
     * @param list     input list
     * @param function mapping function
     * @param <T>      output element type
     * @param <V>      input element type
     * @return new list
     */
    public static <T, V> List<T> flatMapStream(Collection<V> list, Function<V, Stream<T>> function) {
        return list == null ? List.of() : list.stream().flatMap(function).collect(Collectors.toList());
    }

    /**
     * Flat-map the elements of the given list.
     *
     * @param list     input list
     * @param function mapping function
     * @param <T>      output element type
     * @param <V>      input element type
     * @return new list
     */
    public static <T, V> List<T> flatMap(Collection<V> list, Function<V, Collection<T>> function) {
        return flatMapStream(list, e -> function.apply(e).stream());
    }

    /**
     * Flat-map the elements of the given list.
     *
     * @param list input list
     * @param <T>  element type
     * @return new list
     */
    public static <T> List<T> flatMap(Collection<? extends Collection<T>> list) {
        return flatMapStream(list, Collection::stream);
    }

    /**
     * Concat the given lists.
     *
     * @param list1 list 1
     * @param list2 list 2
     * @param <T>   element type
     * @return new list
     */
    public static <T> List<T> addAll(Collection<T> list1, Collection<T> list2) {
        List<T> list = new LinkedList<>();
        if (list1 != null) {
            list.addAll(list1);
        }
        if (list2 != null) {
            list.addAll(list2);
        }
        return list;
    }

    /**
     * Concat the given lists.
     *
     * @param list1    list 1
     * @param elements elements elements
     * @param <T>      element type
     * @return new list
     */
    @SafeVarargs
    public static <T> List<T> addAll(Collection<T> list1, T... elements) {
        List<T> list = new LinkedList<>();
        if (list1 != null) {
            list.addAll(list1);
        }
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Create a new {@link ArrayList} with the given elements.
     *
     * @param elements input elements
     * @param <T>      element type
     * @return new list
     */
    @SafeVarargs
    public static <T> List<T> of(T... elements) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Map and join the given list.
     *
     * @param list      input list
     * @param function  mapping function
     * @param delimiter delimiter
     * @param <T>       element type
     * @return string
     */
    public static <T> String join(Collection<T> list, Function<T, String> function, String delimiter) {
        return list.stream().map(function).collect(Collectors.joining(delimiter));
    }

    /**
     * Separate the given list into groups.
     *
     * @param list     input list
     * @param function grouping function
     * @param <T>      list element type
     * @param <U>      key type
     * @return map of groups
     */
    public static <T, U> List<List<T>> groupingBy(Collection<T> list, Function<T, U> function) {
        return new ArrayList<>(list.stream().collect(Collectors.groupingBy(function)).values());
    }
}
