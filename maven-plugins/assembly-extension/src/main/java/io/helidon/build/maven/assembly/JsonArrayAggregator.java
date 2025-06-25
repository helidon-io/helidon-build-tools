/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.assembly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class JsonArrayAggregator implements AssemblyPluginAggregator {

    private final List<JsonValue> jsonArray;

    private JsonArrayAggregator() {
        this.jsonArray = new ArrayList<>();
    }

    @Override
    public void aggregate(FileInfo fileInfo) throws IOException {
        JsonArray sourceArray = readJsonFile(fileInfo);
        jsonArray.addAll(sourceArray);
    }

    @Override
    public File writeFile() throws ArchiverException {
        String simpleName = new File(path()).getName();
        File jsonFile;
        try {
            jsonFile = Files.createTempFile("helidon-assembly-" + simpleName, ".tmp").toFile();
            jsonFile.deleteOnExit();
            writeJsonFile(jsonFile);
            return jsonFile;
        } catch (IOException e) {
            throw new ArchiverException("Could not write aggregated content for "
                    + path() + ": " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean isEmpty() {
        return jsonArray.isEmpty();
    }

    // Read JSON data from Helidon services JSON file
    private JsonArray readJsonFile(FileInfo fileInfo) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(fileInfo.getContents(), UTF_8);
            JsonReader reader = Json.createReader(isr)) {
            return reader.readArray();
        }
    }

    private void writeJsonFile(File jsonFile) throws IOException {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        jsonArray.forEach(arrayBuilder::add);
        try (Writer fileWriter = new FileWriter(jsonFile, UTF_8); JsonWriter writer = Json.createWriter(fileWriter)) {
            writer.write(arrayBuilder.build());
            fileWriter.flush();
        }
    }

    // Set source path to constant expressions to avoid PATH_TRAVERSAL_IN spotbugs issue

    static final class ServiceRegistryAggregator extends JsonArrayAggregator {

        @Override
        public String path() {
            return "META-INF/helidon/service-registry.json";
        }

    }

    static final class ConfigMetadataAggregator extends JsonArrayAggregator {

        @Override
        public String path() {
            return "META-INF/helidon/config-metadata.json";
        }

    }

    static final class FeatureMetadataAggregator extends JsonArrayAggregator {

        @Override
        public String path() {
            return "META-INF/helidon/feature-registry.json";
        }

    }

}
