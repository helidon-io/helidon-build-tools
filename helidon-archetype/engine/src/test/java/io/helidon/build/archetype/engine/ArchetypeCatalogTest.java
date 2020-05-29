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
package io.helidon.build.archetype.engine;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Tests {@link ArchetypeCatalog}.
 */
public class ArchetypeCatalogTest {

    @Test
    public void testUnmarshall() {
        InputStream is = ArchetypeCatalogTest.class.getResourceAsStream("catalog.xml");
        assertThat(is, is(notNullValue()));

        ArchetypeCatalog catalog = ArchetypeCatalog.read(is);
        assertThat(catalog.id(), is("test-catalog"));
        assertThat(catalog.groupId(), is("io.helidon.archetypes"));
        assertThat(catalog.version(), is("2.0.0-SNAPSHOT"));

        List<ArchetypeCatalog.ArchetypeEntry> entries = catalog.entries();
        assertThat(entries.size(), is(2));

        ArchetypeCatalog.ArchetypeEntry quickstartSe = entries.get(0);
        assertThat(quickstartSe.groupId(), is("io.helidon.archetypes"));
        assertThat(quickstartSe.artifactId(), is("helidon-quickstart-se"));
        assertThat(quickstartSe.version(), is("2.0.0-SNAPSHOT"));
        assertThat(quickstartSe.name(), is("quickstart"));
        assertThat(quickstartSe.title(), is("Helidon Quickstart SE"));
        assertThat(quickstartSe.summary(), is("Simple Hello World REST service using Helidon SE"));
        assertThat(quickstartSe.description().isPresent(), is(true));
        assertThat(quickstartSe.description().get(), is("A simple project that shows how to implement a REST service using Helidon SE"));
        assertThat(quickstartSe.tags(), hasItems("se", "rest"));

        ArchetypeCatalog.ArchetypeEntry quickstartMp = entries.get(1);
        assertThat(quickstartMp.groupId(), is("io.helidon.archetypes"));
        assertThat(quickstartMp.artifactId(), is("helidon-quickstart-mp"));
        assertThat(quickstartMp.version(), is("2.0.0-SNAPSHOT"));
        assertThat(quickstartMp.name(), is("quickstart"));
        assertThat(quickstartMp.title(), is("Helidon Quickstart MP"));
        assertThat(quickstartMp.summary(), is("Simple Hello World REST service using MicroProfile"));
        assertThat(quickstartMp.description().isPresent(), is(false));
        assertThat(quickstartMp.tags(), hasItems("mp", "rest"));
    }
}
