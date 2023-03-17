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

package io.helidon.build.archetype.engine.v2;

import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Version;
import io.helidon.build.common.VirtualFileSystem;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Tests {@link VersionLoader}.
 */
public class VersionLoaderTest {

    @Test
    public void testVersions() throws URISyntaxException {
        List<Version> versions = VersionLoader.load(fileSystem());

        assertThat(versions.size(), is(2));
        assertThat(versions.stream().map(Version::id).collect(Collectors.toList()), contains("3.1.2", "2.6.0"));
        assertThat(versions.stream().map(v->v.supportedCli().toString())
                           .collect(Collectors.toList()), contains("(,1.0],[1.2,)", "[2.1,3)"));
    }

    private FileSystem fileSystem() throws URISyntaxException {
        Path path = Paths.get(VersionLoaderTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path testPath = path.resolve("loader");
        return VirtualFileSystem.create(testPath);
    }
}
