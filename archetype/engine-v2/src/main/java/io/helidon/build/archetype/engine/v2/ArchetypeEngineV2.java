/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextSerializer;
import io.helidon.build.common.FileUtils;

import static java.util.Objects.requireNonNull;

/**
 * Archetype engine (v2).
 */
public class ArchetypeEngineV2 {

    private static final String ENTRYPOINT = "main.xml";
    private static final String ARTIFACT_ID = "artifactId";

    private final Path cwd;
    private final InputResolver inputResolver;
    private final Map<String, String> externalValues;
    private final Map<String, String> externalDefaults;
    private final Runnable onResolved;
    private final Function<String, Path> directorySupplier;
    private final Path outputPropsFile;

    private ArchetypeEngineV2(Builder builder) {
        this.cwd = builder.cwd;
        this.inputResolver = builder.inputResolver;
        this.externalValues = builder.externalValues;
        this.externalDefaults = builder.externalDefaults;
        this.onResolved = builder.onResolved;
        this.directorySupplier = builder.directorySupplier;
        this.outputPropsFile = builder.outputPropsFile;
    }

    /**
     * Generate a project.
     *
     * @return output directory
     */
    public Path generate() {
        Context context = Context.builder()
                                 .cwd(cwd)
                                 .externalValues(externalValues)
                                 .externalDefaults(externalDefaults)
                                 .build();

        Script script = ScriptLoader.load(cwd.resolve(ENTRYPOINT));

        // resolve inputs (full traversal)
        Controller.walk(inputResolver, script, context);
        context.requireRootScope();
        onResolved.run();

        // resolve output directory
        // TODO use a Function<ContextScope, Path> instead of hard-coding artifactId here...
        String artifactId = requireNonNull(context.getValue(ARTIFACT_ID), ARTIFACT_ID + " is null").asString();
        Path directory = directorySupplier.apply(artifactId);

        // resolve model  (full traversal)
        MergedModel model = MergedModel.resolveModel(script, context);

        //  generate output  (full traversal)
        OutputGenerator outputGenerator = new OutputGenerator(model, directory);
        Controller.walk(outputGenerator, script, context);
        context.requireRootScope();

        if (outputPropsFile != null) {
            Map<String, String> userInputsMap = ContextSerializer.serialize(context);
            Path path = outputPropsFile.isAbsolute() ? outputPropsFile : directory.resolve(outputPropsFile);
            FileUtils.saveToPropertiesFile(userInputsMap, path);
        }

        return directory;
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ArchetypeEngineV2 builder.
     */
    public static final class Builder {

        private Path cwd;
        private InputResolver inputResolver;
        private Map<String, String> externalValues = Map.of();
        private Map<String, String> externalDefaults = Map.of();
        private Runnable onResolved = () -> {};
        private Function<String, Path> directorySupplier;
        private Path outputPropsFile;

        private Builder() {
        }

        /**
         * Set the path to the output properties file to save user inputs.
         *
         * @param outputPropsFile path to the output properties file
         * @return this builder
         */
        public Builder outputPropsFile(Path outputPropsFile) {
            this.outputPropsFile = outputPropsFile;
            return this;
        }

        /**
         * Set the output directory supplier.
         *
         * @param directorySupplier output directory supplier
         * @return this builder
         */
        public Builder directorySupplier(Function<String, Path> directorySupplier) {
            this.directorySupplier = requireNonNull(directorySupplier, "directorySupplier is null");
            return this;
        }

        /**
         * Set the callback executed when inputs are fully resolved.
         *
         * @param onResolved callback executed when inputs are fully resolved
         * @return this builder
         */
        public Builder onResolved(Runnable onResolved) {
            this.onResolved = requireNonNull(onResolved, "onResolved is null");
            return this;
        }

        /**
         * Set external defaults.
         *
         * @param externalDefaults external defaults
         * @return this builder
         */
        public Builder externalDefaults(Map<String, String> externalDefaults) {
            this.externalDefaults = requireNonNull(externalDefaults, "externalDefaults is null");
            return this;
        }

        /**
         * Set external values.
         *
         * @param externalValues external values
         * @return this builder
         */
        public Builder externalValues(Map<String, String> externalValues) {
            this.externalValues = requireNonNull(externalValues, "externalValues is null");
            return this;
        }

        /**
         * Set the input resolver.
         *
         * @param inputResolver input resolver
         * @return this builder
         */
        public Builder inputResolver(InputResolver inputResolver) {
            this.inputResolver = requireNonNull(inputResolver, "inputResolver is null");
            return this;
        }

        /**
         * Set the archetype file system.
         *
         * @param fileSystem archetype file system
         * @return this builder
         */
        public Builder fileSystem(FileSystem fileSystem) {
            this.cwd = fileSystem.getPath("/");
            return this;
        }

        /**
         * Build the ArchetypeEngineV2 instance.
         *
         * @return new ArchetypeEngineV2
         */
        public ArchetypeEngineV2 build() {
            return new ArchetypeEngineV2(this);
        }
    }
}
