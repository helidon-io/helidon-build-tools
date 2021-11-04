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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class ArchetypeDescriptorTest {

    public static final String DESCRIPTOR_RESOURCE_NAME = "schema/archetype.xml";

    @Test
    public void testUnmarshall() {
        InputStream is = ArchetypeDescriptorTest.class.getClassLoader()
                .getResourceAsStream(DESCRIPTOR_RESOURCE_NAME);

        assertThat(is, is(notNullValue()));

        Path archetypePath = Paths.get("");
        Path descriptorPath = Paths.get(DESCRIPTOR_RESOURCE_NAME);
        ArchetypeDescriptor desc = ArchetypeDescriptor.read(archetypePath, descriptorPath, is);

        assertThat(desc.descriptorPath(), is (descriptorPath));

        assertThat(desc.archetypeAttributes(), is(notNullValue()));
        assertThat(desc.archetypeAttributes().get("xmlns"), is("https://helidon.io/archetype/2.0"));

        assertThat(desc.contexts().size(), is(1));

        Context context = desc.contexts().getLast();

        assertThat(context.nodes().size(), is(4));
        assertThat(context.nodes().getFirst().path(), is("test.option1"));

        ContextList list = (ContextList) context.nodes().get(1);

        assertThat(list.values().size(), is(1));
        assertThat(list.values().getFirst(), is("hello"));

        Step step = desc.steps().getLast();

        assertThat(step.label(), is("A Step Title"));
        assertThat(step.help(), is("help message"));

        Input input = step.inputs().getLast();
        InputText text = (InputText) input.nodes().getLast();

        assertThat(text.label(), is("Some text input"));
        assertThat(text.name(), is("some-input"));

        input = desc.inputs().getFirst();
        Exec exec = input.execs().getFirst();

        assertThat(exec.src(), is("test.xml"));
        assertThat(exec.url(), is(nullValue()));

        InputList inputList = (InputList) input.steps().getFirst().inputs().getFirst().nodes().get(3);

        assertThat(inputList.min(), is("0"));
        assertThat(inputList.max(), is("100"));
        assertThat(inputList.name(), is("array1"));

        Option option = inputList.options().getFirst();

        assertThat(option.label(), is("Foo"));
        assertThat(option.value(), is("foo"));

        Source source = desc.sources().getFirst();

        assertThat(source.source(), is("dir1/dir2/file.xml"));

        Output output = desc.output();

        assertThat(output.ifProperties(), is("something"));

        Model model = output.model();

        assertThat(model.keyValues().getFirst().order(), is(100));
        assertThat(model.keyMaps().getFirst().key(), is("foo"));
        assertThat(model.keyMaps().getFirst().keyValues().getFirst().key(), is("first"));
        assertThat(model.keyMaps().getFirst().keyLists().getFirst().key(), is("second"));
        assertThat(model.keyMaps().getLast().keyMaps().getLast().keyLists().getLast().maps().getLast().keyValues().getFirst().value(),
                is("io.helidon.build-tools"));

        ModelKeyValue execution = model.keyMaps().getLast().keyMaps().getLast().keyLists().getLast().maps().getLast()
                .keyMaps().getLast().keyMaps().getLast().keyValues().getLast();
        assertThat(execution.key(), is("id"));
        assertThat(execution.value(), is("third-party-license-report"));

        Transformation transformation = output.transformations().getFirst();

        assertThat(transformation.id(), is("t1"));
        assertThat(transformation.replacements().getFirst().regex(), is("foo"));
        assertThat(transformation.replacements().getFirst().replacement(), is("token"));

        FileSets fileList = output.filesList().getFirst();

        assertThat(fileList.directory().get(), is("files"));
        assertThat(fileList.transformations().getFirst(), is("t1"));

        Templates templates = output.templates().getFirst();

        assertThat(templates.engine(), is("mustache"));
        assertThat(templates.includes().getFirst(), is("**/*.foo"));
    }
}
