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
package io.helidon.build.cli.codegen;

import java.io.IOException;
import java.util.List;

import io.helidon.build.cli.codegen.CompilerHelper.JavaSourceFromString;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.Strings.normalizeNewLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link FileHeaderJavacPlugin}.
 */
final class FileHeaderJavacPluginTest {

    private static final List<String> COMPILER_OPTS = List.of(
            "-implicit:class",
            "-Xlint:unchecked",
            "-Xplugin:file-header",
            "-Werror");

    @Test
    public void testHeader1() throws IOException {
        String header = ""
                + "/*\n"
                + " * line 1\n"
                + " * line2\n"
                + " */\n";
        CompilerHelper compiler = new CompilerHelper(null, COMPILER_OPTS,
                new JavaSourceFromString("HeaderTest1", ""
                        + header
                        + "package com.acme.test;\n"
                        + "class HeaderTest1 {\n"
                        + "}"));
        assertThat(compiler.call(true), is(true));
        assertThat(FileHeaderJavacPlugin.header("com.acme.test.HeaderTest1"), is(normalizeNewLines(header)));
    }

    @Test
    public void testHeader2() throws IOException {
        String header = ""
                + "/*\n"
                + " * line 1\n"
                + " * line2\n"
                + " */\n";
        CompilerHelper compiler = new CompilerHelper(null, COMPILER_OPTS,
                new JavaSourceFromString("HeaderTest2", ""
                        + header
                        + "package com.acme.test;\n"
                        + "class HeaderTest2 {\n"
                        + "    static final class Nested {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(true));
        assertThat(FileHeaderJavacPlugin.header("com.acme.test.HeaderTest2.Nested"),
                is(normalizeNewLines(header)));
    }
}
