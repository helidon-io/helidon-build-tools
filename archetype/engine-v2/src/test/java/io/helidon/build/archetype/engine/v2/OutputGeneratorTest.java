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

package io.helidon.build.archetype.engine.v2;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import io.helidon.build.archetype.engine.v2.interpreter.ContextAST;
import io.helidon.build.archetype.engine.v2.interpreter.Flow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class OutputGeneratorTest {

    private static Path tempDir;
    private Archetype archetype;

    @BeforeAll
    static void bootstrap() throws IOException {
        tempDir = Files.createTempDirectory("generated");
    }

    @Test
    public void testOutputFileGenerator() throws IOException {
        List<String> expectedFiles = List.of("generatedDocker.xml", "Readme2.md", "pom.xml", "README.md");
        archetype = getArchetype();

        Flow flow = Flow.builder().archetype(archetype).startDescriptorPath("archetype.xml").build();
        flow.build(new ContextAST());
        flow.build(new ContextAST());
        OutputGenerator generator = new OutputGenerator(flow.result().get());

        generator.generate(tempDir.toFile());

        assertThat(tempDir.toFile().listFiles(), is(notNullValue()));

        List<File> resultFiles = new ArrayList<>();
        getFiles(tempDir.toFile(), resultFiles);
        List<String> generatedFiles = resultFiles.stream()
                .map(File::getName)
                .collect(Collectors.toList());

        assertThat(true, is(generatedFiles.size() == expectedFiles.size()));
        assertThat(true, is(generatedFiles.containsAll(expectedFiles)));
    }

    private void getFiles(File file, List<File> files) {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File f : listFiles) {
                getFiles(f, files);
            }
        } else {
            files.add(file);
        }
    }

    private Archetype getArchetype() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("outputGenerator-test-resources").getFile());
        archetype = ArchetypeFactory.create(file);
        return archetype;
    }

}
