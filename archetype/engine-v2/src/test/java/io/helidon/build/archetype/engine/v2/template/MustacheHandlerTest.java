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

package io.helidon.build.archetype.engine.v2.template;

import io.helidon.build.archetype.engine.v2.MustacheHandler;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v2.interpreter.ModelAST;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNull.notNullValue;

public class MustacheHandlerTest {

    public static final String DESCRIPTOR         = "template/templateArchetype.xml";
    public static final String DESCRIPTOR_SIBLING = "template/siblingArchetype.xml";
    public static final String TEMPLATE_RESOURCE  = "template/test.xml.mustache";
    public static final String EXPECTED_RESOURCE  = "template/expected.xml";

    public static String expected;

    @BeforeAll
    static void bootstrap() throws IOException {
        InputStream expectedStream = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(EXPECTED_RESOURCE);
        assert expectedStream != null;
        expected = new String(expectedStream.readAllBytes());
        expectedStream.close();
    }

    @Test
    public void parseModel() throws IOException {
        InputStream descStream = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(DESCRIPTOR);
        InputStream descSiblingStream = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(DESCRIPTOR_SIBLING);
        testDescriptor(descSiblingStream, descStream);
    }

    private void testDescriptor(InputStream ... descriptors) throws IOException {
        OutputStream stream = new ByteArrayOutputStream();
        TemplateModel model = new TemplateModel();
        InputStream template = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_RESOURCE);
        assertThat(template, is(notNullValue()));

        for (InputStream is : descriptors) {
            assertThat(is, is(notNullValue()));
            model.mergeModel(ModelAST.create(
                    ArchetypeDescriptor.read(is).output().model(),
                    null,
                    null)
            );
        }

        MustacheHandler.renderMustacheTemplate(template, TEMPLATE_RESOURCE, stream, model);

        assertThat(stream.toString(), containsString(expected));

        Arrays.stream(descriptors).forEach(desc -> {
            try {
                desc.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        template.close();
    }

}
