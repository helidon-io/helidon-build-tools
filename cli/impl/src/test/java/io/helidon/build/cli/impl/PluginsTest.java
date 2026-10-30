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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.RestoreSystemProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link Plugins}.
 */
@RestoreSystemProperties
class PluginsTest {

    @Test
    void testPluginJarFollowsUserHome(@TempDir Path tempDir) throws IOException {
        String jarName = "cli-plugins-" + BuildProperties.buildRevision() + ".jar";
        Path firstHome = Files.createDirectory(tempDir.resolve("first"));
        Path secondHome = Files.createDirectory(tempDir.resolve("second"));

        System.setProperty("user.home", firstHome.toString());
        Path firstJar = Files.writeString(UserConfig.current().pluginsDir().resolve(jarName), "first").toAbsolutePath();
        assertThat(Plugins.pluginJar(), is(firstJar));

        System.setProperty("user.home", secondHome.toString());
        Path secondJar = Files.writeString(UserConfig.current().pluginsDir().resolve(jarName), "second").toAbsolutePath();
        assertThat(Plugins.pluginJar(), is(secondJar));
    }
}
