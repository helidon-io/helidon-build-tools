/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
package io.helidon.lsp.server.service.metadata;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import io.helidon.build.common.FileUtils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MetadataTest {

    @Test
    public void testProcessMetadataJson() throws IOException {
        String resourcePath = "metadata/helidon-common-configurable.config-metadata.json";
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        MetadataProvider provider = MetadataProvider.instance();
        JsonReaderFactory readerFactory = Json.createReaderFactory(Map.of());
        JsonReader reader = readerFactory.createReader(Files.newBufferedReader(FileUtils.pathOf(resource)));
        List<ConfiguredType> configuredTypes = provider.processMetadataJson(reader.readArray());
        assertThat(configuredTypes.size(), is(4));
    }
}
