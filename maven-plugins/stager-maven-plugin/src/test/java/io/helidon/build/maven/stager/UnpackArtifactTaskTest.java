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
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link UnpackArtifactTask}.
 */
class UnpackArtifactTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testUnpackWithVariables() throws Exception {
        var stage = tempDir.resolve("stage");
        Files.createDirectories(stage);
        Files.writeString(stage.resolve("readme.txt"), "content");
        FileUtils.zip(tempDir.resolve("artifact.zip"), stage);

        execute(new UnpackArtifactTask(XMLElement.read("""
                <unpack-artifact groupId="io.helidon"
                                 artifactId="{artifact}"
                                 version="{version}"
                                 type="zip"
                                 classifier="tests"
                                 target="expanded/{groupId}/{artifactId}-{version}-{classifier}-{type}"/>
                """)), Map.of("artifact", "stager", "version", "4.0.0", "repository", "central"));

        var target = tempDir.resolve("expanded/io.helidon/stager-4.0.0-tests-zip");
        assertThat(target, fileExists());
        var readme = target.resolve("readme.txt");
        assertThat(readme, fileExists());
        assertThat(Files.readString(readme), is("content"));
    }

    void execute(UnpackArtifactTask task, Map<String, String> vars) throws Exception {
        try {
            task.execute(new StagingContext() {
                @Override
                public Path resolve(ArtifactGAV gav) {
                    return tempDir.resolve("artifact.zip");
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
                    FileUtils.unzip(archive, target);
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
