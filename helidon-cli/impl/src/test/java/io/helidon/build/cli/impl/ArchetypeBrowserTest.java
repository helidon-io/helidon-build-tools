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

import io.helidon.build.archetype.engine.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.cli.impl.InitCommand.Flavor;

import org.junit.jupiter.api.Test;

import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Class AppTypeBrowserTest.
 */
public class ArchetypeBrowserTest extends MetadataAccessTestBase {

    private ArchetypeBrowser newBrowser(Flavor flavor) throws Exception {
        return new ArchetypeBrowser(metadata(), flavor, helidonTestVersion());
    }

    @Test
    public void testMpBrowser() throws Exception {
        ArchetypeBrowser browser = newBrowser(Flavor.MP);
        ArchetypeEntry entry = findEntry("bare", browser);
        assertThat(entry.name(), is("bare"));
        assertThat(entry.artifactId(), is("helidon-bare-mp"));
        assertThat(Files.exists(browser.archetypeJar(entry)), is(true));
    }

    @Test
    public void testSeBrowser() throws Exception {
        ArchetypeBrowser browser = newBrowser(Flavor.SE);
        ArchetypeEntry entry = findEntry("bare", browser);
        assertThat(entry.name(), is("bare"));
        assertThat(entry.artifactId(), is("helidon-bare-se"));
        assertThat(Files.exists(browser.archetypeJar(entry)), is(true));
    }

    private static ArchetypeEntry findEntry(String name, ArchetypeBrowser browser) {
        return browser.archetypes()
                      .stream()
                      .filter(e -> e.name().equals(name))
                      .findFirst()
                      .orElseThrow();
    }
}
