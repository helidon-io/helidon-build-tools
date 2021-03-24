/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor9;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.Types;

import io.helidon.build.cli.codegen.MetaModel.ArgumentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CLIMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CommandMetaModel;
import io.helidon.build.cli.codegen.MetaModel.FlagMetaModel;
import io.helidon.build.cli.codegen.MetaModel.FragmentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValueMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValuesMetaModel;
import io.helidon.build.cli.codegen.MetaModel.ParameterMetaModel;
import io.helidon.build.cli.codegen.TypeInfo.ElementInfo;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.CommandLineInterface;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option;

/**
 * Meta-model visitor.
 */
final class Visitor {

    //CHECKSTYLE:OFF
    static final String MISSING_CLI_ANNOTATION = String.format("Missing @%s annotation", CommandLineInterface.class.getSimpleName());
    static final String MISSING_FRAGMENT_ANNOTATION = String.format("Missing @%s annotation", CommandFragment.class.getSimpleName());
    static final String MISSING_COMMAND_ANNOTATION = String.format("Missing @%s annotation", Command.class.getSimpleName());
    static final String MISSING_CREATOR_ANNOTATION = String.format("Missing a constructor annotated with @%s", Creator.class.getSimpleName());
    static final String MISSING_COMMAND_EXECUTION = String.format("Classes annotated with @%s must implement %s", Command.class.getSimpleName(), CommandExecution.class.getSimpleName());
    static final String FRAGMENT_OPTION_DUPLICATES = "Fragment has duplicated options";
    static final String INVALID_ARGUMENT_TYPE = String.format("Invalid @%s type", Option.Argument.class.getSimpleName());
    static final String INVALID_FLAG_TYPE = String.format("Invalid @%s type", Option.Flag.class.getSimpleName());
    static final String INVALID_KEY_VALUE_TYPE = String.format("Invalid @%s type", Option.KeyValue.class.getSimpleName());
    static final String INVALID_KEY_VALUES_TYPE = String.format("Invalid @%s type", Option.KeyValues.class.getSimpleName());
    static final String INVALID_KEY_VALUES_TYPE_PARAMETER = String.format("Invalid @%s type parameter", Option.KeyValues.class.getSimpleName());
    static final String INVALID_OPTION = "Invalid option";
    static final String INVALID_NAME = "Invalid name";
    static final String INVALID_DESCRIPTION = "Invalid description";
    static final String OPTION_ALREADY_DEFINED = "Option already defined";
    //CHECKSTYLE:ON

    private final List<CLIMetaModel<ElementInfo>> clis = new LinkedList<>();
    private final Map<TypeInfo, FragmentMetaModel<ElementInfo>> fragments = new HashMap<>();
    private final Map<TypeInfo, CommandMetaModel<ElementInfo>> commands = new HashMap<>();
    private final Set<TypeInfo> rootTypes;
    private final Types types;

    /**
     * Create a new instance.
     *
     * @param env       processing environment
     * @param rootTypes root types
     */
    Visitor(ProcessingEnvironment env, Set<TypeInfo> rootTypes) {
        types = env.getTypeUtils();
        this.rootTypes = rootTypes;
    }

    /**
     * Get the meta-models for the visited commands.
     *
     * @return collection of CommandMetaModel
     */
    Collection<CommandMetaModel<ElementInfo>> commands() {
        return commands.values();
    }

    /**
     * Get the meta-models for the visited command fragments.
     *
     * @return collection of CommandFragmentMetaModel
     */
    Collection<FragmentMetaModel<ElementInfo>> fragments() {
        return fragments.values();
    }

    /**
     * Get the meta-models for the visited CLI classes.
     *
     * @return collection of CLIMetaModel
     */
    Collection<CLIMetaModel<ElementInfo>> clis() {
        return clis;
    }

    /**
     * Visit a command fragment class.
     *
     * @param element element to visit
     * @throws VisitorError if a visitor error occurs
     */
    void visitFragment(Element element) throws VisitorError {
        FragmentMetaModel<ElementInfo> metaModel = ElementTypeVisitor.accept(element, this::doVisitFragment);
        fragments.put(metaModel.annotatedType(), metaModel);
    }

    /**
     * Visit a command class.
     *
     * @param element element to visit
     * @throws VisitorError if a visitor error occurs
     */
    void visitCommand(Element element) throws VisitorError {
        CommandMetaModel<ElementInfo> metaModel = ElementTypeVisitor.accept(element, this::doVisitCommand);
        commands.put(metaModel.annotatedType(), metaModel);
    }

