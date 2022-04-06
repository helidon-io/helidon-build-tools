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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParser;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.common.GenericType;

import static io.helidon.build.archetype.v2.json.JsonArrayCollector.toJsonArray;
import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;

/**
 * JSON-P utility.
 * {@link Json} always invoke {@link JsonProvider#provider}, this class provides access to a unique provider instance
 * in order to avoid the provider lookups.
 */
public final class JsonFactory {

    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private static final JsonBuilderFactory JSON_FACTORY = JSON_PROVIDER.createBuilderFactory(Collections.emptyMap());
    private static final JsonReaderFactory JSON_READER_FACTORY = JSON_PROVIDER.createReaderFactory(Map.of());
    private static final JsonWriterFactory JSON_WRITER_PRETTY_FACTORY =
            JSON_PROVIDER.createWriterFactory(Map.of(PRETTY_PRINTING, true));

    private JsonFactory() {
    }

    /**
     * Create a new parser.
     *
     * @param is input stream
     * @return JsonReader
     */
    public static JsonParser createParser(InputStream is) {
        return JSON_PROVIDER.createParser(is);
    }

    /**
     * Create a new {@link JsonObjectBuilder}.
     *
     * @return JsonObjectBuilder
     */
    public static JsonObjectBuilder createObjectBuilder() {
        return JSON_FACTORY.createObjectBuilder();
    }

    /**
     * Create a new {@link JsonArrayBuilder}.
     *
     * @return JsonArrayBuilder
     */
    public static JsonArrayBuilder createArrayBuilder() {
        return JSON_FACTORY.createArrayBuilder();
    }

    /**
     * Create a {@link JsonValue} from an integer.
     *
     * @param value integer value
     * @return JsonValue
     */
    public static JsonValue createValue(int value) {
        return JSON_PROVIDER.createValue(value);
    }

    /**
     * Get the {@link JsonValue} for a given boolean.
     *
     * @param value boolean value
     * @return JsonValue
     */
    public static JsonValue createValue(boolean value) {
        return value ? JsonValue.TRUE : JsonValue.FALSE;
    }

    /**
     * Create a {@link JsonValue} from a string.
     *
     * @param value string value
     * @return JsonValue
     */
    public static JsonValue createValue(String value) {
        return JSON_PROVIDER.createValue(value);
    }

    /**
     * Create a {@link JsonValue} from a list of string.
     *
     * @param value list of string values
     * @return JsonValue
     */
    public static JsonValue createValue(List<String> value) {
        return value.stream().collect(toJsonArray(JsonArrayBuilder::add));
    }

    /**
     * Create a {@link JsonValue} from a {@link Value}.
     *
     * @param value value
     * @return JsonValue
     */
    public static JsonValue createValue(Value value) {
        if (value.equals(Value.NULL)) {
            return JsonValue.NULL;
        }
        GenericType<?> type = value.type();
        if (type.equals(ValueTypes.STRING)) {
            return createValue(value.asString());
        } else if (type.equals(ValueTypes.BOOLEAN)) {
            return createValue(value.asBoolean());
        } else if (type.equals(ValueTypes.INT)) {
            return createValue(value.asInt());
        } else if (type.equals(ValueTypes.STRING_LIST)) {
            return createValue(value.asList());
        }
        throw new UnsupportedOperationException("Unsupported value type: " + type);
    }

    /**
     * Read a JSON value as a AST value.
     *
     * @param value JSON value
     * @return Value
     */
    public static Value readValue(JsonValue value) {
        JsonValue.ValueType type = value.getValueType();
        switch (type) {
            case NULL:
                return Value.NULL;
            case STRING:
                return Value.create(((JsonString) value).getString());
            case TRUE:
                return Value.TRUE;
            case FALSE:
                return Value.FALSE;
            case ARRAY:
                return Value.create(value.asJsonArray()
                                         .stream()
                                         .map(JsonFactory::readString)
                                         .collect(Collectors.toList()));
            default:
                throw new IllegalArgumentException("Unsupported value type: " + type);
        }
    }

    /**
     * Read a JSON value as a string.
     *
     * @param value JSON value
     * @return string
     */
    public static String readString(JsonValue value) {
        if (value == null) {
            return null;
        } else if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        throw new IllegalArgumentException("Value is not a JSON string: " + value);
    }

    /**
     * Read the given JSON file.
     *
     * @param file JSON file
     * @return JsonStructure
     * @throws IOException if an IO error occurs
     */
    public static JsonStructure readJson(Path file) throws IOException {
        return readJson(Files.newInputStream(file));
    }

    /**
     * Read the given JSON file.
     *
     * @param is input stream
     * @return JsonStructure
     */
    public static JsonStructure readJson(InputStream is) {
        JsonReader reader = JSON_READER_FACTORY.createReader(is);
        return reader.read();
    }

    /**
     * Diff the given {@link JsonStructure}.
     *
     * @param actual   actual
     * @param expected expected
     * @return JsonArray
     */
    public static JsonArray jsonDiff(JsonStructure actual, JsonStructure expected) {
        return Json.createDiff(actual, expected).toJsonArray();
    }

    /**
     * Convert the given {@link JsonValue} to a pretty string.
     *
     * @param jsonValue JSON value
     * @return String
     */
    @SuppressWarnings("unused")
    public static String toPrettyString(JsonValue jsonValue) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = JSON_WRITER_PRETTY_FACTORY.createWriter(stringWriter);
        jsonWriter.write(jsonValue);
        return stringWriter.toString();
    }
}
