/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;

import io.helidon.build.common.test.utils.WorkDirExtension;
import io.helidon.build.maven.sitegen.models.PageFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.build.common.FileUtils.location;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests links.
 */
@ExtendWith(WorkDirExtension.class)
class LinksTest {

    @Test
    void testLinks(Path workDir) throws DiffException, IOException {
        Path sourceDir = location(VuetifyBackendTest.class).resolve("links");

        Site.builder()
            .page(PageFilter.builder().includes("**/*.adoc"))
            .backend(VuetifyBackend.builder().home("index.adoc"))
            .build()
            .generate(sourceDir, workDir);

        Path actual = workDir.resolve("pages/index.js");
        assertThat(Files.exists(actual), is(true));
        assertRendering(actual, sourceDir.resolve("expected"));
    }

    private static void assertRendering(Path actual, Path expected) throws IOException, DiffException {
        Patch<String> patch = DiffUtils.diff(
                Files.readAllLines(expected),
                Files.readAllLines(actual));
        if (patch.getDeltas().size() > 0) {
            fail("rendered file " + actual.toAbsolutePath() + " differs from expected: " + patch);
        }
    }
}
