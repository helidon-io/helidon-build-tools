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
package io.helidon.build.maven.archetype.postgenerate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class to invoke the archetype engine using reflection.
 */
final class ReflectedEngine {

    private static final String ENGINE_FCN = "io.helidon.build.archetype.engine.v2.ArchetypeEngineV2";
    private static final String ENGINE_FCN_BUILDER = "io.helidon.build.archetype.engine.v2.ArchetypeEngineV2$Builder";

    private final Object engineInstance;
    private final Method generateMethod;

    /**
     * Create a new engine.
     *
     * @param cl               class loader
     * @param fileSystem       archetype file system
     * @param isInteractive    {@code true} if interactive
     * @param externalValues   external values
     * @param externalDefaults external defaults
     * @param dirSupplier      directory supplier
     */
    ReflectedEngine(ClassLoader cl,
                    FileSystem fileSystem,
                    boolean isInteractive,
                    Map<String, String> externalValues,
                    Map<String, String> externalDefaults,
                    Supplier<Path> dirSupplier) {
        try {
            Class<?> engineClass = cl.loadClass(ENGINE_FCN);
            Class<?> engineBuilderClass = cl.loadClass(ENGINE_FCN_BUILDER);
            Constructor<?> builderClassConstructor = engineBuilderClass.getDeclaredConstructor();
            builderClassConstructor.setAccessible(true);
            Object builder = builderClassConstructor.newInstance();
            engineBuilderClass.getDeclaredMethod("output", Supplier.class)
                    .invoke(builder, dirSupplier);
            engineBuilderClass.getDeclaredMethod("externalDefaults", Map.class)
                    .invoke(builder, externalDefaults);
            engineBuilderClass.getDeclaredMethod("externalValues", Map.class)
                    .invoke(builder, externalValues);
            engineBuilderClass.getDeclaredMethod("batch", boolean.class)
                    .invoke(builder, !isInteractive);
            engineBuilderClass.getDeclaredMethod("fileSystem", FileSystem.class)
                    .invoke(builder, fileSystem);
            Constructor<?> constructor = engineClass.getDeclaredConstructor(engineBuilderClass);
            constructor.setAccessible(true);
            engineInstance = constructor.newInstance(builder);
            generateMethod = engineClass.getDeclaredMethod("generate");
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
     */
    Path generate() {
        try {
            return (Path) generateMethod.invoke(engineInstance);
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
}
