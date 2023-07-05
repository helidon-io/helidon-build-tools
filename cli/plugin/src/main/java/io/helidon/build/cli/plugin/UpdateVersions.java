/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.cli.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A plugin that fetches the latest metadata and updates the cache.
 */
class UpdateVersions extends UpdateBase {
    private static final String VERSIONS_FILE_NAME = "versions.xml";

    /**
     * Constructor.
     */
    UpdateVersions() {
        super();
    }

    @Override
    protected void doExecute() throws IOException {
        URL url = resolve(VERSIONS_FILE_NAME);
        Map<String, String> headers = headers();
        URLConnection connection = httpGet(url, headers);
        Path versionsFile = versionsFile();
        Files.copy(connection.getInputStream(), versionsFile, REPLACE_EXISTING);
        if (Log.isDebug()) {
            Log.debug("wrote %s to %s", VERSIONS_FILE_NAME, versionsFile);
        }
    }
}
