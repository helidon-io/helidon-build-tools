/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.util.Set;

/**
 * Validation for plugin configuration.
 */
@SuppressWarnings("unused")
public class Validation {

    private Set<String> patterns;
    private String match;
    private boolean fail;

    /**
     * Return the patterns values as a {@link Set}.
     *
     * @return patterns
     */
    public Set<String> getPatterns() {
        return patterns;
    }

    public String getMatch() {
        return match;
    }

    public boolean getFail() {
        return fail;
    }

    public void setPatterns(Set<String> patterns) {
        this.patterns = patterns;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}
