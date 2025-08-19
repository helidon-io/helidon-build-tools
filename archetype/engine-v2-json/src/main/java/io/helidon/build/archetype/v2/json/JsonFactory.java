/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.io.Writer;
import java.util.Map;

import javax.json.Json;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;

/**
 * JSON-P utility.
 * {@link Json} always invoke {@link JsonProvider#provider}, this class provides access to a unique provider instance
 * in order to avoid the provider lookups.
 */
public final class JsonFactory {

    static final Map<String, ?> PRETTY_OPTIONS = Map.of(PRETTY_PRINTING, true);
    static final JsonProvider PROVIDER = JsonProvider.provider();
    static final JsonGeneratorFactory GENERATOR_FACTORY = PROVIDER.createGeneratorFactory(Map.of());
    static final JsonGeneratorFactory GENERATOR_PRETTY_FACTORY = PROVIDER.createGeneratorFactory(PRETTY_OPTIONS);

    private JsonFactory() {
    }

    /**
     * Create a JSON generator.
     *
     * @param writer writer
     * @param pretty pretty
     * @return JsonGenerator
     */
    public static JsonGenerator createGenerator(Writer writer, boolean pretty) {
        return pretty ? GENERATOR_PRETTY_FACTORY.createGenerator(writer) : GENERATOR_FACTORY.createGenerator(writer);
    }
}
