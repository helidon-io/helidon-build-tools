/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.helidon.build.common.Unchecked.unchecked;
import static io.helidon.build.common.Strings.normalizeNewLines;
import static io.helidon.build.common.Strings.read;
import static java.util.stream.Collectors.toList;

/**
 * Test utility to run the compiler from within the tests.
 */
final class CompilerHelper {

    private final DiagnosticCollector<JavaFileObject> diagnostics;
    private final List<File> classpath;
    private final List<Processor> processors;
    private final List<String> options;
    private final List<JavaFileObject> compilationUnits;
    private final Path outputDir;
    private boolean called;
    private boolean success;

    CompilerHelper(Processor processor, List<String> options, List<JavaFileObject> compilationUnits) throws IOException {
        diagnostics = new DiagnosticCollector<>();
        classpath = resolveClasspath("java.class.path", "jdk.module.path");
        outputDir = Files.createTempDirectory("compiler-helper");
        processors = processor != null ? List.of(processor) : Collections.emptyList();
        this.options = options;
        this.compilationUnits = compilationUnits;
    }

    CompilerHelper(Processor processor, List<String> options, JavaFileObject... compilationUnits) throws IOException {
        this(processor, options, Arrays.asList(compilationUnits));
    }

    CompilerHelper(Processor processor, List<String> options, String... resources) throws IOException {
        this(processor, options, resolveCompilationUnits(resources));
    }

    /**
     * Call the compilation task.
     *
     * @return {@code true} if the task was successful, {@code false} otherwise
     */
    boolean call() throws IOException {
        if (!called) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null);
            manager.setLocation(StandardLocation.CLASS_PATH, classpath);
            manager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            CompilationTask task = compiler.getTask(null, manager, diagnostics, options, null, compilationUnits);
            task.setProcessors(processors);
            success = task.call();
            called = true;
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.err.println(diagnostic);
            }
        }
        return success;
    }

    /**
     * Get the output directory.
     *
     * @return Path
     */
    Path outputDir() {
        return outputDir;
    }

    /**
     * Get the diagnostics.
     *
     * @return list of messages
     */
    List<String> diagnostics() {
        return diagnostics.getDiagnostics()
                          .stream()
                          .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                          .map(d -> d.getMessage(null))
                          .collect(toList());
    }

    private static List<File> resolveClasspath(String... properties) {
        return Arrays.stream(properties)
                     .map(prop -> System.getProperty(prop, ""))
                     .flatMap(prop -> Arrays.stream(prop.split(File.pathSeparator)).map(File::new))
                     .collect(toList());
    }

    private static List<JavaFileObject> resolveCompilationUnits(String... resources) {
        return Arrays.stream(resources)
                     .map(CompilerHelper.class.getClassLoader()::getResource)
                     .map(unchecked(JavaSourceFromURL::new))
                     .collect(toList());
    }

    static class JavaSourceFromString extends SimpleJavaFileObject {

        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    static class JavaSourceFromURL extends SimpleJavaFileObject {

        JavaSourceFromURL(URL url) throws URISyntaxException {
            super(url.toURI(), Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return normalizeNewLines(read(uri.toURL().openStream()));
        }
    }
}
