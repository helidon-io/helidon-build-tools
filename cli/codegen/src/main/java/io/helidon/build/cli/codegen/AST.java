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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Simplistic Java AST builder.
 */
class AST {

    private AST() {
    }

    /**
     * Modifiers.
     */
    enum Modifier {

        /**
         * default modifier, i.e same as no modifier.
         */
        DEFAULT,

        /**
         * {@code private} modifier.
         */
        PRIVATE,

        /**
         * {@code protected} modifier.
         */
        PROTECTED,

        /**
         * {@code public} modifier.
         */
        PUBLIC,

        /**
         * {@code static} modifier.
         */
        STATIC,

        /**
         * {@code final} modifier.
         */
        FINAL;

        /**
         * Get the bit mask for this enum.
         *
         * @return int
         */
        int bitMask() {
            return 1 << ordinal();
        }

        /**
         * Bit mask for all the access modifiers.
         */
        static final int ACCESS_MASK = 15;
    }

    /**
     * Modifiers utility.
     */
    static class Modifiers {

        /**
         * Empty modifiers.
         */
        static final Modifier[] EMPTY = new Modifier[0];

        /**
         * Default modifiers.
         */
        static final Modifier[] DEFAULT = new Modifier[]{
                Modifier.DEFAULT
        };

        /**
         * Modifiers order array.
         */
        static final Modifier[] ORDER = new Modifier[]{
                Modifier.STATIC,
                Modifier.PUBLIC,
                Modifier.PROTECTED,
                Modifier.DEFAULT,
                Modifier.PRIVATE,
                Modifier.FINAL
        };

        private Modifiers() {
        }

        /**
         * Vararg conversion.
         *
         * @param modifiers modifiers
         * @return Modifier[]
         */
        static Modifier[] of(Modifier... modifiers) {
            return modifiers;
        }

