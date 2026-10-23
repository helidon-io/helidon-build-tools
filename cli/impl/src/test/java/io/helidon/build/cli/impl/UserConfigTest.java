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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests {@link UserConfig}.
 */
@RestoreSystemProperties
class UserConfigTest {

    @Test
    void testSystemProperty(@TempDir Path homeDir) throws IOException {
        Path configFile = homeDir.resolve(".helidon/config");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "system_existing=configured\nsystem_new=configured\n");

        System.setProperty("existing", "original");
        System.clearProperty("new");
        UserConfig config = UserConfig.create(homeDir);

        assertThat(System.getProperty("existing"), is("original"));
        assertThat(System.getProperty("new"), nullValue());
        try (var ignored = config.applySystemProperties()) {
            assertThat(System.getProperty("existing"), is("configured"));
            assertThat(System.getProperty("new"), is("configured"));
        }
        assertThat(System.getProperty("existing"), is("original"));
        assertThat(System.getProperty("new"), nullValue());
    }

    @Test
    void testCurrent(@TempDir Path tempDir) throws IOException {
        Path firstHome = Files.createDirectory(tempDir.resolve("first"));
        Path secondHome = Files.createDirectory(tempDir.resolve("second"));

        System.setProperty("user.home", firstHome.toString());
        UserConfig first = UserConfig.current();
        assertThat(UserConfig.current(), sameInstance(first));

        System.setProperty("user.home", secondHome.toString());
        UserConfig second = UserConfig.current();
        assertThat(second, not(sameInstance(first)));
        assertThat(UserConfig.current(), sameInstance(second));
    }
}
