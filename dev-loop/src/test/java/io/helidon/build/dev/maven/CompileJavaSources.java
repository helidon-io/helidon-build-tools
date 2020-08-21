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

package io.helidon.build.dev.maven;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import io.helidon.build.dev.BuildComponent;
import io.helidon.build.dev.BuildFile;
import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.Project;

import static io.helidon.build.dev.BuildRootType.JAVA_CLASSES;
import static io.helidon.build.dev.BuildRootType.JAVA_SOURCES;

/**
 * A build step that compiles java sources using the ToolProvider API.
 * See configuration https://github.com/apache/maven-compiler-plugin/blob/master/src/main/java/org/apache/maven/plugin/compiler/AbstractCompilerMojo.java#L612
 */
public class CompileJavaSources implements BuildStep {
    private static final int JAVA_SUFFIX_LENGTH = ".java".length();
    private static final String CLASS_SUFFIX = ".class";
    private static final String INNER_CLASS_SEPARATOR = "$";
    private final JavaCompiler compiler;
    private final Charset sourceEncoding;
    private final boolean verbose;

    /**
     * Constructor.
     *
     * @param sourceEncoding The source encoding.
     * @param verbose Verbose flag.
     */
    public CompileJavaSources(Charset sourceEncoding, boolean verbose) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("System java compiler not available");
        }
        this.sourceEncoding = sourceEncoding;
        this.verbose = verbose;
    }

    @Override
    public BuildRootType inputType() {
        return JAVA_SOURCES;
    }

    @Override
    public BuildRootType outputType() {
        return JAVA_CLASSES;
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 Consumer<String> stdOut,
                                 Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
            final BuildRoot sources = changes.root();
            final BuildComponent component = sources.component();
            final Project project = component.project();

            // Delete classes for any removed sources

            final Set<Path> removed = changes.removed();
            if (!removed.isEmpty()) {
                final Path srcDir = sources.path();
                final Path outDir = component.outputRoot().path();
                stdOut.accept("Removing class files of " + removed.size() + " removed source files");
                for (final Path srcFile : removed) {
                    final Path relativePackageDir = srcDir.relativize(srcFile).getParent();
                    final Path outputPackageDir = outDir.resolve(relativePackageDir);
                    if (Files.isDirectory(outputPackageDir)) { // Just in case we're in some weird state
                        final String srcFileName = srcFile.getFileName().toString();
                        final String className = srcFileName.substring(0, srcFileName.length() - JAVA_SUFFIX_LENGTH);
                        final List<Path> classFiles = Files.walk(outputPackageDir)
                                                           .filter(path -> isClassOrInnerClass(path, className))
                                                           .collect(Collectors.toList());
                        for (Path classFile : classFiles) {
                            if (verbose) {
                                stdOut.accept("Removing: " + classFile);
                            }
                            Files.delete(classFile);
                        }
                    }
                }
            }

            // Recompile any changed sources or all of them if any removals

            final Set<Path> recompile = sourcesToCompile(changes);
            if (!recompile.isEmpty()) {
                final DiagnosticListener<JavaFileObject> diagnostics = diagnostic -> stdErr.accept(format(diagnostic));
                final List<String> compilerFlags = project.compilerFlags();
                final List<File> sourceFiles = recompile.stream().map(Path::toFile).collect(Collectors.toList());
                stdOut.accept("Compiling " + sourceFiles.size() + " source file" + (sourceFiles.size() == 1 ? "" : "s"));
                if (verbose) {
                    stdOut.accept("Classpath: " + project.classpath());
                }
                try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, sourceEncoding)) {
                    manager.setLocation(StandardLocation.CLASS_PATH, project.classpath());
                    manager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(component.outputRoot().path().toFile()));
                    Iterable<? extends JavaFileObject> javaSources = manager.getJavaFileObjectsFromFiles(sourceFiles);
                    JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, compilerFlags,
                                                                         null, javaSources);
                    if (!task.call()) {
                        throw new Exception("Compilation failed");
                    }
                }
            }
        }
    }

    private static boolean isClassOrInnerClass(Path classFile, String className) {
        final String fileName = classFile.getFileName().toString();
        if (fileName.equals(className + CLASS_SUFFIX)) {
            return true;
        } else {
            return fileName.startsWith(className + INNER_CLASS_SEPARATOR)
                   && fileName.endsWith(CLASS_SUFFIX);
        }
    }

    private static Set<Path> sourcesToCompile(BuildRoot.Changes changes) {
        if (changes.removed().isEmpty()) {
            return changes.addedOrModified();
        } else {
            // Recompile everything in case the removed file(s) cause breakage.
            // To get the right set of files, we need to collect them directly
            // or force the root to update here. For now, we'll just do the
            // latter.
            final BuildRoot root = changes.root();
            root.update();
            return root.stream()
                       .map(BuildFile::path)
                       .collect(Collectors.toSet());
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
