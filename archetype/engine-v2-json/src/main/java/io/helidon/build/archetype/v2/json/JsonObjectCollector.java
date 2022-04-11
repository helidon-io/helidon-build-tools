/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Stream collector to collect a {@link JsonObject}.
 *
 * @param <T> stream element type
 */
public final class JsonObjectCollector<T> implements Collector<T, JsonObjectBuilder, JsonObject> {

    private final BiConsumer<JsonObjectBuilder, T> accumulator;

    private JsonObjectCollector(BiConsumer<JsonObjectBuilder, T> accumulator) {
        this.accumulator = accumulator;
    }

    /**
     * Create a new {@link JsonObject} collector.
     *
     * @param accumulator accumulator
     * @param <T>         stream element type
     * @return JsonArrayCollector
     */
    public static <T> JsonObjectCollector<T> toJsonObject(BiConsumer<JsonObjectBuilder, T> accumulator) {
        return new JsonObjectCollector<>(accumulator);
    }

    @Override
    public Supplier<JsonObjectBuilder> supplier() {
        return JsonFactory::createObjectBuilder;
    }

    @Override
    public BiConsumer<JsonObjectBuilder, T> accumulator() {
        return accumulator;
    }

    @Override
    public BinaryOperator<JsonObjectBuilder> combiner() {
        return (a1, a2) -> a1;
    }

    @Override
    public Function<JsonObjectBuilder, JsonObject> finisher() {
        return JsonObjectBuilder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