        /**
         * Test if the given actual modifiers are any of the expected modifiers.
         *
         * @param expected the expected modifiers
         * @param actual   the actual modifiers
         * @return {@code true} if expected is empty, {@code false} if actual is empty, {@code true} if actual
         * contains any of the expected modifiers, {@code false} otherwise
         */
        static boolean containsAny(Modifier[] actual, Modifier... expected) {
            if (expected.length == 0) {
                return true;
            }
            if (actual.length == 0) {
                return false;
            }
            int mask = 0;
            for (Modifier modifier : expected) mask |= modifier.bitMask();
            for (Modifier modifier : actual) {
                if ((mask & modifier.bitMask()) == 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Test if the given modifiers have valid access modifiers.
         *
         * @param modifiers the modifiers to test
         * @return {@code true} modifiers is empty or valid, {@code false} otherwise
         */
        static boolean validAccess(Modifier... modifiers) {
            if (modifiers.length == 0) {
                return true;
            }
            int bitset = 0;
            for (Modifier modifier : modifiers) {
                bitset |= modifier.bitMask();
                int access = bitset & Modifier.ACCESS_MASK;
                if ((access & (access - 1)) == 0) {
                    // zero or one bit set
                    continue;
                }
                return false;
            }
            return true;
        }

        /**
         * Test if the given modifiers are distinct.
         *
         * @param modifiers modifiers to test
         * @return {@code true} if all modifiers are distinct, {@code false} if there are duplicates
         */
        static boolean distinct(Modifier... modifiers) {
            int bitset = 0;
            for (Modifier modifier : modifiers) {
                int mask = modifier.bitMask();
                if ((bitset & mask) == 0) {
                    bitset |= mask;
                } else {
                    return false;
                }
            }
            return true;
        }

        /**
         * Compare the given modifiers.
         *
         * @param m1 modifiers
         * @param m2 modifiers
         * @return positive if m1 is greater than m2, equal if m1 is equal to m2 and negative if m1 is lower than m2
         */
        static int compare(Modifier[] m1, Modifier[] m2) {
            for (Modifier modifier : Modifiers.ORDER) {
                boolean f1m = Modifiers.containsAny(m1, modifier);
                boolean f2m = Modifiers.containsAny(m2, modifier);
                if ((f1m && f2m) || (!f1m && !f2m)) {
                    continue;
                }
                return f1m ? -1 : 1;
            }
            return 0;
        }
    }

    /**
     * Base interface.
     */
    interface Node {

        /**
         * Resolve all types.
         *
         * @param resolver type resolver
         */
        default void resolveImports(Consumer<TypeInfo> resolver) {
        }
    }

    /**
     * Value, literals, assigned values or invocation arguments.
     */
    interface Value extends Node {
    }

    /**
     * Static factory methods for values.
     */
    interface Values {

        /**
         * Create a new class literal.
         *
         * @param type type
         * @return ClassLiteral
         */
        static ClassLiteral classLiteral(TypeInfo type) {
            return new ClassLiteral(type);
        }

        /**
         * Create a new string literal.
         *
         * @param value value
         * @return StringLiteral
         */
        static StringLiteral stringLiteral(String value) {
            return new StringLiteral(value);
        }

        /**
         * Create a new boolean literal.
         *
         * @param value value
         * @return BooleanLiteral
         */
        static BooleanLiteral booleanLiteral(boolean value) {
            return new BooleanLiteral(value);
        }

        /**
         * Create a new array literal.
         *
         * @param aClass array type
         * @param values values
         * @return ArrayLiteral
         */
        static ArrayLiteral arrayLiteral(Class<?> aClass, Value... values) {
            return new ArrayLiteral(aClass, values);
        }

        /**
         * Create a new null literal.
         *
         * @return NullLiteral
         */
        static NullLiteral nullLiteral() {
            return new NullLiteral();
        }

        /**
         * Create a new value cast.
         *
         * @param type  the type to be casted to
         * @param value the value being casted
         */
        static ValueCast valueCast(TypeInfo type, Value value) {
            return new ValueCast(type, value);
        }

        /**
         * Create a new value cast.
         *
         * @param aClass the type to be casted to
         * @param value  the value being casted
         */
        static ValueCast valueCast(Class<?> aClass, Value value) {
            return new ValueCast(TypeInfo.of(aClass), value);
        }

        /**
         * Create a local array element value reference.
         *
         * @param ref   reference name
         * @param index array index
         */
        static ArrayValueRef arrayValueRef(String ref, int index) {
            return new ArrayValueRef(ref, index);
        }

        /**
         * Create a local value reference.
         *
         * @param ref reference name
         */
        static ValueRef valueRef(String ref) {
            return new ValueRef(null, ref);
        }

        /**
         * Create a parented value reference.
         *
         * @param parent parent reference
         * @param ref    reference name
         */
        static ValueRef valueRef(Ref parent, String ref) {
            return new ValueRef(parent, ref);
        }
    }

    /**
     * Reference, anything that is separated by {@code .}.
     */
    interface Ref extends Node {
    }

    /**
     * Static factory methods for references.
     */
    interface Refs {

        /**
         * Create a static reference.
         *
         * @param aClass type
         */
        static StaticRef staticRef(Class<?> aClass) {
            return new StaticRef(TypeInfo.of(aClass));
        }

        /**
         * Create a static reference.
         *
         * @param type type
         */
        static StaticRef staticRef(TypeInfo type) {
            return new StaticRef(type);
        }

        /**
         * Create a new reference cast.
         *
         * @param type the type to be casted to
         * @param ref  the reference being casted
         */
        static RefCast refCast(TypeInfo type, Ref ref) {
            return new RefCast(type, ref);
        }

        /**
         * Create a new reference cast.
         *
         * @param aClass the type to be casted to
         * @param ref    the reference being casted
         */
        static RefCast refCast(Class<?> aClass, Ref ref) {
            return new RefCast(TypeInfo.of(aClass), ref);
        }
    }

    /**
     * Statement, anything that ends with a {@code ;}.
     */
    interface Statement extends Node {
    }

    /**
     * Static factory methods for statements.
     */
    interface Statements {

        /**
         * Create a new no-value return statement.
         *
         * @return ReturnStatement
         */
        static ReturnStatement returnStatement() {
            return new ReturnStatement(null);
        }

        /**
         * Create a new return statement.
         *
         * @param value return value
         * @return ReturnStatement
         */
        static ReturnStatement returnStatement(Value value) {
            return new ReturnStatement(value);
        }

        /**
         * Create a new return statement.
         *
         * @param supplier return value supplier
         * @return ReturnStatement
         */
        static ReturnStatement returnStatement(Supplier<? extends Value> supplier) {
            return new ReturnStatement(supplier.get());
        }

        /**
         * Create a new super statement.
         *
         * @param args arguments
         * @return SuperStatement
         */
        static SuperStatement superStatement(Value... args) {
            return SuperStatement.builder().args(args).build();
        }
    }

    /**
     * Invocation.
     */
    interface Invocation extends Value, Statement {

        /**
         * Invocation style.
         */
        enum Style {
            /**
             * Arguments are written out in a single line.
             */
            SINGLE_LINE,

            /**
             * Arguments are written out on separate lines.
             */
            MULTI_LINE
        }

        /**
         * Get the invocation style.
         *
         * @return Style
         */
        Style style();
    }

    /**
     * Static factory methods for invocations.
     */
    interface Invocations {

        /**
         * Create a method invocation.
         *
         * @param methodName method name
         * @param args       arguments
         * @return ConstructorInvocation
         */
        static MethodInvocation methodInvocation(String methodName, Value... args) {
            return MethodInvocation.builder().method(methodName).args(args).build();
        }

        /**
         * Create a constructor invocation.
         *
         * @param type invoked type
         * @param args arguments
         * @return ConstructorInvocation
         */
        static ConstructorInvocation constructorInvocation(TypeInfo type, Value... args) {
            return ConstructorInvocation.builder().type(type).args(args).build();
        }

        /**
         * Create a constructor invocation.
         *
         * @param aClass invoked type
         * @param args   arguments
         * @return ConstructorInvocation
         */
        static ConstructorInvocation constructorInvocation(Class<?> aClass, Value... args) {
            return ConstructorInvocation.builder().type(aClass).args(args).build();
        }
    }

    /**
     * Body, anything enclosed with &#123;&#125;.
     */
    interface Body extends Node {
    }

    /**
     * Declaration.
     */
    static class Declaration implements Node {

        private final List<Annotation> annotations;
        private final Modifier[] modifiers;
        private final String javadoc;

        protected Declaration(Builder<?, ?> builder) {
            javadoc = builder.javadoc;
            annotations = builder.annotations;
            if (!Modifiers.distinct(builder.modifiers) || !Modifiers.validAccess(builder.modifiers)) {
                throw new IllegalArgumentException("Invalid modifiers: " + Arrays.toString(builder.modifiers));
            }
            modifiers = builder.modifiers;
        }

        /**
         * Get the annotations.
         *
         * @return list of {@link Annotation}, never {@code null}
         */
        List<Annotation> annotations() {
            return annotations;
        }

        /**
         * Get the modifiers.
         *
         * @return Modifier[], never {@code null}
         */
        Modifier[] modifiers() {
            return modifiers;
        }

        /**
         * Get the declaration javadoc.
         *
         * @return optional of String
         */
        Optional<String> javadoc() {
            return Optional.ofNullable(javadoc);
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            annotations.forEach(a -> a.resolveImports(resolver));
        }

        /**
         * Declaration builder.
         */
        @SuppressWarnings("unchecked")
        abstract static class Builder<T extends Builder<T, U>, U extends Declaration>
                implements Supplier<U> {

            private final List<Annotation> annotations = new LinkedList<>();
            private Modifier[] modifiers = Modifiers.DEFAULT;
            private String javadoc;

            private Builder() {
            }

            /**
             * Set the modifiers.
             *
             * @param modifiers modifiers
             * @return this builder
             */
            T modifiers(Modifier... modifiers) {
                this.modifiers = modifiers;
                return (T) this;
            }

            /**
             * Set the javadoc.
             *
             * @param javadoc javadoc
             * @return this builder
             */
            T javadoc(String javadoc) {
                this.javadoc = javadoc;
                return (T) this;
            }

            /**
             * Add a method annotation.
             *
             * @param annotation annotation class
             * @return this builder
             */
            T annotation(Class<?> annotation) {
                annotations.add(new Annotation(annotation));
                return (T) this;
            }

            /**
             * Add a method annotation.
             *
             * @param annotation annotation class
             * @param value annotation value
             * @return this builder
             */
            T annotation(Class<?> annotation, Value value) {
                annotations.add(new Annotation(annotation, value));
                return (T) this;
            }

            /**
             * Build the declaration instance.
             *
             * @return U
             */
            abstract U build();

            @Override
            public U get() {
                return build();
            }
        }
    }

    /**
     * Type body.
     */
    static class TypeBody implements Body {

        private final List<FieldDeclaration> fields;
        private final List<MethodDeclaration> methods;

        private TypeBody() {
            fields = Collections.emptyList();
            methods = Collections.emptyList();
        }

        private TypeBody(Builder<?, ?> builder) {
            fields = builder.fields;
            methods = builder.methods;
        }

        /**
         * Get the fields.
         *
         * @return list of FieldDeclaration
         */
        List<FieldDeclaration> fields() {
            return fields;
        }

        /**
         * Get the methods.
         *
         * @return list of MethodDeclaration
         */
        List<MethodDeclaration> methods() {
            return methods;
        }

        /**
         * Resolve the imports.
         *
         * @param resolver import resolver
         */
        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            fields.forEach(f -> f.resolveImports(resolver));
            methods.forEach(m -> m.resolveImports(resolver));
        }

        /**
         * Type body builder.
         *
         * @param <T>
         * @param <U>
         */
        @SuppressWarnings("unchecked")
        abstract static class Builder<T extends Builder<T, U>, U extends TypeBody> implements Supplier<U> {

            private final List<FieldDeclaration> fields = new LinkedList<>();
            private final List<MethodDeclaration> methods = new LinkedList<>();

            private Builder() {
            }

            /**
             * Add a field declaration.
             *
             * @param supplier field declaration supplier
             * @return this builder
             */
            T field(Supplier<? extends FieldDeclaration> supplier) {
                fields.add(supplier.get());
                return (T) this;
            }

            /**
             * Add a field declaration.
             *
             * @param fieldDeclaration field declaration
             * @return this builder
             */
            T field(FieldDeclaration fieldDeclaration) {
                fields.add(fieldDeclaration);
                return (T) this;
            }

            /**
             * Add a method declaration.
             *
             * @param supplier method declaration supplier
             * @return this builder
             */
            T method(Supplier<MethodDeclaration> supplier) {
                methods.add(supplier.get());
                return (T) this;
            }

            /**
             * Add a method declaration.
             *
             * @param method method declaration
             * @return this builder
             */
            T method(MethodDeclaration method) {
                methods.add(method);
                return (T) this;
            }

            /**
             * Build the type body instance.
             *
             * @return U
             */
            abstract U build();

            @Override
            public U get() {
                return build();
            }
        }
    }

    /**
     * Type declaration.
     */
    abstract static class TypeDeclaration<T extends TypeBody> extends Declaration {

        private final TypeInfo type;

        private TypeDeclaration(Builder<?, ?, T> builder) {
            super(builder);
            type = Objects.requireNonNull(builder.type, "type is null");
        }

        /**
         * Get the type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the body.
         *
         * @return TypeBody, never {@code null}
         */
        abstract T body();

        /**
         * Resolve the imports.
         *
         * @param resolver import resolver
         */
        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
            body().resolveImports(resolver);
        }

        /**
         * Type declaration builder.
         *
         * @param <T> builder type param
         * @param <U> type declaration type param
         */
        @SuppressWarnings("unchecked")
        abstract static class Builder<T extends Builder<T, U, V>, U extends TypeDeclaration<V>, V extends TypeBody>
                extends Declaration.Builder<T, TypeDeclaration<V>> {

            private TypeInfo type;

            private Builder() {
            }

            /**
             * Set the type body.
             *
             * @param body type body
             * @return this builder
             */
            abstract T body(V body);

            /**
             * Set the type body.
             *
             * @param supplier type body supplier
             * @return this builder
             */
            abstract T body(Supplier<V> supplier);

            /**
             * Set the class type.
             *
             * @param type class type
             * @return this builder
             */
            T type(TypeInfo type) {
                this.type = type;
                return (T) this;
            }
        }
    }

