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

import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;

/**
 * Test file utilities.
 */
public class TestFiles {
    private static Path CURRENT_DIR = Paths.get(System.getProperty("user.dir"));
    private static Path OUR_TARGET_DIR = ourTargetDir();
    private static Path PROJECT_DIR = OUR_TARGET_DIR.getParent().getParent();

    private static Path ourTargetDir() {
        final Path ourCodeSource = Paths.get(TestFiles.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        return ourCodeSource.getParent();
    }

    public static Path currentDir() {
        return CURRENT_DIR;
    }

    public static Path targetDir() {
        return OUR_TARGET_DIR;
    }

    public static Path helidonSeJar() {
        final Path targetDir = Paths.get("/Users/batsatt/dev/helidon-quickstart-se/target");  // TODO generate via archetype?
        return targetDir.resolve("helidon-quickstart-se.jar");
    }

    public static Path helidonMpJar() {
        final Path targetDir = Paths.get("/Users/batsatt/dev/helidon-quickstart-mp/target");  // TODO generate via archetype?
        return targetDir.resolve("helidon-quickstart-mp.jar");
    }

    public static Path jandexJar() {
        return Paths.get("/Users/batsatt/.m2/repository/org/jboss/jandex/2.1.1.Final/jandex-2.1.1.Final.jar"); // TODO
    }

    public static Path signedJar() {
        return Paths.get("/Users/batsatt/.m2/repository/org/bouncycastle/bcpkix-jdk15on/1.60/bcpkix-jdk15on-1.60.jar");  // TODO
    }

    public static Path agentJar() {
        return assertFile(PROJECT_DIR.resolve("agent/target/helidon-jre-agent.jar"));
    }

    public static Path patchesDir() {
        return assertDir(PROJECT_DIR.resolve("etc/patches"));
    }

    public static Path weldJrtJar() {
        return assertFile(OUR_TARGET_DIR.resolve("lib/helidon-weld-jrt.jar"));
    }
}
