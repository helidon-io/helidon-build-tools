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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import io.helidon.build.cli.codegen.MetaModel.CommandFragmentMetaModel;
import io.helidon.build.cli.codegen.MetaModel.CommandMetaModel;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandFragment;

/**
 * Command annotation processor.
 */
@SupportedAnnotationTypes(value = {
    "io.helidon.build.cli.harness.Command",
    "io.helidon.build.cli.harness.CommandFragment",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class CommandAnnotationProcessor extends AbstractProcessor {

    private static final String REGISTRY_SERVICE_FILE = "META-INF/services/io.helidon.build.cli.harness.CommandRegistry";

    private final Map<String, List<CommandMetaModel>> commandsByPkg = new HashMap<>();
    private final Map<String, CommandFragmentMetaModel> fragmentsByQualifiedName = new HashMap<>();
    private boolean done;

    // TODO enforce options only for command fragments (i.e no argument)
    // TODO support inheritance
    // TODO add unit tests

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!done) {
            // store the qualified names of the root element to infer the compilation units and play nice
            // with the incremental compilation performed by IDEs.
            Set<String> rootTypes = new HashSet<>();
            for (Element elt : roundEnv.getRootElements()) {
                if (elt instanceof TypeElement) {
                    rootTypes.add(((TypeElement) elt).getQualifiedName().toString());
                }
            }
            Visitor visitor = new Visitor(processingEnv, rootTypes);
            // process and cache the fragments
            for (Element elt : roundEnv.getElementsAnnotatedWith(CommandFragment.class)) {
                CommandFragmentMetaModel fragment = visitor.visitCommandFragment(elt);
                if (fragment != null) {
                    fragmentsByQualifiedName.put(fragment.type().getQualifiedName().toString(), fragment);
                }
            }
            // process the command classes
            for (Element elt : roundEnv.getElementsAnnotatedWith(Command.class)) {
                CommandMetaModel command = visitor.visitCommand(elt);
                if (command != null) {
                    String pkg = command.pkg();
                    List<CommandMetaModel> commands = commandsByPkg.get(pkg);
                    if (commands == null) {
                        commands = new ArrayList<>();
                        commandsByPkg.put(pkg, commands);
                    }
                    commands.add(command);
                }
            }
            try {
               generateSources();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            done = true;
        }
        return true;
    }

    private void generateSources() throws IOException {
        Filer filer = processingEnv.getFiler();

        // comment fragments
        for (CommandFragmentMetaModel model : fragmentsByQualifiedName.values()) {
            TypeElement type = model.type();
            String pkg = model.pkg();
            String clazz = type.getSimpleName().toString();
            String infoName = clazz + CodeGenerator.INFO_IMPL_SUFFIX;
            JavaFileObject fileObject = filer.createSourceFile(pkg + "." + infoName, type);
            try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                bw.append(CodeGenerator.generateCommandFragmentInfo(pkg, infoName, clazz, model.params()));
            }
        }

        // commands
        for (Entry<String, List<CommandMetaModel>> entry : commandsByPkg.entrySet()) {
            List<CommandMetaModel> models = entry.getValue();
            for (CommandMetaModel model : models) {
                TypeElement type = model.type();
                String pkg = model.pkg();
                String clazz = type.getSimpleName().toString();
                String modelName = clazz + CodeGenerator.MODEL_IMPL_SUFFIX;
                JavaFileObject fileObject = filer.createSourceFile(pkg + "." + modelName, type);
                try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                    bw.append(CodeGenerator.generateCommandModel(pkg, modelName, model.annotation(), clazz, model.params()));
                }
            }
            String pkg = entry.getKey();
            String registryQualifiedName = pkg + "." + CodeGenerator.REGISTRY_NAME;
            JavaFileObject fileObject = filer.createSourceFile(registryQualifiedName);
            try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                bw.append(CodeGenerator.generateCommandRegistry(pkg, CodeGenerator.REGISTRY_NAME, models));
            }
            FileObject serviceFileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", REGISTRY_SERVICE_FILE);
            try (BufferedWriter bw = new BufferedWriter(serviceFileObject.openWriter())) {
                bw.append(registryQualifiedName).append("\n");
            }
        }
    }
}
