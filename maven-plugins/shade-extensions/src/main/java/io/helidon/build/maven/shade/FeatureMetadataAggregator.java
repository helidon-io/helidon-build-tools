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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Feature properties can't be effectively aggregated, instead single feature file is created.
 *
 * Example:
 * <pre>{@code
 * # Features aggregated by HelidonServiceTransformer during shading:
 * # - CDI
 * # - Config
 * # - Object Mapping
 * # - YAML
 * # - Observe
 * # - CORS
 * # - WebServer
 * # - Media
 * # - Encoding
 * # - Static Content
 * # - JSONP
 * # - Config
 * # - Encryption
 * # - WebClient
 * # - HTTP/1.1
 * n=Helidon Shaded
 * d=Helidon modules shaded in to the single module.
 * }</pre>
 */
class FeatureMetadataAggregator implements Aggregator {

    static final String FEATURE_METADATA_PATH = "META-INF/helidon/feature-metadata.properties";
    private final List<String> features = new LinkedList<>();

    @Override
    public String path() {
        return FEATURE_METADATA_PATH;
    }

    @Override
    public boolean hasTransformedResource() {
        return true;
    }

    @Override
    public void aggregate(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        Optional.ofNullable(properties.getProperty("n")).ifPresent(features::add);
    }

    @Override
    public void writeToJar(JarOutputStream jos) throws IOException {
        JarEntry entry = new JarEntry(path());
        entry.setTime(Long.MIN_VALUE);
        jos.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(jos, UTF_8);
        writer.write("# Features aggregated by HelidonServiceTransformer during shading:");
        writer.write("\n# - " + String.join("\n# - ", features));
        writer.write("\nn=Helidon Shaded");
        writer.write("\nd=Helidon modules shaded in to the single module.");
        writer.write("\n");
        writer.flush();
    }
}
