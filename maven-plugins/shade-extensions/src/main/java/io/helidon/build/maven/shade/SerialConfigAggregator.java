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
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

class SerialConfigAggregator implements Aggregator {

    static final String SERIAL_CONFIG_PATH = "META-INF/helidon/serial-config.properties";
    private static final String REJECT_ALL_PATTERN = "!*";
    private final Set<String> parts = new LinkedHashSet<>();

    @Override
    public String path() {
        return SERIAL_CONFIG_PATH;
    }

    @Override
    public boolean hasTransformedResource() {
        return !parts.isEmpty();
    }

    @Override
    public void aggregate(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);
        String pattern = props.getProperty("pattern");
        parts.addAll(Stream.of(pattern.split(";"))
                             .map(String::trim)
                             .filter(s -> !s.isEmpty())
                             .collect(Collectors.toList()));
    }

    @Override
    public void writeToJar(JarOutputStream jos) throws IOException {
        if (parts.isEmpty()) {
            return;
        }

        moveRejectAllToEnd();

        JarEntry entry = new JarEntry(path());
        entry.setTime(Long.MIN_VALUE);
        jos.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(jos, UTF_8);
        writer.write("# Serial configuration aggregated by HelidonServiceTransformer during shading");
        writer.write("\npattern=");
        writer.write(String.join(";\\\n  ", parts));
        writer.write("\n");
        writer.flush();
    }

    private void moveRejectAllToEnd() {
        // When reject all pattern is present, it should be always at the end of aggregation
        if (parts.contains(REJECT_ALL_PATTERN)) {
            parts.remove(REJECT_ALL_PATTERN);
            parts.add(REJECT_ALL_PATTERN);
        }
    }
}