    static final class ClassBody extends TypeBody {

        private static final ClassBody EMPTY = new ClassBody();

        private final List<ConstructorDeclaration> constructors;

        private ClassBody() {
            super();
            constructors = Collections.emptyList();
        }

        private ClassBody(Builder builder) {
            super(builder);
            constructors = builder.constructors;
        }

        /**
         * Get the constructors.
         *
         * @return list of ConstructorDeclaration
         */
        List<ConstructorDeclaration> constructors() {
            return constructors;
        }

        /**
         * Resolve the imports.
         *
         * @param resolver import resolver
         */
        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            super.resolveImports(resolver);
            constructors.forEach(c -> c.resolveImports(resolver));
        }

        /**
         * Create a new class body builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Class declaration builder.
         */
        static final class Builder extends TypeBody.Builder<Builder, ClassBody> {

            private final List<ConstructorDeclaration> constructors = new LinkedList<>();

            /**
             * Add a constructor declaration.
             *
             * @param supplier constructor declaration supplier
             * @return this builder
             */
            Builder constructor(Supplier<ConstructorDeclaration> supplier) {
                constructors.add(supplier.get());
                return this;
            }

            /**
             * Add a constructor declaration.
             *
             * @param constructor constructor declaration
             * @return this builder
             */
            Builder constructor(ConstructorDeclaration constructor) {
                constructors.add(constructor);
                return this;
            }

