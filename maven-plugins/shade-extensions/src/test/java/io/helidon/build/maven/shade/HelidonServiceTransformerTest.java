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

package io.helidon.build.maven.shade;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import org.junit.jupiter.api.Test;

import static io.helidon.build.maven.shade.HelidonServiceTransformer.CONFIG_METADATA_PATH;
import static io.helidon.build.maven.shade.HelidonServiceTransformer.SERVICE_REGISTRY_PATH;
import static io.helidon.build.maven.shade.SerialConfigAggregator.SERIAL_CONFIG_PATH;
import static io.helidon.build.maven.shade.ServiceLoaderAggregator.SERVICE_LOADER_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HelidonServiceTransformerTest {

    static JsonWriterFactory JSON_WRITER = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, false));

    @Test
    void testAll() throws IOException {
        String jarName = "target/output.jar";
        HelidonServiceTransformer transformer = new HelidonServiceTransformer();
        transformer.processResource(SERVICE_REGISTRY_PATH,
                                    ClassLoader.getSystemResourceAsStream("service-registry-1.json"),
                                    null);
        transformer.processResource(SERVICE_REGISTRY_PATH,
                                    ClassLoader.getSystemResourceAsStream("service-registry-2.json"),
                                    null);
        transformer.processResource(CONFIG_METADATA_PATH,
                                    ClassLoader.getSystemResourceAsStream("config-metadata-1.json"),
                                    null);
        transformer.processResource(CONFIG_METADATA_PATH,
                                    ClassLoader.getSystemResourceAsStream("config-metadata-2.json"),
                                    null);
        transformer.processResource(SERVICE_LOADER_PATH,
                                    ClassLoader.getSystemResourceAsStream("service-1.loader"),
                                    null);
        transformer.processResource(SERVICE_LOADER_PATH,
                                    ClassLoader.getSystemResourceAsStream("service-2.loader"),
                                    null);
        transformer.processResource(SERIAL_CONFIG_PATH,
                                    ClassLoader.getSystemResourceAsStream("serial-config-1.properties"),
                                    null);
        transformer.processResource(SERIAL_CONFIG_PATH,
                                    ClassLoader.getSystemResourceAsStream("serial-config-2.properties"),
                                    null);
        transformer.processResource(SERIAL_CONFIG_PATH,
                                    ClassLoader.getSystemResourceAsStream("serial-config-3.properties"),
                                    null);

        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarName));
        transformer.modifyOutputStream(jarOutputStream);
        jarOutputStream.close();

        // Resulting service-registry.json
        JsonArray svcRegArray = Json.createReader(new StringReader(readFromJar(jarName, SERVICE_REGISTRY_PATH))).readArray();
        JsonObject svcRegObj1 = svcRegArray.getJsonObject(0);
        JsonObject svcRegObj2 = svcRegArray.getJsonObject(1);

        assertEqualJson(Json.createArrayBuilder().add(svcRegObj1).build(), "service-registry-1.json");
        assertEqualJson(Json.createArrayBuilder().add(svcRegObj2).build(), "service-registry-2.json");

        // Resulting config-metadata.json
        JsonArray cfgMetaArray = Json.createReader(new StringReader(readFromJar(jarName, CONFIG_METADATA_PATH))).readArray();
        JsonObject cfgMetaObj1 = cfgMetaArray.getJsonObject(0);
        JsonObject cfgMetaObj2 = cfgMetaArray.getJsonObject(1);

        assertEqualJson(Json.createArrayBuilder().add(cfgMetaObj1).build(), "config-metadata-1.json");
        assertEqualJson(Json.createArrayBuilder().add(cfgMetaObj2).build(), "config-metadata-2.json");

        // Resulting service.loader
        String expectedsl = new String(ClassLoader.getSystemResourceAsStream("service-expected.loader").readAllBytes());
        String actualsl = readFromJar(jarName, SERVICE_LOADER_PATH);
        assertEquals(expectedsl, actualsl);

        // Resulting serial-config.properties
        String expectedsc = new String(ClassLoader.getSystemResourceAsStream("serial-config-expected.properties").readAllBytes());
        String actualsc = readFromJar(jarName, SERIAL_CONFIG_PATH);
        assertEquals(expectedsc, actualsc);
    }

    @Test
    void testSingles() throws IOException {
        String jarName = "target/output-singles.jar";
        HelidonServiceTransformer transformer = new HelidonServiceTransformer();
        transformer.processResource(SERVICE_REGISTRY_PATH,
                                    ClassLoader.getSystemResourceAsStream("service-registry-1.json"),
                                    null);
        transformer.processResource(CONFIG_METADATA_PATH,
                                    ClassLoader.getSystemResourceAsStream("config-metadata-1.json"),
                                    null);
        transformer.processResource(SERVICE_LOADER_PATH,
                                    ClassLoader.getSystemResourceAsStream("service-1.loader"),
                                    null);

        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarName));
        transformer.modifyOutputStream(jarOutputStream);
        jarOutputStream.close();

        // Resulting service-registry.json
        JsonArray svcRegArray = Json.createReader(new StringReader(readFromJar(jarName, SERVICE_REGISTRY_PATH))).readArray();

        assertEqualJson(svcRegArray, "service-registry-1.json");

        // Resulting config-metadata.json
        JsonArray cfgMetaArray = Json.createReader(new StringReader(readFromJar(jarName, CONFIG_METADATA_PATH))).readArray();

        assertEqualJson(cfgMetaArray, "config-metadata-1.json");

        // Resulting service.loader
        String expected = new String(ClassLoader.getSystemResourceAsStream("service-1.loader").readAllBytes());
        String actual = readFromJar(jarName, SERVICE_LOADER_PATH);
        assertEquals(expected, actual);
    }

    private void assertEqualJson(JsonArray actual, String expectedResourceName) throws IOException {
        String expected = new String(ClassLoader.getSystemResourceAsStream(expectedResourceName).readAllBytes());
        StringWriter actStringWriter = new StringWriter();
        StringWriter expStringWriter = new StringWriter();
        JSON_WRITER.createWriter(actStringWriter).writeArray(actual);
        JSON_WRITER.createWriter(expStringWriter).writeArray(Json.createReader(new StringReader(expected)).readArray());
        assertEquals(expStringWriter.toString(), actStringWriter.toString());
    }

    private String readFromJar(String jarPath, String fileName) throws IOException {
        JarInputStream jis = new JarInputStream(new FileInputStream(jarPath));
        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            if (fileName.equals(entry.getName())) {
                break;
            }
        }
        assertNotNull(entry);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while (-1 != (n = jis.read(buffer))) {
            baos.write(buffer, 0, n);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
