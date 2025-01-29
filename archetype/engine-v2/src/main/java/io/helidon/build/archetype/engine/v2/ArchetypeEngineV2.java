/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.util.function.Supplier;

import io.helidon.build.archetype.engine.v2.InputResolver.BatchResolver;
import io.helidon.build.archetype.engine.v2.InputResolver.InteractiveResolver;

import static io.helidon.build.common.FileUtils.saveToPropertiesFile;
import static java.util.Objects.requireNonNull;

/**
 * Archetype engine (v2).
 */
public class ArchetypeEngineV2 {

    private final Path cwd;
    private final boolean batch;
    private final Map<String, String> externalValues;
    private final Map<String, String> externalDefaults;
    private final Runnable onResolved;
    private final Function<Context, Path> outputResolver;
    private final String outputPropsFile;

    private ArchetypeEngineV2(Builder builder) {
        this.cwd = builder.cwd;
        this.batch = builder.batch;
        this.externalValues = builder.externalValues;
        this.externalDefaults = builder.externalDefaults;
        this.onResolved = builder.onResolved;
        this.outputResolver = builder.outputResolver;
        this.outputPropsFile = builder.outputPropsFile;
    }

    /**
     * Generate a project.
     *
     * @return output directory
     */
    public Path generate() {

        Context context = new Context()
                .externalValues(externalValues)
                .externalDefaults(externalDefaults)
                .pushCwd(cwd);

        // entrypoint
        Node node = Script.load(cwd.resolve("main.xml"));

        // resolve inputs (full traversal)
        ScriptInvoker.invoke(node, context, batch ? new BatchResolver(context) : new InteractiveResolver(context));

        // resolve model (full traversal)
        TemplateModel model = new TemplateModel(context);
        ScriptInvoker.invoke(node, context, new BatchResolver(context), model);

        if (onResolved != null) {
            onResolved.run();
        }

        // resolve output directory
        Path directory = outputResolver.apply(context);

        // generate output  (full traversal)
        Generator generator = new Generator(model, context, outputResolver.apply(context));
        ScriptInvoker.invoke(node, context, new BatchResolver(context), generator);

        if (outputPropsFile != null) {
            Path propsFile = Path.of(outputPropsFile);
            saveToPropertiesFile(context.toMap(), propsFile.isAbsolute() ? propsFile : directory.resolve(propsFile));
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
        private boolean batch;
        private Map<String, String> externalValues = Map.of();
        private Map<String, String> externalDefaults = Map.of();
        private Function<Context, Path> outputResolver;
        private Runnable onResolved;
        private String outputPropsFile;

        private Builder() {
        }

        /**
         * Set the output properties file to save user inputs.
         *
         * @param propsFile properties file-path
         * @return this builder
         */
        public Builder outputPropsFile(String propsFile) {
            this.outputPropsFile = propsFile;
            return this;
        }

        /**
         * Set the output directory resolver.
         *
         * @param function resolver function
         * @return this builder
         */
        public Builder output(Function<Context, Path> function) {
            this.outputResolver = requireNonNull(function, "function is null");
            return this;
        }

        /**
         * Set the output directory resolver.
         *
         * @param supplier supplier
         * @return this builder
         */
        public Builder output(Supplier<Path> supplier) {
            requireNonNull(supplier, "supplier is null");
            this.outputResolver = c -> supplier.get();
            return this;
        }

        /**
         * Set the callback executed when inputs are fully resolved.
         *
         * @param onResolved callback executed when inputs are fully resolved
         * @return this builder
         */
        public Builder onResolved(Runnable onResolved) {
            this.onResolved = onResolved;
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
         * Use a batch input resolver.
         *
         * @param batch batch
         * @return this builder
         */
        public Builder batch(boolean batch) {
            this.batch = batch;
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
