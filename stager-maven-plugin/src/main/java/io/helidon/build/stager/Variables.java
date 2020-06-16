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
package io.helidon.build.stager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model for a list of variables.
 */
final class Variables implements Iterable<Variable> {

    private final List<Variable> variables;

    Variables(List<Variable> variables) {
        this.variables = variables;
    }

    Map<String, VariableValue> asMap() {
        return variables.stream().collect(Collectors.toMap(Variable::name, Variable::value));
    }

    @Override
    public Iterator<Variable> iterator() {
        return variables.iterator();
    }
}
