/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import java.net.URL;
import java.nio.file.Path;

import io.helidon.build.test.TestFiles;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.UserConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for class {@link Metadata}.
 */
class MetadataTestIT {
    private static final String RC1_URL = baseUrl("rc1").toExternalForm();
    private static final String RC2_URL = baseUrl("rc2").toExternalForm();

    static URL baseUrl(String name) {
        final String resourcePath = "cli-data-" + name;
        return requireNonNull(MetadataTestIT.class.getClassLoader().getResource(resourcePath));
    }

    private Path cacheDir;

    @BeforeEach
    public void beforeEach() throws IOException {
        Config.setUserHome(TestFiles.targetDir().resolve("bob"));
        final UserConfig userConfig = Config.userConfig();
        userConfig.clearCache();
        cacheDir = userConfig.cacheDir();
    }

    @Test
    void testLatestVersion() throws Exception {
        Metadata meta = Metadata.newInstance(cacheDir, RC1_URL, 2);
        MavenVersion version = meta.latestVersion();
        ConfigProperties props = meta.properties(version);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("helidon.version"), is("2.0.0-RC1"));
        assertThat(props.property("build-tools.version"), is("2.0.0-RC1"));
        assertThat(props.property("cli.version"), is("2.0.0-RC1"));
        assertThat(props.contains("cli.2.0.0-M2.message"), is(true));
        assertThat(props.contains("cli.2.0.0-M3.message"), is(false));
        assertThat(props.contains("cli.2.0.0-M4.message"), is(true));
        assertThat(props.contains("cli.2.0.0-RC1.message"), is(true));
    }
}
