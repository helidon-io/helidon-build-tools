/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.dev.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import io.helidon.build.dev.BuildComponent;
import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.DirectoryType;
import io.helidon.build.util.ConsolePrinter;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A build step that copies resources.
 */
public class CopyResources implements BuildStep {

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 ConsolePrinter stdOut,
                                 ConsolePrinter stdErr)
            throws Exception {

        if (!changes.isEmpty()) {
            final BuildRoot sources = changes.root();
            if (sources.buildType().directoryType() == DirectoryType.Resources) {
                final BuildComponent component = sources.component();
                final Path srcDir = sources.path();
                final Path outDir = component.outputRoot().path();

                // Copy any changes

                final Set<Path> changed = changes.addedOrModified();
                if (!changed.isEmpty()) {
                    stdOut.println("Copying " + changed.size() + " resource files");
                    stdOut.flush();
                    for (final Path srcFile : changed) {
                        final Path outFile = toOutputFile(srcDir, srcFile, outDir);
                        copy(srcFile, outFile, stdOut);
                    }
                }

                // Remove any removed files

                final Set<Path> removed = changes.removed();
                if (!removed.isEmpty()) {
                    for (final Path srcFile : removed) {
                        final Path outFile = toOutputFile(srcDir, srcFile, outDir);
                        remove(outFile, stdOut);
                    }
                }
            }
        }
    }

    private void copy(Path srcFile, Path outFile, ConsolePrinter stdOut) throws IOException {
        stdOut.println("Copying resource " + srcFile);
        stdOut.flush();
        Files.copy(srcFile, outFile, REPLACE_EXISTING);
    }

    private void remove(Path outFile, ConsolePrinter stdOut) throws IOException {
        if (Files.exists(outFile)) {
            stdOut.println("Removing resource " + outFile);
            stdOut.flush();
            Files.delete(outFile);
        }
    }

    private Path toOutputFile(Path srcDir, Path srcFile, Path outDir) {
        final Path relativePath = srcDir.relativize(srcFile);
        return outDir.resolve(relativePath);
    }

    @Override
    public String toString() {
        return "CopyResources{}";
    }
}
