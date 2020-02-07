/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build.steps;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.BuildStep;
import io.helidon.dev.build.BuildRootType;
import io.helidon.dev.build.Project;

/**
 * A build step that compiles java sources using the ToolProvider API.
 * TODO: See configuration https://github.com/apache/maven-compiler-plugin/blob/master/src/main/java/org/apache/maven/plugin/compiler/AbstractCompilerMojo.java#L612
 */
public class CompileJavaSources implements BuildStep {
    private final JavaCompiler compiler;
    private final Charset sourceEncoding;

    /**
     * Constructor.
     */
    public CompileJavaSources(Charset sourceEncoding) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("System java compiler not available");
        }
        this.sourceEncoding = sourceEncoding;
    }

    @Override
    public BuildRootType inputType() {
        return BuildRootType.JavaSources;
    }

    @Override
    public BuildRootType outputType() {
        return BuildRootType.JavaClasses;
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 Consumer<String> stdOut,
                                 Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
            final BuildComponent component = changes.root().component();
            final Project project = component.project();
            final DiagnosticListener<JavaFileObject> diagnostics = diagnostic -> stdErr.accept(format(diagnostic));
            final List<String> compilerFlags = project.compilerFlags();
            final List<File> sourceFiles = changes.addedOrModified().stream().map(Path::toFile).collect(Collectors.toList());
            stdOut.accept("Compiling " + sourceFiles.size() + " source file" + (sourceFiles.size() == 1 ? "" : "s"));
            stdOut.accept("Classpath: " + project.classpath());
            try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, sourceEncoding)) {
                manager.setLocation(StandardLocation.CLASS_PATH, project.classpath());
                manager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(component.outputRoot().path().toFile()));
                Iterable<? extends JavaFileObject> sources = manager.getJavaFileObjectsFromFiles(sourceFiles);
                JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, compilerFlags,
                                                                     null, sources);
                if (!task.call()) {
                    throw new Exception("Compilation failed");
                }
            }
        }
    }

    private static String format(Diagnostic<? extends JavaFileObject> diagnostic) {
        return String.format("%s, line %d in %s",
                             diagnostic.getMessage(null),
                             diagnostic.getLineNumber(),
                             diagnostic.getSource() == null ? "[unknown source]" : diagnostic.getSource().getName());
    }

    @Override
    public String toString() {
        return "CompileJavaSources{}";
    }
}
