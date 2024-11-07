/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

import com.acme.TestClassWithInterface;
import com.acme.TestClass1;
import com.acme.TestClassWithNestedClass;
import com.acme.TestInterface;
import com.acme.TestClassWithSuperClass;
import com.acme.TestClassWithParam1;
import com.acme.TestClassWithParam2;
import com.acme.TestEnum;
import com.acme.TestAnnotation;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link TypeInfo}.
 */
class TypeInfoTest {

    @Test
    void testPseudoType() {
        TypeInfo type;

        type = TypeInfo.of("com.acme.Foo", false);
        assertThat(type.qualifiedName(), is("com.acme.Foo"));
        assertThat(type.simpleName(), is("Foo"));
        assertThat(type.pkg(), is("com.acme"));

        type = TypeInfo.of("Foo", false);
        assertThat(type.qualifiedName(), is("Foo"));
        assertThat(type.simpleName(), is("Foo"));
        assertThat(type.pkg(), is(""));
    }

    @Test
    void testIsArray() {
        TypeInfo type;

        type = TypeInfo.of("[Lcom.acme.Foo;", false);
        assertThat(type.qualifiedName(), is("[Lcom.acme.Foo;"));
        assertThat(type.simpleName(), is("Foo[]"));
        assertThat(type.typeName(), is("com.acme.Foo[]"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(true));

        type = TypeInfo.of("[Lcom.acme.TestClass1;", true);
        assertThat(type.qualifiedName(), is("[Lcom.acme.TestClass1;"));
        assertThat(type.simpleName(), is("TestClass1[]"));
        assertThat(type.typeName(), is("com.acme.TestClass1[]"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(true));

        type = TypeInfo.of(TestClass1[].class);
        assertThat(type.qualifiedName(), is("[Lcom.acme.TestClass1;"));
        assertThat(type.simpleName(), is("TestClass1[]"));
        assertThat(type.typeName(), is("com.acme.TestClass1[]"));
        assertThat(TestClass1[].class.getName(), is(type.qualifiedName()));
        assertThat(TestClass1[].class.getSimpleName(), is(type.simpleName()));
        assertThat(TestClass1[].class.getTypeName(), is(type.typeName()));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(true));
    }

    @Test
    void testClass1() {
        TypeInfo type = TypeInfo.of(TestClass1.class);
        assertThat(type.qualifiedName(), is("com.acme.TestClass1"));
        assertThat(type.simpleName(), is("TestClass1"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(false));
        assertThat(type == TypeInfo.of(TestClass1.class), is(true));
    }

    @Test
    void testClassWithSuperClass() {
        TypeInfo type = TypeInfo.of(TestClassWithSuperClass.class);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithSuperClass"));
        assertThat(type.simpleName(), is("TestClassWithSuperClass"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.superClass().orElse(null), is(TypeInfo.of(TestClass1.class)));
        assertThat(type == TypeInfo.of(TestClassWithSuperClass.class), is(true));
    }

    @Test
    void testClassWithInterface() {
        TypeInfo type = TypeInfo.of(TestClassWithInterface.class);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithInterface"));
        assertThat(type.simpleName(), is("TestClassWithInterface"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(false));
        assertThat(type.interfaces(), arrayContaining(TypeInfo.of(TestInterface.class)));
        assertThat(type == TypeInfo.of(TestClassWithInterface.class), is(true));
    }

    @Test
    void testInterface() {
        TypeInfo type = TypeInfo.of(TestInterface.class);
        assertThat(type.qualifiedName(), is("com.acme.TestInterface"));
        assertThat(type.simpleName(), is("TestInterface"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type == TypeInfo.of(TestInterface.class), is(true));
    }

    @Test
    void testEnum() {
        TypeInfo type = TypeInfo.of(TestEnum.class);
        assertThat(type.qualifiedName(), is("com.acme.TestEnum"));
        assertThat(type.simpleName(), is("TestEnum"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(true));
        assertThat(type == TypeInfo.of(TestEnum.class), is(true));
    }

    @Test
    void testAnnotation() {
        TypeInfo type = TypeInfo.of(TestAnnotation.class);
        assertThat(type.qualifiedName(), is("com.acme.TestAnnotation"));
        assertThat(type.simpleName(), is("TestAnnotation"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(true));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type == TypeInfo.of(TestAnnotation.class), is(true));
    }

    @Test
    void testClassWithParam1() {
        TypeInfo type = TypeInfo.of(TestClassWithParam1.class);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithParam1"));
        assertThat(type.simpleName(), is("TestClassWithParam1"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.typeParams(), arrayContaining(TypeInfo.of(TestInterface.class)));
        assertThat(type == TypeInfo.of(TestClassWithParam1.class), is(true));
    }

    @Test
    void testClassWithParam2() {
        TypeInfo type = TypeInfo.of(TestClassWithParam2.class);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithParam2"));
        assertThat(type.simpleName(), is("TestClassWithParam2"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.typeParams(), arrayContaining(type));
        assertThat(type == TypeInfo.of(TestClassWithParam2.class), is(true));
    }

    @Test
    void testClassWithNestedClass() {
        TypeInfo type = TypeInfo.of(TestClassWithNestedClass.NestedClass.class);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithNestedClass$NestedClass"));
        assertThat(type.simpleName(), is("TestClassWithNestedClass$NestedClass"));
        assertThat(TestClassWithNestedClass.NestedClass.class.getName(), is(type.qualifiedName()));
        assertThat(TestClassWithNestedClass.NestedClass.class.getTypeName(), is(type.typeName()));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type == TypeInfo.of(TestClassWithNestedClass.NestedClass.class), is(true));
    }

    @Test
    void testTypeElementClass() throws IOException {
        TestAP ap = ap("com/acme/TestClass1.java");
        TypeElement typeElement = (TypeElement) ap.elements.iterator().next();
        TypeInfo type = TypeInfo.of(typeElement, ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestClass1"));
        assertThat(type.simpleName(), is("TestClass1"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(false));
        assertThat(type == TypeInfo.of(typeElement, ap.types), is(true));
    }

    @Test
    void testTypeElementClassWithInterface() throws IOException {
        TestAP ap = ap("com/acme/TestClassWithInterface.java", "com/acme/TestInterface.java");
        Map<String, TypeElement> typeElements = ap.elements
                .stream()
                .map(TypeElement.class::cast)
                .collect(Collectors.toMap(t -> t.getSimpleName().toString(), Function.identity()));
        TypeInfo type = TypeInfo.of(typeElements.get("TestClassWithInterface"), ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithInterface"));
        assertThat(type.simpleName(), is("TestClassWithInterface"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(false));
        assertThat(type.interfaces(), arrayContaining(TypeInfo.of(typeElements.get("TestInterface"), ap.types)));
        assertThat(type == TypeInfo.of(typeElements.get("TestClassWithInterface"), ap.types), is(true));
    }

    @Test
    void testTypeElementWithNestedClass() throws IOException {
        TestAP ap = ap("com/acme/TestClassWithNestedClass.java");
        TypeElement typeElement = (TypeElement) ap.elements.iterator().next();
        TypeInfo type = TypeInfo.of(typeElement, ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithNestedClass.NestedClass"));
        assertThat(type.simpleName(), is("NestedClass"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(false));
        assertThat(type == TypeInfo.of(typeElement, ap.types), is(true));
    }

    @Test
    void testTypeElementClassWithParams1() throws IOException {
        TestAP ap = ap("com/acme/TestClassWithParam1.java", "com/acme/TestInterface.java");
        Map<String, TypeElement> typeElements = ap.elements
                .stream()
                .map(TypeElement.class::cast)
                .collect(Collectors.toMap(t -> t.getSimpleName().toString(), Function.identity()));

        TypeInfo type = TypeInfo.of(typeElements.get("TestClassWithParam1"), ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithParam1"));
        assertThat(type.simpleName(), is("TestClassWithParam1"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.typeParams(), arrayContaining(TypeInfo.of(typeElements.get("TestInterface"), ap.types)));
        assertThat(type == TypeInfo.of(typeElements.get("TestClassWithParam1"), ap.types), is(true));
    }

    @Test
    void testTypeElementClassWithParams2() throws IOException {
        TestAP ap = ap("com/acme/TestClassWithParam2.java");
        TypeElement typeElement = (TypeElement) ap.elements.iterator().next();
        TypeInfo type = TypeInfo.of(typeElement, ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithParam2"));
        assertThat(type.simpleName(), is("TestClassWithParam2"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.typeParams(), arrayContaining(type));
        assertThat(type == TypeInfo.of(typeElement, ap.types), is(true));
    }

    @Test
    void testTypeElementClassWithSuperClass() throws IOException {
        TestAP ap = ap("com/acme/TestClassWithSuperClass.java", "com/acme/TestClass1.java");
        Map<String, TypeElement> typeElements = ap.elements
                .stream()
                .map(TypeElement.class::cast)
                .collect(Collectors.toMap(t -> t.getSimpleName().toString(), Function.identity()));
        TypeInfo type = TypeInfo.of(typeElements.get("TestClassWithSuperClass"), ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestClassWithSuperClass"));
        assertThat(type.simpleName(), is("TestClassWithSuperClass"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.superClass().orElse(null), is(TypeInfo.of(typeElements.get("TestClass1"), ap.types)));
        assertThat(type == TypeInfo.of(typeElements.get("TestClassWithSuperClass"), ap.types), is(true));
    }

    @Test
    void testTypeElementEnum() throws IOException {
        TestAP ap = ap("com/acme/TestEnum.java");
        TypeElement typeElement = (TypeElement) ap.elements.iterator().next();
        TypeInfo type = TypeInfo.of(typeElement, ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestEnum"));
        assertThat(type.simpleName(), is("TestEnum"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isEnum(), is(true));
        assertThat(type == TypeInfo.of(typeElement, ap.types), is(true));
    }

    @Test
    void testTypeElementInterface() throws IOException {
        TestAP ap = ap("com/acme/TestInterface.java");
        TypeElement typeElement = (TypeElement) ap.elements.iterator().next();
        TypeInfo type = TypeInfo.of(typeElement, ap.types);
        assertThat(type.qualifiedName(), is("com.acme.TestInterface"));
        assertThat(type.simpleName(), is("TestInterface"));
        assertThat(type.pkg(), is("com.acme"));
        assertThat(type.isArray(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type == TypeInfo.of(typeElement, ap.types), is(true));
    }

    private static TestAP ap(String... path) throws IOException {
        TestAP ap = new TestAP();
        CompilerHelper compilerHelper = new CompilerHelper(ap, null, path);
        assertThat(compilerHelper.call(true), is(true));
        assertThat(ap.types, is(notNullValue()));
        assertThat(ap.elements, is(notNullValue()));
        assertThat(ap.elements.isEmpty(), is(false));
        return ap;
    }

    @SupportedAnnotationTypes(value = "com.acme.TestAnnotation")
    @SupportedSourceVersion(SourceVersion.RELEASE_11)
    static class TestAP extends AbstractProcessor {

        private boolean done;
        private Set<? extends Element> elements;
        private Types types;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!done) {
                elements = roundEnv.getElementsAnnotatedWith(TestAnnotation.class);
                types = processingEnv.getTypeUtils();
                done = true;
            }
            return true;
        }
    }
}
