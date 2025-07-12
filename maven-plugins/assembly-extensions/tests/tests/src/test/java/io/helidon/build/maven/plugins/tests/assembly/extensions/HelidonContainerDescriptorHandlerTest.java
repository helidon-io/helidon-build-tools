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
package io.helidon.build.maven.plugins.tests.assembly.extensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelidonContainerDescriptorHandlerTest {

    private static final String ARCHIVE1_URL_PREFIX = archive1UrlPrefix();
    private static final String ARCHIVE2_URL_PREFIX = archive2UrlPrefix();
    private static final String BUNDLE_URL_PREFIX = bundleUrlPrefix();
    private static final String REJECT_ALL_PATTERN = "!*";

    @BeforeAll
    static void setup() {

    }

    @Test
    void testServiceRegistry() throws IOException {
        validateJsonAggregate("/META-INF/helidon/service-registry.json");
    }

    @Test
    void testConfigMetadata() throws IOException {
        validateJsonAggregate("/META-INF/helidon/config-metadata.json");
    }

    @Test
    void testFeatureRegistry() throws IOException {
        validateJsonAggregate("/META-INF/helidon/feature-registry.json");
    }

    @Test
    void testServiceLoader() throws IOException {
        LinkedHashSet<String> sourceLines = new LinkedHashSet<>();
        LinkedHashSet<String> bundleLines = new LinkedHashSet<>();
        try (InputStream is = archiveIS(ARCHIVE1_URL_PREFIX, "/META-INF/helidon/service.loader");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            reader.lines()
                    .filter(s -> !s.startsWith("#"))
                    .forEach(sourceLines::add);
        }
        try (InputStream is = archiveIS(ARCHIVE2_URL_PREFIX, "/META-INF/helidon/service.loader");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            reader.lines()
                    .filter(s -> !s.startsWith("#"))
                    .forEach(sourceLines::add);
        }
        try (InputStream is = archiveIS(BUNDLE_URL_PREFIX, "/META-INF/helidon/service.loader");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            reader.lines()
                    .filter(s -> !s.startsWith("#"))
                    .forEach(bundleLines::add);
        }
        // Validate sizes
        assertEquals(bundleLines.size(), sourceLines.size(), "Bundle and sources array sizes do not match for " + "/META-INF/helidon/service.loader");
        for (String sourceLine : sourceLines) {
            if (!sourceLine.startsWith("#")) {
                System.out.println("LINE: " + sourceLine);
                assertTrue(bundleLines.contains(sourceLine), "Line \"" + sourceLine + "\" was not found in bundle");
            }
        }
    }

    @Test
    void testSerialConfig() throws IOException {
        // Build aggregated content from source files
        Set<String> parts = new LinkedHashSet<>();
        addSerialConfigPatterns(parts, readSerialConfigPattern(ARCHIVE1_URL_PREFIX));
        addSerialConfigPatterns(parts, readSerialConfigPattern(ARCHIVE2_URL_PREFIX));
        moveRejectAllToEnd(parts);
        // Read aggregated content from target file
        Set<String> target = new LinkedHashSet<>();
        addSerialConfigPatterns(target, readSerialConfigPattern(BUNDLE_URL_PREFIX));
        // Verify source and target aggregated content
        assertEquals(parts.size(),
                     target.size(),
                     "Source and target parts size differs: source = " + parts.size() + " target = " + target.size());
        Iterator<String> partsIterator = parts.iterator();
        Iterator<String> targetIterator = target.iterator();
        while (partsIterator.hasNext() && targetIterator.hasNext()) {
            String partsItem = partsIterator.next();
            String targetItem = targetIterator.next();
            assertEquals(partsItem, targetItem);
        }
    }

    private static void validateJsonAggregate(String file) throws IOException {
        // Read JsonArray content from JAR archives
        JsonArray archive1;
        JsonArray archive2;
        JsonArray bundle;
        try (InputStream is = archiveIS(ARCHIVE1_URL_PREFIX, file)) {
            archive1 = readJsonArray(is);
        }
        try (InputStream is = archiveIS(ARCHIVE2_URL_PREFIX, file)) {
            archive2 = readJsonArray(is);
        }
        try (InputStream is = archiveIS(BUNDLE_URL_PREFIX, file)) {
            bundle = readJsonArray(is);
        }
        // Validate sizes
        assertEquals(bundle.size(), archive1.size() + archive2.size(), "Bundle and sources array sizes do not match for " + file);
        int bundleIndex = 0;
        // Search for 1st archive items
        for (JsonValue value : archive1) {
            JsonValue bundleValue = bundle.get(bundleIndex);
            assertEquals(value, bundleValue);
            bundleIndex++;
        }
        // Search for 2nd archive items
        for (JsonValue value : archive2) {
            JsonValue bundleValue = bundle.get(bundleIndex);
            assertEquals(value, bundleValue);
            bundleIndex++;
        }
    }

    private static JsonArray readJsonArray(InputStream is) {
        return Json.createReader(is).readArray();
    }

    private static String readSerialConfigPattern(String prefix) throws IOException {
        Properties props = new Properties();
        try (InputStream is = archiveIS(prefix, "/META-INF/helidon/serial-config.properties")) {
            props.load(is);
        }
        String pattern = props.getProperty("pattern");
        assertNotNull(pattern);
        return pattern;
    }

    private static void addSerialConfigPatterns(Set<String> parts, String pattern) {
        parts.addAll(Stream.of(pattern.split(";"))
                             .map(String::trim)
                             .filter(s -> !s.isEmpty())
                             .collect(Collectors.toList()));
    }

    private static void moveRejectAllToEnd(Set<String> parts) {
        if (parts.contains(REJECT_ALL_PATTERN)) {
            parts.remove(REJECT_ALL_PATTERN);
            parts.add(REJECT_ALL_PATTERN);
        }
    }

    private static InputStream archiveIS(String prefix, String path) throws IOException {
        URL url = new URL(prefix + path);
        return url.openStream();
    }

    private static String archive1UrlPrefix() {
        String path = System.getProperty("archive-1");
        return "jar:file:" + path + "!";
    }

    private static String archive2UrlPrefix() {
        String path = System.getProperty("archive-2");
        return "jar:file:" + path + "!";
    }

    private static String bundleUrlPrefix() {
        String path = System.getProperty("bundle");
        return "jar:file:" + path + "!";
    }

}
