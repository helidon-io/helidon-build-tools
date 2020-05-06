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

package io.helidon.build.dev;

/**
 * A build type.
 */
public enum BuildType {
    /**
     * A complete build that is cleaned first.
     */
    CleanComplete("clean, full"),

    /**
     * A complete build.
     */
    Complete("full"),

    /**
     * A forked complete build that is cleaned first.
     */
    ForkedCleanComplete("forked, clean, full"),

    /**
     * A forked complete build.
     */
    ForkedComplete("forked, full"),

    /**
     * An incremental build.
     */
    Incremental("incremental"),

    /**
     * Skipped.
     */
    Skipped("skipped");

    private final String description;

    BuildType(String description) {
        this.description = description;
    }

    /**
     * Returns a complete type based on the given flags.
     *
     * @param forked {@code true} if build is forked.
     * @param clean {@code true} if build is clean.
     * @return The type.
     */
    public static BuildType completeType(boolean forked, boolean clean) {
        if (forked) {
            return clean ? ForkedCleanComplete : ForkedComplete;
        } else {
            return clean ? CleanComplete : Complete;
        }
    }

    @Override
    public String toString() {
        return description;
    }
}
