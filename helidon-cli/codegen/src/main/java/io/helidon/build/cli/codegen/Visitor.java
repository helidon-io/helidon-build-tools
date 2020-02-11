/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.SimpleTypeVisitor9;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import io.helidon.build.cli.codegen.MetaModel.ArgumentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CommandFragmentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CommandMetaModel;
import io.helidon.build.cli.codegen.MetaModel.FlagMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValueMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValuesMetaModel;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option;

/**
 * Meta-model visitor.
 */
final class Visitor {

    private static final List<String> ARGUMENT_TYPES =
            Option.Argument.SUPPORTED_TYPES.stream().map(Class::getName).collect(Collectors.toList());
    private static final List<String> KEY_VALUE_TYPES =
            Option.KeyValue.SUPPORTED_TYPES.stream().map(Class::getName).collect(Collectors.toList());
    private static final List<String> KEY_VALUES_TYPES =
            Option.KeyValues.SUPPORTED_TYPES.stream().map(Class::getName).collect(Collectors.toList());

    private final Map<String, CommandFragmentMetaModel> fragmentsByQualifiedName = new HashMap<>();
    private final ProcessingEnvironment env;
    private final Set<String> rootTypes;

    Visitor(ProcessingEnvironment env, Set<String> rootTypes) {
        this.env = env;
        this.rootTypes = rootTypes;
    }

    /**
     * Visitor a command fragment class.
     *
     * @param elt element to visit
     * @return meta-model
     */
    CommandFragmentMetaModel visitCommandFragment(Element elt) {
        return elt.accept(new CommandFragmentVisitor(), null);
    }

    /**
     * Visitor a command class.
     *
     * @param elt element to visit
     * @return meta-model
     */
    CommandMetaModel visitCommand(Element elt) {
        return elt.accept(new CommandVisitor(), null);
    }

    private final class TypeParamVisitor extends SimpleTypeVisitor9<TypeElement, Void> {

        @Override
        public TypeElement visitDeclared(DeclaredType t, Void p) {
            List<? extends TypeMirror> typeArguments = t.getTypeArguments();
            if (typeArguments.size() == 1) {
                TypeElement type = (TypeElement) env.getTypeUtils().asElement(typeArguments.get(0));
                return type;
            }
            return null;
        }
    }

    private final class ConstructorVisitor extends SimpleElementVisitor9<List<MetaModel<?>>, Void> {

        private boolean isValidType(TypeElement type, Class<?> validType) {
            String typeQualifiedName = type.getQualifiedName().toString();
            if (!validType.getName().equals(typeQualifiedName)) {
                TypeElement superTypeElt = (TypeElement) env.getTypeUtils().asElement(type.getSuperclass());
                String superTypeQualifiedName = superTypeElt.getQualifiedName().toString();
                return validType.getName().equals(superTypeQualifiedName);
            }
            return true;
        }

        private boolean isValidType(TypeElement type, List<String> validTypes) {
            String typeQualifiedName = type.getQualifiedName().toString();
            if (!validTypes.contains(typeQualifiedName)) {
                TypeElement superTypeElt = (TypeElement) env.getTypeUtils().asElement(type.getSuperclass());
                String superTypeQualifiedName = superTypeElt.getQualifiedName().toString();
                return validTypes.contains(superTypeQualifiedName);
            }
            return true;
        }

