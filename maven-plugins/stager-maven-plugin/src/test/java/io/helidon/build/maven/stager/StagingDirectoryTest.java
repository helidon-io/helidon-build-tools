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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.helidon.build.common.CurrentThreadExecutorService;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link StagingDirectory}.
 */
class StagingDirectoryTest {

    @TempDir
    private Path tempDir;

    @Test
    void testNestedTask() throws Exception {
        execute(new StagingDirectory(XMLElement.read("""
                <directory target="releases/current"/>
                """), List.of(new FileTask(XMLElement.read("""
                <file target="docs/readme.txt">staged content</file>
                """), List.of()))), Map.of());

        var file = tempDir.resolve("releases/current/docs/readme.txt");
        assertThat(Files.readString(file), is("staged content"));
        assertThat(Files.exists(tempDir.resolve("docs/readme.txt")), is(false));
    }

    void execute(StagingDirectory task, Map<String, String> vars) throws Exception {
        try {
            task.execute(new StagingContext() {
                @Override
                public Path resolve(String path) {
                    return tempDir.resolve(path);
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
                public Executor executor() {
                    return new CurrentThreadExecutorService();
                }
            }, tempDir, vars).toCompletableFuture().get();
        }  catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
