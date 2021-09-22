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

package io.helidon.build.archetype.maven.url.handler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Directory/File Visitor.
 */
public class Visitor {

    private File currentFile;
    private File[] files;

    /**
     * Constructor to set the root directory/file.
     *
     * @param directory root
     */
    Visitor(File directory) {
        this.currentFile = directory;
        files = resolveFiles(directory);
    }

    /**
     * Visit the target file or directory.
     *
     * @param targets       path to target file to be resolved
     * @return              target file
     * @throws IOException  if wrong path
     */
    public File visit(String[] targets) throws IOException {
        for (String directory : targets) {
            visit(directory);
        }
        return currentFile;
    }

    private void visit(String target) throws IOException {
        AtomicReference<File> resolved = new AtomicReference<>();
        Arrays.stream(files).forEach(file -> {
            if (file.getName().equals(target)) {
                resolved.set(file);
            }
        });
        if (resolved.get() == null) {
            throw new IOException("File or directory not found : " + target);
        }
        currentFile = resolved.get();
        files = resolveFiles(currentFile);
    }

    private File[] resolveFiles(File directory) {
        return directory.listFiles();
    }

}