            @Override
            ClassBody build() {
                return new ClassBody(this);
            }
        }
    }

    /**
     * Class declaration.
     */
    static final class ClassDeclaration extends TypeDeclaration<ClassBody> {

        private final TypeInfo superClass;
        private final ClassBody body;

        private ClassDeclaration(Builder builder) {
            super(builder);
            superClass = builder.superClass;
            body = builder.body;
            for (ConstructorDeclaration constructor : body.constructors) {
                constructor.type = super.type;
            }
        }

        /**
         * Get the super class.
         *
         * @return optional of TypeInfo
         */
        Optional<TypeInfo> superClass() {
            return Optional.ofNullable(superClass);
        }

        @Override
        ClassBody body() {
            return body;
        }

        /**
         * Resolve the imports.
         *
         * @param resolver import resolver
         */
        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            super.resolveImports(resolver);
            if (superClass != null) {
                resolver.accept(superClass);
            }
        }

        /**
         * Create a new class declaration builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Class declaration builder.
         */
        static final class Builder extends TypeDeclaration.Builder<Builder, ClassDeclaration, ClassBody> {

            private TypeInfo superClass;
            private ClassBody body = ClassBody.EMPTY;

            private Builder() {
            }

            @Override
            Builder body(ClassBody body) {
                this.body = body;
                return this;
            }

            @Override
            Builder body(Supplier<ClassBody> supplier) {
                this.body = supplier.get();
                return this;
            }

            /**
             * Set the superClass.
             *
             * @param superClass superClass
             * @return this builder
             */
            Builder superClass(Class<?> superClass) {
                this.superClass = TypeInfo.of(superClass);
                return this;
            }

            /**
             * Set the superClass.
             *
             * @param superClass superClass
             * @return this builder
             */
            Builder superClass(TypeInfo superClass) {
                this.superClass = superClass;
                return this;
            }

