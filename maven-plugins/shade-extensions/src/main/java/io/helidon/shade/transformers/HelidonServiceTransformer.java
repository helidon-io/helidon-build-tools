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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonValue;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;

/**
 * Maven Shade plugin custom transformer for merging Helidon service-registry files.
 * Usage:
 * <pre>{@code
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
 * }</pre>
 */
public class HelidonServiceTransformer implements ReproducibleResourceTransformer {

    private static final String PATH = "META-INF/helidon/service-registry.json";
    private final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
    private JsonArray result = null;

    @Override
    public boolean canTransformResource(String resource) {
        return resource.equals(PATH);
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
        processResource(resource, is, relocators, 0);
    }

    @Override
    public boolean hasTransformedResource() {
        return getResult().isEmpty();
    }

    @Override
    public void modifyOutputStream(JarOutputStream jarOutputStream) throws IOException {
        JarEntry jarEntry = new JarEntry(PATH);
        jarEntry.setTime(Long.MIN_VALUE);
        jarOutputStream.putNextEntry(jarEntry);
        Writer writer = new OutputStreamWriter(jarOutputStream, StandardCharsets.UTF_8);
        Json.createWriter(writer).write(getResult());
        writer.close();
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators, long time) throws IOException {
        JsonArray array = Json.createReader(is).readArray();
        for (JsonValue value : array) {
            jsonArrayBuilder.add(value);
        }
    }

    private JsonArray getResult() {
        if (result == null) {
            result = jsonArrayBuilder.build();
        }

        return result;
    }

}
