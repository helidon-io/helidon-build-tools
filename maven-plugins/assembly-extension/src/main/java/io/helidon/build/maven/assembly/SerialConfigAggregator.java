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
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SerialConfigAggregator implements AssemblyPluginAggregator {

    static final String SERIAL_CONFIG_PATH = "META-INF/helidon/serial-config.properties";
    private static final String REJECT_ALL_PATTERN = "!*";
    private final Set<String> parts = new LinkedHashSet<>();

    @Override
    public String path() {
        return SERIAL_CONFIG_PATH;
    }

    @Override
    public void aggregate(FileInfo fileInfo) throws IOException {
        Properties props = new Properties();
        try (InputStreamReader isr = new InputStreamReader(fileInfo.getContents(), UTF_8)) {
            props.load(isr);
        }
        String pattern = props.getProperty("pattern");
        parts.addAll(Stream.of(pattern.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList()));
    }

    @Override
    public File writeFile() throws ArchiverException {
        String simpleName = new File(SERIAL_CONFIG_PATH).getName();
        File serviceFile;
        moveRejectAllToEnd(parts);
        try {
            serviceFile = Files.createTempFile("helidon-assembly-" + simpleName, ".tmp").toFile();
            serviceFile.deleteOnExit();
            try (Writer fileWriter = new FileWriter(serviceFile, UTF_8)) {
                fileWriter.write("# Serial configuration aggregated by HelidonServiceTransformer during shading");
                fileWriter.write("\npattern=");
                fileWriter.write(String.join(";\\\n  ", parts));
                fileWriter.write("\n");
                fileWriter.flush();
            }
            return serviceFile;
        } catch (IOException e) {
            throw new ArchiverException("Could not write aggregated content for "
                    + SERIAL_CONFIG_PATH + ": " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean isEmpty() {
        return parts.isEmpty();
    }

    private static void moveRejectAllToEnd(Set<String> parts) {
        // When reject all pattern is present, it should be always at the end of aggregation
        if (parts.contains(REJECT_ALL_PATTERN)) {
            parts.remove(REJECT_ALL_PATTERN);
            parts.add(REJECT_ALL_PATTERN);
        }
    }

}
