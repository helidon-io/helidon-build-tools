/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * URI utility.
 */
public final class URIs {

    private URIs() {
    }

    /**
     * Convert this URI to a path.
     *
     * @param uri URI to convert
     * @return path
     */
    public static Path toPath(URI uri) {
        switch (uri.getScheme()) {
            case "jar":
                FileSystem fs;
                try {
                    fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                } catch (FileSystemAlreadyExistsException ex) {
                    fs = FileSystems.getFileSystem(uri);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
                String relativePath = uri.getSchemeSpecificPart();
                int idx = relativePath.indexOf("!");
                if (idx > 0) {
                    relativePath = relativePath.substring(idx + 1);
                }
                return fs.getPath(relativePath);
            case "file":
                return Paths.get(uri);
            default:
                throw new IllegalStateException(uri.toASCIIString() + " expecting jar: or file:");
        }
    }
}
