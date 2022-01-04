/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.linker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import io.helidon.build.common.InputStreams;
import io.helidon.build.common.Log;
import io.helidon.build.linker.util.JavaRuntime;

import static io.helidon.build.common.InputStreams.toPrintStream;
import static io.helidon.build.linker.util.Constants.EOL;
import static io.helidon.build.linker.util.Constants.EXCLUDED_MODULES;
import static io.helidon.build.linker.util.Constants.JDEPS_REQUIRES_MISSING_DEPS_OPTION;
import static java.util.Objects.requireNonNull;

/**
 * Collects Java module dependencies for a set of jars.
 */
public final class JavaDependencies {
    private static final ToolProvider JDEPS = ToolProvider.findFirst("jdeps")
                                                          .orElseThrow(() -> new IllegalStateException("jdeps not found"));
    private static final String MULTI_RELEASE_ARG = "--multi-release";
    private static final String SYSTEM_ARG = "--system";
    private static final String LIST_DEPS_ARG = "--list-deps";
    private static final String IGNORE_MISSING_DEPS_ARG = "--ignore-missing-deps";
    private static final String JAVA_BASE_MODULE_NAME = "java.base";
    private static final Set<String> KNOWN_SPLIT_PACKAGES = Set.of("javax.annotation", "javax.activation");
    private static final Map<String, BiConsumer<String, Jar>> PREFIX_HANDLERS = Map.of(
            "split package", JavaDependencies::split,
            "not found", JavaDependencies::ignore,
            "unnamed module", JavaDependencies::ignore,
            "jdk8internals", JavaDependencies::debug,
            "JDK removed internal API", JavaDependencies::debug
    );

    private final JavaRuntime javaHome;
    private final Set<String> javaModuleNames;
    private final Set<String> dependencies;

    /**
     * Collect the dependencies of the given jars on the given Java Runtime.
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
                addModule(jar.moduleDescriptor());
            } else {
                Optional<Jar.Entry> moduleInfo = jar.entries()
                                                    .filter(it -> it.getName().endsWith("/module-info.class"))
                                                    .findFirst();

                if (moduleInfo.isPresent()) {
                    addModule(moduleInfo.get());
                } else {
                    addJar(jar);
                }
            }
        });

        final Set<String> closure = new HashSet<>();
        dependencies.forEach(moduleName -> addDependency(moduleName, closure));
        return closure;
    }

    private void addDependency(String moduleName, Set<String> result) {
        if (!result.contains(moduleName)) {
            result.add(moduleName);
            final Jar jar = javaHome.jmod(moduleName);
            jar.moduleDescriptor().requires().forEach(r -> addDependency(r.name(), result));
        }
    }

    private void addModule(Jar.Entry entry) {
        try {
            addModule(ModuleDescriptor.read(entry.data()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addModule(ModuleDescriptor descriptor) {
        Log.info("  checking module %s", descriptor.name());
        descriptor.requires()
                  .stream()
                  .map(ModuleDescriptor.Requires::name)
                  .filter(javaModuleNames::contains)
                  .forEach(dependencies::add);
    }

    private void addJar(Jar jar) {
        Log.info("  checking %s", jar);
        final List<String> args = new ArrayList<>();
        if (!javaHome.isCurrent()) {
            args.add(SYSTEM_ARG);
            args.add(javaHome.path().toString());
        }
        if (jar.isMultiRelease()) {
            args.add(MULTI_RELEASE_ARG);
            args.add(javaHome.featureVersion());
        }
        if (JDEPS_REQUIRES_MISSING_DEPS_OPTION) {
            args.add(IGNORE_MISSING_DEPS_ARG);
        }
        args.add(LIST_DEPS_ARG);
        args.add(jar.path().toString());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int result = JDEPS.run(toPrintStream(out, false), System.err, args.toArray(new String[0]));
        if (result != 0) {
            throw new RuntimeException("Could not collect dependencies of " + jar);
        }

        Arrays.stream(InputStreams.toString(out).split(EOL))
              .map(String::trim)
              .filter(line -> !line.isEmpty())
              .forEach(line -> handleJdepsResultLine(line, jar));
    }

    private void handleJdepsResultLine(String line, Jar jar) {
        if (javaModuleNames.contains(line)) {
            dependencies.add(line);
        } else {
            for (Map.Entry<String, BiConsumer<String, Jar>> entry : PREFIX_HANDLERS.entrySet()) {
                if (line.contains(entry.getKey())) {
                    entry.getValue().accept(line, jar);
                    return;
                }
            }
            if (!line.contains(":") && line.contains("/")) {
                handleJdepsResultLine(line.split("/")[0], jar);
            } else if (!EXCLUDED_MODULES.contains(line)) {
                throw new IllegalStateException("Unhandled dependency: " + toString(line, jar));
            }
        }
    }

    private static String toString(String line, Jar jar) {
        return jar + " -> " + line;
    }

    private static void ignore(String line, Jar jar) {
    }

    private static void debug(String line, Jar jar) {
        Log.debug(toString(line, jar));
    }

    private static void warn(String line, Jar jar) {
        Log.warn(toString(line, jar));
    }

    private static void split(String line, Jar jar) {
        for (String ignored : KNOWN_SPLIT_PACKAGES) {
            if (line.contains(ignored)) {
                return;
            }
        }
        warn(line, jar);
    }
}
