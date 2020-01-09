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
 *
 */
package io.helidon.codegen.openapi;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class CodeGenModel {

    private Collection<Type> types;
    private Set<SnakeYAMLMojo.Import> imports;

    CodeGenModel(Collection<Type> types, Set<SnakeYAMLMojo.Import> imports) {
        this.types = types;
        this.imports = imports;
    }

    Collection<Type> typesToAugment() {
        return types.stream().filter(t -> t.implementationType != null).collect(Collectors.toList());
    }

    List<SnakeYAMLMojo.Import> javaImports() {
        return filteredImports("java.");
    }

    List<SnakeYAMLMojo.Import> implImports() {
        return filteredImports("io.smallrye");
    }

    List<SnakeYAMLMojo.Import> openAPIImports() {
        return filteredImports("org.eclipse.microprofile.openapi");
    }

    private List<SnakeYAMLMojo.Import> filteredImports(String namePrefix) {
        return imports.stream().filter(i -> i.name.startsWith(namePrefix)).sorted().collect(Collectors.toList());
    }
}
