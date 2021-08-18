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

package io.helidon.build.archetype.engine.v2.template.mustache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.MustacheTemplateEngine;
import io.helidon.build.archetype.engine.v2.TemplateEngine;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class MustacheTemplateEngineTest {

    @Test
    void testRender() throws IOException {
        String templateFileName = "test.template";
        File templateFile = new File(getClass().getClassLoader().getResource(templateFileName).getFile());
        Map<String, String> scope = new HashMap<>();
        scope.put("title", "title content");
        scope.put("name", "name content");

        try (
                OutputStream stream = new ByteArrayOutputStream();
                InputStream template = new FileInputStream(templateFile)
        ) {
            TemplateEngine engine = new MustacheTemplateEngine();
            engine.render(template, templateFileName, StandardCharsets.UTF_8, stream, scope);

            assertThat(stream.toString(), containsString("title content"));
            assertThat(stream.toString(), containsString("name content"));
        }
    }

    @Test
    void testAllEngines() {
        List<TemplateEngine> engines = TemplateEngine.allEngines();

        assertThat(engines.stream().filter(engine -> engine instanceof MustacheTemplateEngine).findAny().orElse(null),
                notNullValue());
    }

    @Test
    void testGetEngineByName() {
        assertThat(TemplateEngine.getEngineByName("mustache").orElse(null), instanceOf(MustacheTemplateEngine.class));
    }

    @Test
    void testName() {
        assertThat(new MustacheTemplateEngine().name(), is("mustache"));
    }
}
