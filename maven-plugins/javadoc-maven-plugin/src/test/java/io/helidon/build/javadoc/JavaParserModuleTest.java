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
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link JavaParser#module(java.io.InputStream)}.
 */
class JavaParserModuleTest {

    @Test
    void testTopLevelComments() {
        String src = """
                /*
                 * module not.com.acme1;
                 */
                module com.acme1 {}
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
    }

    @Test
    void testLeadingWhitespaces1() {
        String src = """
                    module com.acme1{}
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
    }

    @Test
    void testLeadingWhitespaces2() {
        String src = """
                    module     com.acme1{}
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
    }

    @Test
    void testTrailingWhitespaces() {
        //noinspection TrailingWhitespacesInTextBlock
        String src = """
                module com.acme1 
                    {}
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
    }

    @Test
    void testCommentedModuleDecl1() {
        String src = """
                /*
                 * module not.com.acme1{}
                 */
                                
                module com.acme1 {
                }
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
    }

    @Test
    void testCommentedModuleDecl2() {
        String src = """
                // module not.com.acme1{}
                                
                module com.acme1 {
                }
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
    }

    @Test
    void testRequires() {
        String src = """
                module com.acme1 {
                    //requires com.acme2;
                    requires com.acme3;
                    requires com.acme4;
                }
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1"));
        Requires[] requires = module.requires()
                .stream()
                .sorted(Comparator.comparing(Requires::name))
                .filter(r -> !r.modifiers().contains(Requires.Modifier.MANDATED))
                .toArray(Requires[]::new);
        assertThat(requires.length, is(2));
        assertThat(requires[0].name(), is("com.acme3"));
        assertThat(requires[1].name(), is("com.acme4"));
    }

    @Test
    void testRequiresStatic() {
        String src = """
                module com.acme1 {
                    requires static com.acme3;
                    requires com.acme4;
                }
                """;
        ModuleDescriptor module = parse(src);
        Requires[] requires = module.requires()
                .stream()
                .sorted(Comparator.comparing(Requires::name))
                .filter(r -> !r.modifiers().contains(Requires.Modifier.MANDATED))
                .toArray(Requires[]::new);
        assertThat(requires.length, is(2));
        assertThat(requires[0].name(), is("com.acme3"));
        assertThat(requires[0].modifiers(), contains(Requires.Modifier.STATIC));
        assertThat(requires[1].name(), is("com.acme4"));
        assertThat(requires[1].modifiers(), is(empty()));
    }

    @Test
    void testRequiresTransitive() {
        String src = """
                module com.acme1 {
                    requires transitive com.acme3;
                    requires com.acme4;
                }
                """;
        ModuleDescriptor module = parse(src);
        Requires[] requires = module.requires()
                .stream()
                .sorted(Comparator.comparing(Requires::name))
                .filter(r -> !r.modifiers().contains(Requires.Modifier.MANDATED))
                .toArray(Requires[]::new);
        assertThat(requires.length, is(2));
        assertThat(requires[0].name(), is("com.acme3"));
        assertThat(requires[0].modifiers(), contains(Requires.Modifier.TRANSITIVE));
        assertThat(requires[1].name(), is("com.acme4"));
        assertThat(requires[1].modifiers(), is(empty()));
    }

    @Test
    void testUses1() {
        String src = """
                module com.acme1 {
                    uses com.acme3.Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        String[] uses = module.uses().stream().sorted().toArray(String[]::new);
        assertThat(uses.length, is(1));
        assertThat(uses[0], is("com.acme3.Foo"));
    }

    @Test
    void testUses2() {
        String src = """
                module com.acme1 {
                    uses com.acme3.Foo;
                    uses com.acme3.Bar;
                }
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.uses(), containsInAnyOrder("com.acme3.Bar", "com.acme3.Foo"));
    }

    @Test
    void testUses3() {
        String src = """
                import com.acme3.Foo;
                module com.acme1 {
                    uses Foo;
                    uses com.acme3.Bar;
                }
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.uses(), containsInAnyOrder("com.acme3.Bar", "com.acme3.Foo"));
    }

