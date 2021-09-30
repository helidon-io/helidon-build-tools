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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.acme.TestClass2;
import com.acme.TestClassWithNestedClass;
import com.acme.TestClassWithParam1;
import io.helidon.build.cli.codegen.AST.ArgumentDeclaration;
import io.helidon.build.cli.codegen.AST.ClassBody;
import io.helidon.build.cli.codegen.AST.ClassDeclaration;
import io.helidon.build.cli.codegen.AST.ConstructorDeclaration;
import io.helidon.build.cli.codegen.AST.ConstructorInvocation;
import io.helidon.build.cli.codegen.AST.FieldDeclaration;
import io.helidon.build.cli.codegen.AST.Invocation.Style;
import io.helidon.build.cli.codegen.AST.MethodBody;
import io.helidon.build.cli.codegen.AST.MethodDeclaration;
import io.helidon.build.cli.codegen.AST.MethodInvocation;
import io.helidon.build.cli.codegen.TypeInfo.CompositeTypeInfo;

import com.acme.TestClass1;
import io.helidon.build.util.Unchecked.CheckedConsumer;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.codegen.AST.Modifier.*;
import static io.helidon.build.cli.codegen.AST.Values.*;
import static io.helidon.build.cli.codegen.AST.Refs.*;
import static io.helidon.build.cli.codegen.AST.Invocations.*;
import static io.helidon.build.cli.codegen.AST.Statements.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link ASTWriter}.
 */
class ASTWriterTest {

    private static String wtos(CheckedConsumer<ASTWriter, IOException> consumer) throws IOException {
        StringWriter sw = new StringWriter();
        ASTWriter scw = new ASTWriter(sw);
        consumer.accept(scw);
        return sw.toString();
    }

    @Test
    void testWriteValueWithBooleanLiteral() throws IOException {
        assertThat(wtos(w -> w.writeValue(booleanLiteral(true))), is("true"));
        assertThat(wtos(w -> w.writeValue(booleanLiteral(false))), is("false"));
    }

    @Test
    void testWriteValueWithNullLiteral() throws IOException {
        assertThat(wtos(w -> w.writeValue(nullLiteral())), is("null"));
    }

    @Test
    void testWriteValueWithArrayLiteral() throws IOException {
        assertThat(wtos(w -> w.writeValue(arrayLiteral(TestClass2[].class))), is("new TestClass2[0]"));
        assertThat(wtos(w -> w.writeValue(arrayLiteral(TestClass2[].class, nullLiteral(), nullLiteral()))),
                is("new TestClass2[]{\n        null,\n        null\n}"));
    }

    @Test
    void testWriteValueWithClassLiteral() throws IOException {
        assertThat(wtos(w -> w.writeValue((classLiteral(TypeInfo.of(TestClass1.class))))), is("TestClass1.class"));
    }

    @Test
    void testWriteValueWithValueCast() throws IOException {
        assertThat(wtos(w -> w.writeValue(valueCast(TypeInfo.of(TestClass1.class), stringLiteral("bar")))),
                is("(TestClass1) \"bar\""));
    }

    @Test
    void testWriteValueWithValueCastParameterized() throws IOException {
        CompositeTypeInfo typeWithParams = TypeInfo.of(TypeInfo.of("com.acme.Bob", false),
                TypeInfo.of("com.acme.Alice", false));
        assertThat(wtos(w -> w.writeValue(valueCast(typeWithParams, stringLiteral("bar")))),
                is("(Bob<Alice>) \"bar\""));
    }

    @Test
    void testWriteValueWithArrayValueRef() throws IOException {
        assertThat(wtos(w -> w.writeValue(arrayValueRef("bars", 0))), is("bars[0]"));
    }

    @Test
    void testWriteValueWithValueRef() throws IOException {
        assertThat(wtos(w -> w.writeValue(valueRef("foo"))), is("foo"));
    }

    @Test
    void testWriteValueWithValueStaticRef() throws IOException {
        assertThat(wtos(w -> w.writeValue(valueRef(staticRef(TestClass1.class), "BAR"))), is("TestClass1.BAR"));
    }

