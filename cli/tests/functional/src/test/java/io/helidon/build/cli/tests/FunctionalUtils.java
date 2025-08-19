/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.cli.tests;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.LazyValue;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static io.helidon.build.common.FileUtils.requireJavaExecutable;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;

class FunctionalUtils {

    static final LazyValue<String> CLI_DATA_URL = new LazyValue<>(FunctionalUtils::cliDataUrl);
    static final LazyValue<String> MAVEN_LOCAL_REPO = new LazyValue<>(FunctionalUtils::mavenLocalRepoUrl);
    static final LazyValue<String> CLI_VERSION = new LazyValue<>(() -> requiredProperty("cli.version"));
    static final LazyValue<String> HELIDON_CLI_JAR = new LazyValue<>(FunctionalUtils::helidonCliJar);
    static final LazyValue<String> JAVA_BIN = new LazyValue<>(() -> requireJavaExecutable().toString());
    static final LazyValue<Path> EXECUTABLE_DIR = new LazyValue<>(() -> Path.of(requiredProperty("helidon.executable.directory")));

    static String requiredProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(String.format("System property %s is not set", key));
        }
        return value;
    }

    static String helidonCliJar() {
        return EXECUTABLE_DIR.get()
                .resolve("target/helidon-cli.jar")
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    static String cliDataUrl() {
        return FileUtils.urlOf(targetDir(FunctionalUtils.class)
                        .resolve("it/projects/archetype/target/cli-data")
                        .toAbsolutePath()
                        .normalize())
                .toString();
    }

    static String mavenLocalRepoUrl() {
        try {
            return FileUtils.pathOf(FunctionalUtils.class
                            .getClassLoader()
                            .loadClass("org.hamcrest.MatcherAssert")
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .resolve("../../../..")
                    .toAbsolutePath()
                    .normalize()
                    .toString();
        } catch (ClassNotFoundException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
