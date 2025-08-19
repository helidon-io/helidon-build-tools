/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.maven.archetype.MustacheHelper.renderMustacheTemplate;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link MustacheHelper}.
 */
class MustacheHelperTest {

    private static final Path OUTPUT_DIR = targetDir(MustacheHelperTest.class).resolve("mustache-helper-ut");

    @BeforeAll
    static void beforeAll() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    void testRender() throws IOException {
        Path target = unique(OUTPUT_DIR, "test", ".txt");
        InputStream tpl = new ByteArrayInputStream((""
                + "{{#set}}color:{{.}},{{/set}}"
                + "{{#map}}{{key}}:{{value}}{{/map}},"
                + "{{#list}}color:{{.}},{{/list}}").getBytes(UTF_8));
        Map<String, Object> scope = new HashMap<>();
        Set<String> set1 = Set.of("blue", "red");
        scope.put("set", Set.of("blue", "red"));
        scope.put("map", Map.of("color", "green"));
        scope.put("list", List.of("yellow", "orange"));
        renderMustacheTemplate(tpl, "testRender", target, scope);
        String setStr = set1.stream().map(s -> "color:" + s).collect(joining(","));
        assertThat(Files.readString(target), is(setStr + ",color:green,color:yellow,color:orange,"));
    }
}
