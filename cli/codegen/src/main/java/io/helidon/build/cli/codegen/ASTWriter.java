/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

import io.helidon.build.cli.codegen.AST.AbstractInvocation;
import io.helidon.build.cli.codegen.AST.Annotation;
import io.helidon.build.cli.codegen.AST.ArgumentDeclaration;
import io.helidon.build.cli.codegen.AST.ArrayLiteral;
import io.helidon.build.cli.codegen.AST.ArrayValueRef;
import io.helidon.build.cli.codegen.AST.Body;
import io.helidon.build.cli.codegen.AST.BooleanLiteral;
import io.helidon.build.cli.codegen.AST.ClassBody;
import io.helidon.build.cli.codegen.AST.ClassDeclaration;
import io.helidon.build.cli.codegen.AST.ClassLiteral;
import io.helidon.build.cli.codegen.AST.ConstructorDeclaration;
import io.helidon.build.cli.codegen.AST.ConstructorInvocation;
import io.helidon.build.cli.codegen.AST.Declaration;
import io.helidon.build.cli.codegen.AST.FieldDeclaration;
import io.helidon.build.cli.codegen.AST.FieldGroup;
import io.helidon.build.cli.codegen.AST.Invocation;
import io.helidon.build.cli.codegen.AST.Invocation.Style;
import io.helidon.build.cli.codegen.AST.MethodBody;
import io.helidon.build.cli.codegen.AST.MethodDeclaration;
import io.helidon.build.cli.codegen.AST.MethodInvocation;
import io.helidon.build.cli.codegen.AST.Modifier;
import io.helidon.build.cli.codegen.AST.NullLiteral;
import io.helidon.build.cli.codegen.AST.Ref;
import io.helidon.build.cli.codegen.AST.RefCast;
import io.helidon.build.cli.codegen.AST.ReturnStatement;
import io.helidon.build.cli.codegen.AST.Statement;
import io.helidon.build.cli.codegen.AST.StaticRef;
import io.helidon.build.cli.codegen.AST.StringLiteral;
import io.helidon.build.cli.codegen.AST.SuperStatement;
import io.helidon.build.cli.codegen.AST.TypeDeclaration;
import io.helidon.build.cli.codegen.AST.Value;
import io.helidon.build.cli.codegen.AST.ValueCast;
import io.helidon.build.cli.codegen.AST.ValueRef;
import io.helidon.build.common.Unchecked.CheckedConsumer;

import static io.helidon.build.common.Unchecked.unchecked;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * AST writer.
 */
class ASTWriter {

    private final Writer writer;
    private int lineCount = 0;
    private int indentLevel = 0;

    ASTWriter(Writer writer) {
        this.writer = writer;
    }

    private static String typeSimpleName(TypeInfo type) {
        String simpleName = type.simpleName();
        int index = simpleName.lastIndexOf('$');
        if (index > 0) {
            return simpleName.substring(index + 1);
        }
        return simpleName;
    }

    private static <T> void write(Iterator<T> iterator, CheckedConsumer<T, IOException> consumer) {
        iterator.forEachRemaining(unchecked(consumer));
    }

