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

/**
 * Enforcer native-image exception.
 */
public class EnforcerNativeImageException extends RuntimeException {

    /**
     * Create a new enforcer native-image exception.
     *
     * @param matcher          invalid matcher
     * @param availableMatcher list of valid matcher
     */
    EnforcerNativeImageException(String matcher, List<String> availableMatcher) {
        super(String.format("Invalid matcher '%s'. Has to be one of: %s", matcher,
                Lists.join(availableMatcher, r -> r, ",")));
    }
}