    @Test
    void testOpens1() {
        String src = """
                module com.acme1 {
                    opens com.acme1.spi to com.acme2;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Opens[] opens = module.opens().toArray(ModuleDescriptor.Opens[]::new);
        assertThat(opens.length, is(1));
        assertThat(opens[0].source(), is("com.acme1.spi"));
        assertThat(opens[0].targets(), contains("com.acme2"));
    }

    @Test
    void testOpens2() {
        String src = """
                module com.acme1 {
                    opens com.acme1.spi to com.acme2,   com.acme3;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Opens[] opens = module.opens().toArray(ModuleDescriptor.Opens[]::new);
        assertThat(opens.length, is(1));
        assertThat(opens[0].source(), is("com.acme1.spi"));
        assertThat(opens[0].targets(), containsInAnyOrder("com.acme2", "com.acme3"));
    }

    @Test
    void testProvides1() {
        String src = """
                module com.acme1 {
                    provides com.acme.Service with com.acme1.Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), contains("com.acme1.Foo"));
    }

    @Test
    void testProvides2() {
        String src = """
                module com.acme1 {
                    provides com.acme.Service with com.acme1.Foo, com.acme1.Bar ;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), containsInAnyOrder("com.acme1.Foo", "com.acme1.Bar"));
    }

    @Test
    void testProvides3() {
        String src = """
                module com.acme1 {
                    provides com.acme.Service
                            with com.acme1.Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), contains("com.acme1.Foo"));
    }

    @Test
    void testProvides4() {
        String src = """
                module com.acme1 {
                    provides com.acme.Service with
                            com.acme1.Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), contains("com.acme1.Foo"));
    }

    @Test
    void testProvides5() {
        String src = """
                import com.acme1.Foo;
                module com.acme1 {
                    provides com.acme.Service with Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), contains("com.acme1.Foo"));
    }

    @Test
    void testProvides6() {
        String src = """
                import com.acme.Service;
                module com.acme1 {
                    provides Service with com.acme1.Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), contains("com.acme1.Foo"));
    }

    @Test
    void testModuleAnnotation() {
        String src = """
                @Annot(value = "Foo",
                         description = "Description",
                         in = MyEnum.BAR
                )
                module com.acme1 {
                    provides com.acme.Service with
                            com.acme1.Foo;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Provides[] provides = module.provides().toArray(ModuleDescriptor.Provides[]::new);
        assertThat(provides.length, is(1));
        assertThat(provides[0].service(), is("com.acme.Service"));
        assertThat(provides[0].providers(), contains("com.acme1.Foo"));
    }

    @Test
    void testExports() {
        String src = """
                module com.acme1 {
                    exports com.acme1;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Exports[] exports = module.exports().toArray(ModuleDescriptor.Exports[]::new);
        assertThat(exports.length, is(1));
        assertThat(exports[0].source(), is("com.acme1"));
        assertThat(exports[0].targets().size(), is(0));
    }

    @Test
    void testExports1() {
        String src = """
                module com.acme1 {
                    exports com.acme1 to com.acme2;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Exports[] exports = module.exports().toArray(ModuleDescriptor.Exports[]::new);
        assertThat(exports.length, is(1));
        assertThat(exports[0].source(), is("com.acme1"));
        assertThat(exports[0].targets(), contains("com.acme2"));
    }

    @Test
    void testExports2() {
        String src = """
                module com.acme1 {
                    exports com.acme1 to com.acme2, com.acme3;
                }
                """;
        ModuleDescriptor module = parse(src);
        ModuleDescriptor.Exports[] exports = module.exports().toArray(ModuleDescriptor.Exports[]::new);
        assertThat(exports.length, is(1));
        assertThat(exports[0].source(), is("com.acme1"));
        assertThat(exports[0].targets(), containsInAnyOrder("com.acme2", "com.acme3"));
    }

    @Test
    void testNameWithContextualKeyword() {
        String src = """
                module com.acme1.open {
                }
                """;
        ModuleDescriptor module = parse(src);
        assertThat(module.name(), is("com.acme1.open"));
    }

    private ModuleDescriptor parse(String src) {
        return JavaParser.module(new ByteArrayInputStream(src.getBytes(StandardCharsets.UTF_8)));
    }
}
