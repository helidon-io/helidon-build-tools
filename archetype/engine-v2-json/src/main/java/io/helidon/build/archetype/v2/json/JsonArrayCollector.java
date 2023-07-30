/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.v2.json;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

/**
 * Stream collector to collect a {@link JsonArray}.
 *
 * @param <T> stream element type
 */
public final class JsonArrayCollector<T> implements Collector<T, JsonArrayBuilder, JsonArray> {

    private final BiConsumer<JsonArrayBuilder, T> accumulator;

    private JsonArrayCollector(BiConsumer<JsonArrayBuilder, T> accumulator) {
        this.accumulator = accumulator;
    }

    /**
     * Create a new {@link JsonArray} collector.
     *
     * @param accumulator accumulator
     * @param <T>         stream element type
     * @return JsonArrayCollector
     */
    public static <T> JsonArrayCollector<T> toJsonArray(BiConsumer<JsonArrayBuilder, T> accumulator) {
        return new JsonArrayCollector<>(accumulator);
    }

    @Override
    public Supplier<JsonArrayBuilder> supplier() {
        return JsonFactory::createArrayBuilder;
    }

    @Override
    public BiConsumer<JsonArrayBuilder, T> accumulator() {
        return accumulator;
    }

    @Override
    public BinaryOperator<JsonArrayBuilder> combiner() {
        return (a1, a2) -> a1;
    }

    @Override
    public Function<JsonArrayBuilder, JsonArray> finisher() {
        return JsonArrayBuilder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
