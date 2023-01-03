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
package io.helidon.build.maven.enforcer.nativeimage;

import java.util.List;

import io.helidon.build.common.Lists;

import org.apache.maven.plugins.annotations.Parameter;

public class NativeImageConfig {
    /**
     * Fail if error found.
     */
    @Parameter(defaultValue = "false")
    private boolean failOnError;

    /**
     * List of forbidden version.
     */
    @Parameter(required = true)
    private List<VersionConfig> rules;

    @Override
    public String toString() {
        return "NativeImageConfig{"
                + "failOnError=" + failOnError
                + ", versions=" + Lists.join(rules, Object::toString, ",")
                + '}';
    }

    /**
     * Fail if version does not respect rules.
     *
     * @return fail on error
     */
    public boolean failOnError() {
        return failOnError;
    }

    /**
     * Get rules.
     *
     * @return rules
     */
    public List<VersionConfig> rules() {
        if (rules == null) {
            return List.of();
        }
        return rules;
    }
}
