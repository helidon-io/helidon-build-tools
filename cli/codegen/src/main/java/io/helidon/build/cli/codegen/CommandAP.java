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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import io.helidon.build.cli.codegen.AST.ClassBody;
import io.helidon.build.cli.codegen.AST.ClassDeclaration;
import io.helidon.build.cli.codegen.AST.ConstructorDeclaration;
import io.helidon.build.cli.codegen.AST.ConstructorInvocation;
import io.helidon.build.cli.codegen.AST.FieldDeclaration;
import io.helidon.build.cli.codegen.AST.Invocation.Style;
import io.helidon.build.cli.codegen.AST.MethodBody;
import io.helidon.build.cli.codegen.AST.MethodDeclaration;
import io.helidon.build.cli.codegen.AST.MethodInvocation;
import io.helidon.build.cli.codegen.AST.Value;
import io.helidon.build.cli.codegen.MetaModel.ArgumentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CLIMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CommandMetaModel;
import io.helidon.build.cli.codegen.MetaModel.FlagMetaModel;
import io.helidon.build.cli.codegen.MetaModel.FragmentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValueMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValuesMetaModel;
import io.helidon.build.cli.codegen.MetaModel.ParameterMetaModel;
import io.helidon.build.cli.codegen.TypeInfo.ElementInfo;
import io.helidon.build.cli.codegen.Visitor.VisitorError;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.CommandLineInterface;
import io.helidon.build.cli.harness.CommandModel;
import io.helidon.build.cli.harness.CommandParameters.ParameterInfo;
import io.helidon.build.cli.harness.CommandParser.Resolver;
import io.helidon.build.cli.harness.CommandRegistry;
import io.helidon.build.cli.harness.Option;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.cli.harness.Option.KeyValues;

import static io.helidon.build.cli.codegen.AST.Invocations.constructorInvocation;
import static io.helidon.build.cli.codegen.AST.Invocations.methodInvocation;
import static io.helidon.build.cli.codegen.AST.Modifier.FINAL;
import static io.helidon.build.cli.codegen.AST.Modifier.PRIVATE;
import static io.helidon.build.cli.codegen.AST.Modifier.PUBLIC;
import static io.helidon.build.cli.codegen.AST.Modifier.STATIC;
import static io.helidon.build.cli.codegen.AST.Refs.refCast;
import static io.helidon.build.cli.codegen.AST.Refs.staticRef;
import static io.helidon.build.cli.codegen.AST.Statements.returnStatement;
import static io.helidon.build.cli.codegen.AST.Statements.superStatement;
import static io.helidon.build.cli.codegen.AST.Values.arrayLiteral;
import static io.helidon.build.cli.codegen.AST.Values.arrayValueRef;
import static io.helidon.build.cli.codegen.AST.Values.booleanLiteral;
import static io.helidon.build.cli.codegen.AST.Values.classLiteral;
import static io.helidon.build.cli.codegen.AST.Values.nullLiteral;
import static io.helidon.build.cli.codegen.AST.Values.stringLiteral;
import static io.helidon.build.cli.codegen.AST.Values.valueCast;
import static io.helidon.build.cli.codegen.AST.Values.valueRef;

/**
 * Command annotation processor.
 */
