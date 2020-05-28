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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import io.helidon.build.cli.codegen.MetaModel.ArgumentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CommandMetaModel;
import io.helidon.build.cli.codegen.MetaModel.FlagMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValueMetaModel;
import io.helidon.build.cli.codegen.MetaModel.KeyValuesMetaModel;
import io.helidon.build.cli.codegen.MetaModel.ParametersMetaModel;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.CommandModel;
import io.helidon.build.cli.harness.CommandParameters;
import io.helidon.build.cli.harness.CommandParser;
import io.helidon.build.cli.harness.CommandRegistry;
import io.helidon.build.cli.harness.Option;

/**
 * Code generator utility.
 */
final class CodeGenerator {

    private static final String INDENT = "    ";
    static final String REGISTRY_NAME = "CommandRegistryImpl";
    static final String MODEL_IMPL_SUFFIX = "Model";
    static final String INFO_IMPL_SUFFIX = "Info";

    private CodeGenerator() {
    }

    /**
     * Generate a command fragment info class.
     *
     * @param pkg java package
     * @param infoName name of the class
     * @param clazz described class
     * @param params parameters models
     * @return source code for the generated class
     */
    static String generateCommandFragmentInfo(String pkg, String infoName, String clazz, List<MetaModel<?>> params) {
        return "package " + pkg + ";\n"
                + "\n"
                + "import " + CommandModel.class.getName() + ";\n"
                + "import " + CommandParameters.class.getName() + ";\n"
                + "import " + CommandParser.class.getName() + ";\n"
                + "\n"
                + "final class " + infoName + " extends CommandParameters.CommandFragmentInfo<" + clazz + "> {\n"
                + "\n"
                + declareParameters(params, INDENT)
                + INDENT + "static final " + infoName + " INSTANCE = new " + infoName + "();\n"
                + "\n"
                + INDENT + "private " + infoName + "() {\n"
                + INDENT + INDENT + "super(" + clazz + ".class);\n"
                + addParameter(params.size(), INDENT + INDENT) + "\n"
                + INDENT + "}\n"
                + "\n"
                + INDENT + "@Override\n"
                + INDENT + "public " + clazz + " resolve(CommandParser parser) {\n"
                + INDENT + INDENT + "return new " + clazz + "(\n"
                + resolveParams(params, INDENT + INDENT + INDENT) + ");\n"
                + INDENT + "}\n"
                + "}\n";
    }

    /**
     * Generate a command registry class.
     *
     * @param pkg java package
     * @param infoName simple name of the class
     * @param clazz described class
     * @param params parameters models
     * @return source code for the generated class
     */
    static String generateCommandRegistry(String pkg, String name, List<CommandMetaModel> models) {
        return "package " + pkg + ";\n"
                + "\n"
                + "import " + CommandRegistry.class.getName() + ";\n"
                + "\n"
                + "public final class CommandRegistryImpl extends CommandRegistry {\n"
                + "\n"
                + INDENT + "public " + name + "() {\n"
                + INDENT + INDENT + "super(\"" + pkg + "\");\n"
                + registerModels(models, INDENT + INDENT) + "\n"
                + INDENT + "}\n"
                + "}\n";
    }

    /**
     * Generate a command model class.
     *
     * @param pkg java package
     * @param modelName simple name of the class
     * @param command command annotation
     * @param clazz class being described
     * @param params parameters models
     * @return source code for the generated class
     */
    static String generateCommandModel(String pkg, String modelName, Command command, String clazz, List<MetaModel<?>> params) {
        return "package " + pkg + ";\n"
                + "\n"
                + "import " + CommandExecution.class.getName() + ";\n"
                + "import " + CommandModel.class.getName() + ";\n"
                + "import " + CommandParameters.class.getName() + ";\n"
                + "import " + CommandParser.class.getName() + ";\n"
                + "\n"
                + "final class " + modelName + " extends CommandModel {\n"
                + "\n"
                + declareParameters(params, INDENT)
                + "\n"
                + INDENT + modelName + "() {\n"
                + INDENT + INDENT + "super(" + "new CommandInfo(\"" + command.name()
                + "\", \"" + command.description() + "\"));\n"
                + addParameter(params.size(), INDENT + INDENT) + "\n"
                + INDENT + "}\n"
                + "\n"
                + INDENT + "@Override\n"
                + INDENT + "public CommandExecution createExecution(CommandParser parser) {\n"
                + INDENT + INDENT + "return new " + clazz + "(\n"
                + resolveParams(params, INDENT + INDENT + INDENT) + ");\n"
                + INDENT + "}\n"
                + "}\n";
    }