        private boolean checkOptionName(String name, VariableElement var, List<String> options) {
            if (name.isEmpty()) {
                env.getMessager().printMessage(Diagnostic.Kind.ERROR, "option name cannot be empty", var);
                return false;
            }
            if (!Option.NAME_PREDICATE.test(name)) {
                env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        String.format("'%s' is not a valid option name", name),
                        var);
                return false;
            }
            if (options.contains(name)) {
                env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        String.format("option named '%s' is already defined", name),
                        var);
                return false;
            }
            return true;
        }

        private boolean checkOptionDescription(String description, VariableElement var) {
            if (description.isEmpty()) {
                env.getMessager().printMessage(Diagnostic.Kind.ERROR, "description cannot be empty", var);
                return false;
            }
            return true;
        }

        private boolean processOption(VariableElement var, TypeElement type, List<String> options, List<MetaModel<?>> params) {
            Option.Argument argument = var.getAnnotation(Option.Argument.class);
            if (argument != null) {
                if (checkOptionDescription(argument.description(), var)) {
                    if (isValidType(type, ARGUMENT_TYPES)) {
                        params.add(new ArgumentMetaModel(type, argument));
                        return true;
                    } else {
                        env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format("%s is not a valid argument type: ", type),
                                var);
                    }
                }
                return false;
            }
            Option.Flag flag = var.getAnnotation(Option.Flag.class);
            if (flag != null) {
                if (checkOptionName(flag.name(), var, options) && checkOptionDescription(flag.description(), var)) {
                    if (isValidType(type, Boolean.class)) {
                        params.add(new FlagMetaModel(flag));
                        options.add(flag.name());
                        return true;
                    } else {
                        env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format("%s is not a valid flag type: ", type),
                                var);
                    }
                }
                return false;
            }
            Option.KeyValue keyValue = var.getAnnotation(Option.KeyValue.class);
            if (keyValue != null) {
                if (checkOptionName(keyValue.name(), var, options) && checkOptionDescription(keyValue.description(), var)) {
                    if (isValidType(type, KEY_VALUE_TYPES)) {
                        params.add(new KeyValueMetaModel(type, keyValue));
                        options.add(keyValue.name());
                        return true;
                    } else {
                        env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format("%s is not a valid key-value type: ", type),
                                var);
                    }
                }
                return false;
            }
            Option.KeyValues keyValues = var.getAnnotation(Option.KeyValues.class);
            if (keyValues != null) {
                if (checkOptionName(keyValues.name(), var, options)
                        && checkOptionDescription(keyValues.description(), var)) {
                    if (isValidType(type, Collection.class)) {
                        TypeElement paramTypeElt = var.asType().accept(new TypeParamVisitor(), null);
                        if (paramTypeElt != null) {
                            if (isValidType(paramTypeElt, KEY_VALUES_TYPES)) {
                                params.add(new KeyValuesMetaModel(paramTypeElt, keyValues));
                                options.add(keyValues.name());
                                return true;
                            } else {
                                env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                        String.format("%s is not a valid option values type parameter: ", paramTypeElt),
                                        var);
                            }
                        }
                    } else {
                        env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format("%s is not a valid key-values type: ", type),
                                var);
                    }
                }
                return false;
            }
            return false;
        }

        private boolean processArgument(VariableElement var, TypeElement type, List<MetaModel<?>> params) {
            Option.Argument argumentAnnot = var.getAnnotation(Option.Argument.class);
            if (argumentAnnot != null) {
                if (argumentAnnot.description() == null) {
                    env.getMessager().printMessage(Diagnostic.Kind.ERROR, "description cannot be null", var);
                } else {
                    if (isValidType(type, ARGUMENT_TYPES)) {
                        env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format("%s is not a valid argument value type: ", type),
                                var);
                    } else {
                        params.add(new ArgumentMetaModel(type, argumentAnnot));
                    }
                }
                return true;
            }
            return false;
        }

        private void processFragment(VariableElement var, TypeElement type, List<String> options, List<MetaModel<?>> params) {
            String fragmentQualifiedName = type.getQualifiedName().toString();
            CommandFragmentMetaModel fragmentModel = fragmentsByQualifiedName.get(fragmentQualifiedName);
            if (fragmentModel == null) {
                if (rootTypes.contains(fragmentQualifiedName)) {
                    // report an error if the fragment related file is present in the compilation unit
                    env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            String.format("type '%s' is not annotated with @%s", type,
                                    CommandFragment.class.getSimpleName()),
                            var);
                }
            } else {
                List<String> optionDuplicates = fragmentModel.optionDuplicates(options);
                if (!optionDuplicates.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    Iterator<String> it = optionDuplicates.iterator();
                    while (it.hasNext()) {
                        sb.append(it.next());
                        if (it.hasNext()) {
                            sb.append(", ");
                        }
                    }
                    env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            String.format("command fragment duplicates options: '%s'", sb),
                            var);
                }
                options.addAll(fragmentModel.optionNames());
                params.add(fragmentModel);
            }
        }

        @Override
        public List<MetaModel<?>> visitExecutable(ExecutableElement elt, Void p) {
            Types types = env.getTypeUtils();
            List<MetaModel<?>> params = new LinkedList<>();
            List<String> optionNames = new ArrayList<>();
            for (VariableElement var : elt.getParameters()) {
                String varName = var.getSimpleName().toString();
                // resolve the type
                TypeMirror varType = var.asType();
                TypeKind varTypeKind = varType.getKind();
                boolean primitive = varTypeKind.isPrimitive();
                TypeElement type;
                if (primitive) {
                    type = types.boxedClass(types.getPrimitiveType(varTypeKind));
                } else {
                    type = (TypeElement) types.asElement(var.asType());
                }
                if (type == null) {
                    throw new IllegalStateException("Unable to resolve type for variable: " + varName);
                }

                // process the variable
                if (!processOption(var, type, optionNames, params)
                        && !processArgument(var, type, params)) {

                    if (!primitive) {
                        processFragment(var, type, optionNames, params);
                    } else {
                        env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format("%s is not a valid attribute", varName),
                                var);
                        return null;
                    }
                }
            }
            return params;
        }
    }

    private final class CommandVisitor extends SimpleElementVisitor9<CommandMetaModel, Void> {

        private boolean implementsCommandExecution(TypeElement type) {
            for (TypeMirror iface : type.getInterfaces()) {
                TypeElement ifaceTypeElt = (TypeElement) env.getTypeUtils().asElement(iface);
                if (CommandExecution.class.getName().equals(ifaceTypeElt.getQualifiedName().toString())) {
                    return true;
                }
            }
            env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    String.format("%s does not implement %s", type, CommandExecution.class.getSimpleName()),
                    type);
            return false;
        }

        @Override
        public CommandMetaModel visitType(TypeElement type, Void p) {
            if (implementsCommandExecution(type)) {
                Command annotation = type.getAnnotation(Command.class);
                CommandFragmentMetaModel fragment = type.accept(new CommandFragmentVisitor(), null);
                if (fragment != null) {
                    return new CommandMetaModel(fragment.type(), fragment.pkg(), fragment.params(), annotation);
                }
            }
            return null;
        }
    }

    private final class CommandFragmentVisitor extends SimpleElementVisitor9<CommandFragmentMetaModel, Void> {

        @Override
        public CommandFragmentMetaModel visitType(TypeElement type, Void p) {
            List<MetaModel<?>> params = null;
            for (Element elt : type.getEnclosedElements()) {
                if (elt.getKind() == ElementKind.CONSTRUCTOR && elt.getAnnotation(Creator.class) != null) {
                    params = elt.accept(new ConstructorVisitor(), null);
                    break;
                }
            }
            if (params != null) {
                String pkg = env.getElementUtils().getPackageOf(type).getQualifiedName().toString();
                CommandFragmentMetaModel model = new CommandFragmentMetaModel(type, pkg, params);
                fragmentsByQualifiedName.put(type.getQualifiedName().toString(), model);
                return model;
            }
            env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        String.format("No constructor annotated with @%s found", Creator.class.getSimpleName()),
                        type);
            return null;
        }
    }
}
