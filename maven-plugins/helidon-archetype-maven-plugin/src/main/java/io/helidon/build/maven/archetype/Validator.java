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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.util.ArchetypeValidator;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Validation operations.
 */
class Validator {

    private Validator() {
    }

    /**
     * Validate archetype regular expression.
     *
     * @param script archetype script
     * @return list of errors
     */
    static List<String> validateArchetype(Path script) {
        List<String> errors = ArchetypeValidator.validate(script);
        List<String> regexErrors = RegexValidator.validate(script);
        errors.addAll(regexErrors);
        return errors;
    }

    /**
     * Validate generated files using {@link io.helidon.build.maven.archetype.config.Validation} configuration.
     *
     * @param directory   generated file directory
     * @param validations validations
     * @throws MojoExecutionException if validation mismatch
     */
    static void validateProject(File directory,
                                List<io.helidon.build.maven.archetype.config.Validation> validations)
            throws MojoExecutionException {
        if (Objects.isNull(validations)) {
            return;
        }
        for (io.helidon.build.maven.archetype.config.Validation validation : validations) {
            validation.validate(directory);
        }
    }

}
