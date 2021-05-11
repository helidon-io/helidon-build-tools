/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 *
 * @author rgrecour
 */
public class BasicBackendTest {

    private static final File OUTPUT_DIR = TestHelper.getFile("target/basic-backend-test");

    @Test
    public void testBasic1() throws Exception {
        File sourcedir = TestHelper.getFile(TestHelper.SOURCE_DIR_PREFIX + "testbasic1");
        Site.builder()
                .pages(List.of(SourcePathFilter.builder()
                        .includes(List.of("**/*.adoc"))
                        .excludes(List.of("**/_*"))
                        .build()))
                .build()
                .generate(sourcedir, OUTPUT_DIR);
        TestHelper.assertRendering(
                OUTPUT_DIR,
                new File(sourcedir, "_expected.ftl"),
                new File(OUTPUT_DIR, "basic.html"));
    }

    @Test
    public void testBasic2() throws Exception {
        File sourcedir = TestHelper.getFile(TestHelper.SOURCE_DIR_PREFIX + "testbasic2");
        Site.builder()
                .pages(List.of(SourcePathFilter.builder()
                        .includes(List.of("**/*.adoc"))
                        .excludes(List.of("**/_*"))
                        .build()))
                .build()
                .generate(sourcedir, OUTPUT_DIR);
        TestHelper.assertRendering(
                OUTPUT_DIR,
                new File(sourcedir, "_expected.ftl"),
                new File(OUTPUT_DIR, "example-manual.html"));
    }

    @Test
    public void testBasic3() throws Exception {
        File sourcedir = TestHelper.getFile(TestHelper.SOURCE_DIR_PREFIX + "testbasic3");
        Site.builder()
                .pages(List.of(SourcePathFilter.builder()
                        .includes(List.of("**/*.adoc"))
                        .excludes(List.of("**/_*"))
                        .build()))
                .build()
                .generate(sourcedir, OUTPUT_DIR);
        TestHelper.assertRendering(
                OUTPUT_DIR,
                new File(sourcedir, "_expected.ftl"),
                new File(OUTPUT_DIR, "passthrough.html"));
    }
}
