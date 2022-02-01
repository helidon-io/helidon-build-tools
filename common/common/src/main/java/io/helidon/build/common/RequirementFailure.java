/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common;

/**
 * Requirement failed exception with message formatted strings.
 */
public final class RequirementFailure extends IllegalStateException {

    /**
     * Constructor.
     *
     * @param message The message.
     * @param args    The message arguments.
     */
    public RequirementFailure(String message, Object... args) {
        super(render(message, args));
    }

    private static String render(String message, Object... args) {
        Strings.requireValid(message, "message is null or empty");
        return RichTextRenderer.render(message, args);
    }
}
