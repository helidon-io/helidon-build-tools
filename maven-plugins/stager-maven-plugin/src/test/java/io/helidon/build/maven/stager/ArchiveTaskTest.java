/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.helidon.build.common.CurrentThreadExecutorService;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests {@link ArchiveTask}.
 */
class ArchiveTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testArchiveWithNestedTask() throws Exception {
        execute(new ArchiveTask(XMLElement.read("""
                <archive target="dist/{name}.zip" includes="**/*.txt" excludes="**/draft/**"/>
                """), List.of(new FileTask(XMLElement.read("""
                <file target="docs/readme.txt">{name} readme</file>
                """), List.of()),
                new FileTask(XMLElement.read("""
                <file target="draft/one.txt">draft one</file>
                """), List.of()))),
                Map.of("name", "release"));

        try (var fs = newZipFileSystem(tempDir.resolve("dist/release.zip"))) {
            var readme = fs.getPath("docs/readme.txt");
            assertThat(readme, fileExists());
            assertThat(Files.readString(readme), is("release readme"));

            var draft = fs.getPath("draft/one.txt");
            assertThat(draft, not(fileExists()));
        }
    }

    void execute(ArchiveTask task, Map<String, String> vars) {
        try {
            task.execute(new StagingContext() {
                @Override
                public Path createTempDirectory(String prefix) throws IOException {
                    return Files.createTempDirectory(tempDir, prefix);
                }

                @Override
                public void ensureDirectory(Path directory) {
                    try {
                        Files.createDirectories(directory);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public void archive(Path directory, Path zip, String includes, String excludes) {
                    var includesList = Arrays.asList(includes.split(","));
                    var excludesList = Arrays.asList(excludes.split(","));
                    zip(zip, directory, p -> {}, includesList, excludesList);
                }

                @Override
                public Executor executor() {
                    return new CurrentThreadExecutorService();
                }
            }, tempDir, vars).toCompletableFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
