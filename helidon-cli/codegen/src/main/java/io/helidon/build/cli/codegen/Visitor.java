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
import io.helidon.build.cli.codegen.MetaModel.ParametersMetaModel;
import io.helidon.build.cli.codegen.TypeInfo.ElementInfo;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.CommandLineInterface;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option;
import io.helidon.build.cli.harness.Option.Argument;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.cli.harness.Option.KeyValues;

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

    private static final ParameterProcessor<?>[] OPTION_PROCESSORS = new ParameterProcessor<?>[]{
            new ArgumentProcessor(),
            new FlagProcessor(),
            new KeyValueProcessor(),
            new KeyValuesProcessor()
    };
    private static final CommandProcessor COMMAND_PROCESSOR = new CommandProcessor();
    private static final FragmentProcessor FRAGMENT_PROCESSOR = new FragmentProcessor();

    private final List<CLIMetaModel<ElementInfo>> clis = new LinkedList<>();
    private final Map<TypeInfo, FragmentMetaModel<ElementInfo>> fragments = new HashMap<>();
    private final Map<TypeInfo, CommandMetaModel<ElementInfo>> commands = new HashMap<>();
    private final ConstructorVisitor constructorVisitor = new ConstructorVisitor();
    private final ParametersVisitor<FragmentMetaModel<ElementInfo>, CommandFragment> fragmentVisitor
            = new ParametersVisitor<>(FRAGMENT_PROCESSOR);
    private final ParametersVisitor<CommandMetaModel<ElementInfo>, Command> commandVisitor
            = new ParametersVisitor<>(COMMAND_PROCESSOR);
    private final CLIVisitor cliVisitor = new CLIVisitor();
    private final Set<TypeInfo> rootTypes;
    private final Types typeUtils;

    /**
     * Create a new instance.
     *
     * @param env       processing environment
     * @param rootTypes root types
     */
    Visitor(ProcessingEnvironment env, Set<TypeInfo> rootTypes) {
        typeUtils = env.getTypeUtils();
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
     * @param elt element to visit
     * @throws VisitorError if a visitor error occurs
     */
    void visitCommandFragment(Element elt) throws VisitorError {
        FragmentMetaModel<ElementInfo> metaModel = elt.accept(fragmentVisitor, null);
        fragments.put(metaModel.annotatedType(), metaModel);
    }

    /**
     * Visit a command class.
     *
     * @param elt element to visit
     * @throws VisitorError if a visitor error occurs
     */
    void visitCommand(Element elt) throws VisitorError {
        CommandMetaModel<ElementInfo> metaModel = elt.accept(commandVisitor, null);
        commands.put(metaModel.annotatedType(), metaModel);
    }

    /**
     * Visit a CLI class.
     *
     * @param elt element to visit
     * @throws VisitorError if a visitor error occurs
     */
    void visitCLI(Element elt) throws VisitorError {
        CLIMetaModel<ElementInfo> metaModel = elt.accept(cliVisitor, null);
        clis.add(metaModel);
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

    private final class CLIVisitor extends SimpleElementVisitor9<CLIMetaModel<ElementInfo>, Void> {

        AnnotationValue annotationValue(String name, AnnotationMirror mirror) {
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
            for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                if (name.equals(entry.getKey().accept(new SimpleElementVisitor9<String, Void>() {
                    @Override
                    public String visitExecutable(ExecutableElement e, Void o) {
                        return e.getSimpleName().toString();
                    }
                }, null))) {
                    return entry.getValue();
                }
            }
            throw new IllegalStateException("Annotation value not found: " + name + ", mirror: " + mirror);
        }

        List<ElementInfo> valueAsTypes(AnnotationValue value) {
            return value.accept(
                    new SimpleAnnotationValueVisitor9<List<ElementInfo>, Void>() {
                        @Override
                        public List<ElementInfo> visitArray(List<? extends AnnotationValue> values, Void unused) {
                            List<ElementInfo> elementInfos = new LinkedList<>();
                            for (AnnotationValue value : values) {
                                elementInfos.add(value.accept(new SimpleAnnotationValueVisitor9<ElementInfo, Void>() {

                                    @Override
                                    public ElementInfo visitType(TypeMirror t, Void unused) {
                                        return TypeInfo.of((TypeElement) typeUtils.asElement(t), typeUtils);
                                    }
                                }, null));
                            }
                            return elementInfos;
                        }
                    }, null);
        }

        CommandLineInterface annotationProxy(String name, String description) {
            return (CommandLineInterface) Proxy.newProxyInstance(
                    this.getClass().getClassLoader(), new Class<?>[]{CommandLineInterface.class},
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
        }

        @Override
        public CLIMetaModel<ElementInfo> visitType(TypeElement type, Void p) {
            // Accessing the class objects directly from the annotation instance throws exceptions if the class is
            // not already compiled.
            // Using the annotation mirrors instead...
            AnnotationMirror mirror = null;
            for (AnnotationMirror am : type.getAnnotationMirrors()) {
                ElementInfo elementInfo = TypeInfo.of((TypeElement) am.getAnnotationType().asElement(), typeUtils);
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
            AnnotationValue commandClasses = annotationValue("commands", mirror);
            List<ElementInfo> elementInfos = valueAsTypes(commandClasses);
            List<CommandMetaModel<ElementInfo>> commands = new LinkedList<>();
            for (ElementInfo elementInfo : elementInfos) {
                CommandMetaModel<ElementInfo> command = Visitor.this.commands.get(elementInfo);
                if (command != null) {
                    commands.add(command);
                } else {
                    if (rootTypes.contains(rootTypeOf(elementInfo))) {
                        // report an error if the file is present in the compilation unit
                        throw new VisitorError(MISSING_COMMAND_ANNOTATION, type);
                    }
                }
            }
            return new CLIMetaModel<>(annotationProxy(name, description), TypeInfo.of(type, typeUtils), commands);
        }
    }

    private ElementInfo rootTypeOf(ElementInfo elementInfo) {
        Element element = elementInfo.element();
        while (element.getEnclosingElement().accept(
                new SimpleElementVisitor9<>() {
                    @Override
                    protected Boolean defaultAction(Element e, Object o) {
                        return true;
                    }

                    @Override
                    public Boolean visitPackage(PackageElement e, Object o) {
                        return false;
                    }
                }, null)) {
            element = element.getEnclosingElement();
        }
        return TypeInfo.of(element, typeUtils);
    }

    private final class ConstructorVisitor extends SimpleElementVisitor9<List<ParameterMetaModel>, Void> {

        @Override
        public List<ParameterMetaModel> visitExecutable(ExecutableElement executable, Void p) {
            List<ParameterMetaModel> params = new LinkedList<>();
            List<String> names = new ArrayList<>();
            for (VariableElement variable : executable.getParameters()) {
                ElementInfo elementInfo = TypeInfo.of(variable, typeUtils);
                ParameterContext context = new ParameterContext(elementInfo, names, params);
                boolean isOption = false;
                for (ParameterProcessor<?> processor : OPTION_PROCESSORS) {
                    if (processor.process(context)) {
                        isOption = true;
                        break;
                    }
                }
                if (!isOption) {
                    if (!elementInfo.isPrimitive()) {
                        FragmentMetaModel<ElementInfo> fragment = fragments.get(elementInfo);
                        if (fragment != null) {
                            if (!fragment.duplicates(names).isEmpty()) {
                                throw new VisitorError(FRAGMENT_OPTION_DUPLICATES, elementInfo.element());
                            }
                            names.addAll(fragment.paramNames());
                            params.add(fragment);
                        } else {
                            if (rootTypes.contains(rootTypeOf(elementInfo))) {
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
    }

    private class ParametersVisitor<T extends ParametersMetaModel<U, ElementInfo>, U extends Annotation>
            extends SimpleElementVisitor9<T, Void> {

        private final ParametersProcessor<T, U> processor;

        ParametersVisitor(ParametersProcessor<T, U> processor) {
            this.processor = processor;
        }

        @Override
        public T visitType(TypeElement type, Void unused) {
            ElementInfo elementInfo = TypeInfo.of(type, typeUtils);
            U annotation = type.getAnnotation(processor.annotationClass);
            if (annotation == null) {
                throw new IllegalStateException(String.format("Missing @%s annotation",
                        processor.annotationClass.getSimpleName()));
            }
            processor.processType(annotation, elementInfo);
            Element creatorElement = null;
            for (Element element : type.getEnclosedElements()) {
                if (element.getKind() == ElementKind.CONSTRUCTOR && element.getAnnotation(Creator.class) != null) {
                    creatorElement = element;
                    break;
                }
            }
            if (creatorElement == null) {
                throw new VisitorError(MISSING_CREATOR_ANNOTATION, type);
            }
            List<ParameterMetaModel> params = creatorElement.accept(constructorVisitor, null);
            return processor.processParameters(annotation, new ParametersContext(elementInfo, params));
        }
    }

    private static final class CommandProcessor extends ParametersProcessor<CommandMetaModel<ElementInfo>, Command> {

        CommandProcessor() {
            super(Command.class);
        }

        @Override
        void processType(Command command, ElementInfo elementInfo) {
            if (command.name().isEmpty() || !Option.VALID_NAME.test(command.name())) {
                throw new VisitorError(INVALID_NAME, elementInfo.element());
            }
            if (command.description().isEmpty()) {
                throw new VisitorError(INVALID_DESCRIPTION, elementInfo.element());
            }
            if (!elementInfo.hasInterface(CommandExecution.class)) {
                throw new VisitorError(MISSING_COMMAND_EXECUTION, elementInfo.element());
            }
        }

        @Override
        CommandMetaModel<ElementInfo> processParameters(Command annotation, ParametersContext context) {
            return new CommandMetaModel<>(annotation, context.elementInfo, context.params);
        }
    }

    private static final class FragmentProcessor extends ParametersProcessor<FragmentMetaModel<ElementInfo>, CommandFragment> {

        FragmentProcessor() {
            super(CommandFragment.class);
        }

        @Override
        FragmentMetaModel<ElementInfo> processParameters(CommandFragment annotation, ParametersContext context) {
            return new FragmentMetaModel<>(annotation, context.elementInfo, context.params);
        }
    }

    private static class ParametersContext {

        private final ElementInfo elementInfo;
        private final List<ParameterMetaModel> params;

        ParametersContext(ElementInfo elementInfo, List<ParameterMetaModel> params) {
            this.elementInfo = elementInfo;
            this.params = params;
        }
    }

    private static class ParameterContext {

        private final ElementInfo elementInfo;
        private final List<String> names;
        private final List<ParameterMetaModel> params;

        ParameterContext(ElementInfo elementInfo, List<String> names, List<ParameterMetaModel> params) {
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

    private abstract static class ParametersProcessor<T extends ParametersMetaModel<U, ElementInfo>, U extends Annotation> {

        private final Class<U> annotationClass;

        ParametersProcessor(Class<U> annotationClass) {
            this.annotationClass = annotationClass;
        }

        abstract T processParameters(U annotation, ParametersContext context);

        void processType(U annotation, ElementInfo elementInfo) {
        }
    }

    private abstract static class ParameterProcessor<T extends Annotation> {

        private final Class<T> annotationClass;

        ParameterProcessor(Class<T> annotationClass) {
            this.annotationClass = annotationClass;
        }

        boolean process(ParameterContext context) {
            T annotation = context.elementInfo.element().getAnnotation(annotationClass);
            if (annotation != null) {
                context.params.add(processOption(annotation, context));
                return true;
            }
            return false;
        }

        abstract ParameterMetaModel processOption(T annotation, ParameterContext context);
    }

    private static final class ArgumentProcessor extends ParameterProcessor<Argument> {

        ArgumentProcessor() {
            super(Option.Argument.class);
        }

        @Override
        ArgumentMetaModel<?> processOption(Option.Argument annotation, ParameterContext context) {
            context.checkOptionDescription(annotation.description());
            if (context.elementInfo.is(Option.Argument.SUPPORTED_TYPES)) {
                return new ArgumentMetaModel<>(annotation, context.elementInfo);
            } else {
                throw new VisitorError(INVALID_ARGUMENT_TYPE, context.elementInfo.element());
            }
        }
    }

    private static final class FlagProcessor extends ParameterProcessor<Flag> {

        FlagProcessor() {
            super(Option.Flag.class);
        }

        @Override
        ParameterMetaModel processOption(Option.Flag annotation, ParameterContext context) {
            context.checkOptionName(annotation.name());
            context.checkOptionDescription(annotation.description());
            if (context.elementInfo.is(Boolean.class)) {
                context.names.add(annotation.name());
                return new FlagMetaModel(annotation);
            } else {
                throw new VisitorError(INVALID_FLAG_TYPE, context.elementInfo.element());
            }
        }
    }

    private static final class KeyValueProcessor extends ParameterProcessor<KeyValue> {

        KeyValueProcessor() {
            super(Option.KeyValue.class);
        }

        @Override
        ParameterMetaModel processOption(Option.KeyValue annotation, ParameterContext context) {
            context.checkOptionName(annotation.name());
            context.checkOptionDescription(annotation.description());
            if (context.elementInfo.is(Option.KeyValue.SUPPORTED_TYPES)) {
                context.names.add(annotation.name());
                return new KeyValueMetaModel<>(annotation, context.elementInfo);
            } else {
                throw new VisitorError(INVALID_KEY_VALUE_TYPE, context.elementInfo.element());
            }
        }
    }

    private static final class KeyValuesProcessor extends ParameterProcessor<KeyValues> {

        KeyValuesProcessor() {
            super(Option.KeyValues.class);
        }

        @Override
        ParameterMetaModel processOption(Option.KeyValues annotation, ParameterContext context) {
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
    }
}