    private static String resolveParams(List<MetaModel<?>> params, String indent) {
        String s = "";
        Iterator<MetaModel<?>> it = params.iterator();
        for (int i = 1; it.hasNext(); i++) {
            MetaModel param = it.next();
            if (param instanceof ParametersMetaModel) {
                s += indent + "OPTION" + i + ".resolve(parser)";
            } else {
                s += indent + "parser.resolve(OPTION" + i + ")";
            }
            if (it.hasNext()) {
                s += ",\n";
            }
        }
        return s;
    }

    private static String registerModels(List<CommandMetaModel> commands, String indent) {
        String s = "";
        List<CommandMetaModel> sortedCommands = new ArrayList<>(commands);
        Collections.sort(sortedCommands);
        Iterator<CommandMetaModel> it = sortedCommands.iterator();
        while (it.hasNext()) {
            String modelSimpleName = it.next().type().getSimpleName() + MODEL_IMPL_SUFFIX;
            s += indent + "register(new " + modelSimpleName + "());";
            if (it.hasNext()) {
                s += "\n";
            }
        }
        return s;
    }

    private static String declareParameters(List<MetaModel<?>> params, String indent) {
        String s = "";
        Iterator<MetaModel<?>> it = params.iterator();
        for (int i = 1; it.hasNext(); i++) {
            MetaModel param = it.next();
            if (param == null) {
                continue;
            }
            s += indent + "static final ";
            if (param instanceof KeyValuesMetaModel) {
                String paramType = ((KeyValuesMetaModel) param).paramType().getQualifiedName().toString();
                Option.KeyValues option = ((KeyValuesMetaModel) param).annotation();
                s += "CommandModel.KeyValuesInfo<" + paramType + "> OPTION" + i + " = new CommandModel.KeyValuesInfo<>("
                        + paramType + ".class, \"" + option.name() + "\", \"" + option.description() + "\", "
                        + String.valueOf(option.required()) + ");";
            } else if (param instanceof FlagMetaModel) {
                Option.Flag option = ((FlagMetaModel) param).annotation();
                s += "CommandModel.FlagInfo OPTION" + i + " = new CommandModel.FlagInfo(\""
                        + option.name() + "\", \"" + option.description() + "\", " + option.visible() + ");";
            } else if (param.type() != null) {
                String typeQualifedName = param.type().getQualifiedName().toString();
                if (param instanceof KeyValueMetaModel) {
                    Option.KeyValue option = ((KeyValueMetaModel) param).annotation();
                    s += "CommandModel.KeyValueInfo<" + typeQualifedName + "> OPTION" + i + " = new CommandModel.KeyValueInfo<>("
                            + typeQualifedName + ".class, \"" + option.name() + "\", \"" + option.description()
                            + "\", " + defaultValue(param.type(), option.defaultValue())
                            + ", " + String.valueOf(option.required())
                            + ", " + String.valueOf(option.visible()) + ");";
                } else if (param instanceof ArgumentMetaModel) {
                    Option.Argument option = ((ArgumentMetaModel) param).annotation();
                    s += "CommandModel.ArgumentInfo<" + typeQualifedName + "> OPTION" + i + " = new CommandModel.ArgumentInfo<>("
                            + typeQualifedName + ".class, \"" + option.description() + "\", "
                            + String.valueOf(option.required()) + ");";
                } else {
                    String typeSimpleName = param.type().getSimpleName().toString();
                    s += "CommandParameters.CommandFragmentInfo<" + typeSimpleName + "> OPTION" + i + " = "
                            + typeSimpleName + INFO_IMPL_SUFFIX + ".INSTANCE;";
                }
            }
            s += "\n";
        }
        return s;
    }

    private static String addParameter(int numParams, String indent) {
        String s = "";
        for (int i = 1; i <= numParams; i++) {
            s += indent + "addParameter(OPTION" + i + ");";
            if (i < numParams) {
                s += "\n";
            }
        }
        return s;
    }

    private static String defaultValue(TypeElement type, String value) {
        String typeQualifedName = type.getQualifiedName().toString();
        if (value == null || value.isEmpty()) {
            return "null";
        }
        if (String.class.getName().equals(typeQualifedName)) {
            return "\"" + value + "\"";
        }
        if (Integer.class.getName().equals(typeQualifedName)) {
            return "Integer.parseInt(\"" + value + "\")";
        }
        if (File.class.getName().equals(typeQualifedName)) {
            return "new java.io.File(\"" + value + "\")";
        }
        if (type.getKind().equals(ElementKind.ENUM)) {
            return typeQualifedName + ".valueOf(\"" + value + "\"" + ")";
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
}
