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
import io.helidon.build.common.PathFinder;
import io.helidon.build.common.logging.Log;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.FileUtils.unzip;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;

class FunctionalUtils {

    static final String CLI_VERSION_KEY = "cli.version";
    static final String CLI_DIRNAME = "helidon-" + requiredProperty(CLI_VERSION_KEY);
    static final String CLI_NATIVE_BIN_NAME = System.getProperty("native.image.name", "helidon");

    static final LazyValue<String> CLI_DATA_URL = new LazyValue<>(FunctionalUtils::cliDataUrl);
    static final LazyValue<String> MAVEN_LOCAL_REPO = new LazyValue<>(FunctionalUtils::mavenLocalRepoUrl);
    static final LazyValue<String> CLI_VERSION = new LazyValue<>(() -> requiredProperty("cli.version"));
    static final LazyValue<Path> EXECUTABLE_DIR = new LazyValue<>(() -> Path.of(requiredProperty("helidon.executable.directory")));

    static final LazyValue<Path> CLI_NATIVE = new LazyValue<>(FunctionalUtils::cliNative);
    static final LazyValue<Path> CLI_ZIP = new LazyValue<>(FunctionalUtils::cliZip);
    static final LazyValue<Path> CLI_DIR = new LazyValue<>(FunctionalUtils::cliInstallDir);
    static final LazyValue<Path> CLI_BIN_DIR = new LazyValue<>(FunctionalUtils::cliBinDir);
    static final LazyValue<Path> CLI_EXE = new LazyValue<>(FunctionalUtils::cliExe);

    static String requiredProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(String.format("System property %s is not set", key));
        }
        return value;
    }

    static Path cliInstallDir() {
        Path installDir = unique(targetDir(FunctionalUtils.class).resolve("install"), "helidon-cli");
        Log.debug("Unzipping " + CLI_ZIP.get());
        unzip(CLI_ZIP.get(), installDir);
        return installDir;
    }

    static Path cliZip() {
        return EXECUTABLE_DIR.get()
                .resolve("target/helidon-cli.zip")
                .toAbsolutePath()
                .normalize();
    }

    static Path cliBinDir() {
        return CLI_DIR.get()
                .resolve(CLI_DIRNAME)
                .resolve("bin");
    }

    static Path cliExe() {
        return PathFinder.find("helidon", List.of(CLI_BIN_DIR.get()))
                .orElseThrow();
    }

    static Path cliNative() {
        return EXECUTABLE_DIR.get()
                .resolve("target")
                .resolve(CLI_NATIVE_BIN_NAME);
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