    @Test
    void testWriteValueWithConstructorInvocation() throws IOException {
        assertThat(wtos(w -> w.writeValue(constructorInvocation(TypeInfo.of(TestClass1.class), booleanLiteral(true)))),
                is("new TestClass1(true)"));
    }

    @Test
    void testWriteValueWithMethodInvocation() throws IOException {
        assertThat(wtos(w -> w.writeValue(methodInvocation("foo", booleanLiteral(true)))),
                is("foo(true)"));
    }

    @Test
    void testWriteRefWithValueRef() throws IOException {
        assertThat(wtos(w -> w.writeRef(valueRef("foo"))), is("foo"));
        assertThat(wtos(w -> w.writeRef(valueRef(valueRef("foo"), "bar"))), is("foo.bar"));
    }

    @Test
    void testWriteRefWithRefCast() throws IOException {
        assertThat(wtos(w -> w.writeRef(refCast(TestClass1.class, valueRef("foo")))), is("((TestClass1) foo)"));
        assertThat(wtos(w -> w.writeRef(valueRef(refCast(TestClass1.class, valueRef("bar")), "foo"))),
                is("((TestClass1) bar).foo"));
        assertThat(wtos(w -> w.writeRef(valueRef(refCast(TestClass1.class, arrayValueRef("bar", 0)), "foo"))),
                is("((TestClass1) bar[0]).foo"));
    }

    @Test
    void testWriteRefWithValueStaticRef() throws IOException {
        assertThat(wtos(w -> w.writeRef(valueRef(staticRef(TestClass1.class), "BAR"))), is("TestClass1.BAR"));
    }

    @Test
    void testWriteStatementWithReturnStatement() throws IOException {
        assertThat(wtos(w -> w.writeStatement(returnStatement())), is("return;\n"));
        assertThat(wtos(w -> w.writeStatement(returnStatement(valueCast(TestClass1.class, valueRef("bar"))))),
                is("return (TestClass1) bar;\n"));
    }

    @Test
    void testWriteStatementWithSuperStatement() throws IOException {
        assertThat(wtos(w -> w.writeStatement(superStatement())), is("super();\n"));
        assertThat(wtos(w -> w.writeStatement(superStatement(valueCast(TestClass1.class, valueRef("bar")), valueRef("foo")))),
                is("super((TestClass1) bar, foo);\n"));
    }

    @Test
    void testWriteStatementWithConstructorInvocation() throws IOException {
        assertThat(wtos(w -> w.writeStatement(constructorInvocation(TestClass1.class))), is("new TestClass1();\n"));
        assertThat(wtos(w -> w.writeStatement(constructorInvocation(TestClass1.class, valueRef("foo"), valueRef("bar")))),
                is("new TestClass1(foo, bar);\n"));
    }

    @Test
    void testWriteStatementWithMethodInvocation() throws IOException {
        assertThat(wtos(w -> w.writeStatement(methodInvocation("foo"))), is("foo();\n"));
        assertThat(wtos(w -> w.writeStatement(methodInvocation("foo", valueRef("bob"), valueRef("alice")))),
                is("foo(bob, alice);\n"));
    }

    @Test
    void testWriteInvocationWithConstructorInvocation() throws IOException {
        assertThat(wtos(w -> w.writeInvocation(constructorInvocation(TestClass1.class))), is("new TestClass1()"));
        assertThat(wtos(w -> w.writeInvocation(constructorInvocation(TestClass1.class, valueRef("foo"), valueRef("bar")))),
                is("new TestClass1(foo, bar)"));
    }

    @Test
    void testWriteInvocationWithMultiLineArgs() throws IOException {
        assertThat(wtos(w -> w.writeInvocation(ConstructorInvocation
                        .builder()
                        .style(Style.MULTI_LINE)
                        .type(TestClass1.class)
                        .args(valueRef("foo"), valueRef("bar"))
                        .build())),
                is("new TestClass1(\n        foo,\n        bar)"));
    }

    @Test
    void testWriteInvocationWithParameterizedConstructorInvocation() throws IOException {
        assertThat(wtos(w -> w.writeInvocation(constructorInvocation(TestClassWithParam1.class))),
                is("new TestClassWithParam1<>()"));
    }

