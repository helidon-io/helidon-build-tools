/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.maven.url.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class MavenResolverTest {

    private static final MavenResolver RESOLVER = new MavenResolver();
    private static final String CONTENT = "Archetype content";
    private static final Path TEMP_DIR = Path.of(System.getProperty("user.home")
            + "/.m2/repository/io/helidon/archetypes/helidon-archetype/3.0.0-SNAPSHOT/archetype/");

    @BeforeAll
    static void bootstrap() throws IOException {
        Files.createDirectory(TEMP_DIR);
        Files.write(TEMP_DIR.resolve("helidon-archetype.xml"), CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    static void cleanUp() throws IOException {
        Files.walk(TEMP_DIR)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testInputStream() throws IOException {
        InputStream is = RESOLVER.getInputStream("mvn://io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT/archetype/helidon-archetype.xml");
        assertThat(is, is(notNullValue()));
        assertThat(CONTENT, is(new String(is.readNBytes(CONTENT.length()))));
    }

}
