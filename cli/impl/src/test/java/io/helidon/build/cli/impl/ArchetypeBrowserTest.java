/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.cli.impl.InitOptions.Flavor;
import io.helidon.build.common.test.utils.WorkDirExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.build.common.FileUtils.directory;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.test.utils.FunctionalMatchers.hasProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.contains;

/**
 * Tests {@link ArchetypeBrowser}.
 */
@ExtendWith(WorkDirExtension.class)
class ArchetypeBrowserTest {

    private final Path workDir;

    ArchetypeBrowserTest(Path workDir) {
        this.workDir = workDir;
    }

    @Test
    void testMpBrowser() {
        var metadata = metadata();
        var browser = new ArchetypeBrowser(metadata, Flavor.MP, "2.0.1");
        assertThat(browser.archetypes(), contains(allOf(
                hasProperty("artifactId", ArchetypeEntry::artifactId, is("acme-bare-mp")),
                hasProperty("name", ArchetypeEntry::name, is("bare")),
                hasProperty("title", ArchetypeEntry::title, is("ACME MP Bare")),
                hasProperty("summary", ArchetypeEntry::summary, is("Minimal ACME MP project")),
                hasProperty("description", ArchetypeEntry::description, is(Optional.of("Suitable to start from scratch")))
        )));
    }

    @Test
    void testSeBrowser() {
        var metadata = metadata();
        var browser = new ArchetypeBrowser(metadata, Flavor.SE, "2.0.1");
        assertThat(browser.archetypes(), contains(allOf(
                hasProperty("artifactId", ArchetypeEntry::artifactId, is("acme-bare-se")),
                hasProperty("name", ArchetypeEntry::name, is("bare")),
                hasProperty("title", ArchetypeEntry::title, is("ACME SE Bare")),
                hasProperty("summary", ArchetypeEntry::summary, is("Minimal ACME SE project")),
                hasProperty("description", ArchetypeEntry::description, is(Optional.of("Suitable to start from scratch")))
        )));
    }

    Metadata metadata() {
        return Metadata.builder()
                .rootDir(workDir)
                .url(url())
                .debugPlugin(true)
                .build();
    }

    String url() {
        return url(Map.of(
                "versions.xml", """
                        <data>
                            <archetypes>
                                <version default="true">2.0.1</version>
                            </archetypes>
                        </data>
                        """,
                "2.0.1/cli-data.zip", zip(Map.of(
                        "metadata.properties", """
                                cli.version=0.0.1
                                """,
                        "archetype-catalog.xml", """
                                <archetype-catalog modelVersion="1.0" name="" groupId="" version="2.0.1">
                                      <archetype
                                          artifactId="acme-bare-mp"
                                          name="bare"
                                          title="ACME MP Bare"
                                          summary="Minimal ACME MP project"
                                          description="Suitable to start from scratch"
                                          tags="mp" />
                                      <archetype
                                          artifactId="acme-bare-se"
                                          name="bare"
                                          title="ACME SE Bare"
                                          summary="Minimal ACME SE project"
                                          description="Suitable to start from scratch"
                                          tags="se" />
                                </archetype-catalog>
                                """))
        ));
    }

    String url(Map<?, ?> entries) {
        return directory(workDir.resolve("cli-data"), entries).toUri().toString();
    }
}
