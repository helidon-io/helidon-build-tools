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

import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests {@link UnpackTask}.
 */
class UnpackTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testDownloadAndUnpack() throws Exception {
        var stage = tempDir.resolve("stage");
        Files.createDirectories(stage.resolve("docs"));
        Files.createDirectories(stage.resolve("draft"));
        Files.createDirectories(stage.resolve("root"));
        Files.writeString(stage.resolve("docs/readme.txt"), "readme content");
        Files.writeString(stage.resolve("root/file.txt"), "file content");
        Files.writeString(stage.resolve("draft/one.txt"), "draft one");
        var archive = tempDir.resolve("archive.zip");
        FileUtils.zip(archive, stage);

        execute(new UnpackTask(XMLElement.read("""
                <unpack url="%s" target="expanded/{name}" includes="**/*.txt" excludes="draft/**">
                    <mappers>
                        <mapper match="^root/(.*)$" replace="$1"/>
                    </mappers>
                </unpack>
                """.formatted(archive.toUri()))), Map.of("name", "site"));

        assertThat(Files.readString(tempDir.resolve("expanded/site/docs/readme.txt")), is("readme content"));
        assertThat(Files.readString(tempDir.resolve("expanded/site/file.txt")), is("file content"));
        assertThat(tempDir.resolve("expanded/site/draft/one.txt"), not(fileExists()));
    }

    void execute(UnpackTask task, Map<String, String> vars) throws Exception {
        try {
            task.execute(new StagingContext() {
                @Override
                public Path resolve(ArtifactGAV gav) {
                    return tempDir.resolve("archive.zip");
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
                public void unpack(Path archive, Path target, String includes, String excludes) {
                }

                @Override
                public void unpack(Path archive,
                                   Path target,
                                   String includes,
                                   String excludes,
                                   List<Mapper> mappers,
                                   Map<String, String> vars) {

                    var includesList = Arrays.asList(includes.split(","));
                    var excludesList = Arrays.asList(excludes.split(","));
                    FileUtils.unzip(archive, target, p -> {
                        String r = p;
                        for (var mapper : mappers) {
                            r = r.replaceAll(mapper.match(vars), mapper.replace(vars));
                        }
                        return r;
                    }, includesList, excludesList);
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
