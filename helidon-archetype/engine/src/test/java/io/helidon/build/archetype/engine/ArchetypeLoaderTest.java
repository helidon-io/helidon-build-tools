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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class ArchetypeLoaderTest.
 */
public class ArchetypeLoaderTest extends ArchetypeBaseTest {

    /**
     * Creates simple JAR file to test archetype loader.
     *
     * @throws IOException If an IO error occurs.
     */
    @BeforeAll
    public static void createSimpleJar() throws IOException {
        Manifest manifest = new Manifest();
        File file = targetDir().toPath().resolve("test.jar").toFile();
        JarOutputStream os = new JarOutputStream(new FileOutputStream(file), manifest);
        JarEntry entry = new JarEntry("META-INF/helidon-archetype.xml");
        os.putNextEntry(entry);
        os.write("<archetype-descriptor></archetype-descriptor>".getBytes(StandardCharsets.US_ASCII));
        os.close();
    }

    /**
     * Deletes simple JAR file after completion.
     *
     * @throws IOException If an IO error occurs.
     */
    @AfterAll
    public static void deleteSimpleJar() throws IOException {
        Path path = targetDir().toPath().resolve("test.jar");
        Files.delete(path);
    }

    @Test
    public void testLoadFromJar() throws IOException {
        File file = new File(ArchetypeLoader.class.getResource("/test.jar").getFile());
        ArchetypeLoader loader = new ArchetypeLoader(file);
        try (InputStream is = loader.loadResourceAsStream("META-INF/helidon-archetype.xml")) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = is.read()) != -1) {
                sb.append((char) c);
            }
            String s = sb.toString();
            assertThat(s.length(), is(greaterThan(0)));
            assertThat(s, containsString("<archetype-descriptor>"));
            assertThat(s, containsString("</archetype-descriptor>"));
        }
    }

    @Test
    public void testLoadFromDir() throws IOException {
        ArchetypeLoader loader = new ArchetypeLoader(targetDir());
        try (InputStream is = loader.loadResourceAsStream("META-INF/helidon-archetype.xml")) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = is.read()) != -1) {
                sb.append((char) c);
            }
            String s = sb.toString();
            assertThat(s.length(), is(greaterThan(0)));
            assertThat(s, containsString("<archetype-descriptor>"));
            assertThat(s, containsString("</archetype-descriptor>"));
        }
    }

    @Test
    public void testBadFile() {
        Path targetDirPath = targetDir().toPath().resolve("test.properties");
        assertThrows(IOException.class, () -> new ArchetypeLoader(targetDirPath.toFile()));
    }
}
