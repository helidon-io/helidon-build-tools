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

package io.helidon.build.cli.impl;

import java.util.ArrayList;
import java.util.List;

import io.helidon.build.common.maven.VersionRange;

/**
 * Information about archetypes versions.
 */
class ArchetypesData {

    private final List<String> versions;
    private final List<Rule> rules;

    private ArchetypesData(Builder builder) {
        this.versions = builder.versions;
        this.rules = builder.rules;
    }

    List<String> versions() {
        return versions;
    }

    List<Rule> rules() {
        return rules;
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * ArchetypesData builder.
     */
    static class Builder {

        private List<String> versions = new ArrayList<>();
        private List<Rule> rules = new ArrayList<>();

        private Builder() {
        }

        List<String> versions() {
            return versions;
        }

        List<Rule> rules() {
            return rules;
        }

        void addVersion(String version) {
            versions.add(version);
        }

        void addRule(Rule rule){
            rules.add(rule);
        }

        /**
         * Create new instance of {@link ArchetypesData}.
         *
         * @return version
         */
        ArchetypesData build() {
            return new ArchetypesData(this);
        }
    }

    static class Rule {
        private final VersionRange archetypeRange;
        private final VersionRange cliRange;

        Rule(VersionRange archetypeRange, VersionRange cliRange) {
            this.archetypeRange = archetypeRange;
            this.cliRange = cliRange;
        }

        VersionRange archetypeRange() {
            return archetypeRange;
        }

        VersionRange cliRange() {
            return cliRange;
        }
    }
}
