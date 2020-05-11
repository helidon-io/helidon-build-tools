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

import java.net.URL;
import java.nio.file.Path;

import io.helidon.build.cli.impl.InitCommand.Flavor;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static io.helidon.build.cli.impl.AppTypeBrowser.REMOTE_REPO;

/**
 * Class AppTypeBrowserTest.
 */
public class AppTypeBrowserTest {

    private static final String HELIDON_VERSION = "2.0.0-M2";

    /**
     * Test a simple file download from remote repo.
     *
     * @throws Exception If error occurs.
     */
    @Test
    public void testDownload() throws Exception {
        Path file = Path.of("maven-metadata.xml");
        AppTypeBrowser browser = new AppTypeBrowser(Flavor.SE, HELIDON_VERSION);
        browser.downloadArtifact(new URL(REMOTE_REPO + "/io/helidon/build-tools/maven-metadata.xml"), file);
        assertThat(file.toFile().exists(), is(true));
        assertThat(file.toFile().delete(), is(true));
    }

    /**
     * No assertions since we don't have a release yet. This code only exercises
     * the cache logic for now.
     */
    @Test
    public void testCache() {
        AppTypeBrowser browser = new AppTypeBrowser(Flavor.MP, "2.0.0-SNAPSHOT");
        browser.appTypes().stream()
                .map(browser::archetypeJar)
                .forEach(System.out::println);
        browser = new AppTypeBrowser(Flavor.SE, "2.0.0-SNAPSHOT");
        browser.appTypes().stream()
                .map(browser::archetypeJar)
                .forEach(System.out::println);
    }
}
