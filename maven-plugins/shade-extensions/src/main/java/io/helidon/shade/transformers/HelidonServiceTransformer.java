/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.shade.transformers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonValue;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Maven Shade plugin custom transformer for merging Helidon service-registry files.
 * Usage:
 * <pre><![CDATA[{@code
 * <plugin>
 *    <groupId>org.apache.maven.plugins</groupId>
 *    <artifactId>maven-shade-plugin</artifactId>
 *    <version>3.5.1</version>
 *    <executions>
 *        <execution>
 *            <phase>package</phase>
 *            <goals>
 *                <goal>shade</goal>
 *            </goals>
 *            <configuration>
 *                <transformers>
 *                    <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
 *                    <transformer implementation="io.helidon.shade.transformers.HelidonServiceTransformer"/>
 *                </transformers>
 *            </configuration>
 *        </execution>
 *    </executions>
 *    <dependencies>
 *        <dependency>
 *            <groupId>io.helidon.build-tools</groupId>
 *            <artifactId>helidon-shade-extensions</artifactId>
 *            <version>4.0.0-SNAPSHOT</version>
 *        </dependency>
 *    </dependencies>
 * </plugin>
 * }]]</pre>
 */
public class HelidonServiceTransformer implements ReproducibleResourceTransformer {

    static final String SERVICE_REGISTRY_PATH = "META-INF/helidon/service-registry.json";
    static final String CONFIG_METADATA_PATH = "META-INF/helidon/config-metadata.json";
    static final String SERVICE_LOADER_PATH = "META-INF/helidon/service.loader";

    private final JsonArrayBuilder serviceRegistryArrayBuilder = Json.createArrayBuilder();
    private final JsonArrayBuilder configMetadataArrayBuilder = Json.createArrayBuilder();
    private final LinkedHashSet<String> serviceLoaderLines = new LinkedHashSet<>();
    private boolean hasTransformedResource;

    @Override
    public boolean canTransformResource(String resource) {
        return resource.equals(SERVICE_REGISTRY_PATH);
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
        processResource(resource, is, relocators, 0);
    }

    @Override
    public boolean hasTransformedResource() {
        return hasTransformedResource;
    }

    @Override
    public void modifyOutputStream(JarOutputStream jarOutputStream) throws IOException {
        writeJson(SERVICE_REGISTRY_PATH, serviceRegistryArrayBuilder.build(), jarOutputStream);
        writeJson(CONFIG_METADATA_PATH, configMetadataArrayBuilder.build(), jarOutputStream);
        writeLines(SERVICE_LOADER_PATH, serviceLoaderLines, jarOutputStream);
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators, long time) throws IOException {
        if (SERVICE_REGISTRY_PATH.equals(resource)) {
            JsonArray array = Json.createReader(is).readArray();
            for (JsonValue value : array) {
                serviceRegistryArrayBuilder.add(value);
            }
        }

        if (CONFIG_METADATA_PATH.equals(resource)) {
            JsonArray array = Json.createReader(is).readArray();
            for (JsonValue value : array) {
                configMetadataArrayBuilder.add(value);
            }
        }

        if (SERVICE_LOADER_PATH.equals(resource)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
                reader.lines().forEach(serviceLoaderLines::add);
            }
        }
    }

    private void writeLines(String path, HashSet<String> lines, JarOutputStream jos) throws IOException {
        if (lines.isEmpty()) {
            return;
        }
        hasTransformedResource = true;
        JarEntry entry = new JarEntry(path);
        entry.setTime(Long.MIN_VALUE);
        jos.putNextEntry(entry);
        try (Writer writer = new OutputStreamWriter(jos, UTF_8)) {
            for (String line : lines) {
                writer.append(line);
                writer.append('\n');
            }
        }
    }

    private void writeJson(String path, JsonArray jsonArray, JarOutputStream jos) throws IOException {
        if (jsonArray.isEmpty()) {
            return;
        }
        hasTransformedResource = true;
        JarEntry entry = new JarEntry(path);
        entry.setTime(Long.MIN_VALUE);
        jos.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(jos, UTF_8);
            Json.createWriter(writer).write(jsonArray);
        writer.flush();
    }
}
