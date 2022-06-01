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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * List utilities.
 */
public class Lists {

    private Lists() {
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
    public static <T, V> List<T> mapElement(List<V> list, Function<V, T> function) {
        return list == null ? List.of() : list.stream().map(function).collect(Collectors.toList());
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
    public static <T, V> List<T> flatMapElement(List<V> list, Function<V, Stream<T>> function) {
        return list == null ? List.of() : list.stream().flatMap(function).collect(Collectors.toList());
    }

    /**
     * Concat the given lists.
     *
     * @param list1 list 1
     * @param list2 list 2
     * @param <T>   element type
     * @return new list
     */
    public static <T> List<T> addAll(List<T> list1, List<T> list2) {
        List<T> list = new LinkedList<>();
        list.addAll(list1);
        list.addAll(list2);
        return list;
    }
}
