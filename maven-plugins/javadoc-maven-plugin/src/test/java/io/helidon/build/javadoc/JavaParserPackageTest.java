/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.javadoc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link JavaParser#packge(java.io.InputStream)}.
 */
@SuppressWarnings("TrailingWhitespacesInTextBlock")
class JavaParserPackageTest {

    @Test
    void testTopLevelComments() {
        String src = """
                /*
                 * package not.com.acme1;
                 */
                package com.acme1;
                """;
        String pkg = parse(src);
        assertThat(pkg, is("com.acme1"));
    }

    @Test
    void testLeadingWhitespaces1() {
        String src = """
                    package com.acme1;
                """;
        String pkg = parse(src);
        assertThat(pkg, is("com.acme1"));
    }

    @Test
    void testLeadingWhitespaces2() {
        String src = """
                    package     com.acme1;
                """;
        String pkg = parse(src);
        assertThat(pkg, is("com.acme1"));
    }

    @Test
    void testTrailingWhitespaces() {
        String src = """
                package com.acme1 
                    ;
                """;
        String pkg = parse(src);
        assertThat(pkg, is("com.acme1"));
    }

    @Test
    void testNoPackageDecl1() {
        String src = """
                /*
                 * package not.com.acme1;
                 */
                
                public class Acme1 {
                }
                """;
        String pkg = parse(src);
        assertThat(pkg, is(""));
    }

    @Test
    void testNoPackageDecl2() {
        String src = """
                /*
                 * package not.com.acme1;
                 */
                
                public interface Acme1 {
                }
                """;
        String pkg = parse(src);
        assertThat(pkg, is(""));
    }

    @Test
    void testNoPackageDecl3() {
        String src = """
                /*
                 * package not.com.acme1;
                 */
                
                public enum Acme1 {
                }
                """;
        String pkg = parse(src);
        assertThat(pkg, is(""));
    }

    @Test
    void testCommentBeforeSemiColon() {
        String src = """
                package com.acme1 /* foo */ ;
                public enum Acme1 {
                }
                """;
        String pkg = parse(src);
        assertThat(pkg, is("com.acme1"));
    }

    @Test
    void testUnamedClassWithImports() {
        String src = """
                import com.acme1;
                public enum Acme1 {
                }
                """;
        String pkg = parse(src);
        assertThat(pkg, is(""));
    }

    private String parse(String src) {
        return JavaParser.packge(new ByteArrayInputStream(src.getBytes(StandardCharsets.UTF_8)));
    }
}
