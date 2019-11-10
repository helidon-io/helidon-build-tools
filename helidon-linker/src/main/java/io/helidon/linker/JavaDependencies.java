/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;

import static java.util.Objects.requireNonNull;

/**
 * Collects Java module dependencies for a set of jars.
 */
public class JavaDependencies {
    private static final String JDEPS_TOOL_NAME = "jdeps";
    private static final String MULTI_RELEASE_ARG = "--multi-release";
    private static final String SYSTEM_ARG = "--system";
    private static final String LIST_DEPS_ARG = "--list-deps";
    private static final String JAVA_BASE_MODULE_NAME = "java.base";
    private static final String EOL = System.getProperty("line.separator");
    private static final ToolProvider JDEPS = ToolProvider.findFirst(JDEPS_TOOL_NAME).orElseThrow();
    private final JavaRuntime javaHome;
    private final Set<String> javaModuleNames;
    private final Set<String> dependencies;

    /**
     * Collect the dependencies of the given jars on the given Java Home.
     *
     * @param jars The jars.
     * @param javaHome The Java Home.
     * @return The module names.
     */
    public static Set<String> collect(Stream<Jar> jars, JavaRuntime javaHome) {
        return new JavaDependencies(javaHome).collect(jars);
    }

    private JavaDependencies(JavaRuntime javaHome) {
        this.javaHome = requireNonNull(javaHome);
        this.javaModuleNames = javaHome.moduleNames();
        this.dependencies = new HashSet<>();
        this.dependencies.add(JAVA_BASE_MODULE_NAME);
    }

    private Set<String> collect(Stream<Jar> jars) {
        jars.forEach(jar -> {
            if (jar.hasModuleDescriptor()) {
                addModule(jar);
            } else {
                addJar(jar);
            }
        });

        final Set<String> closure = new HashSet<>();
        dependencies.forEach(moduleName -> addDependency(moduleName, closure));
        return closure;
    }

    private void addDependency(String moduleName, Set<String> result) {
        if (!result.contains(moduleName)) {
            result.add(moduleName);
            final Jar jar = Jar.open(javaHome.jmodFile(moduleName));
            jar.moduleDescriptor().requires().forEach(r -> addDependency(r.name(), result));
        }
    }

    private void addModule(Jar module) {
        module.moduleDescriptor()
              .requires()
              .stream()
              .map(ModuleDescriptor.Requires::name)
              .filter(javaModuleNames::contains)
              .forEach(dependencies::add);
    }

    private void addJar(Jar jar) {
        Log.info("Collecting dependencies of %s", jar);
        final List<String> args = new ArrayList<>();
        if (!javaHome.isCurrent()) {
            args.add(SYSTEM_ARG);
            args.add(javaHome.path().toString());
        }
        if (jar.isMultiRelease()) {
            args.add(MULTI_RELEASE_ARG);
            args.add(javaHome.featureVersion());
        }
        args.add(LIST_DEPS_ARG);
        args.add(jar.path().toString());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int result = JDEPS.run(new PrintStream(out), System.err, args.toArray(new String[0]));
        if (result != 0) {
            throw new RuntimeException("Could not collect dependencies of " + jar);
        }

        Arrays.stream(out.toString().split(EOL))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .filter(javaModuleNames::contains)
              .forEach(dependencies::add);
    }
}
