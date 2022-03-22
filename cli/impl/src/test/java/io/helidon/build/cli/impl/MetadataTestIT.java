/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.common.ConfigProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_MP;
import static io.helidon.build.cli.impl.TestMetadata.HELIDON_BARE_SE;
import static io.helidon.build.cli.impl.TestMetadata.LAST_UPDATE_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.LATEST_FILE_NAME;
import static io.helidon.build.cli.impl.TestMetadata.VERSION_RC2;
import static io.helidon.build.common.Unchecked.unchecked;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for class {@link Metadata} using {@code helidon.io} default url.
 */
public class MetadataTestIT extends MetadataTestBase {

    static final String RC2_LAST_UPDATE = VERSION_RC2 + File.separator + LAST_UPDATE_FILE_NAME;

    @BeforeEach
    public void beforeEach(TestInfo info) {
        beforeEach(info, Metadata.DEFAULT_URL);
    }

    @Test
    void smokeTestRc2() throws Exception {

        // Do the initial catalog request for RC2

        final Runnable request = unchecked(() -> catalogRequest(VERSION_RC2, true));
        assertInitialRequestPerformsUpdate(request, 24, HOURS, VERSION_RC2, "", false);

        // Check latest version. Should not perform update.

        LOG_RECORDER.clear();
        latestVersion = meta.latestVersion();
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", LATEST_FILE_NAME);

        // Check properties. Should not perform update.

        LOG_RECORDER.clear();
        ConfigProperties props = meta.propertiesOf(VERSION_RC2);
        assertThat(props, is(not(nullValue())));
        assertThat(props.keySet().isEmpty(), is(false));
        assertThat(props.property("build-tools.version"), is("2.0.0-RC3"));
        assertThat(props.property("cli.version"), is("2.0.0-RC3"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC2_LAST_UPDATE);

        // Check catalog again. Should not perform update.

        LOG_RECORDER.clear();
        ArchetypeCatalog catalog = meta.catalogOf(VERSION_RC2);
        assertThat(catalog, is(not(nullValue())));
        assertThat(catalog.entries().size() >= 2, is(true));
        Map<String, ArchetypeEntry> entriesById = catalog.entries()
                                                         .stream()
                                                         .collect(Collectors.toMap(ArchetypeEntry::artifactId, entry -> entry));
        assertThat(entriesById.get(HELIDON_BARE_SE), is(notNullValue()));
        assertThat(entriesById.get(HELIDON_BARE_SE).name(), is("bare"));
        assertThat(entriesById.get(HELIDON_BARE_MP), is(notNullValue()));
        assertThat(entriesById.get(HELIDON_BARE_MP).name(), is("bare"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC2_LAST_UPDATE);

        // Check archetype. Should not perform update.

        LOG_RECORDER.clear();
        Path archetypeJar = meta.archetypeV1Of(entriesById.get("helidon-bare-se"));
        assertThat(archetypeJar, is(not(nullValue())));
        assertThat(Files.exists(archetypeJar), is(true));
        assertThat(archetypeJar.getFileName().toString(), is("helidon-bare-se-2.0.0-RC2.jar"));
        assertThat(LOG_RECORDER.size(), is(1));
        assertLinesContainingAll(1, "stale check", "is false", RC2_LAST_UPDATE);

        // Check that more calls do not update

        LOG_RECORDER.clear();
        assertThat(meta.propertiesOf(VERSION_RC2), is(props));
        assertThat(meta.catalogOf(VERSION_RC2), is(catalog));

        assertThat(LOG_RECORDER.size(), is(2));
        assertLinesContainingAll(2, "stale check", "is false", RC2_LAST_UPDATE);
    }
}
