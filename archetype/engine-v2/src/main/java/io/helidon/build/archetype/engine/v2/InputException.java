/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2;

/**
 * Base input exception.
 */
public abstract class InputException extends RuntimeException {

    /**
     * Input path.
     */
    private final String inputPath;

    /**
     * Constructor.
     *
     * @param message   message.
     * @param inputPath input path
     */
    protected InputException(String message, String inputPath) {
        super(message);
        this.inputPath = inputPath;
    }

    /**
     * Get the input path.
     *
     * @return The path.
     */
    public String inputPath() {
        return inputPath;
    }
}
