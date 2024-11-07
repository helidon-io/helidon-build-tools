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

import com.acme.TestClass1;
import io.helidon.build.cli.codegen.AST.ArgumentDeclaration;
import io.helidon.build.cli.codegen.AST.ClassDeclaration;
import io.helidon.build.cli.codegen.AST.ConstructorDeclaration;
import io.helidon.build.cli.codegen.AST.FieldDeclaration;
import io.helidon.build.cli.codegen.AST.FieldGroup;
import io.helidon.build.cli.codegen.AST.MethodDeclaration;

import com.acme.TestClassWithSuperClass;
import io.helidon.build.cli.codegen.AST.Modifier;
import io.helidon.build.cli.codegen.AST.Modifiers;
import io.helidon.build.cli.codegen.AST.Value;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.codegen.AST.FieldGroup.*;
import static io.helidon.build.cli.codegen.AST.Invocations.*;
import static io.helidon.build.cli.codegen.AST.Modifier.*;
import static io.helidon.build.cli.codegen.AST.Refs.*;
import static io.helidon.build.cli.codegen.AST.Values.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TypeInfo}.
 */
class ASTTest {

    private static FieldDeclaration field(Modifier... modifiers) {
        return FieldDeclaration.builder().type(TestClassWithSuperClass.class).name("foo").modifiers(modifiers).build();
    }

    private static ArgumentDeclaration argument(Modifier... modifiers) {
        return ArgumentDeclaration.builder().type(TestClassWithSuperClass.class).name("foo").modifiers(modifiers).build();
    }

    @Test
    void testValidArgumentDeclarationModifiers() {
        assertThat(argument(DEFAULT, FINAL).modifiers(), arrayContaining(DEFAULT, FINAL));
        assertThat(argument(DEFAULT).modifiers(), arrayContaining(DEFAULT));
        assertThat(argument(FINAL).modifiers(), arrayContaining(FINAL));
    }

