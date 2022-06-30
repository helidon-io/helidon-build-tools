/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.test.utils.TestFiles;
import io.helidon.build.maven.sitegen.models.PageFilter;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link BasicBackend}.
 */
public class BasicBackendTest {

    private static final Path OUTPUT_DIR = targetDir(BasicBackendTest.class).resolve("basic");

    @Test
    public void testBasic1() throws Exception {
        Path sourceDir = targetDir(BasicBackendTest.class).resolve("test-classes/basic1");

        Site.builder()
            .page(PageFilter.builder().includes("**/*.adoc").excludes("**/_*"))
            .build()
            .generate(sourceDir, OUTPUT_DIR);

        assertRendering(sourceDir.resolve("_expected.ftl"), OUTPUT_DIR.resolve("basic.html"));
    }

    @Test
    public void testBasic2() throws Exception {
        Path sourceDir = targetDir(BasicBackendTest.class).resolve("test-classes/basic2");

        Site.builder()
            .page(PageFilter.builder().includes("**/*.adoc").excludes("**/_*"))
            .build()
            .generate(sourceDir, OUTPUT_DIR);

        assertRendering(sourceDir.resolve("_expected.ftl"), OUTPUT_DIR.resolve("example-manual.html"));
    }

    @Test
    public void testBasic3() throws Exception {
        Path sourceDir = targetDir(BasicBackendTest.class).resolve("test-classes/basic3");

        Site.builder()
            .page(PageFilter.builder().includes("**/*.adoc").excludes("**/_*"))
            .build()
            .generate(sourceDir, OUTPUT_DIR);

        assertRendering(sourceDir.resolve("_expected.ftl"), OUTPUT_DIR.resolve("passthrough.html"));
    }

    private static void assertRendering(Path expectedTpl, Path actual) throws Exception {

        assertThat(Files.exists(actual), is(true));

        // render expected
        FileTemplateLoader ftl = new FileTemplateLoader(expectedTpl.getParent().toFile());
        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        config.setTemplateLoader(ftl);
        Template template = config.getTemplate(expectedTpl.getFileName().toString());
        Path expected = OUTPUT_DIR.resolve("expected_" + actual.getFileName());
        Map<String, Object> model = new HashMap<>();
        model.put("basedir", TestFiles.pathOf(Path.of("").toAbsolutePath()));
        template.process(model, Files.newBufferedWriter(expected));

        // diff expected and rendered
        List<String> expectedLines = Files.readAllLines(expected);
        List<String> actualLines = Files.readAllLines(actual);

        // compare expected and rendered
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
        if (patch.getDeltas().size() > 0) {
            fail("rendered file " + actual.toAbsolutePath() + " differs from expected: " + patch);
        }
    }
}
