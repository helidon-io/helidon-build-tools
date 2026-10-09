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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.helidon.build.common.CurrentThreadExecutorService;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link CopyTask}.
 */
class CopyTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testDirectoryContentCopy() throws Exception {
        var readme = tempDir.resolve("src/readme.txt");
        var index = tempDir.resolve("src/nested/index.html");
        Files.createDirectories(index.getParent());
        Files.writeString(readme, "readme");
        Files.writeString(index, "index");

        execute(new CopyTask(XMLElement.read("""
                <copy source="src" target="docs"/>
                """)), Map.of());

        assertThat(Files.readString(tempDir.resolve("docs/readme.txt")), is("readme"));
        assertThat(Files.readString(tempDir.resolve("docs/nested/index.html")), is("index"));
        assertThat(Files.exists(tempDir.resolve("docs/src/readme.txt")), is(false));
    }

    @Test
    void testFiltering() throws Exception {
        var keep = tempDir.resolve("src/docs/keep/readme.txt");
        var markdown = tempDir.resolve("src/docs/keep/readme.md");
        var draft = tempDir.resolve("src/docs/draft/readme.txt");
        Files.createDirectories(keep.getParent());
        Files.createDirectories(draft.getParent());
        Files.writeString(keep, "keep");
        Files.writeString(markdown, "markdown");
        Files.writeString(draft, "draft");

        execute(new CopyTask(XMLElement.read("""
                <copy source="src/docs" target=".">
                    <includes>
                        <include>**/*.txt</include>
                    </includes>
                    <excludes>
                        <exclude>draft/**</exclude>
                    </excludes>
                </copy>
                """)), Map.of());

        assertThat(Files.readString(tempDir.resolve("keep/readme.txt")), is("keep"));
        assertThat(Files.exists(tempDir.resolve("keep/readme.md")), is(false));
        assertThat(Files.exists(tempDir.resolve("draft/readme.txt")), is(false));
    }

    @Test
    void testNoMatchedFiles() throws Exception {
        var readme = tempDir.resolve("src/docs/readme.txt");
        Files.createDirectories(readme.getParent());
        Files.writeString(readme, "readme");

        execute(new CopyTask(XMLElement.read("""
                <copy source="src/docs" target="docs">
                    <include>**/*.md</include>
                </copy>
                """)), Map.of());

        assertThat(Files.exists(tempDir.resolve("docs")), is(false));
    }

    @Test
    void testSymlinkPreservation() throws Exception {
        var source = tempDir.resolve("src");
        var target = Path.of("real.txt");
        Files.createDirectories(source);
        Files.writeString(source.resolve(target), "real");
        Files.createSymbolicLink(source.resolve("link.txt"), target);
        var task = new CopyTask(XMLElement.read("""
                <copy source="src" target="."/>
                """));

        execute(task, Map.of());

        var link = tempDir.resolve("link.txt");
        assertThat(Files.isSymbolicLink(link), is(true));
        assertThat(Files.readSymbolicLink(link), is(target));
    }

    @Test
    void testOverwriteReplacement() throws Exception {
        var source = tempDir.resolve("src");
        Files.createDirectories(source);
        Files.writeString(source.resolve("readme.txt"), "new");
        Files.writeString(tempDir.resolve("readme.txt"), "old");
        var value = Path.of("readme.txt");
        Files.createSymbolicLink(tempDir.resolve("link.txt"), Path.of("old.txt"));
        Files.createSymbolicLink(source.resolve("link.txt"), value);
        var task = new CopyTask(XMLElement.read("""
                <copy source="src" target="."/>
                """));

        execute(task, Map.of());

        assertThat(Files.readString(tempDir.resolve("readme.txt")), is("new"));
        assertThat(Files.isSymbolicLink(tempDir.resolve("link.txt")), is(true));
        assertThat(Files.readSymbolicLink(tempDir.resolve("link.txt")), is(value));
    }

    @Test
    void testMissingSourceFails() {
        var task = new CopyTask(XMLElement.read("""
                <copy source="missing" target="."/>
                """));

        var ex = assertThrows(IllegalStateException.class, () -> execute(task, Map.of()));
        assertThat(ex.getMessage(), containsString("does not exist"));
    }

    @Test
    void testNonDirectorySourceFails() throws IOException {
        var source = tempDir.resolve("src/readme.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "readme");
        var task = new CopyTask(XMLElement.read("""
                <copy source="src/readme.txt" target="."/>
                """));

        var ex = assertThrows(IllegalStateException.class, () -> execute(task, Map.of()));
        assertThat(ex.getMessage(), containsString("is not a directory"));
    }

    @Test
    void testCreatesNestedTargetDirectories() throws Exception {
        Path source = tempDir.resolve("src/docs/nested/file.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "file");
        var task = new CopyTask(XMLElement.read("""
                <copy source="src/docs" target="copied"/>
                """));

        execute(task, Map.of());
        assertThat(Files.readString(tempDir.resolve("copied/nested/file.txt")), is("file"));
    }

    void execute(CopyTask task, Map<String, String> vars) throws Exception {
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
