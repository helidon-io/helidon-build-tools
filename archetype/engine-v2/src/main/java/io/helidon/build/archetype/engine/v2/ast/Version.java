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

package io.helidon.build.archetype.engine.v2.ast;

import io.helidon.build.common.maven.VersionRange;

/**
 * Helidon version.
 */
public class Version {

    private final String id;
    private final VersionRange supportedCli;

    private Version(Builder builder) {
        this.id = builder.id;
        this.supportedCli = builder.supportedCli;
    }

    /**
     * Version id.
     *
     * @return id
     */
    public String id() {
        return id;
    }

    /**
     * Supported CLI versions.
     *
     * @return version range
     */
    public VersionRange supportedCli() {
        return supportedCli;
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Version builder.
     */
    public static class Builder {
        private String id;
        private VersionRange supportedCli;

        private Builder() {
        }

        /**
         * Set version id.
         *
         * @param id id
         */
        public void id(String id) {
            this.id = id;
        }

        /**
         * Set supported CLI version range.
         *
         * @param supportedCli supported CLI
         */
        public void supportedCli(VersionRange supportedCli) {
            this.supportedCli = supportedCli;
        }

        /**
         * Create new instance of {@link Version}.
         *
         * @return version
         */
        public Version build() {
            return new Version(this);
        }
    }
}
