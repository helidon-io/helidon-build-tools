/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.cli.common;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.helidon.build.common.Lists;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.common.ArchetypesData.Version;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

/**
 * Tests {@link ArchetypesDataLoader}.
 */
public class ArchetypesDataLoaderTest {

    @Test
    public void testArchetypesData() throws URISyntaxException {
        ArchetypesData archetypesData = ArchetypesDataLoader.load(versionsFileFolder().resolve("versions.xml"));
        Version defaultVersion = archetypesData.rawVersions().stream().filter(Version::isDefault).findFirst().get();

        assertThat(archetypesData.rawVersions().size(), is(29));
        assertThat(archetypesData.rules().size(), is(3));
        assertThat(archetypesData.versions(), hasItems("2.0.0", "2.3.4", "3.1.2"));
        assertThat(defaultVersion.id(), is("4.0.0-SNAPSHOT"));
        assertThat(archetypesData.rules().get(0).archetypeRange().toString(), is("[2.0.0,3.0.0)"));
        assertThat(archetypesData.rules().get(0).cliRange().toString(), is("[2.0.0,5.0.0)"));
        assertThat(archetypesData.rules().get(2).archetypeRange().toString(), is("[4.0.0,5.0.0)"));
        assertThat(archetypesData.rules().get(2).cliRange().toString(), is("[4.0.0,5.0.0)"));
    }

    private Path versionsFileFolder() throws URISyntaxException {
        Path path = Paths.get(ArchetypesDataLoaderTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return path.resolve("versions").resolve("cli-data");
    }
}
