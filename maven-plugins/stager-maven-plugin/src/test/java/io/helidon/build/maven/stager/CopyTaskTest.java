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
    void testRequiresSourceAndTarget() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> task(Map.of("target", "."), List.of(), List.of()));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> task(Map.of("source", "src"), List.of(), List.of()));

        assertThat(ex1.getMessage(), containsString("source is required"));
        assertThat(ex2.getMessage(), containsString("target is required"));
    }

    @Test
    void testDirectoryContentCopy() throws Exception {
        write("project/src/readme.txt", "readme");
        write("project/src/nested/index.html", "index");

        Path stage = execute("src", "docs", List.of(), List.of(), Map.of());

        assertThat(Files.readString(stage.resolve("docs/readme.txt")), is("readme"));
        assertThat(Files.readString(stage.resolve("docs/nested/index.html")), is("index"));
        assertThat(Files.exists(stage.resolve("docs/src/readme.txt")), is(false));
    }

    @Test
    void testNestedIncludeExcludeFiltering() throws Exception {
        write("project/src/docs/keep/readme.txt", "keep");
        write("project/src/docs/keep/readme.md", "markdown");
        write("project/src/docs/draft/readme.txt", "draft");

        Path stage = execute("src/docs", ".", List.of("**/*.txt"), List.of("draft/**"), Map.of());

        assertThat(Files.readString(stage.resolve("keep/readme.txt")), is("keep"));
        assertThat(Files.exists(stage.resolve("keep/readme.md")), is(false));
        assertThat(Files.exists(stage.resolve("draft/readme.txt")), is(false));
    }

    @Test
    void testNoMatchedFilesIsNoOp() throws Exception {
        write("project/src/docs/readme.txt", "readme");

        Path stage = execute("src/docs", "docs", List.of("**/*.md"), List.of(), Map.of());

        assertThat(Files.exists(stage.resolve("docs")), is(false));
    }

    @Test
    void testSymlinkPreservation() throws Exception {
        write("project/src/real.txt", "real");
        Files.createSymbolicLink(tempDir.resolve("project/src/link.txt"), Path.of("real.txt"));

        Path stage = execute("src", ".", List.of(), List.of(), Map.of());

        Path link = stage.resolve("link.txt");
        assertThat(Files.isSymbolicLink(link), is(true));
        assertThat(Files.readSymbolicLink(link).toString(), is("real.txt"));
    }

    @Test
    void testOverwriteReplacement() throws Exception {
        write("project/src/readme.txt", "new");
        write("stage/readme.txt", "old");
        Files.createSymbolicLink(tempDir.resolve("stage/link.txt"), Path.of("old.txt"));
        Files.createSymbolicLink(tempDir.resolve("project/src/link.txt"), Path.of("readme.txt"));

        execute("src", ".", List.of(), List.of(), Map.of());

        assertThat(Files.readString(tempDir.resolve("stage/readme.txt")), is("new"));
        assertThat(Files.isSymbolicLink(tempDir.resolve("stage/link.txt")), is(true));
        assertThat(Files.readSymbolicLink(tempDir.resolve("stage/link.txt")).toString(), is("readme.txt"));
    }

    @Test
    void testMissingSourceFails() {
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> execute("missing", ".", List.of(), List.of(), Map.of()));

        assertThat(ex.getCause().getMessage(), containsString("does not exist"));
    }

    @Test
    void testNonDirectorySourceFails() {
        write("project/src/readme.txt", "readme");

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> execute("src/readme.txt", ".", List.of(), List.of(), Map.of()));

        assertThat(ex.getCause().getMessage(), containsString("is not a directory"));
    }

    @Test
    void testIteratorVariableResolution() throws Exception {
        write("project/src/one/readme.txt", "one");
        write("project/src/two/readme.txt", "two");

        XMLElement element = XMLElement.read("""
                <copy source="src/{name}" target="{name}">
                    <iterators>
                        <variables>
                            <variable name="name"><value>one</value><value>two</value></variable>
                        </variables>
                    </iterators>
                </copy>
                """);
        CopyTask task = new CopyTask(element);
        task.execute(context(), tempDir.resolve("stage"), Map.of()).toCompletableFuture().get();

        assertThat(Files.readString(tempDir.resolve("stage/one/readme.txt")), is("one"));
        assertThat(Files.readString(tempDir.resolve("stage/two/readme.txt")), is("two"));
    }

    @Test
    void testDirectoryCreationDelegatesToContext() throws Exception {
        write("project/src/docs/readme.txt", "readme");
        write("project/src/docs/nested/file.txt", "file");
        Path stage = tempDir.resolve("stage");
        StagingContext context = context();

        task(Map.of("source", "src/docs", "target", "copied"), List.of(), List.of())
                .execute(context, stage, Map.of()).toCompletableFuture().get();

        assertThat(Files.exists(stage.resolve("copied")), is(true));
        assertThat(Files.exists(stage.resolve("copied/nested")), is(true));
    }

    Path execute(String source,
                 String target,
                 List<String> includes,
                 List<String> excludes,
                 Map<String, String> vars) throws Exception {

        CopyTask task = task(Map.of("source", source, "target", target), includes, excludes);
        Path dir = tempDir.resolve("stage");
        task.execute(context(), dir, vars).toCompletableFuture().get();
        return dir;
    }

    void write(String path, String value) {
        try {
            Path file = tempDir.resolve(path);
            Files.createDirectories(file.getParent());
            Files.writeString(file, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    StagingContext context() {
        return new StagingContext() {
            @Override
            public Path resolve(String path) {
                return tempDir.resolve("project").resolve(path);
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
        };
    }

    private static CopyTask task(Map<String, String> attributes, List<String> includes, List<String> excludes) {
        XMLElement.Builder element = XMLElement.builder().name("copy").attributes(attributes);
        addFilters(element, "includes", "include", includes);
        addFilters(element, "excludes", "exclude", excludes);
        XMLElement copy = element.build();
        return new CopyTask(copy);
    }

    private static void addFilters(XMLElement.Builder element,
                                   String wrapper,
                                   String name,
                                   List<String> patterns) {
        if (!patterns.isEmpty()) {
            element.child(filters -> {
                filters.name(wrapper);
                for (String pattern : patterns) {
                    filters.child(filter -> filter.name(name).value(pattern));
                }
            });
        }
    }

}
