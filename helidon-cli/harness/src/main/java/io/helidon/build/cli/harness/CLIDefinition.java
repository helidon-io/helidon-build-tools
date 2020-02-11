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
package io.helidon.build.cli.harness;

import java.util.Objects;

/**
 * CLI definition.
 */
public final class CLIDefinition {

    private final String name;
    private final String description;

    private CLIDefinition(String name, String description) {
        this.name = Objects.requireNonNull(name, "name is null");
        this.description = Objects.requireNonNull(description, "description is null");
    }

    /**
     * The CLI name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * The CLI description.
     *
     * @return description
     */
    public String description() {
        return description;
    }

    /**
     * Create a new CLI definition.
     * @param name CLI name
     * @param description CLI description
     * @return CLI definition, never {@code null}
     */
    public static CLIDefinition create(String name, String description) {
        return new CLIDefinition(name, description);
    }
}