@SupportedAnnotationTypes(value = {
        "io.helidon.build.cli.harness.CommandLineInterface",
        "io.helidon.build.cli.harness.Command",
        "io.helidon.build.cli.harness.CommandFragment",
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CommandAP extends AbstractProcessor {

    private static final String REGISTRY_SERVICE_FILE = "META-INF/services/io.helidon.build.cli.harness.CommandRegistry";
    private static final String REGISTRY_IMPL_SUFFIX = "Registry";
    private static final String MODEL_IMPL_SUFFIX = "Model";
    private static final String INFO_IMPL_SUFFIX = "Info";

    private boolean done;

    // TODO detect bad duplicates (options with same name but of different type)
    // TODO detect command name duplicates
    // TODO enforce options only for command fragments (i.e no argument)
    // TODO support inheritance

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!done) {

            // store the qualified names of the root element to infer the compilation units and play nice
            // with the incremental compilation performed by IDEs.
            Set<TypeInfo> rootTypes = new HashSet<>();
            for (Element element : roundEnv.getRootElements()) {
                if (element instanceof TypeElement) {
                    rootTypes.add(TypeInfo.of((TypeElement) element, processingEnv.getTypeUtils()));
                }
            }
            Visitor visitor = new Visitor(processingEnv, rootTypes);

            try {
                // process the fragments classes
                for (Element element : roundEnv.getElementsAnnotatedWith(CommandFragment.class)) {
                    visitor.visitFragment(element);
                }

                // process the command classes
                for (Element element : roundEnv.getElementsAnnotatedWith(Command.class)) {
                    visitor.visitCommand(element);
                }

                // process the cli classes
                for (Element element : roundEnv.getElementsAnnotatedWith(CommandLineInterface.class)) {
                    visitor.visitCLI(element);
                }

                generateSources(visitor);

            } catch (VisitorError ex) {
                processingEnv.getMessager().printMessage(Kind.ERROR, ex.getMessage(), ex.element());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            done = true;
        }
        return true;
    }

    private void generateSources(Visitor visitor) throws IOException {
        Filer filer = processingEnv.getFiler();

        // comment fragments
        for (FragmentMetaModel<ElementInfo> metaModel : visitor.fragments()) {
            ClassDeclaration modelClass = fragmentInfo(metaModel);
            JavaFileObject fileObject = filer.createSourceFile(
                    modelClass.type().qualifiedName(),
                    metaModel.annotatedType().element());
            try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                bw.write(FileHeaderJavacPlugin.header(metaModel.annotatedType().qualifiedName()));
                new ASTWriter(bw).write(modelClass);
            }
        }

        // commands
        for (CommandMetaModel<ElementInfo> metaModel : visitor.commands()) {
            ClassDeclaration modelClass = commandModel(metaModel);
            JavaFileObject fileObject = filer.createSourceFile(
                    modelClass.type().qualifiedName(),
                    metaModel.annotatedType().element());
            try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                bw.write(FileHeaderJavacPlugin.header(metaModel.annotatedType().qualifiedName()));
                new ASTWriter(bw).write(modelClass);
            }
        }

        // registry
        for (CLIMetaModel<ElementInfo> metaModel : visitor.clis()) {
            ClassDeclaration declaration = commandRegistry(metaModel);
            JavaFileObject fileObject = filer.createSourceFile(
                    declaration.type().qualifiedName(),
                    metaModel.annotatedType().element());
            try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                bw.write(FileHeaderJavacPlugin.header(metaModel.annotatedType().qualifiedName()));
                new ASTWriter(bw).write(declaration);
            }
            FileObject serviceFileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                    REGISTRY_SERVICE_FILE);
            try (BufferedWriter bw = new BufferedWriter(serviceFileObject.openWriter())) {
                bw.append(declaration.type().qualifiedName()).append("\n");
            }
        }
    }

    /**
     * Create a fragment info class declaration.
     *
     * @param metaModel command fragment meta-model
     * @return ClassDeclaration
     */
    static ClassDeclaration fragmentInfo(FragmentMetaModel<ElementInfo> metaModel) {
        TypeInfo typeInfo = fragmentInfoType(metaModel);
        return ClassDeclaration
                .builder()
                .javadoc("Command fragment info (generated).")
                .modifiers(PUBLIC, FINAL)
                .superClass(metaModel.paramInfoType())
                .type(typeInfo)
                .body(paramsClass(typeInfo, metaModel)
                        .constructor(ConstructorDeclaration
                                .builder()
                                .modifiers(PRIVATE)
                                .body(MethodBody
                                        .builder()
                                        .statement(superStatement(
                                                classLiteral(metaModel.annotatedType()),
                                                valueRef("PARAMS")))))
                        .method(MethodDeclaration
                                .builder()
                                .annotation(Override.class)
                                .annotation(SuppressWarnings.class, stringLiteral("unchecked"))
                                .modifiers(PUBLIC)
                                .returnType(metaModel.annotatedType())
                                .name("resolve")
                                .arg(Resolver.class, "resolver")
                                .body(MethodBody
                                        .builder()
                                        .statement(returnStatement(
                                                ConstructorInvocation
                                                        .builder()
                                                .style(Style.MULTI_LINE)
                                                .type(metaModel.annotatedType())
                                                .args(resolvedParams(metaModel.params())))))))
                .build();
    }

    /**
     * Create a command model class declaration.
     *
     * @param metaModel command metamodel
     * @return ClassDeclaration
     */
    static ClassDeclaration commandModel(CommandMetaModel<ElementInfo> metaModel) {
        TypeInfo typeInfo = commandInfoType(metaModel);
        return ClassDeclaration
                .builder()
                .javadoc("Command model (generated).")
                .modifiers(PUBLIC, FINAL)
                .superClass(CommandModel.class)
                .type(typeInfo)
                .body(paramsClass(typeInfo, metaModel)
                        .field(FieldDeclaration
                                .builder()
                                .javadoc("Command name.")
                                .modifiers(PUBLIC, STATIC, FINAL)
                                .type(String.class)
                                .name("NAME")
                                .value(stringLiteral(metaModel.annotation().name())))
                        .field(FieldDeclaration
                                .builder()
                                .javadoc("Command description.")
                                .modifiers(PUBLIC, STATIC, FINAL)
                                .type(String.class)
                                .name("DESCRIPTION")
                                .value(stringLiteral(metaModel.annotation().description())))
                        .constructor(ConstructorDeclaration
                                .builder()
                                .modifiers(PRIVATE)
                                .body(MethodBody
                                        .builder()
                                        .statement(superStatement(
                                                constructorInvocation(
                                                        metaModel.paramInfoType(),
                                                        valueRef("NAME"),
                                                        valueRef("DESCRIPTION")),
                                                valueRef("PARAMS")))))
                        .method(MethodDeclaration
                                .builder()
                                .annotation(Override.class)
                                .annotation(SuppressWarnings.class, stringLiteral("unchecked"))
                                .modifiers(PUBLIC)
                                .returnType(metaModel.annotatedType())
                                .name("createExecution")
                                .arg(Resolver.class, "resolver")
                                .body(MethodBody
                                        .builder()
                                        .statement(returnStatement(
                                                ConstructorInvocation
                                                        .builder()
                                                        .style(Style.MULTI_LINE)
                                                        .type(metaModel.annotatedType())
                                                        .args(resolvedParams(metaModel.params())))))))
                .build();
    }

    /**
     * Create a command registry class declaration.
     *
     * @param metaModel command line metamodel
     * @return ClassDeclaration
     */
    static ClassDeclaration commandRegistry(CLIMetaModel<ElementInfo> metaModel) {
        TypeInfo typeInfo = registryType(metaModel);
        return ClassDeclaration
                .builder()
                .javadoc("Command registry for " + metaModel.annotatedType().simpleName() + " (generated).")
                .modifiers(PUBLIC, FINAL)
                .type(typeInfo)
                .superClass(CommandRegistry.class)
                .body(ClassBody
                        .builder()
                        .field(FieldDeclaration
                                .builder()
                                .modifiers(PRIVATE, STATIC, FINAL)
                                .type(String.class)
                                .name("CLI_CLASS")
                                .value(MethodInvocation
                                        .builder()
                                        .method(valueRef(staticRef(metaModel.annotatedType()), "class"), "getName")))
                        .field(FieldDeclaration
                                .builder()
                                .modifiers(PRIVATE, STATIC, FINAL)
                                .type(String.class)
                                .name("CLI_NAME")
                                .value(stringLiteral(metaModel.annotation().name())))
                        .field(FieldDeclaration
                                .builder()
                                .modifiers(PRIVATE, STATIC, FINAL)
                                .type(String.class)
                                .name("CLI_DESCRIPTION")
                                .value(stringLiteral(metaModel.annotation().description())))
                        .field(FieldDeclaration
                                .builder()
                                .modifiers(PRIVATE, STATIC, FINAL)
                                .type(CommandModel[].class)
                                .name("COMMANDS")
                                .value(arrayLiteral(CommandModel[].class,
                                        metaModel.commands()
                                                 .stream()
                                                 .map(cmd -> valueRef(staticRef(commandInfoType(cmd)), "INSTANCE"))
                                                 .toArray(Value[]::new))))
                        .constructor(ConstructorDeclaration
                                .builder()
                                .javadoc("Create a new instance.")
                                .modifiers(PUBLIC)
                                .body(MethodBody
                                        .builder()
                                        .statement(superStatement(
                                                valueRef("CLI_CLASS"),
                                                valueRef("CLI_NAME"),
                                                valueRef("CLI_DESCRIPTION")))
                                        .statement(methodInvocation("register", valueRef("COMMANDS"))))))
                .build();
    }

    private static ClassBody.Builder paramsClass(TypeInfo typeInfo, MetaModel.ParametersMetaModel<?, ?> metaModel) {
        return ClassBody
                .builder()
                .field(FieldDeclaration
                        .builder()
                        .javadoc("Parameters.")
                        .modifiers(PUBLIC, STATIC, FINAL)
                        .type(ParameterInfo[].class)
                        .name("PARAMS")
                        .value(arrayLiteral(ParameterInfo[].class,
                                metaModel.params()
                                         .stream()
                                         .map(CommandAP::paramValue)
                                         .toArray(Value[]::new))))
                .field(FieldDeclaration
                        .builder()
                        .javadoc("Singleton instance.")
                        .modifiers(PUBLIC, STATIC, FINAL)
                        .type(typeInfo)
                        .name("INSTANCE")
                        .value(constructorInvocation(typeInfo)));
    }

    private static Value paramValue(ParameterMetaModel param) {
        if (param instanceof KeyValuesMetaModel<?> model) {
            KeyValues annotation = model.annotation();
            return ConstructorInvocation
                    .builder()
                    .style(Style.MULTI_LINE)
                    .type(model.paramInfoType())
                    .arg(classLiteral(model.annotatedType()))
                    .arg(stringLiteral(annotation.name()))
                    .arg(stringLiteral(annotation.description()))
                    .arg(booleanLiteral(annotation.required()))
                    .build();
        }
        if (param instanceof FlagMetaModel model) {
            Flag annotation = model.annotation();
            return ConstructorInvocation
                    .builder()
                    .style(Style.MULTI_LINE)
                    .type(model.paramInfoType())
                    .arg(stringLiteral(annotation.name()))
                    .arg(stringLiteral(annotation.description()))
                    .arg(booleanLiteral(annotation.visible()))
                    .build();
        }
        if (param instanceof KeyValueMetaModel<?> model) {
            TypeInfo annotatedType = model.annotatedType();
            KeyValue annotation = model.annotation();
            return ConstructorInvocation
                    .builder()
                    .style(Style.MULTI_LINE)
                    .type(model.paramInfoType())
                    .arg(classLiteral(annotatedType))
                    .arg(stringLiteral(annotation.name()))
                    .arg(stringLiteral(annotation.description()))
                    .arg(defaultValue(annotatedType, annotation.defaultValue()))
                    .arg(booleanLiteral(annotation.required()))
                    .arg(booleanLiteral(annotation.visible()))
                    .build();
        }
        if (param instanceof ArgumentMetaModel<?> model) {
            Option.Argument annotation = model.annotation();
            return ConstructorInvocation
                    .builder()
                    .style(Style.MULTI_LINE)
                    .type(model.paramInfoType())
                    .arg(classLiteral(model.annotatedType()))
                    .arg(stringLiteral(annotation.description()))
                    .arg(booleanLiteral(annotation.required()))
                    .build();
        }
        if (param instanceof MetaModel.FragmentMetaModel) {
            return valueRef(staticRef(fragmentInfoType((FragmentMetaModel<?>) param)), "INSTANCE");
        }
        throw new IllegalStateException("Unsupported parameter meta-model : " + param);
    }

    private static Value defaultValue(TypeInfo type, String defaultValue) {
        if (defaultValue != null && !defaultValue.isEmpty()) {
            if (type.is(String.class)) {
                return stringLiteral(defaultValue);
            }
            if (type.is(Integer.class)) {
                return MethodInvocation
                        .builder()
                        .method(Integer.class, "parseInt")
                        .arg(stringLiteral(defaultValue))
                        .build();
            }
            if (type.is(File.class)) {
                return ConstructorInvocation
                        .builder()
                        .type(File.class)
                        .arg(stringLiteral(defaultValue))
                        .build();
            }
            if (type.isEnum()) {
                return MethodInvocation
                        .builder()
                        .method(type, "valueOf")
                        .arg(stringLiteral(defaultValue))
                        .build();
            }
        }
        return nullLiteral();
    }

    private static Value[] resolvedParams(List<ParameterMetaModel> params) {
        List<Value> resolvedParams = new LinkedList<>();
        Iterator<ParameterMetaModel> it = params.iterator();
        for (int i = 0; it.hasNext(); i++) {
            ParameterMetaModel param = it.next();
            if (param instanceof FragmentMetaModel<?> fragment) {
                resolvedParams.add(MethodInvocation
                        .builder()
                        .method(refCast(fragmentInfoType(fragment), arrayValueRef("PARAMS", i)), "resolve")
                        .arg(valueRef("resolver"))
                        .build());
            } else {
                resolvedParams.add(MethodInvocation
                        .builder()
                        .method("resolver", "resolve")
                        .arg(valueCast(param.paramInfoType(), arrayValueRef("PARAMS", i)))
                        .build());
            }
        }
        return resolvedParams.toArray(Value[]::new);
    }

    private static TypeInfo registryType(CLIMetaModel<?> metaModel) {
        return pseudoType(metaModel, REGISTRY_IMPL_SUFFIX);
    }

    private static TypeInfo fragmentInfoType(FragmentMetaModel<?> metaModel) {
        return pseudoType(metaModel, INFO_IMPL_SUFFIX);
    }

    private static TypeInfo commandInfoType(CommandMetaModel<?> metaModel) {
        return pseudoType(metaModel, MODEL_IMPL_SUFFIX);
    }

    private static TypeInfo pseudoType(MetaModel<?, ?> metaModel, String suffix) {
        TypeInfo typeInfo = metaModel.annotatedType();
        String pkg = typeInfo.pkg();
        String name = typeInfo.qualifiedName()
                              .substring(pkg.length() + 1)
                              .replace(".", "");
        return TypeInfo.of(pkg + "." + name + suffix, false);
    }
}
