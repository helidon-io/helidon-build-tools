/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.BatchInputResolver;
import io.helidon.build.archetype.engine.v2.Controller;
import io.helidon.build.archetype.engine.v2.InvocationException;
import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.ValidationException;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextValue;
import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link Validation}.
 */
class ValidationTest {

    private final String VALIDATION_ALL_IN_ONE = "validation/validations-all-in-one.xml";
    private final String VALIDATION_EXEC =  "validation/archetype-base.xml";

    @ParameterizedTest
    @ValueSource(strings = {VALIDATION_ALL_IN_ONE, VALIDATION_EXEC})
    void testSuccessfulValidation(String path) {
        Context context = createContext();
        context.scope()
                .putValue("input1", Value.create("foo"), ContextValue.ValueKind.EXTERNAL);
        context.scope()
                .putValue("input2", Value.create("FOO"), ContextValue.ValueKind.EXTERNAL);
        resolveInputs(path, context);
    }

    @ParameterizedTest
    @CsvSource({
            "foo,dummy,^[A-Z]+$",
            "foo,A,[^A]+",
            "foo,B,[^B]+",
            "foo,C,[^C]+"})
    void testRegexException(String input1, String input2, String regex) {
        Context context = createContext();
        context.scope()
                .putValue("input1", Value.create(input1), ContextValue.ValueKind.EXTERNAL);
        context.scope()
                .putValue("input2", Value.create(input2), ContextValue.ValueKind.EXTERNAL);

        InvocationException ex = assertThrows(InvocationException.class, () -> resolveInputs(VALIDATION_EXEC, context));
        assertThat(ex.getCause(), is(instanceOf(ValidationException.class)));
        assertThat(ex.getCause().getMessage(), containsString(input2));
        assertThat(ex.getCause().getMessage(), containsString(regex));
    }

    private void resolveInputs(String path, Context context) {
        Script script = load(path);
        Controller.walk(new BatchInputResolver(), null, null, script, context);
    }

    private Context createContext() {
        Path target = TestFiles.targetDir(ScriptLoader.class);
        return Context.builder().cwd(target.resolve("test-classes/validation")).build();
    }
}
