/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.sitegen;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rgrecour
 */
public abstract class TestHelper {

    public static final String SOURCE_DIR_PREFIX = "src/test/resources/";

    /**
     * Get the base directory path of the project.
     *
     * @return base directory path
     */
    static String getBasedirPath() {
        String basedirPath = System.getProperty("basedir");
        if (basedirPath == null) {
            basedirPath = new File("").getAbsolutePath();
        }
        return basedirPath.replace("\\","/");
    }

    /**
     * Get a file in the project.
     *
     * @param path a relative path within the project directory
     * @return the corresponding for the given path
     */
    public static File getFile(String path) {
        return new File(getBasedirPath(), path);
    }

    static void assertString(String expected, String actual, String name) {
        if (expected == null) {
            assertNull(actual, name);
        } else {
            assertNotNull(actual, name);
            assertEquals(expected, actual, name);
        }
    }

    static void assertList(int expectedSize, List list, String name) {
        assertNotNull(list, name);
        assertEquals(expectedSize, list.size(), name + ".size");
        for (int i = 0; i < expectedSize; i++) {
            assertNotNull(list.get(i), name + "[" + i + "]");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T assertType(Object actual, Class<T> expected, String name) {
        assertNotNull(actual, name);
        assertEquals(actual.getClass(), expected, name);
        T t = (T) actual;
        return t;
    }

    /**
     * Render the expected template and compare it with the given actual file.
     * The actual file must exist and be identical to the rendered template,
     * otherwise assert errors will be thrown.
     *
     * @param outputdir the output directory where to render the expected template
     * @param expectedTpl the template used for comparing the actual file
     * @param actual the rendered file to be compared
     * @throws Exception if an error occurred
     */
    public static void assertRendering(File outputdir, File expectedTpl, File actual)
            throws Exception {

        assertTrue(actual.exists(), actual.getAbsolutePath() + " does not exist");

        // render expected
        FileTemplateLoader ftl = new FileTemplateLoader(expectedTpl.getParentFile());
        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        config.setTemplateLoader(ftl);
        Template template = config.getTemplate(expectedTpl.getName());
        File expected = new File(outputdir, "expected_" + actual.getName());
        FileWriter writer = new FileWriter(expected);
        Map<String, Object> model = new HashMap<>();
        model.put("basedir", getBasedirPath());
        template.process(model, writer);

        // diff expected and rendered
        List<String> expectedLines = Files.readAllLines(expected.toPath());
        List<String> actualLines = Files.readAllLines(actual.toPath());

        // compare expected and rendered
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
        if (patch.getDeltas().size() > 0) {
            fail("rendered file " + actual.getAbsolutePath() + " differs from expected: " + patch.toString());
        }
    }
}
