/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.javadoc;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.helidon.build.common.Lists;

import org.apache.maven.artifact.Artifact;

import static java.lang.module.ModuleDescriptor.Requires.Modifier.STATIC;
import static java.lang.module.ModuleDescriptor.Requires.Modifier.TRANSITIVE;
import static java.util.stream.Collectors.toSet;

/**
 * Javadoc module.
 * A Maven artifact augmented with data used to invoke {@code javadoc}.
 */
interface JavadocModule {

    /**
     * The constant for the name of the modules without a {@link ModuleDescriptor}.
     */
    String INVALID = "INVALID!";

    /**
     * The Maven artifact of this module.
     *
     * @return Artifact
     */
    Artifact artifact();

    /**
     * The java module descriptor of this module.
     *
     * @return Artifact
     */
    ModuleDescriptor descriptor();

    /**
     * The source roots of this module.
     *
     * @return set of {@link SourceRoot}
     */
    Set<SourceRoot> sourceRoots();

    /**
     * Get this module name.
     *
     * @return module name or {@link #INVALID} if {@link #descriptor()} is {@code null}
     */
    default String name() {
        ModuleDescriptor md = descriptor();
        return md != null ? md.name() : INVALID;
    }

    /**
     * Get the module names required by this module.
     *
     * @param direct indicate if this query is done for direct requires,
     *               if {@code false} {@code require static my.module} directives are ignored
     * @return set of module names
     */
    default Set<String> requires(boolean direct) {
        ModuleDescriptor md = descriptor();
        return md == null ? Set.of() : md.requires().stream()
                // Ignore 'requires static' for indirect module dependencies
                // BUT do include 'requires static transitive'
                .filter(r -> direct || !r.modifiers().contains(STATIC) || r.modifiers().contains(TRANSITIVE))
                .map(ModuleDescriptor.Requires::name)
                .collect(toSet());
    }

    /**
     * Indicate if a module is "visible" in the dependency tree of the current Maven project.
     * <br/>
     * <br/>
     * If an unresolved module is required by a "visible" module, we introspect the "optional"/"provided" Maven dependencies
     * of the requiring module.
     * <br/>
     * <br/>
     * Otherwise, we introspect all Maven dependencies as the path to the requiring module is already "optional"/"provided".
     *
     * @return {@code true} if visible
     */
    default boolean visible() {
        return true;
    }

    /**
     * Flattens {@link CompositeJavadocModule}.
     *
     * @return stream of {@link JavadocModule}
     */
    default Stream<JavadocModule> stream() {
        return Stream.of(this);
    }

    /**
     * Merges a module entry.
     * This method is meant to be used with {@link Map#compute(Object, java.util.function.BiFunction)}.
     *
     * @param ignored module name
     * @param current current value
     * @return computed value
     */
    default JavadocModule merge(String ignored, JavadocModule current) {
        if (current instanceof CompositeJavadocModule composite) {
            composite.elements.add(this);
            return composite;
        }
        if (current != null) {
            return new CompositeJavadocModule(Lists.of(current, this));
        }
        return this;
    }

    /**
     * A "compile" source root.
     *
     * @param dir   a top-level package directory
     * @param files the map of all {@code .java} sources within the directory
     */
    record SourceRoot(Path dir, Map<String, Set<Path>> files) {
    }

    /**
     * A source Javadoc module.
     *
     * @param artifact    Maven artifact
     * @param sourceRoots source roots
     * @param descriptor  module descriptor
     */
    record SourceModule(Artifact artifact, Set<SourceRoot> sourceRoots, ModuleDescriptor descriptor)
            implements JavadocModule {
    }

    /**
     * A binary ({@code .jar}) Javadoc module.
     *
     * @param artifact   Maven artifact
     * @param descriptor module descriptor
     * @param visible    {@code true} if {@link #artifact} is in the current project dependencies
     */
    record JarModule(Artifact artifact, ModuleDescriptor descriptor, boolean visible)
            implements JavadocModule {

        @Override
        public Set<SourceRoot> sourceRoots() {
            return Set.of();
        }
    }

    /**
     * A composite Javadoc module that allows to retain the information about duplicated modules.
     *
     * @param elements composed modules
     */
    record CompositeJavadocModule(List<JavadocModule> elements) implements JavadocModule {

        @Override
        public Artifact artifact() {
            // rely on the ordering and pick the first one
            return elements.stream()
                    .findFirst()
                    .map(JavadocModule::artifact)
                    .orElseThrow();
        }

        @Override
        public ModuleDescriptor descriptor() {
            // rely on the ordering and pick the first one
            return elements.stream()
                    .findFirst()
                    .orElseThrow()
                    .descriptor();
        }

        @Override
        public Set<SourceRoot> sourceRoots() {
            // flatten the source roots of this composite modules
            // as this means that the error is within the project and thus is fixable
            return stream()
                    .map(JavadocModule::sourceRoots)
                    .flatMap(Collection::stream)
                    .collect(toSet());
        }

        @Override
        public Stream<JavadocModule> stream() {
            Set<JavadocModule> result = new HashSet<>();
            Deque<JavadocModule> stack = new ArrayDeque<>(elements);
            while (!stack.isEmpty()) {
                JavadocModule elt = stack.pop();
                if (elt instanceof CompositeJavadocModule eltCm) {
                    eltCm.elements.forEach(stack::push);
                } else {
                    result.add(elt);
                }
            }
            return result.stream();
        }

        Set<Artifact> artifacts() {
            return stream().map(JavadocModule::artifact).collect(toSet());
        }
    }
}
