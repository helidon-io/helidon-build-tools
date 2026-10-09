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
 * Tests {@link FileTask}.
 */
class FileTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testLiteralContent() throws Exception {
        execute(new FileTask(XMLElement.read("""
                <file target="docs/{name}.txt">Hello {name}</file>
                """), List.of()), Map.of("name", "Helidon"));

        assertThat(Files.readString(tempDir.resolve("docs/Helidon.txt")), is("Hello Helidon"));
    }

    @Test
    void testSourceFileCopy() throws Exception {
        var source = tempDir.resolve("input/source.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "source content");

        execute(new FileTask(XMLElement.read("""
                <file source="input/{name}.txt" target="copied/{name}.txt"/>
                """), List.of()), Map.of("name", "source"));

        assertThat(Files.readString(tempDir.resolve("copied/source.txt")), is("source content"));
    }

    @Test
    void testListFiles() throws Exception {
        var guide = tempDir.resolve("docs/guide/index.html");
        var draft = tempDir.resolve("docs/draft/index.html");
        var image = tempDir.resolve("docs/guide/image.png");
        Files.createDirectories(guide.getParent());
        Files.createDirectories(draft.getParent());
        Files.writeString(guide, "guide");
        Files.writeString(draft, "draft");
        Files.writeString(image, "image");

        execute(new FileTask(XMLElement.read("""
                <file target="sitemap.txt"/>
                """), List.of(new ListFilesTask(XMLElement.read("""
                <list-files dir="docs">
                    <includes>
                        <include>**/*.html</include>
                    </includes>
                    <excludes>
                        <exclude>**/draft/**</exclude>
                    </excludes>
                    <substitutions>
                        <substitution match="^docs/(.*)/index[.]html$" replace="$1/"/>
                        <substitution match="^docs/" replace="/" regex="false"/>
                    </substitutions>
                </list-files>
                """)))), Map.of());

        assertThat(Files.readString(tempDir.resolve("sitemap.txt")), is("guide/\n"));
    }

    void execute(FileTask task, Map<String, String> vars) throws Exception {
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
