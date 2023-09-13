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

package io.helidon.build.maven.enforcer.rules;

import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

/**
 * Thrown when an unsupported type is observed.
 */
public class ViolationException extends EnforcerRuleException {

    private final List<String> violations;

    /**
     * Constructor.
     *
     * @param message    the message
     * @param violations the gavs in violation
     */
    public ViolationException(String message,
                              List<String> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    /**
     * The GAV violations.
     *
     * @return gav violations
     */
    public List<String> violations() {
        return violations;
    }

}
