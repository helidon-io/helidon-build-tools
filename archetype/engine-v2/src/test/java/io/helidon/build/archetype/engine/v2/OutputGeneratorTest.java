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
import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.interpreter.ContextAST;
import io.helidon.build.archetype.engine.v2.interpreter.Flow;
import io.helidon.build.archetype.engine.v2.template.TemplateModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OutputGeneratorTest {

    private static final String GENERATOR_TEST_DIRECTORY = "outputGenerator-test-resources";
    private static Path tempDir;

    @BeforeAll
    static void bootstrap() throws IOException {
        tempDir = Files.createTempDirectory("generated");
    }

    @Test
    public void testOutputFileGenerator() throws IOException {
        Archetype archetype = getArchetype(GENERATOR_TEST_DIRECTORY);

        Flow flow = Flow.builder().archetype(archetype).startDescriptorPath("archetype.xml").build();
        flow.build(new ContextAST());
        flow.build(new ContextAST());

        OutputGenerator generator = new OutputGenerator(flow.result().get().outputs());
        //generator.generate(tempDir.toFile());
    }

    @Test
    public void testUniqueModel() {
        Archetype archetype = getArchetype(GENERATOR_TEST_DIRECTORY);

        Flow flow = Flow.builder().archetype(archetype).startDescriptorPath("archetype.xml").build();
        flow.build(new ContextAST());
        flow.build(new ContextAST());

        OutputGenerator generator = new OutputGenerator(flow.result().get().outputs());
        TemplateModel model = generator.createUniqueModel();
        Model modelDescriptor = model.model();

        assertThat(modelDescriptor.keyValues().size(), is(1));
        assertThat(modelDescriptor.keyLists().size(), is(1));
        assertThat(modelDescriptor.keyMaps().size(), is(2));
    }

    private Archetype getArchetype(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(name).getFile());
        return ArchetypeFactory.create(file);
    }

}
