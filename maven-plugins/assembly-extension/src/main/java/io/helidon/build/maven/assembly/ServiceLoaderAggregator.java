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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.LinkedHashSet;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServiceLoaderAggregator implements AssemblyPluginAggregator {

    private static final String SERVICE_LOADER_PATH = "META-INF/helidon/service.loader";
    private final LinkedHashSet<String> lines = new LinkedHashSet<>();


    @Override
    public String path() {
        return SERVICE_LOADER_PATH;
    }

    @Override
    public void aggregate(FileInfo fileInfo) throws IOException {
        try (InputStream is = fileInfo.getContents();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
                 reader.lines().forEach(lines::add);
        }
    }

    @Override
    public File writeFile() throws ArchiverException {
        String simpleName = new File(SERVICE_LOADER_PATH).getName();
        File serviceFile;
        try {
            serviceFile = Files.createTempFile("helidon-assembly-" + simpleName, ".tmp").toFile();
            serviceFile.deleteOnExit();
            try (Writer fileWriter = new FileWriter(serviceFile, UTF_8)) {
                for (String line : lines) {
                    fileWriter.append(line);
                    fileWriter.append('\n');
                }
                fileWriter.flush();
            }
            return serviceFile;
        } catch (IOException e) {
            throw new ArchiverException("Could not write aggregated content for "
                    + SERVICE_LOADER_PATH + ": " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean isEmpty() {
        return lines.isEmpty();
    }

}
