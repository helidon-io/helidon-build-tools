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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executor;

import io.helidon.build.common.CurrentThreadExecutorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link CopyArtifactTask}.
 */
class CopyArtifactTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testCopyArtifactTargetResolvesIteratorVariables() throws Exception {
        Path outputDir = execute(
                Map.of("groupId", "io.helidon",
                        "artifactId", "helidon",
                        "version", "{it}",
                        "target", "helidon-{it}.jar"),
                Map.of("it", "4.2.0"));

        assertThat(Files.exists(outputDir.resolve("helidon-4.2.0.jar")), is(true));
        assertThat(Files.exists(outputDir.resolve("helidon-{it}.jar")), is(false));
    }

    @Test
    void testCopyArtifactDefaultTargetResolvesGavVariables() throws Exception {
        Path outputDir = execute(
                Map.of("groupId", "io.helidon",
                        "artifactId", "helidon",
                        "version", "{it}"),
                Map.of("it", "4.2.0"));
        assertThat(Files.exists(outputDir.resolve("helidon-4.2.0.jar")), is(true));
    }

    Path execute(Map<String, String> attrs, Map<String, String> vars) throws Exception {
        Path artifact = tempDir.resolve("artifact.jar");
        Files.writeString(artifact, "artifact");
        Path outputDir = tempDir.resolve("stage");
        CopyArtifactTask task = new CopyArtifactTask(null, attrs);
        task.execute(context(artifact), outputDir, vars).toCompletableFuture().get();
        return outputDir;
    }

    StagingContext context(Path artifact) {
        return new StagingContext() {

            @Override
            public Path resolve(ArtifactGAV gav) {
                return artifact;
            }

            @Override
            public void ensureDirectory(Path directory) {
                try {
                    Files.createDirectories(directory);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Executor executor() {
                return new CurrentThreadExecutorService();
            }
        };
    }
}
