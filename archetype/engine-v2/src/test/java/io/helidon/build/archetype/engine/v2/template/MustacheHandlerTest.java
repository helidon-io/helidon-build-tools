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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class MustacheHandlerTest {

    public static final String DESCRIPTOR_NAME = "template/customArchetype.xml";
    public static final String DESCRIPTOR_NAME_1 = "template/customArchetype1.xml";
    public static final String TEMPLATE_RESOURCE_NAME = "template/test.xml.mustache";

    @Test
    public void parseModel() throws IOException {
        InputStream is = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(DESCRIPTOR_NAME);
        InputStream is1 = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(DESCRIPTOR_NAME_1);

        InputStream templateStream = MustacheHandlerTest.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_RESOURCE_NAME);

        assertThat(is, is(notNullValue()));
        assertThat(is1, is(notNullValue()));
        assertThat(templateStream, is(notNullValue()));

        ArchetypeDescriptor desc = ArchetypeDescriptor.read(is);
        ArchetypeDescriptor desc1 = ArchetypeDescriptor.read(is1);

        TemplateModel model = new TemplateModel();
        model.mergeModel(desc.output().model());
        model.mergeModel(desc1.output().model());

        MustacheHandler.renderMustacheTemplate(templateStream, "test.xml", Path.of("src/test/resources/template/output.xml"), model.createScope());
    }

}