    @Test
    void testWriteInvocationWithMethodInvocation() throws IOException {
        assertThat(wtos(w -> w.writeInvocation(methodInvocation("foo"))), is("foo()"));
        assertThat(wtos(w -> w.writeInvocation(methodInvocation("foo", valueRef("bob"), valueRef("alice")))),
                is("foo(bob, alice)"));
        assertThat(wtos(w -> w.writeInvocation(
                MethodInvocation
                        .builder()
                        .method(valueRef(staticRef(System.class), "out"), "println")
                        .arg(stringLiteral("Hello World!"))
                        .build())),
                is("System.out.println(\"Hello World!\")"));
    }

    @Test
    void testWriteConstructorInvocationWithNestedClass() throws IOException {
        assertThat(wtos(w -> w.writeInvocation(constructorInvocation(TestClassWithNestedClass.NestedClass.class))),
                is("new NestedClass()"));
    }

    @Test
    void testWriteArgumentDeclarationWithNestedClass() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ArgumentDeclaration
                        .builder()
                        .type(TestClassWithNestedClass.NestedClass.class)
                        .name("arg")
                        .build())),
                is("NestedClass arg"));
    }

    @Test
    void testWriteFieldDeclarationWithAnnotation() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(FieldDeclaration
                        .builder()
                        .annotation(Override.class)
                        .annotation(SuppressWarnings.class, stringLiteral("unchecked"))
                        .type(String.class)
                        .name("foo")
                        .build())),
                is("@Override\n"
                        + "@SuppressWarnings(\"unchecked\")\n"
                        + "String foo;\n"));
    }

    @Test
    void testWriteFieldDeclarationWithNestedClass() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                FieldDeclaration
                        .builder()
                        .type(TestClassWithNestedClass.NestedClass.class)
                        .name("field")
                        .build())),
                is("NestedClass field;\n"));
    }

    @Test
    void testWriteFieldDeclaration1() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                FieldDeclaration
                        .builder()
                        .javadoc("Singleton instance.")
                        .modifiers(PUBLIC, STATIC, FINAL)
                        .type(TestClass1.class)
                        .name("INSTANCE")
                        .value(constructorInvocation(TestClass1.class))
                        .build())),
                is("/**\n"
                        + " * Singleton instance.\n"
                        + " */\n"
                        + "public static final TestClass1 INSTANCE = new TestClass1();\n"));
    }

    @Test
    void testWriteFieldDeclaration2() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                FieldDeclaration
                        .builder()
                        .javadoc("Bars.")
                        .modifiers(PUBLIC, STATIC, FINAL)
                        .type(TestClass1[].class)
                        .name("BARS")
                        .value(arrayLiteral(TestClass1[].class,
                                constructorInvocation(TestClass1.class, booleanLiteral(true)),
                                constructorInvocation(TestClass1.class, booleanLiteral(false))))
                        .build())),
                is("/**\n"
                        + " * Bars.\n"
                        + " */\n"
                        + "public static final TestClass1[] BARS = new TestClass1[]{\n"
                        + "        new TestClass1(true),\n"
                        + "        new TestClass1(false)\n"
                        + "};\n"));
    }

    @Test
    void testWriteMethodDeclarationWithNestedClass() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                MethodDeclaration
                        .builder()
                        .returnType(TestClassWithNestedClass.NestedClass.class)
                        .name("foobar")
                        .build())),
                is("\n"
                        + "NestedClass foobar() {\n"
                        + "}\n"));
    }

    @Test
    void testWriteMethodDeclaration1() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                MethodDeclaration
                        .builder()
                        .name("foobar")
                        .build())),
                is("\n"
                        + "void foobar() {\n"
                        + "}\n"));
    }

    @Test
    void testWriteMethodDeclaration2() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                MethodDeclaration
                        .builder()
                        .annotation(Override.class)
                        .javadoc("Foobar method.")
                        .modifiers(PUBLIC)
                        .returnType(TestClass1.class)
                        .name("foobar")
                        .arg(TestClass1.class, "foo")
                        .arg(TestClass2.class, "bar")
                        .body(MethodBody
                                .builder()
                                .statement(MethodInvocation
                                        .builder()
                                        .method(valueRef(staticRef(System.class), "out"), "println")
                                        .arg(stringLiteral("Hello World!"))))
                        .build())),
                is("\n"
                        + "/**\n"
                        + " * Foobar method.\n"
                        + " */\n"
                        + "@Override\n"
                        + "public TestClass1 foobar(TestClass1 foo, TestClass2 bar) {\n"
                        + "    System.out.println(\"Hello World!\");\n"
                        + "}\n"));
    }

    @Test
    void testWriteConstructorDeclaration1() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ConstructorDeclaration
                        .builder()
                        .type(TypeInfo.of("com.acme.Bob", false))
                        .body(MethodBody.builder().statement(superStatement()))
                        .build())),
                is("\n"
                        + "Bob() {\n"
                        + "    super();\n"
                        + "}\n"));
    }

    @Test
    void testWriteConstructorDeclaration2() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ConstructorDeclaration
                        .builder()
                        .javadoc("Create a new Bob instance.")
                        .modifiers(PUBLIC)
                        .type(TypeInfo.of("com.acme.Bob", false))
                        .arg(TestClass1.class, "foo")
                        .arg(TestClass2.class, "bar")
                        .body(MethodBody
                                .builder()
                                .statement(superStatement(valueRef("foo"), valueRef("bar")))
                                .statement(methodInvocation("foobar")))
                        .build())),
                is("\n"
                        + "/**\n"
                        + " * Create a new Bob instance.\n"
                        + " */\n"
                        + "public Bob(TestClass1 foo, TestClass2 bar) {\n"
                        + "    super(foo, bar);\n"
                        + "    foobar();\n"
                        + "}\n"));
    }

    @Test
    void testWriteClassDeclarationWithSuperNestedClass() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ClassDeclaration
                        .builder()
                        .type(TypeInfo.of("com.acme.Bob", false))
                        .superClass(TestClassWithNestedClass.NestedClass.class)
                        .build())),
                is("\n"
                        + "class Bob extends NestedClass {\n"
                        + "}\n"));
    }

    @Test
    void testWriteClassDeclaration1() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ClassDeclaration
                        .builder()
                        .type(TypeInfo.of("com.acme.Bob", false))
                        .build())),
                is("\n"
                        + "class Bob {\n"
                        + "}\n"));
    }

    @Test
    void testWriteClassDeclaration2() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ClassDeclaration
                        .builder()
                        .javadoc("The Bob class.")
                        .type(TypeInfo.of("com.acme.Bob", false))
                        .superClass(TestClass1.class)
                        .body(ClassBody
                                .builder()
                                .constructor(ConstructorDeclaration
                                        .builder()
                                        .body(MethodBody
                                                .builder()
                                                .statement(superStatement()))))
                        .build())),
                is("\n"
                        + "/**\n"
                        + " * The Bob class.\n"
                        + " */\n"
                        + "class Bob extends TestClass1 {\n"
                        + "\n"
                        + "    Bob() {\n"
                        + "        super();\n"
                        + "    }\n"
                        + "}\n"));
    }

    @Test
    void testWriteClassDeclaration3() throws IOException {
        assertThat(wtos(w -> w.writeDeclaration(
                ClassDeclaration
                        .builder()
                        .javadoc("The Alice class.")
                        .type(TypeInfo.of("com.acme.Alice", false))
                        .superClass(TestClass1.class)
                        .body(ClassBody
                                .builder()
                                .field(FieldDeclaration
                                        .builder()
                                        .javadoc("Foo singleton instance.")
                                        .modifiers(DEFAULT, STATIC, FINAL)
                                        .type(TestClass1.class)
                                        .name("FOO")
                                        .value(constructorInvocation(TestClass1.class)))
                                .field(FieldDeclaration
                                        .builder()
                                        .javadoc("Bar singleton instance.")
                                        .modifiers(PRIVATE, STATIC, FINAL)
                                        .type(TestClass2.class)
                                        .name("BAR")
                                        .value(constructorInvocation(TestClass2.class)))
                                .constructor(ConstructorDeclaration
                                        .builder()
                                        .modifiers(PRIVATE)
                                        .body(MethodBody
                                                .builder()
                                                .statement(superStatement())))
                                .method(MethodDeclaration
                                        .builder()
                                        .annotation(Override.class)
                                        .modifiers(PUBLIC)
                                        .name("doWork")
                                        .arg(String.class, "name")
                                        .body(MethodBody
                                                .builder()
                                                .statement(MethodInvocation
                                                        .builder()
                                                        .method(valueRef(staticRef(System.class), "out"), "println")
                                                        .arg(stringLiteral("Hello World!"))))))
                        .build())),
                is("\n"
                        + "/**\n"
                        + " * The Alice class.\n"
                        + " */\n"
                        + "class Alice extends TestClass1 {\n"
                        + "\n"
                        + "    /**\n"
                        + "     * Foo singleton instance.\n"
                        + "     */\n"
                        + "    static final TestClass1 FOO = new TestClass1();\n"
                        + "\n"
                        + "    /**\n"
                        + "     * Bar singleton instance.\n"
                        + "     */\n"
                        + "    private static final TestClass2 BAR = new TestClass2();\n"
                        + "\n"
                        + "    private Alice() {\n"
                        + "        super();\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void doWork(String name) {\n"
                        + "        System.out.println(\"Hello World!\");\n"
                        + "    }\n"
                        + "}\n"));
    }

    @Test
    void testWrite() throws IOException {
        TypeInfo joeType = TypeInfo.of("com.example.Joe", false);
        TypeInfo aliceType = TypeInfo.of("com.example.Alice", false);
        assertThat(wtos(w -> w.write(ClassDeclaration
                        .builder()
                        .type(TypeInfo.of("com.acme.Bob", false))
                        .superClass(TypeInfo.of(HashMap.class, String.class, String.class))
                        .body(ClassBody
                                .builder()
                                .field(FieldDeclaration
                                        .builder()
                                        .javadoc("Foo singleton instance.")
                                        .modifiers(PUBLIC, STATIC, FINAL)
                                        .type(TestClass1.class)
                                        .name("FOO")
                                        .value(constructorInvocation(TestClass1.class)))
                                .field(FieldDeclaration
                                        .builder()
                                        .modifiers(PRIVATE, STATIC, FINAL)
                                        .type(TestClass2.class)
                                        .name("BAR")
                                        .value(constructorInvocation(TestClass2.class)))
                                .field(FieldDeclaration
                                        .builder()
                                        .modifiers(PRIVATE, STATIC, FINAL)
                                        .type(String.class)
                                        .name("FOOBAR")
                                        .value(stringLiteral("foobar")))
                                .field(FieldDeclaration
                                        .builder()
                                        .modifiers(FINAL)
                                        .type(joeType)
                                        .name("joe")
                                        .value(constructorInvocation(joeType)))
                                .field(FieldDeclaration
                                        .builder()
                                        .modifiers(PRIVATE, FINAL)
                                        .type(aliceType)
                                        .name("alice")
                                        .value(constructorInvocation(aliceType)))
                                .constructor(ConstructorDeclaration
                                        .builder()
                                        .arg(TypeInfo.of(Map.class, String.class, String.class), "map")))
                        .build())),
                is("package com.acme;\n"
                        + "\n"
                        + "import java.util.HashMap;\n"
                        + "import java.util.Map;\n"
                        + "\n"
                        + "import com.example.Alice;\n"
                        + "import com.example.Joe;\n"
                        + "\n"
                        + "class Bob extends HashMap<String, String> {\n"
                        + "\n"
                        + "    /**\n"
                        + "     * Foo singleton instance.\n"
                        + "     */\n"
                        + "    public static final TestClass1 FOO = new TestClass1();\n"
                        + "\n"
                        + "    private static final TestClass2 BAR = new TestClass2();\n"
                        + "    private static final String FOOBAR = \"foobar\";\n"
                        + "\n"
                        + "    final Joe joe = new Joe();\n"
                        + "    private final Alice alice = new Alice();\n"
                        + "\n"
                        + "    Bob(Map<String, String> map) {\n"
                        + "    }\n"
                        + "}\n"));
    }
}
