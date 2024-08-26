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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

class ServiceLoaderAggregator implements Aggregator {

    static final String SERVICE_LOADER_PATH = "META-INF/helidon/service.loader";
    private final LinkedHashSet<String> lines = new LinkedHashSet<>();

    @Override
    public String path() {
        return SERVICE_LOADER_PATH;
    }

    @Override
    public boolean hasTransformedResource() {
        return !lines.isEmpty();
    }

    @Override
    public void aggregate(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            reader.lines().forEach(lines::add);
        }
    }

    @Override
    public void writeToJar(JarOutputStream jos) throws IOException {
        if (lines.isEmpty()) {
            return;
        }
        JarEntry entry = new JarEntry(path());
        entry.setTime(Long.MIN_VALUE);
        jos.putNextEntry(entry);
        Writer writer = new OutputStreamWriter(jos, UTF_8);
        for (String line : lines) {
            writer.append(line);
            writer.append('\n');
        }
        writer.flush();
    }
}
