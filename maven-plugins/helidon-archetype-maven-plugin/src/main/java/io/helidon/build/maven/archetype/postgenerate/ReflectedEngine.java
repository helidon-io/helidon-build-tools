/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype.postgenerate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to invoke the archetype engine using reflection.
 */
final class ReflectedEngine {

    private static final String ENGINE_FCN = "io.helidon.build.archetype.engine.v2.ArchetypeEngineV2";
    private static final String RESOLVER_FCN = "io.helidon.build.archetype.engine.v2.InputResolver";
    private static final String BATCH_RESOLVER_FCN = "io.helidon.build.archetype.engine.v2.BatchInputResolver";
    private static final String TERMINAL_RESOLVER_FCN = "io.helidon.build.archetype.engine.v2.TerminalInputResolver";

    private final ClassLoader classLoader;
    private final Object engineInstance;
    private final Method generateMethod;

    /**
     * Create a new engine.
     *
     * @param cl         class loader
     * @param fileSystem archetype file system
     */
    ReflectedEngine(ClassLoader cl, FileSystem fileSystem) {
        try {
            classLoader = cl;
            Class<?> engineClass = cl.loadClass(ENGINE_FCN);
            Constructor<?> constructor = engineClass.getConstructor(FileSystem.class);
            engineInstance = constructor.newInstance(fileSystem);
            Class<?> inputResolverClass = cl.loadClass(RESOLVER_FCN);
            generateMethod = engineClass.getDeclaredMethod("generate", inputResolverClass, Map.class, Map.class,
                    Function.class);
        } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    /**
     * Generate the project.
     *
     * @param isInteractive     {@code true} if interactive
     * @param externalValues    external values
     * @param externalDefaults  external defaults
     * @param directorySupplier directory supplier
     */
    Path generate(boolean isInteractive,
                  Map<String, String> externalValues,
                  Map<String, String> externalDefaults,
                  Function<String, Path> directorySupplier) {

        try {
            return (Path) generateMethod.invoke(
                    engineInstance,
                    inputResolver(isInteractive),
                    externalValues,
                    externalDefaults,
                    directorySupplier);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(e.getCause());
        }
    }

    private Object inputResolver(boolean isInteractive) {
        try {
            String fcn = isInteractive ? TERMINAL_RESOLVER_FCN : BATCH_RESOLVER_FCN;
            return classLoader.loadClass(fcn).getConstructor().newInstance();
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(ex.getCause());
        }
    }
}
