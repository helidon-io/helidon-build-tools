/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.archetype.engine.v2.ast.Script;

import static java.util.Objects.requireNonNull;

/**
 * Archetype engine (v2).
 */
public class ArchetypeEngineV2 {

    private static final String ENTRYPOINT = "main.xml";
    private static final String PROJECT_NAME = "name";

    private final Path cwd;

    /**
     * Create a new archetype engine.
     *
     * @param fs archetype file system
     */
    public ArchetypeEngineV2(FileSystem fs) {
        this.cwd = fs.getPath("/");
    }

    /**
     * Generate a project.
     *
     * @param inputResolver     input resolver
     * @param externalValues    external values
     * @param externalDefaults  external defaults
     * @param directorySupplier output directory supplier
     * @return output directory
     */
    public Path generate(InputResolver inputResolver,
                         Map<String, String> externalValues,
                         Map<String, String> externalDefaults,
                         Function<String, Path> directorySupplier) {

        Context context = Context.create(cwd, externalValues, externalDefaults);
        Script script = ScriptLoader.load(cwd.resolve(ENTRYPOINT));

        // resolve inputs (full traversal)
        Controller.walk(inputResolver, script, context);
        context.ensureEmptyInputs();

        // resolve output directory
        String projectName = requireNonNull(context.lookup(PROJECT_NAME), "project name is null").asString();
        Path directory = directorySupplier.apply(projectName);

        // resolve model  (full traversal)
        MergedModel model = MergedModel.resolveModel(script, context);

        //  generate output  (full traversal)
        OutputGenerator outputGenerator = new OutputGenerator(model, directory);
        Controller.walk(outputGenerator, script, context);
        context.ensureEmptyInputs();

        return directory;
    }
}
