/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.maven.shade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;

import static java.nio.charset.StandardCharsets.UTF_8;

class JsonArrayAggregator implements Aggregator {

    private final String path;
    private final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

    JsonArrayAggregator(String path) {
        this.path = path;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public boolean hasTransformedResource() {
        return !arrayBuilder.build().isEmpty();
    }

    @Override
    public void aggregate(InputStream is) {
        JsonArray array = Json.createReader(is).readArray();
        for (JsonValue value : array) {
            arrayBuilder.add(value);
        }
    }

    @Override
    public void writeToJar(JarOutputStream jos) throws IOException {
        JsonArray jsonArray = arrayBuilder.build();
        if (jsonArray.isEmpty()) {
            return;
        }
        JarEntry entry = new JarEntry(path);
        entry.setTime(Long.MIN_VALUE);
        jos.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(jos, UTF_8);
        Json.createWriter(writer).write(jsonArray);
        writer.flush();
    }
}