    /**
     * Visit a CLI class.
     *
     * @param elt element to visit
     * @throws VisitorError if a visitor error occurs
     */
    void visitCLI(Element elt) throws VisitorError {
        clis.add(ElementTypeVisitor.accept(elt, this::doVisitCLI));
    }

    /**
     * Visitor error.
     */
    static final class VisitorError extends RuntimeException {

        private final Element element;

        private VisitorError(String message, Element element) {
            super(message);
            this.element = element;
        }

        /**
         * Get the visited element associated with the error.
         *
         * @return Element
         */
        Element element() {
            return element;
        }
    }

    private FragmentMetaModel<ElementInfo> doVisitFragment(TypeElement type) {
        CommandFragment annotation = type.getAnnotation(CommandFragment.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Type is not annotated with @"
                    + CommandFragment.class.getSimpleName() + ": " + type);
        }
        return new FragmentMetaModel<>(annotation, TypeInfo.of(type, types),
                ElementExecutableVisitor.accept(findCreator(type), this::doVisitConstructor));
    }

    private CommandMetaModel<ElementInfo> doVisitCommand(TypeElement type) {
        Command annotation = type.getAnnotation(Command.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Type is not annotated with @"
                    + Command.class.getSimpleName() + ": " + type);
        }
        if (annotation.name().isEmpty() || !Option.VALID_NAME.test(annotation.name())) {
            throw new VisitorError(INVALID_NAME, type);
        }
        if (annotation.description().isEmpty()) {
            throw new VisitorError(INVALID_DESCRIPTION, type);
        }
        ElementInfo elementInfo = TypeInfo.of(type, types);
        if (!elementInfo.hasInterface(CommandExecution.class)) {
            throw new VisitorError(MISSING_COMMAND_EXECUTION, type);
        }
        return new CommandMetaModel<>(annotation, elementInfo,
                ElementExecutableVisitor.accept(findCreator(type), this::doVisitConstructor));
    }

    private CLIMetaModel<ElementInfo> doVisitCLI(TypeElement type) {

        // Accessing the class objects directly from the annotation instance throws exceptions if the class is
        // not already compiled.
        // Using the annotation mirrors instead...
        AnnotationMirror mirror = null;
        for (AnnotationMirror am : type.getAnnotationMirrors()) {
            ElementInfo elementInfo = TypeInfo.of((TypeElement) am.getAnnotationType().asElement(), types);
            if (elementInfo.is(CommandLineInterface.class)) {
                mirror = am;
            }
        }
        if (mirror == null) {
            throw new VisitorError(MISSING_CLI_ANNOTATION, type);
        }

        String name = (String) annotationValue("name", mirror).getValue();
        if (name.isEmpty() || !CommandLineInterface.VALID_NAME.test(name)) {
            throw new VisitorError(INVALID_NAME, type);
        }

        String description = (String) annotationValue("description", mirror).getValue();
        if (description.isEmpty()) {
            throw new VisitorError(INVALID_DESCRIPTION, type);
        }

        // Resolve the annotation value into a list of element info
        List<ElementInfo> elementInfos = AnnotationArrayValueVisitor.accept(annotationValue("commands", mirror),
                v -> AnnotationValueTypeVisitor.accept(v,
                        t -> TypeInfo.of((TypeElement) types.asElement(t), types)));

        List<CommandMetaModel<ElementInfo>> commands = new LinkedList<>();
        for (ElementInfo elementInfo : elementInfos) {
            CommandMetaModel<ElementInfo> command = Visitor.this.commands.get(elementInfo);
            if (command != null) {
                commands.add(command);
            } else {
                if (rootTypes.contains(rootEnclosingTypeOf(elementInfo))) {
                    // report an error if the file is present in the compilation unit
                    throw new VisitorError(MISSING_COMMAND_ANNOTATION, type);
                }
            }
        }

        // create a proxy
        CommandLineInterface annotation = (CommandLineInterface) Proxy.newProxyInstance(
                Visitor.class.getClassLoader(), new Class<?>[]{CommandLineInterface.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "name":
                            return name;
                        case "description":
                            return description;
                        default:
                            throw new UnsupportedOperationException(methodName);
                    }
                });
        return new CLIMetaModel<>(annotation, TypeInfo.of(type, types), commands);
    }


    private static <T extends Annotation> boolean processOption(OptionContext context,
                                                                Class<T> annotationClass,
                                                                BiFunction<T, OptionContext, ParameterMetaModel> function) {

        T annotation = context.elementInfo.element().getAnnotation(annotationClass);
        if (annotation != null) {
            context.params.add(function.apply(annotation, context));
            return true;
        }
        return false;
    }

    private static ParameterMetaModel processArgumentOption(Option.Argument annotation, OptionContext context) {
        context.checkOptionDescription(annotation.description());
        if (context.elementInfo.is(Option.Argument.SUPPORTED_TYPES)) {
            return new ArgumentMetaModel<>(annotation, context.elementInfo);
        } else {
            throw new VisitorError(INVALID_ARGUMENT_TYPE, context.elementInfo.element());
        }
    }

    private static ParameterMetaModel processFlagOption(Option.Flag annotation, OptionContext context) {
        context.checkOptionName(annotation.name());
        context.checkOptionDescription(annotation.description());
        if (context.elementInfo.is(Boolean.class)) {
            context.names.add(annotation.name());
            return new FlagMetaModel(annotation);
        } else {
            throw new VisitorError(INVALID_FLAG_TYPE, context.elementInfo.element());
        }
    }

    private static ParameterMetaModel processKeyValueOption(Option.KeyValue annotation, OptionContext context) {
        context.checkOptionName(annotation.name());
        context.checkOptionDescription(annotation.description());
        if (context.elementInfo.is(Option.KeyValue.SUPPORTED_TYPES)) {
            context.names.add(annotation.name());
            return new KeyValueMetaModel<>(annotation, context.elementInfo);
        } else {
            throw new VisitorError(INVALID_KEY_VALUE_TYPE, context.elementInfo.element());
        }
    }

    private static ParameterMetaModel processKeyValuesOption(Option.KeyValues annotation, OptionContext context) {
        context.checkOptionName(annotation.name());
        context.checkOptionDescription(annotation.description());
        if (context.elementInfo.is(Collection.class)) {
            TypeInfo[] typeParams = context.elementInfo.typeParams();
            if (typeParams.length == 1) {
                TypeInfo typeParam = typeParams[0];
                if (typeParam.is(Option.KeyValues.SUPPORTED_TYPES)) {
                    context.names.add(annotation.name());
                    return new KeyValuesMetaModel<>(annotation, typeParam);
                }
            }
            throw new VisitorError(INVALID_KEY_VALUES_TYPE_PARAMETER, context.elementInfo.element());
        } else {
            throw new VisitorError(INVALID_KEY_VALUES_TYPE, context.elementInfo.element());
        }
    }

    private List<ParameterMetaModel> doVisitConstructor(ExecutableElement executable) {
        List<ParameterMetaModel> params = new LinkedList<>();
        List<String> names = new ArrayList<>();
        for (VariableElement variable : executable.getParameters()) {
            ElementInfo elementInfo = TypeInfo.of(variable, types);
            OptionContext context = new OptionContext(elementInfo, names, params);
            if (!(processOption(context, Option.Argument.class, Visitor::processArgumentOption)
                    || processOption(context, Option.Flag.class, Visitor::processFlagOption)
                    || processOption(context, Option.KeyValue.class, Visitor::processKeyValueOption)
                    || processOption(context, Option.KeyValues.class, Visitor::processKeyValuesOption))) {
                if (!elementInfo.isPrimitive()) {
                    FragmentMetaModel<ElementInfo> fragment = fragments.get(elementInfo);
                    if (fragment != null) {
                        if (!fragment.duplicates(names).isEmpty()) {
                            throw new VisitorError(FRAGMENT_OPTION_DUPLICATES, elementInfo.element());
                        }
                        names.addAll(fragment.paramNames());
                        params.add(fragment);
                    } else {
                        if (rootTypes.contains(rootEnclosingTypeOf(elementInfo))) {
                            // report an error if the file is present in the compilation unit
                            throw new VisitorError(MISSING_FRAGMENT_ANNOTATION, elementInfo.element());
                        }
                    }
                } else {
                    throw new VisitorError(INVALID_OPTION, variable);
                }
            }
        }
        return params;
    }

    private ElementInfo rootEnclosingTypeOf(ElementInfo elementInfo) {
        Element element = elementInfo.element();
        while (IsNotPackageVisitor.accept(element.getEnclosingElement())) {
            element = element.getEnclosingElement();
        }
        return TypeInfo.of(element, types);
    }

    private static Element findCreator(TypeElement type) {
        for (Element element : type.getEnclosedElements()) {
            if (element.getKind() == ElementKind.CONSTRUCTOR && element.getAnnotation(Creator.class) != null) {
                return element;
            }
        }
        throw new VisitorError(MISSING_CREATOR_ANNOTATION, type);
    }

    private static AnnotationValue annotationValue(String name, AnnotationMirror mirror) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            if (name.equals(ElementExecutableVisitor.accept(entry.getKey(), e -> e.getSimpleName().toString()))) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("Annotation value not found: " + name + ", mirror: " + mirror);
    }

    private static class OptionContext {

        private final ElementInfo elementInfo;
        private final List<String> names;
        private final List<ParameterMetaModel> params;

        OptionContext(ElementInfo elementInfo, List<String> names, List<ParameterMetaModel> params) {
            this.elementInfo = elementInfo;
            this.names = names;
            this.params = params;
        }

        void checkOptionName(String name) {
            if (name.isEmpty() || !Option.VALID_NAME.test(name)) {
                throw new VisitorError(INVALID_NAME, elementInfo.element());
            }
            if (names.contains(name)) {
                throw new VisitorError(OPTION_ALREADY_DEFINED, elementInfo.element());
            }
        }

        void checkOptionDescription(String description) {
            if (description.isEmpty()) {
                throw new VisitorError(INVALID_DESCRIPTION, elementInfo.element());
            }
        }
    }

    private static final class ElementExecutableVisitor<T>
            extends SimpleElementVisitor9<T, Function<ExecutableElement, T>> {

        private static final ElementExecutableVisitor<?> INSTANCE = new ElementExecutableVisitor<>();

        @Override
        public T visitExecutable(ExecutableElement e, Function<ExecutableElement, T> function) {
            return function.apply(e);
        }

        @SuppressWarnings("unchecked")
        static <T> T accept(Element element, Function<ExecutableElement, T> function) {
            return element.accept((ElementExecutableVisitor<T>) INSTANCE, function);
        }
    }

    private static final class ElementTypeVisitor<T> extends SimpleElementVisitor9<T, Function<TypeElement, T>> {

        private static final ElementTypeVisitor<?> INSTANCE = new ElementTypeVisitor<>();

        @Override
        public T visitType(TypeElement type, Function<TypeElement, T> function) {
            return function.apply(type);
        }

        @SuppressWarnings("unchecked")
        static <T> T accept(Element element, Function<TypeElement, T> function) {
            return element.accept((ElementTypeVisitor<T>) INSTANCE, function);
        }
    }

    private static final class AnnotationValueTypeVisitor<T>
            extends SimpleAnnotationValueVisitor9<T, Function<TypeMirror, T>> {

        private static final AnnotationValueTypeVisitor<?> INSTANCE = new AnnotationValueTypeVisitor<>();

        @Override
        public T visitType(TypeMirror t, Function<TypeMirror, T> function) {
            return function.apply(t);
        }

        @SuppressWarnings("unchecked")
        static <T> T accept(AnnotationValue value, Function<TypeMirror, T> function) {
            return value.accept((AnnotationValueTypeVisitor<T>) INSTANCE, function);
        }
    }

    private static final class AnnotationArrayValueVisitor<T>
            extends SimpleAnnotationValueVisitor9<List<T>, Function<AnnotationValue, T>> {

        private static final AnnotationArrayValueVisitor<?> INSTANCE = new AnnotationArrayValueVisitor<>();

        @Override
        public List<T> visitArray(List<? extends AnnotationValue> values, Function<AnnotationValue, T> function) {
            List<T> result = new LinkedList<>();
            for (AnnotationValue value : values) {
                result.add(function.apply(value));
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        static <T> List<T> accept(AnnotationValue value, Function<AnnotationValue, T> function) {
            return value.accept((AnnotationArrayValueVisitor<T>) INSTANCE, function);
        }
    }

    private static class IsNotPackageVisitor extends SimpleElementVisitor9<Boolean, Void> {

        private static final IsNotPackageVisitor INSTANCE = new IsNotPackageVisitor();

        @Override
        protected Boolean defaultAction(Element e, Void v) {
            return true;
        }

        @Override
        public Boolean visitPackage(PackageElement e, Void v) {
            return false;
        }

        static Boolean accept(Element element) {
            return element.accept(INSTANCE, null);
        }
    }
}