    private <T> void write(Iterator<T> iterator, CheckedConsumer<T, IOException> consumer, String separator) {
        while (iterator.hasNext()) {
            try {
                consumer.accept(iterator.next());
                if (iterator.hasNext()) {
                    write(separator);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void write(String str) throws IOException {
        writer.write(str);
    }

    private void writeIndent() throws IOException {
        for (int i = 0; i < indentLevel; i++) {
            write("    ");
        }
    }

    private void writeClassLiteral(ClassLiteral literal) throws IOException {
        write(typeSimpleName(literal.value()));
        write(".class");
    }

    private void writeNullLiteral() throws IOException {
        write("null");
    }

    private void writeStringLiteral(StringLiteral literal) throws IOException {
        write("\"");
        write(literal.value());
        write("\"");
    }

    private void writeBooleanLiteral(BooleanLiteral literal) throws IOException {
        write(String.valueOf(literal.value()));
    }

    private void writeArrayLiteral(ArrayLiteral literal) throws IOException {
        write("new ");
        String simpleName = typeSimpleName(literal.type());
        simpleName = simpleName.substring(0, simpleName.length() - 2);
        write(simpleName);
        Value[] values = literal.values();
        if (values.length == 0) {
            write("[0]");
        } else {
            write("[]{\n");
            lineCount++;
            indentLevel += 2;
            write(Arrays.stream(values).iterator(), value -> {
                writeIndent();
                writeValue(value);
            }, ",\n");
            lineCount += values.length - 1;
            indentLevel -= 2;
            write("\n");
            lineCount++;
            writeIndent();
            write("}");
        }
    }

    private void writeValueCast(ValueCast valueCast) throws IOException {
        write("(");
        writeType(valueCast.type());
        write(") ");
        writeValue(valueCast.ref());
    }

    private void writeArrayValueRef(ArrayValueRef arrayValueRef) throws IOException {
        write(arrayValueRef.ref());
        write("[");
        write(String.valueOf(arrayValueRef.index()));
        write("]");
    }

    private void writeValueRef(ValueRef valueRef) throws IOException {
        valueRef.parent().ifPresent(unchecked(parentRef -> {
            writeRef(parentRef);
            write(".");
        }));
        write(valueRef.ref());
    }

    private void writeStaticRef(StaticRef staticRef) throws IOException {
        write(typeSimpleName(staticRef.type()));
    }

    private void writeRefCast(RefCast refCast) throws IOException {
        write("((");
        write(typeSimpleName(refCast.type()));
        write(") ");
        writeRef(refCast.ref());
        write(")");
    }

    private void writeMethodInvocation(MethodInvocation invocation) throws IOException {
        invocation.parentRef().ifPresent(unchecked(ref -> {
            writeRef(ref);
            write(".");
        }));
        write(invocation.method());
        write("(");
        writeInvocationArguments(invocation);
        write(")");
    }

    private void writeInvocationArguments(AbstractInvocation invocation) throws IOException {
        List<Value> args = invocation.args();
        if (invocation.style() == Style.SINGLE_LINE) {
            write(args.iterator(), this::writeValue, ", ");
        } else if (invocation.style() == Style.MULTI_LINE) {
            if (!args.isEmpty()) {
                write("\n");
                indentLevel += 2;
                write(args.iterator(), arg -> {
                    writeIndent();
                    writeValue(arg);
                }, ",\n");
                lineCount += args.size();
                indentLevel -= 2;
            }
        }
    }

    private void writeConstructorInvocation(ConstructorInvocation invocation) throws IOException {
        write("new ");
        writeType(invocation.type(), true);
        write("(");
        writeInvocationArguments(invocation);
        write(")");
    }

    private void writeAnnotations(Declaration declaration) {
        write(declaration.annotations().iterator(), a -> {
            writeIndent();
            writeAnnotation(a);
            write("\n");
            lineCount++;
        });
    }

    private void writeFieldDeclaration(FieldDeclaration declaration) throws IOException {
        declaration.javadoc().ifPresent(unchecked(this::writeJavadoc));
        writeAnnotations(declaration);
        writeIndent();
        writeModifiers(declaration.modifiers());
        writeType(declaration.type());
        write(" ");
        write(declaration.name());
        declaration.value().ifPresent(unchecked(value -> {
            write(" = ");
            writeValue(value);
        }));
        write(";\n");
        lineCount++;
    }

    private void writeConstructorDeclaration(ConstructorDeclaration declaration) throws IOException {
        write("\n");
        lineCount++;
        declaration.javadoc().ifPresent(unchecked(this::writeJavadoc));
        writeAnnotations(declaration);
        writeIndent();
        writeModifiers(declaration.modifiers());
        writeType(declaration.type());
        write("(");
        write(declaration.args().iterator(), this::writeArgumentDeclaration, ", ");
        write(")");
        writeBody(declaration.body());
    }

    private void writeMethodDeclaration(MethodDeclaration declaration) throws IOException {
        write("\n");
        lineCount++;
        declaration.javadoc().ifPresent(unchecked(this::writeJavadoc));
        writeAnnotations(declaration);
        writeIndent();
        writeModifiers(declaration.modifiers());
        write(declaration.returnType().map(ASTWriter::typeSimpleName).orElse("void"));
        write(" ");
        write(declaration.name());
        write("(");
        write(declaration.args().iterator(), this::writeArgumentDeclaration, ", ");
        write(")");
        writeBody(declaration.body());
    }

    private void writeArgumentDeclaration(ArgumentDeclaration declaration) throws IOException {
        writeModifiers(declaration.modifiers());
        writeType(declaration.type());
        write(" ");
        write(declaration.name());
    }

    private void writeType(TypeInfo type) throws IOException {
        writeType(type, false);
    }

    private void writeType(TypeInfo type, boolean infer) throws IOException {
        write(typeSimpleName(type));
        TypeInfo[] typeParams = type.typeParams();
        if (typeParams.length > 0) {
            write("<");
            if (!infer) {
                for (int i = 0; i < typeParams.length; i++) {
                    write(typeSimpleName(typeParams[i]));
                    if (i < typeParams.length - 1) {
                        write(", ");
                    }
                }
            }
            write(">");
        }
    }

    private void writeClassDeclaration(ClassDeclaration declaration) throws IOException {
        write("\n");
        lineCount++;
        writeAnnotations(declaration);
        declaration.javadoc().ifPresent(unchecked(this::writeJavadoc));
        writeIndent();
        writeModifiers(declaration.modifiers());
        write("class ");
        writeType(declaration.type());
        declaration.superClass().ifPresent(unchecked(type -> {
            write(" extends ");
            writeType(type);
        }));
        writeBody(declaration.body());
    }

    private void writeClassBody(ClassBody body) {
        new Object() {
            private FieldGroup lastGroup;
            private boolean insertLine;

            void writeFields(List<FieldDeclaration> fields) {
                fields.stream()
                      .sorted()
                      .collect(groupingBy(FieldGroup::find, TreeMap::new, mapping(Function.identity(), toList())))
                      .forEach(this::writeGroup);
            }

            void writeGroup(FieldGroup group, List<FieldDeclaration> fields) {
                insertLine = lastGroup == null || group != lastGroup;
                write(fields.iterator(), this::writeField);
                lastGroup = group;
            }

            void writeField(FieldDeclaration field) throws IOException {
                if (insertLine || field.javadoc().isPresent()) {
                    write("\n");
                    lineCount++;
                    insertLine = false;
                }
                writeDeclaration(field);
            }
        }.writeFields(body.fields());
        write(body.constructors().iterator(), this::writeDeclaration);
        write(body.methods().iterator(), this::writeDeclaration);
    }

    private void writeMethodBody(MethodBody body) {
        write(body.statements().iterator(), this::writeStatement);
    }

    private void writeAnnotation(Annotation annotation) throws IOException {
        write("@");
        write(typeSimpleName(annotation.type()));
        annotation.value().ifPresent(unchecked(v -> {
            write("(");
            writeValue(v);
            write(")");
        }));
    }

    private void writeReturnStatement(ReturnStatement statement) throws IOException {
        writeIndent();
        write("return");
        statement.value().ifPresent(unchecked(value -> {
            write(" ");
            writeValue(value);
        }));
        write(";\n");
        lineCount++;
    }

    private void writeSuperStatement(SuperStatement statement) throws IOException {
        writeIndent();
        write("super(");
        write(statement.args().iterator(), this::writeValue, ", ");
        write(");\n");
        lineCount++;
    }

    private void writeJavadoc(String javadoc) throws IOException {
        writeIndent();
        write("/**\n");
        lineCount++;
        write(javadoc.lines().iterator(), line -> {
            writeIndent();
            write(" * ");
            write(line);
            write("\n");
            lineCount++;
        });
        writeIndent();
        write(" */\n");
        lineCount++;
    }

    private void writeModifiers(Modifier... modifiers) throws IOException {
        for (Modifier modifier : modifiers) {
            switch (modifier) {
                case PUBLIC:
                    write("public ");
                    break;
                case PROTECTED:
                    write("protected ");
                    break;
                case PRIVATE:
                    write("private ");
                    break;
                case STATIC:
                    write("static ");
                    break;
                case FINAL:
                    write("final ");
                    break;
                default:
                    continue;
            }
        }
    }

    private void writePackage(TypeInfo type) throws IOException {
        write("package ");
        write(type.pkg());
        write(";\n\n");
        lineCount += 2;
    }

    private void writeImport(String type) throws IOException {
        write("import ");
        write(type);
        write(";\n");
        lineCount++;
    }

    private void writeImports(TypeDeclaration<?> typeDeclaration) throws IOException {
        HashSet<String> imports = new HashSet<>();
        typeDeclaration.resolveImports(type -> {
            if (!type.pkg().equals("java.lang")) {
                String pkg = type.pkg();
                String qualifiedName = type.qualifiedName();
                String name = qualifiedName.substring(qualifiedName.indexOf(pkg))
                                           .replace(";", "")
                                           .replace("[]", "")
                                           .replace("$", ".");
                if (!pkg.equals(typeDeclaration.type().pkg()) || name.lastIndexOf(".") > pkg.length()) {
                    imports.add(name);
                }
            }
        });
        int count = lineCount;
        write(imports.stream()
                     .filter(s -> s.startsWith("java."))
                     .sorted().iterator(), this::writeImport);
        if (lineCount > count) {
            write("\n");
            lineCount++;
        }
        count = lineCount;
        write(imports.stream()
                     .filter(s -> s.startsWith("javax."))
                     .sorted().iterator(), this::writeImport);
        if (lineCount > count) {
            write("\n");
            lineCount++;
        }
        write(imports.stream()
                     .filter(s -> !s.startsWith("java.") && !s.startsWith("javax."))
                     .sorted().iterator(), this::writeImport);
    }

    /**
     * Write the given ref.
     *
     * @param ref ref
     * @throws IOException if an IO error occurs
     */
    void writeRef(Ref ref) throws IOException {
        if (ref instanceof ArrayValueRef) {
            writeArrayValueRef((ArrayValueRef) ref);
        } else if (ref instanceof ValueRef) {
            writeValueRef((ValueRef) ref);
        } else if (ref instanceof RefCast) {
            writeRefCast((RefCast) ref);
        } else if (ref instanceof StaticRef) {
            writeStaticRef((StaticRef) ref);
        } else {
            throw new IllegalArgumentException("Unsupported ref: " + ref);
        }
    }

    /**
     * Write the given value.
     *
     * @param value value
     * @throws IOException if an IO error occurs
     */
    void writeValue(Value value) throws IOException {
        if (value instanceof StringLiteral) {
            writeStringLiteral((StringLiteral) value);
        } else if (value instanceof NullLiteral) {
            writeNullLiteral();
        } else if (value instanceof ClassLiteral) {
            writeClassLiteral((ClassLiteral) value);
        } else if (value instanceof BooleanLiteral) {
            writeBooleanLiteral((BooleanLiteral) value);
        } else if (value instanceof ArrayLiteral) {
            writeArrayLiteral((ArrayLiteral) value);
        } else if (value instanceof ValueCast) {
            writeValueCast((ValueCast) value);
        } else if (value instanceof ArrayValueRef) {
            writeArrayValueRef((ArrayValueRef) value);
        } else if (value instanceof ValueRef) {
            writeValueRef((ValueRef) value);
        } else if (value instanceof Invocation) {
            writeInvocation((Invocation) value);
        } else {
            throw new IllegalArgumentException("Unsupported value: " + value);
        }
    }

    /**
     * Write the given declaration.
     *
     * @param declaration declaration
     * @throws IOException if an IO error occurs
     */
    void writeDeclaration(Declaration declaration) throws IOException {
        if (declaration instanceof ClassDeclaration) {
            writeClassDeclaration((ClassDeclaration) declaration);
        } else if (declaration instanceof FieldDeclaration) {
            writeFieldDeclaration((FieldDeclaration) declaration);
        } else if (declaration instanceof ConstructorDeclaration) {
            writeConstructorDeclaration((ConstructorDeclaration) declaration);
        } else if (declaration instanceof MethodDeclaration) {
            writeMethodDeclaration((MethodDeclaration) declaration);
        } else if (declaration instanceof ArgumentDeclaration) {
            writeArgumentDeclaration((ArgumentDeclaration) declaration);
        } else {
            throw new IllegalArgumentException("Unsupported declaration: " + declaration);
        }
    }

    /**
     * Write the given statement.
     *
     * @param statement statement
     * @throws IOException if an IO error occurs
     */
    void writeStatement(Statement statement) throws IOException {
        if (statement instanceof ReturnStatement) {
            writeReturnStatement((ReturnStatement) statement);
        } else if (statement instanceof SuperStatement) {
            writeSuperStatement((SuperStatement) statement);
        } else if (statement instanceof Invocation) {
            writeIndent();
            writeInvocation((Invocation) statement);
            write(";\n");
            lineCount++;
        } else {
            throw new IllegalArgumentException("Unsupported statement: " + statement);
        }
    }

    /**
     * Write the given invocation.
     *
     * @param invocation invocation
     * @throws IOException if an IO error occurs
     */
    void writeInvocation(Invocation invocation) throws IOException {
        if (invocation instanceof ConstructorInvocation) {
            writeConstructorInvocation((ConstructorInvocation) invocation);
        } else if (invocation instanceof MethodInvocation) {
            writeMethodInvocation((MethodInvocation) invocation);
        } else {
            throw new IllegalArgumentException("Unsupported invocation: " + invocation);
        }
    }

    /**
     * Write the given body.
     *
     * @param body body
     * @throws IOException if an IO error occurs
     */
    void writeBody(Body body) throws IOException {
        write(" {\n");
        lineCount++;
        indentLevel++;
        if (body instanceof ClassBody) {
            writeClassBody((ClassBody) body);
        } else if (body instanceof MethodBody) {
            writeMethodBody((MethodBody) body);
        } else {
            throw new IllegalArgumentException("Unsupported body: " + body);
        }
        indentLevel--;
        writeIndent();
        write("}\n");
        lineCount++;
    }

    /**
     * Write the whole type declaration.
     *
     * @param type type declaration
     * @throws IOException if an IO error occurs
     */
    void write(TypeDeclaration<?> type) throws IOException {
        writePackage(type.type());
        writeImports(type);
        writeDeclaration(type);
    }
}