    @Test
    void testInvalidArgumentDeclarationModifiers(){
        IllegalArgumentException ex;

        ex = assertThrows(IllegalArgumentException.class,  () -> argument(FINAL, FINAL));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class, () -> argument(STATIC));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class, () -> argument(PUBLIC));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class, () -> argument(PROTECTED));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class, () -> argument(PRIVATE));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));
    }

    @Test
    void testInvalidArgumentDeclaration(){
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> ArgumentDeclaration.builder().type(TestClassWithSuperClass.class).name(null).build());
        assertThat(ex.getMessage(), is("name is null"));

        ex = assertThrows(NullPointerException.class,  () -> ArgumentDeclaration.builder().type((TypeInfo) null).name("foo").build());
        assertThat(ex.getMessage(), is("type is null"));
    }

    @Test
    void testInvalidTypeDeclaration() {
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> ClassDeclaration.builder().type(null).build());
        assertThat(ex.getMessage(), is("type is null"));
    }

    @Test
    void testInvalidRefCast() {
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> refCast((TypeInfo) null, valueRef("foo")));
        assertThat(ex.getMessage(), is("type is null"));

        ex = assertThrows(NullPointerException.class,  () -> refCast(TestClassWithSuperClass.class, null));
        assertThat(ex.getMessage(), is("ref is null"));
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testInvalidStaticRef() {
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> staticRef((TypeInfo) null));
        assertThat(ex.getMessage(), is("type is null"));
    }

    @Test
    void testInvalidMethodDeclaration() {
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> MethodDeclaration.builder().name(null).build());
        assertThat(ex.getMessage(), is("name is null"));
    }

    @Test
    void testInvalidMethodInvocation() {
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> methodInvocation(null));
        assertThat(ex.getMessage(), is("method is null"));
    }

    @Test
    void testInvalidConstructorInvocation() {
        NullPointerException ex;

        ex = assertThrows(NullPointerException.class,  () -> constructorInvocation((TypeInfo) null));
        assertThat(ex.getMessage(), is("type is null"));
    }

    @Test
    void testInvalidArrayLiteral() {
        NullPointerException ex1;

        ex1 = assertThrows(NullPointerException.class,  () -> arrayLiteral(null));
        assertThat(ex1.getMessage(), is("class is null"));

        ex1 = assertThrows(NullPointerException.class,  () -> arrayLiteral(TestClassWithSuperClass[].class, (Value[]) null));
        assertThat(ex1.getMessage(), is("values is null"));

        IllegalArgumentException ex2;

        ex2 = assertThrows(IllegalArgumentException.class,  () -> arrayLiteral(TestClass1.class));
        assertThat(ex2.getMessage(), startsWith("Not an array type"));
    }

    @Test
    void testInvalidAccessModifiers(){
        IllegalArgumentException ex;

        ex = assertThrows(IllegalArgumentException.class,  () -> field(PUBLIC, DEFAULT, PROTECTED, PRIVATE));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class,  () -> field(PUBLIC, PRIVATE));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class,  () -> field(DEFAULT, PRIVATE));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));

        ex = assertThrows(IllegalArgumentException.class, () -> field(PUBLIC, PUBLIC));
        assertThat(ex.getMessage(), startsWith("Invalid modifiers"));
    }

    @Test
    void testArrayValueRefNegativeIndex(){
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> arrayValueRef("bars", -1));
        assertThat(ex.getMessage(), is("Negative array index"));
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void testConstructorDeclarationWithoutType() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ConstructorDeclaration.builder().build().type());
        assertThat(ex.getMessage(), is("type is not set"));
    }

    @Test
    void testFieldGroup() {
        assertThat(FieldGroup.find(field(PUBLIC, STATIC, FINAL)), is(PUBLIC_STATIC_FINAL_FIELDS));
        assertThat(FieldGroup.find(field(PUBLIC, STATIC)), is(PUBLIC_STATIC_FIELDS));
        assertThat(FieldGroup.find(field(PROTECTED, STATIC, FINAL)), is(PROTECTED_STATIC_FINAL_FIELDS));
        assertThat(FieldGroup.find(field(PROTECTED, STATIC)), is(PROTECTED_STATIC_FIELDS));
        assertThat(FieldGroup.find(field(DEFAULT, STATIC, FINAL)), is(DEFAULT_STATIC_FINAL_FIELDS));
        assertThat(FieldGroup.find(field(PRIVATE, STATIC, FINAL)), is(PRIVATE_STATIC_FINAL_FIELDS));
        assertThat(FieldGroup.find(field(PRIVATE, STATIC)), is(PRIVATE_STATIC_FIELDS));
        assertThat(FieldGroup.find(field(PUBLIC)), is(PUBLIC_FIELDS));
        assertThat(FieldGroup.find(field(PUBLIC, FINAL)), is(PUBLIC_FIELDS));
        assertThat(FieldGroup.find(field(PROTECTED)), is(OTHER_FIELDS));
        assertThat(FieldGroup.find(field(PROTECTED, FINAL)), is(OTHER_FIELDS));
        assertThat(FieldGroup.find(field(DEFAULT)), is(OTHER_FIELDS));
        assertThat(FieldGroup.find(field(DEFAULT, FINAL)), is(OTHER_FIELDS));
        assertThat(FieldGroup.find(field(PRIVATE)), is(OTHER_FIELDS));
        assertThat(FieldGroup.find(field(PRIVATE, FINAL)), is(OTHER_FIELDS));
    }

    @Test
    void testModifierDuplicates() {
        assertThat(Modifiers.distinct(PUBLIC, PUBLIC), is(false));
        assertThat(Modifiers.distinct(PUBLIC, STATIC, FINAL), is(true));
        assertThat(Modifiers.distinct(PUBLIC, STATIC, STATIC), is(false));
        assertThat(Modifiers.distinct(STATIC, STATIC, STATIC), is(false));
        assertThat(Modifiers.distinct(PUBLIC), is(true));
        assertThat(Modifiers.distinct(), is(true));
    }

    @Test
    void testModifierContainsAny() {
        assertThat(Modifiers.containsAny(Modifiers.of(DEFAULT, FINAL), DEFAULT, FINAL), is(true));
        assertThat(Modifiers.containsAny(Modifiers.of(DEFAULT), DEFAULT, FINAL), is(true));
        assertThat(Modifiers.containsAny(Modifiers.of(FINAL), DEFAULT, FINAL), is(true));
        assertThat(Modifiers.containsAny(Modifiers.of(DEFAULT, FINAL)), is(true));
        assertThat(Modifiers.containsAny(Modifiers.EMPTY), is(true));
        assertThat(Modifiers.containsAny(Modifiers.EMPTY, DEFAULT, FINAL), is(false));
        assertThat(Modifiers.containsAny(Modifiers.of(DEFAULT, FINAL), DEFAULT), is(false));
        assertThat(Modifiers.containsAny(Modifiers.of(DEFAULT, FINAL), FINAL), is(false));
        assertThat(Modifiers.containsAny(Modifiers.of(DEFAULT, FINAL), DEFAULT), is(false));
        assertThat(Modifiers.containsAny(Modifiers.EMPTY, DEFAULT), is(false));
    }

    @Test
    void testModifierValidAccess() {
        assertThat(Modifiers.validAccess(DEFAULT), is(true));
        assertThat(Modifiers.validAccess(DEFAULT, STATIC, FINAL), is(true));
        assertThat(Modifiers.validAccess(PRIVATE), is(true));
        assertThat(Modifiers.validAccess(PROTECTED), is(true));
        assertThat(Modifiers.validAccess(PUBLIC), is(true));
        assertThat(Modifiers.validAccess(FINAL), is(true));
        assertThat(Modifiers.validAccess(DEFAULT, PRIVATE, PROTECTED, PUBLIC), is(false));
        assertThat(Modifiers.validAccess(DEFAULT, PUBLIC), is(false));
        assertThat(Modifiers.validAccess(PRIVATE, PUBLIC), is(false));
    }
}
