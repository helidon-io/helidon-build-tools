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
package io.helidon.lsp.server.service.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;

/**
 * Provider for metadata.
 */
public class MetadataProvider {

    private static MetadataProvider instance = new MetadataProvider();

    private static final Logger LOGGER = Logger.getLogger(MetadataProvider.class.getName());
    private static final String HELIDON_PROPERTIES_FILE = "META-INF/helidon/config-metadata.json";

    private MetadataProvider() {
    }

    /**
     * Instance of the class.
     *
     * @return instance of the class.
     */
    public static MetadataProvider instance() {
        return instance;
    }

    /**
     * Read configuration metadata from the helidon jar file.
     *
     * @param jarFilePath path to the helidon jar file.
     * @return list of configured types.
     * @throws IOException IOException
     */
    public List<ConfiguredType> readMetadata(String jarFilePath) throws IOException {
        JarFile jarFile = new JarFile(jarFilePath);
        JarEntry configEntry = jarFile.getJarEntry(HELIDON_PROPERTIES_FILE);
        if (configEntry != null) {
            InputStream is = jarFile.getInputStream(configEntry);
            JsonReaderFactory readerFactory = Json.createReaderFactory(Map.of());
            JsonReader reader = readerFactory.createReader(is, StandardCharsets.UTF_8);
            return processMetadataJson(reader.readArray());
        }
        return List.of();
    }

    /**
     * Get configuration metadata from the json array.
     *
     * @param jsonArray json array.
     * @return list of configured types.
     */
    public List<ConfiguredType> processMetadataJson(JsonArray jsonArray) {
        List<ConfiguredType> result = new LinkedList<>();
        for (JsonValue jsonValue : jsonArray) {
            processTypeArray(result, jsonValue.asJsonObject().getJsonArray("types"));
        }
        return result;
    }

    private void processTypeArray(List<ConfiguredType> configuredTypes, JsonArray jsonArray) {
        if (jsonArray == null) {
            return;
        }
        for (JsonValue jsonValue : jsonArray) {
            JsonObject type = jsonValue.asJsonObject();
            ConfiguredType configuredType = ConfiguredType.create(type);
            configuredTypes.add(configuredType);
        }
    }
}
