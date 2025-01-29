/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.function.Consumer;

import io.helidon.build.archetype.engine.v2.Script;
import io.helidon.build.archetype.engine.v2.ScriptCompiler;

/**
 * Script compiler extension.
 */
class ScriptCompilerExt {

    private static final Schema SCHEMA = new Schema();

    /**
     * Compiler options.
     */
    enum Options implements ScriptCompiler.Option {
        VALIDATE_REGEX(ScriptCompilerExt::validateRegex),
        VALIDATE_SCHEMA(ScriptCompilerExt::validateSchema);

        private final Consumer<ScriptCompiler> consumer;

        Options(Consumer<ScriptCompiler> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void run(ScriptCompiler compiler) {
            consumer.accept(compiler);
        }
    }

    private static void validateRegex(ScriptCompiler compiler) {
        List<String> errors = Regex.validate(compiler.sourceNode());
        compiler.errors().addAll(errors);
    }

    private static void validateSchema(ScriptCompiler compiler) {
        Script script = compiler.sourceNode().script();
        List<String> errors = SCHEMA.validate(script.inputStream(), script.path().toString());
        compiler.errors().addAll(errors);
    }
}
