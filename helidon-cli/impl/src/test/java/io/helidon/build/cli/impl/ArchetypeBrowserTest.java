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

import java.nio.file.Files;
import java.util.List;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.cli.impl.InitCommand.Flavor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Class AppTypeBrowserTest.
 */
public class ArchetypeBrowserTest extends MetadataCommandTest {

    @BeforeEach
    public void beforeEach() {
        startMetadataAccess(false, false);
    }

    @AfterEach
    public void afterEach() {
        stopMetadataAccess();
    }

    private ArchetypeBrowser newBrowser(Flavor flavor) throws Exception {
        return new ArchetypeBrowser(metadata(), flavor, helidonTestVersion());
    }

    @Test
    public void testMpBrowser() throws Exception {
        ArchetypeBrowser browser = newBrowser(Flavor.MP);
        List<ArchetypeCatalog.ArchetypeEntry> archetypes = browser.archetypes();
        assertThat(archetypes.size(), is(1));
        assertThat(archetypes.get(0).name(), is("bare"));
        assertThat(archetypes.get(0).artifactId(), is("helidon-bare-mp"));
        assertThat(Files.exists(browser.archetypeJar(archetypes.get(0))), is(true));
    }

    @Test
    public void testSeBrowser() throws Exception {
        ArchetypeBrowser browser = newBrowser(Flavor.SE);
        List<ArchetypeCatalog.ArchetypeEntry> archetypes = browser.archetypes();
        assertThat(archetypes.get(0).name(), is("bare"));
        assertThat(archetypes.get(0).artifactId(), is("helidon-bare-se"));
        assertThat(Files.exists(browser.archetypeJar(archetypes.get(0))), is(true));
    }
}