            /**
             * Build the class declaration instance.
             *
             * @return ClassDeclaration
             */
            ClassDeclaration build() {
                return new ClassDeclaration(this);
            }
        }
    }

    /**
     * A cast over a reference.
     */
    static final class RefCast implements Ref {

        private final TypeInfo type;
        private final Ref ref;

        private RefCast(TypeInfo type, Ref ref) {
            this.type = Objects.requireNonNull(type, "type is null");
            this.ref = Objects.requireNonNull(ref, "ref is null");
        }

        /**
         * Get the casted type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the ref.
         *
         * @return Ref, never {@code null}
         */
        Ref ref() {
            return ref;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
            ref.resolveImports(resolver);
        }
    }

    /**
     * A cast over a value.
     */
    static final class ValueCast implements Value {

        private final TypeInfo type;
        private final Value value;

        private ValueCast(TypeInfo type, Value value) {
            this.type = type;
            this.value = value;
        }

        /**
         * Get the casted type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the value.
         *
         * @return Value, never {@code null}
         */
        Value ref() {
            return value;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
            value.resolveImports(resolver);
        }
    }

    /**
     * A static reference.
     */
    static final class StaticRef implements Ref {

        private final TypeInfo type;

        private StaticRef(Class<?> aClass) {
            this(TypeInfo.of(aClass));
        }

        private StaticRef(TypeInfo type) {
            this.type = Objects.requireNonNull(type, "type is null");
        }

        /**
         * Get the type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
        }
    }

    /**
     * A value by reference.
     */
    static class ValueRef implements Ref, Value {

        private final Ref parent;
        private final String ref;

        private ValueRef(Ref parent, String ref) {
            this.parent = parent;
            this.ref = Objects.requireNonNull(ref, "ref is null");
        }

        /**
         * Get the parent ref.
         *
         * @return optional of Ref
         */
        Optional<Ref> parent() {
            return Optional.ofNullable(parent);
        }

        /**
         * Get the ref name.
         *
         * @return ref name, never {@code null}
         */
        String ref() {
            return ref;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            if (parent != null) {
                parent.resolveImports(resolver);
            }
        }
    }

    /**
     * An array element value by reference.
     */
    static final class ArrayValueRef implements Ref, Value {

        private final int index;
        private final String ref;

        private ArrayValueRef(String ref, int index) {
            this.ref = Objects.requireNonNull(ref, "ref is null");
            if (index < 0) {
                throw new IllegalArgumentException("Negative array index");
            }
            this.index = index;
        }

        /**
         * Get the ref name.
         *
         * @return ref name, never {@code null}
         */
        String ref() {
            return ref;
        }

        /**
         * Get the index.
         *
         * @return index
         */
        int index() {
            return index;
        }
    }

    /**
     * {@code super()} statement.
     */
    static final class SuperStatement implements Statement {

        private final List<Value> args;

        private SuperStatement(Builder builder) {
            this.args = builder.args;
        }

        /**
         * Get the arguments.
         *
         * @return list of Value
         */
        List<Value> args() {
            return args;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            args.forEach(v -> v.resolveImports(resolver));
        }

        /**
         * Create a new super invocation builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Super invocation builder.
         */
        static final class Builder implements Supplier<SuperStatement> {

            private final List<Value> args = new LinkedList<>();

            private Builder() {
            }

            /**
             * Add a argument.
             *
             * @param supplier argument supplier
             * @return this builder
             */
            Builder arg(Supplier<? extends Value> supplier) {
                args.add(supplier.get());
                return this;
            }

            /**
             * Add a argument.
             *
             * @param arg argument
             * @return this builder
             */
            Builder arg(Value arg) {
                args.add(arg);
                return this;
            }

            /**
             * Add arguments.
             *
             * @param args arguments
             * @return this builder
             */
            Builder args(Value... args) {
                this.args.addAll(Arrays.asList(args));
                return this;
            }

            @Override
            public SuperStatement get() {
                return build();
            }

            /**
             * build the super invocation.
             *
             * @return SuperInvocation
             */
            SuperStatement build() {
                return new SuperStatement(this);
            }
        }
    }

    /**
     * Return statement.
     */
    static final class ReturnStatement implements Statement {

        private final Value value;

        private ReturnStatement(Value value) {
            this.value = value;
        }

        /**
         * Get the return value.
         *
         * @return return optional of value
         */
        Optional<Value> value() {
            return Optional.ofNullable(value);
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            if (value != null) {
                value.resolveImports(resolver);
            }
        }
    }

    /**
     * Abstract invocation.
     */
    abstract static class AbstractInvocation implements Invocation {

        private final List<Value> args;
        private final Style style;

        protected AbstractInvocation(Builder<?, ?> builder) {
            args = builder.args;
            style = builder.style;
        }

        /**
         * Get the invocation arguments.
         *
         * @return list of Value
         */
        List<Value> args() {
            return args;
        }

        @Override
        public Style style() {
            return style;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            args.forEach(v -> v.resolveImports(resolver));
        }

        /**
         * Abstract invocation builder.
         */
        @SuppressWarnings("unchecked")
        abstract static class Builder<T extends Builder<T, U>, U extends AbstractInvocation>
                implements Supplier<U> {

            private final List<Value> args = new LinkedList<>();
            private Style style = Style.SINGLE_LINE;

            private Builder() {
            }

            /**
             * Add invocation arguments.
             *
             * @param args invocation arguments
             * @return this builder
             */
            T args(Value... args) {
                this.args.addAll(Arrays.asList(args));
                return (T) this;
            }

            /**
             * Add an invocation argument.
             *
             * @param arg invocation argument
             * @return this builder
             */
            T arg(Value arg) {
                args.add(arg);
                return (T) this;
            }


            /**
             * Set the invocation style.
             *
             * @param style invocation style
             * @return this builder
             */
            T style(Style style) {
                this.style = style;
                return (T) this;
            }

            /**
             * Build the invocation.
             *
             * @return U
             */
            abstract U build();

            @Override
            public U get() {
                return build();
            }
        }
    }

    /**
     * Method invocation.
     */
    static final class MethodInvocation extends AbstractInvocation {

        private final Ref parentRef;
        private final String method;

        private MethodInvocation(Builder builder) {
            super(builder);
            parentRef = builder.parentRef;
            method = Objects.requireNonNull(builder.method, "method is null");
        }

        /**
         * Get the parent reference.
         *
         * @return optional of Ref
         */
        Optional<Ref> parentRef() {
            return Optional.ofNullable(parentRef);
        }

        /**
         * Get the method name.
         *
         * @return method name, never {@code null}
         */
        String method() {
            return method;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            super.resolveImports(resolver);
            if (parentRef != null) {
                parentRef.resolveImports(resolver);
            }
        }

        /**
         * Create a new builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Invocation builder.
         */
        static final class Builder extends AbstractInvocation.Builder<Builder, MethodInvocation> {

            private Ref parentRef;
            private String method;

            private Builder() {
            }

            /**
             * Set the static method to be invoked.
             *
             * @param aClass type that contains the method to invoke
             * @param method name of the method to invoke
             * @return this builder
             */
            Builder method(Class<?> aClass, String method) {
                return method(new StaticRef(aClass), method);
            }

            /**
             * Set the static method to be invoked.
             *
             * @param type   type that contains the method to invoke
             * @param method name of the method to invoke
             * @return this builder
             */
            Builder method(TypeInfo type, String method) {
                return method(new StaticRef(type), method);
            }

            /**
             * Set the local method name to invoke.
             *
             * @param method name of the method to invoke
             * @return this builder
             */
            Builder method(String method) {
                return method((Ref) null, method);
            }

            /**
             * Set the instance method to invoke.
             *
             * @param varName the instance that contains the method to invoke
             * @param method  name of the method to invoke
             * @return this builder
             */
            Builder method(String varName, String method) {
                return method(new ValueRef(null, varName), method);
            }

            /**
             * Set the method to invoke.
             *
             * @param parentRef the reference that contains the method to invoke
             * @param method    name of the method to invoke
             * @return this builder
             */
            Builder method(Ref parentRef, String method) {
                this.parentRef = parentRef;
                this.method = method;
                return this;
            }

            /**
             * Build the invocation.
             *
             * @return Invocation
             */
            @Override
            MethodInvocation build() {
                return new MethodInvocation(this);
            }
        }
    }

    /**
     * Constructor invocation.
     */
    static final class ConstructorInvocation extends AbstractInvocation {

        private final TypeInfo type;

        private ConstructorInvocation(Builder builder) {
            super(builder);
            type = Objects.requireNonNull(builder.type, "type is null");
        }

        /**
         * Get the constructor type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            super.resolveImports(resolver);
            resolver.accept(type);
        }

        /**
         * Create a new builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Constructor invocation builder.
         */
        static final class Builder extends AbstractInvocation.Builder<Builder, ConstructorInvocation> {

            private TypeInfo type;

            private Builder() {
            }

            /**
             * Type of the constructor invoked.
             *
             * @param aClass class
             * @return this builder
             */
            Builder type(Class<?> aClass) {
                this.type = TypeInfo.of(aClass);
                return this;
            }

            /**
             * Type of the constructor invoked.
             *
             * @param type class
             * @return this builder
             */
            Builder type(TypeInfo type) {
                this.type = type;
                return this;
            }

            /**
             * Build the constructor invocation.
             *
             * @return ConstructorInvocation
             */
            @Override
            ConstructorInvocation build() {
                return new ConstructorInvocation(this);
            }
        }
    }

    /**
     * Array literal.
     */
    static final class ArrayLiteral implements Value {

        private final TypeInfo type;
        private final Value[] values;

        private ArrayLiteral(Class<?> aClass, Value[] values) {
            Objects.requireNonNull(aClass, "class is null");
            if (!aClass.isArray()) {
                throw new IllegalArgumentException("Not an array type: " + aClass);
            }
            this.type = TypeInfo.of(aClass);
            this.values = Objects.requireNonNull(values, "values is null");
        }

        /**
         * Get the array type.
         *
         * @return array type, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the array values.
         *
         * @return array values, never {@code null}
         */
        Value[] values() {
            return values;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
            Arrays.stream(values).forEach(value -> value.resolveImports(resolver));
        }
    }

    /**
     * Class literal.
     */
    static final class ClassLiteral implements Value {

        private final TypeInfo value;

        private ClassLiteral(TypeInfo value) {
            this.value = Objects.requireNonNull(value, "value is null");
        }

        /**
         * Get the literal value.
         *
         * @return literal value, never {@code null}
         */
        TypeInfo value() {
            return value;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(value);
        }
    }

    /**
     * Boolean literal.
     */
    static final class BooleanLiteral implements Value {

        private final boolean value;

        private BooleanLiteral(boolean value) {
            this.value = value;
        }

        /**
         * Get the literal value.
         *
         * @return literal value
         */
        boolean value() {
            return value;
        }
    }

    /**
     * String literal.
     */
    static final class StringLiteral implements Value {

        private final String value;

        private StringLiteral(String value) {
            this.value = Objects.requireNonNull(value, "value is null");
        }

        /**
         * Get the literal value.
         *
         * @return literal value, never {@code null}
         */
        String value() {
            return value;
        }
    }

    /**
     * Null literal.
     */
    static final class NullLiteral implements Value {

        private NullLiteral() {
        }
    }

    /**
     * Field declaration.
     */
    static final class FieldDeclaration extends Declaration implements Comparable<FieldDeclaration> {

        private final TypeInfo type;
        private final String name;
        private final Value value;

        private FieldDeclaration(Builder builder) {
            super(builder);
            type = Objects.requireNonNull(builder.type, "type is null");
            name = Objects.requireNonNull(builder.name, "name is null");
            value = builder.value;
        }

        /**
         * Get the type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the field name.
         *
         * @return field name, never {@code null}
         */
        String name() {
            return name;
        }

        /**
         * Get the field value.
         *
         * @return optional of Value
         */
        Optional<Value> value() {
            return Optional.ofNullable(value);
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
            if (value != null) {
                value.resolveImports(resolver);
            }
        }

        @Override
        public int compareTo(FieldDeclaration field) {
            return Modifiers.compare(modifiers(), field.modifiers());
        }

        /**
         * Create a new builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Field declaration builder.
         */
        static final class Builder extends Declaration.Builder<Builder, FieldDeclaration> {

            private TypeInfo type;
            private String name;
            private Value value;

            private Builder() {
            }

            /**
             * Set the field type.
             *
             * @param aClass class
             * @return this builder
             */
            Builder type(Class<?> aClass) {
                this.type = TypeInfo.of(aClass);
                return this;
            }

            /**
             * Set the field type.
             *
             * @param type field type
             * @return this builder
             */
            Builder type(TypeInfo type) {
                this.type = type;
                return this;
            }

            /**
             * Set the field name.
             *
             * @param name field name
             * @return this builder
             */
            Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Set the field value.
             *
             * @param value field value
             * @return this builder
             */
            Builder value(Value value) {
                this.value = value;
                return this;
            }

            /**
             * Set the field value.
             *
             * @param supplier field value supplier
             * @return this builder
             */
            Builder value(Supplier<? extends Value> supplier) {
                this.value = supplier.get();
                return this;
            }

            @Override
            public FieldDeclaration get() {
                return build();
            }

            /**
             * Build the field declaration.
             *
             * @return FieldDeclaration
             */
            FieldDeclaration build() {
                return new FieldDeclaration(this);
            }
        }
    }

    /**
     * Field groups.
     */
    enum FieldGroup {

        /**
         * Group of {@code public static final} fields.
         */
        PUBLIC_STATIC_FINAL_FIELDS(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),

        /**
         * Group of {@code public static} fields.
         */
        PUBLIC_STATIC_FIELDS(Modifier.PUBLIC, Modifier.STATIC),

        /**
         * Group of {@code protected static final} fields.
         */
        PROTECTED_STATIC_FINAL_FIELDS(Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL),

        /**
         * Group of {@code protected static} fields.
         */
        PROTECTED_STATIC_FIELDS(Modifier.PROTECTED, Modifier.STATIC),

        /**
         * Group of {@code static final} fields.
         */
        DEFAULT_STATIC_FINAL_FIELDS(Modifier.DEFAULT, Modifier.STATIC, Modifier.FINAL),

        /**
         * Group of {@code static} fields.
         */
        DEFAULT_STATIC_FIELDS(Modifier.DEFAULT, Modifier.STATIC),

        /**
         * Group of {@code private static final} fields.
         */
        PRIVATE_STATIC_FINAL_FIELDS(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL),

        /**
         * Group of {@code private static} fields.
         */
        PRIVATE_STATIC_FIELDS(Modifier.PRIVATE, Modifier.STATIC),

        /**
         * Group of {@code public} fields.
         */
        PUBLIC_FIELDS(Modifier.PUBLIC),

        /**
         * Group of every other fields.
         */
        OTHER_FIELDS();

        private final Modifier[] modifiers;

        FieldGroup(Modifier... modifiers) {
            this.modifiers = modifiers;
        }

        /**
         * Find a field group that matches a given field declaration.
         *
         * @param declaration field declaration
         * @return FieldGroup
         */
        static FieldGroup find(FieldDeclaration declaration) {
            for (FieldGroup group : FieldGroup.values()) {
                if (Modifiers.containsAny(group.modifiers, declaration.modifiers())) {
                    return group;
                }
            }
            return OTHER_FIELDS;
        }
    }

    /**
     * Annotation.
     */
    static final class Annotation implements Node {

        private final TypeInfo type;
        private final Value value;

        /**
         * Create a new annotation.
         *
         * @param type annotation type
         */
        Annotation(Class<?> type) {
            this.type = TypeInfo.of(type);
            value = null;
        }

        /**
         * Create a new annotation.
         *
         * @param type  annotation type
         * @param value annotation value
         */
        Annotation(Class<?> type, Value value) {
            this.type = TypeInfo.of(type);
            this.value = value;
        }

        /**
         * Create a new annotation.
         *
         * @param type annotation type
         */
        Annotation(TypeInfo type) {
            this.type = Objects.requireNonNull(type, "type is null");
            this.value = null;
        }

        /**
         * Create a new annotation.
         *
         * @param type  annotation type
         * @param value annotation value
         */
        Annotation(TypeInfo type, Value value) {
            this.type = Objects.requireNonNull(type, "type is null");
            this.value = value;
        }

        /**
         * Get the annotation type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the value.
         *
         * @return optional of Value
         */
        Optional<Value> value() {
            return Optional.ofNullable(value);
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
        }
    }

    /**
     * Argument declaration.
     */
    static final class ArgumentDeclaration extends Declaration {

        private static final Modifier[] ALLOWED_MODIFIERS = new Modifier[]{
                Modifier.DEFAULT,
                Modifier.FINAL
        };

        private final TypeInfo type;
        private final String name;

        private ArgumentDeclaration(Builder builder) {
            super(builder);
            Modifier[] modifiers = modifiers();
            if (modifiers.length > 0 && !Modifiers.containsAny(modifiers, ALLOWED_MODIFIERS)) {
                throw new IllegalArgumentException("Invalid modifiers: " + Arrays.toString(modifiers));
            }
            type = Objects.requireNonNull(builder.type, "type is null");
            name = Objects.requireNonNull(builder.name, "name is null");
        }

        /**
         * Get the argument type.
         *
         * @return TypeInfo, never {@code null}
         */
        TypeInfo type() {
            return type;
        }

        /**
         * Get the argument name.
         *
         * @return name, never {@code null}
         */
        String name() {
            return name;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            resolver.accept(type);
        }

        /**
         * Create a new builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Argument declaration builder.
         */
        static final class Builder extends Declaration.Builder<Builder, ArgumentDeclaration> {

            private TypeInfo type;
            private String name;

            private Builder() {
            }

            /**
             * Argument type.
             *
             * @param type argument type
             * @return this builder
             */
            Builder type(TypeInfo type) {
                this.type = type;
                return this;
            }

            /**
             * Argument type.
             *
             * @param type argument type
             * @return this builder
             */
            Builder type(Class<?> type) {
                this.type = TypeInfo.of(type);
                return this;
            }

            /**
             * Set the argument name.
             *
             * @param name argument name
             * @return this builder
             */
            Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Build the argument declaration.
             *
             * @return ArgumentDeclaration
             */
            ArgumentDeclaration build() {
                return new ArgumentDeclaration(this);
            }
        }
    }

    /**
     * Method body.
     */
    static final class MethodBody implements Body {

        private static final MethodBody EMPTY = new MethodBody();

        private final List<Statement> statements;

        private MethodBody() {
            statements = Collections.emptyList();
        }

        private MethodBody(Builder builder) {
            statements = builder.statements;
        }

        /**
         * Get the statement.
         *
         * @return list of {@link Statement}, never {@code null}
         */
        List<Statement> statements() {
            return statements;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            statements.forEach(s -> s.resolveImports(resolver));
        }

        /**
         * Create a new method body builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Method body builder.
         */
        static final class Builder implements Supplier<MethodBody> {

            private final List<Statement> statements = new LinkedList<>();

            /**
             * Add a statement to the body.
             *
             * @param statement statement
             * @return this builder
             */
            Builder statement(Statement statement) {
                statements.add(statement);
                return this;
            }

            /**
             * Add a statement to the body.
             *
             * @param supplier statement supplier
             * @return this builder
             */
            Builder statement(Supplier<? extends Statement> supplier) {
                statements.add(supplier.get());
                return this;
            }

            MethodBody build() {
                return new MethodBody(this);
            }

            @Override
            public MethodBody get() {
                return build();
            }
        }
    }

    /**
     * Abstract method declaration.
     */
    abstract static class AbstractMethodDeclaration extends Declaration {

        private final List<ArgumentDeclaration> args;
        private final MethodBody body;

        protected AbstractMethodDeclaration(Builder<?, ?> builder) {
            super(builder);
            args = builder.args;
            body = Objects.requireNonNull(builder.body, "body is null");
        }

        /**
         * Get the argument declarations.
         *
         * @return list of {@link ArgumentDeclaration}, never {@code null}
         */
        List<ArgumentDeclaration> args() {
            return args;
        }

        /**
         * Get the method body.
         *
         * @return MethodBody
         */
        MethodBody body() {
            return body;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            args.forEach(a -> a.resolveImports(resolver));
            body.resolveImports(resolver);
        }

        /**
         * Abstract method declaration builder.
         *
         * @param <T> builder sub-class
         * @param <U> method declaration sub-class
         */
        @SuppressWarnings("unchecked")
        abstract static class Builder<T extends Builder<T, U>, U extends AbstractMethodDeclaration>
                extends Declaration.Builder<T, U> {

            private final List<ArgumentDeclaration> args = new LinkedList<>();
            private MethodBody body = MethodBody.EMPTY;

            private Builder() {
            }

            /**
             * Set the method body.
             *
             * @param body method body
             * @return this builder
             */
            T body(MethodBody body) {
                this.body = body;
                return (T) this;
            }

            /**
             * Set the method body.
             *
             * @param supplier method body supplier
             * @return this builder
             */
            T body(Supplier<MethodBody> supplier) {
                this.body = supplier.get();
                return (T) this;
            }

            /**
             * Add an argument declaration.
             *
             * @param argument method argument declaration
             * @return this builder
             */
            T arg(ArgumentDeclaration argument) {
                args.add(argument);
                return (T) this;
            }

            /**
             * Add an argument declaration.
             *
             * @param supplier method argument declaration supplier
             * @return this builder
             */
            T arg(Supplier<ArgumentDeclaration> supplier) {
                args.add(supplier.get());
                return (T) this;
            }

            /**
             * Add an argument declaration.
             *
             * @param type argument type
             * @param name argument name
             * @return this builder
             */
            T arg(Class<?> type, String name) {
                args.add(ArgumentDeclaration.builder().type(type).name(name).build());
                return (T) this;
            }

            /**
             * Add an argument declaration.
             *
             * @param type argument type
             * @param name argument name
             * @return this builder
             */
            T arg(TypeInfo type, String name) {
                args.add(ArgumentDeclaration.builder().type(type).name(name).build());
                return (T) this;
            }

            @Override
            public U get() {
                return build();
            }

            /**
             * Build the method declaration.
             *
             * @return U
             */
            abstract U build();
        }
    }

    /**
     * Method declaration.
     */
    static final class MethodDeclaration extends AbstractMethodDeclaration {

        private final TypeInfo returnType;
        private final String name;

        private MethodDeclaration(Builder builder) {
            super(builder);
            returnType = builder.returnType;
            name = Objects.requireNonNull(builder.name, "name is null");
        }

        /**
         * Get the return type.
         *
         * @return optional of TypeInfo
         */
        Optional<TypeInfo> returnType() {
            return Optional.ofNullable(returnType);
        }

        /**
         * Get the method name.
         *
         * @return method name, never {@code null}
         */
        String name() {
            return name;
        }

        @Override
        public void resolveImports(Consumer<TypeInfo> resolver) {
            super.resolveImports(resolver);
            if (returnType != null) {
                resolver.accept(returnType);
            }
        }

        /**
         * Create a new builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Method declaration builder.
         */
        static final class Builder extends AbstractMethodDeclaration.Builder<Builder, MethodDeclaration> {

            private TypeInfo returnType;
            private String name;

            private Builder() {
            }

            /**
             * Set the method return type.
             *
             * @param returnType return type
             * @return this builder
             */
            Builder returnType(Class<?> returnType) {
                this.returnType = TypeInfo.of(returnType);
                return this;
            }

            /**
             * Set the method return type.
             *
             * @param returnType return type
             * @return this builder
             */
            Builder returnType(TypeInfo returnType) {
                this.returnType = returnType;
                return this;
            }

            /**
             * Set the method name.
             *
             * @param name method name
             * @return this builder
             */
            Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Build the method declaration.
             *
             * @return MethodDeclaration
             */
            @Override
            MethodDeclaration build() {
                return new MethodDeclaration(this);
            }
        }
    }

    /**
     * Constructor declaration.
     */
    static final class ConstructorDeclaration extends AbstractMethodDeclaration {

        private TypeInfo type;

        private ConstructorDeclaration(Builder builder) {
            super(builder);
            this.type = builder.type;
        }

        /**
         * Get the type.
         *
         * @return TypeInfo
         */
        TypeInfo type() {
            if (type == null) {
                throw new IllegalStateException("type is not set");
            }
            return type;
        }

        /**
         * Create a new builder.
         *
         * @return Builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Constructor declaration builder.
         */
        static final class Builder extends AbstractMethodDeclaration.Builder<Builder, ConstructorDeclaration> {

            private TypeInfo type;

            private Builder() {
            }

            /**
             * Set the enclosing type.
             *
             * @param type enclosing type
             * @return this builder
             */
            Builder type(TypeInfo type) {
                this.type = type;
                return this;
            }

            /**
             * Build the constructor declaration.
             *
             * @return ConstructorDeclaration
             */
            @Override
            ConstructorDeclaration build() {
                return new ConstructorDeclaration(this);
            }
        }
    }
}
