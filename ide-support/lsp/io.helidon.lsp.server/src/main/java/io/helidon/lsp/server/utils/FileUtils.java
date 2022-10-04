/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility class to work with files.
 */
public class FileUtils {

    private static FileUtils INSTANCE = new FileUtils();

    private FileUtils() {
    }

    public static FileUtils instance() {
        return INSTANCE;
    }

    /**
     * Get the text file content as a list of strings by its URI.
     *
     * @param fileUri file URI
     * @return content of the file
     * @throws IOException        IOException
     * @throws URISyntaxException URISyntaxException
     */
    public List<String> getTextDocContentByURI(String fileUri) throws IOException, URISyntaxException {
        Path path = Paths.get(new URI(fileUri).getPath());
        return Files.readAllLines(path);
    }
}
