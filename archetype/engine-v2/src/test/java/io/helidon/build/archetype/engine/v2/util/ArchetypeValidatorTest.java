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
package io.helidon.build.archetype.engine.v2.util;

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.common.VirtualFileSystem;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.PRESET_UNRESOLVED;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.PRESET_TYPE_MISMATCH;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.EXPR_UNRESOLVED_VARIABLE;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.EXPR_EVAL_ERROR;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.INPUT_ALREADY_DECLARED;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.INPUT_TYPE_MISMATCH;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.INPUT_NOT_IN_STEP;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.OPTION_VALUE_ALREADY_DECLARED;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.INPUT_OPTIONAL_NO_DEFAULT;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.STEP_DECLARED_OPTIONAL;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.STEP_NOT_DECLARED_OPTIONAL;
import static io.helidon.build.archetype.engine.v2.util.ArchetypeValidator.STEP_NO_INPUT;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link ArchetypeValidator}.
 */
class ArchetypeValidatorTest {

    @Test
    void testOptionalInputWithoutDefault() {
        List<String> errors = validate("optional-input-no-default.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(INPUT_OPTIONAL_NO_DEFAULT));
    }

    @Test
    void testOptionalStepWithNonOptionalInputs() {
        List<String> errors = validate("optional-step-required-input.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(STEP_DECLARED_OPTIONAL));
    }

    @Test
    void testRequiredStepWithOnlyOptionalInputs() {
        List<String> errors = validate("required-step-optional-input.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(STEP_NOT_DECLARED_OPTIONAL));
    }

    @Test
    void testRequiredStepWithinOptionalStep() {
        List<String> errors = validate("required-step-within-optional-step.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(STEP_DECLARED_OPTIONAL));
    }

    @Test
    void testStepWithNoInputs() {
        List<String> errors = validate("step-with-no-inputs.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(STEP_NO_INPUT));
    }

    @Test
    void testOptionalStepWithinRequiredStep() {
        List<String> errors = validate("optional-step-within-required-step.xml");
        assertThat(errors.size(), is(0));
    }

    @Test
    void testUnresolvedPreset() {
        List<String> errors = validate("unresolved-preset.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(PRESET_UNRESOLVED));
    }

    @Test
    void testPresetTypeMismatch() {
        List<String> errors = validate("preset-type-mismatch.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(PRESET_TYPE_MISMATCH));
    }

    @Test
    void testExpressionWithUnresolvedVariable() {
        List<String> errors = validate("expression-unresolved-variable.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(EXPR_UNRESOLVED_VARIABLE));
    }

    @Test
    void testExpressionWithTypeMismatch1() {
        List<String> errors = validate("expression-type-mismatch1.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(EXPR_EVAL_ERROR));
    }

    @Test
    void testExpressionWithTypeMismatch2() {
        List<String> errors = validate("expression-type-mismatch2.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(EXPR_EVAL_ERROR));
    }

    @Test
    void testInputAlreadyDeclared() {
        List<String> errors = validate("input-already-declared.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(INPUT_ALREADY_DECLARED));
    }

    @Test
    void testInputTypeMismatch() {
        List<String> errors = validate("input-type-mismatch.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(INPUT_TYPE_MISMATCH));
    }

    @Test
    void testInputNotInStep() {
        List<String> errors = validate("input-not-in-step.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(INPUT_NOT_IN_STEP));
    }

    @Test
    void testOptionValueAlreadyDeclared() {
        List<String> errors = validate("option-value-already-declared.xml");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), containsString(OPTION_VALUE_ALREADY_DECLARED));
    }

    private List<String> validate(String scriptPath) {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/validator");
        Path script = VirtualFileSystem.create(sourceDir).getPath("/").resolve(scriptPath);
        return ArchetypeValidator.validate(script);
    }
}
