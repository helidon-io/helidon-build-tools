/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;

import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;

/**
 * Test file utilities.
 */
public class TestFiles {
    private static final String ARCHETYPE_VERSION = "1.4.0"; // TODO need to keep this up to date... Bedrock?
    private static final Path OUR_TARGET_DIR = ourTargetDir();
    private static final AtomicReference<Path> SE_JAR = new AtomicReference<>();
    private static final AtomicReference<Path> MP_JAR = new AtomicReference<>();

    private static Path ourTargetDir() {
        final Path ourCodeSource = Paths.get(TestFiles.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        return ourCodeSource.getParent();
    }

    public static Path signedJar() {
        return Paths.get("/Users/batsatt/.m2/repository/org/bouncycastle/bcpkix-jdk15on/1.60/bcpkix-jdk15on-1.60.jar");  // TODO
    }

    public static Path targetDir() {
        return OUR_TARGET_DIR;
    }

    public static Path helidonSeJar() {
        return quickstartJar(SE_JAR, "se");
    }

    public static Path helidonMpJar() {
        return quickstartJar(MP_JAR, "mp");
    }

    private static Path quickstartJar(AtomicReference<Path> holder, String helidonVariant) {
        if (holder.get() == null) {
            holder.set(createQuickstartJar(helidonVariant));
        }
        return holder.get();
    }

    private static Path createQuickstartJar(String helidonVariant) {
        createQuickstartProject(helidonVariant);
        return buildQuickstartProject(helidonVariant);
    }

    private static Path buildQuickstartProject(String helidonVariant) {
        final Path targetDir = ourTargetDir();
        final String id = "helidon-quickstart-" + helidonVariant;
        final Path sourceDir = assertDir(ourTargetDir().resolve(id));
        Log.info("Building %s", id);
        execute(new ProcessBuilder().directory(sourceDir.toFile())
                                    .command(List.of("mvn",
                                                     "clean",
                                                     "package",
                                                     "-DskipTests")));
        return assertFile(sourceDir.resolve("target/" + id + ".jar"));
    }

    private static Path createQuickstartProject(String helidonVariant) {
        final Path targetDir = ourTargetDir();
        final String id = "helidon-quickstart-" + helidonVariant;
        final String pkg = "io.helidon.examples.quickstart." + helidonVariant;
        Log.info("Creating %s from archetype %s", id, ARCHETYPE_VERSION);
        execute(new ProcessBuilder().directory(targetDir.toFile())
                                    .command(List.of("mvn",
                                                     "archetype:generate",
                                                     "-DinteractiveMode=false",
                                                     "-DarchetypeGroupId=io.helidon.archetypes",
                                                     "-DarchetypeArtifactId=" + id,
                                                     "-DarchetypeVersion=" + ARCHETYPE_VERSION,
                                                     "-DgroupId=test",
                                                     "-DartifactId=" + id,
                                                     "-Dpackage=" + pkg
                                    )));
        return assertDir(targetDir.resolve(id));
    }

    private static void execute(ProcessBuilder builder) {
        try {
            ProcessMonitor.builder()
                          .processBuilder(builder)
                          .capture(true)
                          .build()
                          .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
