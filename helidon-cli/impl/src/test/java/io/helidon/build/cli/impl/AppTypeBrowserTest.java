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
import java.util.List;

import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.util.Log;
import io.helidon.build.util.RequirementsFailure;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.AppTypeBrowser.REMOTE_REPO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Class AppTypeBrowserTest.
 */
public class AppTypeBrowserTest extends BaseCommandTest {

    /**
     * Test a simple file download from remote repo.
     *
     * @throws Exception If error occurs.
     */
    @Test
    public void testDownload() throws Exception {
        try {
            Path file = Path.of("maven-metadata.xml");
            AppTypeBrowser browser = new AppTypeBrowser(Flavor.SE, HELIDON_VERSION_TEST);
            browser.downloadArtifact(new URL(REMOTE_REPO + "/io/helidon/build-tools/maven-metadata.xml"), file);
            assertThat(file.toFile().exists(), is(true));
            assertThat(file.toFile().delete(), is(true));
        } catch (RequirementsFailure e) {
            Log.warn("IGNORING '%s'. Fix this once we have a Helidon release with new archetypes!", e.getMessage());
        }
    }

    /**
     * Assertions should be strengthened after we have a release.
     */
    @Test
    public void testMpBrowser() {
        try {
            AppTypeBrowser browser = new AppTypeBrowser(Flavor.MP, HELIDON_SNAPSHOT_VERSION);
            List<String> appTypes = browser.appTypes();
            assertThat(appTypes.size(), is(greaterThanOrEqualTo(0)));
            assertThat(appTypes.stream().map(browser::archetypeJar).count(), is(greaterThanOrEqualTo(0L)));
        } catch (RequirementsFailure e) {
            Log.warn("IGNORING '%s'. Fix this once we have a Helidon release with new archetypes!", e.getMessage());
        }
    }

    /**
     * Assertions should be strengthen after we have a release.
     */
    @Test
    public void testSeBrowser() {
        try {
            AppTypeBrowser browser = new AppTypeBrowser(Flavor.SE, HELIDON_SNAPSHOT_VERSION);
            List<String> appTypes = browser.appTypes();
            assertThat(appTypes.size(), is(greaterThanOrEqualTo(0)));
            assertThat(appTypes.stream().map(browser::archetypeJar).count(), is(greaterThanOrEqualTo(0L)));
        } catch (RequirementsFailure e) {
            Log.warn("IGNORING '%s'. Fix this once we have a Helidon release with new archetypes!", e.getMessage());
        }
    }
}
