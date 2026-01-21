/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.build.common.LazyValue;

import static io.helidon.build.common.FileUtils.javaHome;
import static io.helidon.build.common.FileUtils.requireDirectory;

/**
 * Java Runtime.
 */
public final class JavaRuntime implements ResourceContainer {

    /**
     * Get the current JDK.
     */
    public static final JavaRuntime CURRENT_JDK = new JavaRuntime();

    private final LazyValue<Path> path = new LazyValue<>(this::path0);
    private final Runtime.Version version;
    private final Map<String, ModuleReference> systemModules;
    private final boolean jdepsRequiresMissingDeps;
    private final boolean cdsRequiresUnlock;
    private final boolean cdsSupportsImageCopy;

    private JavaRuntime() {
        this.version = Runtime.version();
        this.systemModules = ModuleFinder.ofSystem().findAll().stream()
                .collect(Collectors.toMap(t -> t.descriptor().name(), Function.identity()));
        int feature = version.feature();
        this.jdepsRequiresMissingDeps = feature > 11 || (feature == 11 && version.update() >= 11);
        this.cdsRequiresUnlock = feature <= 10;
        this.cdsSupportsImageCopy = feature >= 10;
    }

    /**
     * Get the current {@link JavaRuntime} for this JVM.
     *
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JDK.
     */
    public static JavaRuntime current() {
        return CURRENT_JDK;
    }

    /**
     * Returns the module names.
     *
     * @return The module names.
     */
    public Set<String> moduleNames() {
        return systemModules.keySet();
    }

    @Override
    public boolean containsResource(String path) {
        return systemModules.values().stream()
                .anyMatch(ref -> {
                    try (ModuleReader reader = ref.open()) {
                        return reader.find(path).isPresent();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
    }

    /**
     * Returns the {@link ModuleDescriptor} for the given name.
     *
     * @param moduleName The module name.
     * @return The ModuleDescriptor
     * @throws IllegalArgumentException If the jar cannot be found.
     */
    public ModuleDescriptor module(String moduleName) {
        ModuleDescriptor result = systemModules.get(moduleName).descriptor();
        if (result == null) {
            throw new IllegalArgumentException(
                    "Cannot find ModuleDescriptor for module '" + moduleName + "' in " + path());
        }
        return result;
    }

    /**
     * Returns the path.
     *
     * @return The path.
     */
    public Path path() {
        return path.get();
    }

    /**
     * Return the version.
     *
     * @return The version.
     */
    public Runtime.Version version() {
        return version;
    }

    /**
     * Whether JDEPS requires the missing deps option.
     *
     * @return {@code true} if JDEPS requires the missing deps option, {@code false otherwise}
     */
    public boolean jdepsRequiresMissingDeps() {
        return jdepsRequiresMissingDeps;
    }

    /**
     * Whether CDS requires the unlock option.
     *
     * @return {@code true} if CDS requires the unlock option, {@code false otherwise}
     */
    public boolean cdsRequiresUnlock() {
        return cdsRequiresUnlock;
    }

    /**
     * Whether CDS supports image copy (with preserved timestamps).
     *
     * @return {@code true} if CDS supports image copy (with preserved timestamps), {@code false otherwise}
     */
    public boolean cdsSupportsImageCopy() {
        return cdsSupportsImageCopy;
    }

    @Override
    public String toString() {
        return "JDK " + version;
    }

    private Path path0() {
        return requireDirectory(Paths.get(javaHome()));
    }
}
