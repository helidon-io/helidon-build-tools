/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.cli.codegen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandFragment;
import io.helidon.build.cli.harness.CommandLineInterface;
import io.helidon.build.cli.harness.CommandModel.ArgumentInfo;
import io.helidon.build.cli.harness.CommandModel.CommandInfo;
import io.helidon.build.cli.harness.CommandModel.FlagInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValuesInfo;
import io.helidon.build.cli.harness.CommandParameters.CommandFragmentInfo;
import io.helidon.build.cli.harness.CommandParameters.ParameterInfo;
import io.helidon.build.cli.harness.Option;
import io.helidon.build.cli.harness.Option.Argument;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.cli.harness.Option.KeyValues;

/**
 * Meta model API to represent the processed annotations.
 */
abstract class MetaModel<T, U extends TypeInfo> {

    private final U annotatedType;
    private final T annotation;

    protected MetaModel(T option, U annotatedType) {
        this.annotation = Objects.requireNonNull(option, "option is null");
        this.annotatedType = Objects.requireNonNull(annotatedType, "annotatedType is null");
    }

    /**
     * Get the annotation value.
     *
     * @return annotation value, never {@code null}
     */
    T annotation() {
        return annotation;
    }

    /**
     * Get the type info for the annotated element.
     *
     * @return TypeInfo, never {@code null}
     */
    U annotatedType() {
        return annotatedType;
    }

    /**
     * A meta-model with a description.
     */
    interface DescribedOptionMetaModel {

        /**
         * Get the description.
         *
         * @return description, never {@code null}
         */
        String description();
    }

    /**
     * A meta-model with a name.
     */
    interface NamedOptionMetaModel extends DescribedOptionMetaModel {

        /**
         * Get the name.
         *
         * @return name, never {@code null}
         */
        String name();
    }

    /**
     * A meta-model that describes a {@link ParameterInfo}.
     */
    interface ParameterMetaModel {

        /**
         * Get the type of the {@link ParameterInfo}.
         *
         * @return TypeInfo
         */
        TypeInfo paramInfoType();
    }

    /**
     * Meta-model for the elements annotated with {@link Option.Argument}.
     *
     * @param <U> type info type
     */
    static class ArgumentMetaModel<U extends TypeInfo> extends MetaModel<Argument, U>
            implements DescribedOptionMetaModel, ParameterMetaModel {

        private final TypeInfo paramInfoType;

        /**
         * Create a new argument meta-model.
         *
         * @param type       annotated variable type
         * @param annotation annotation
         */
        ArgumentMetaModel(Option.Argument annotation, U type) {
            super(annotation, type);
            paramInfoType = TypeInfo.of(TypeInfo.of(ArgumentInfo.class), type);
        }

        @Override
        public String description() {
            return annotation().description();
        }

        @Override
        public TypeInfo paramInfoType() {
            return paramInfoType;
        }
    }

    /**
     * Meta-model for the elements annotated with {@link Option.Flag}.
     */
    static class FlagMetaModel extends MetaModel<Flag, TypeInfo>
            implements NamedOptionMetaModel, ParameterMetaModel {

        /**
         * Create a new flag meta-model.
         *
         * @param annotation annotation
         */
        FlagMetaModel(Option.Flag annotation) {
            super(annotation, TypeInfo.of(Boolean.class));
        }

        @Override
        public String description() {
            return annotation().description();
        }

        @Override
        public String name() {
            return annotation().name();
        }

        @Override
        public TypeInfo paramInfoType() {
            return TypeInfo.of(FlagInfo.class);
        }
    }

    /**
     * Meta-model for the elements annotated with {@link Option.KeyValue}.
     *
     * @param <U> type info type
     */
    static class KeyValueMetaModel<U extends TypeInfo> extends MetaModel<KeyValue, U>
            implements NamedOptionMetaModel, ParameterMetaModel {

        private final TypeInfo paramInfoType;

        /**
         * Create a new key-value meta-model.
         *
         * @param type       type info of the annotated element
         * @param annotation annotation
         */
        KeyValueMetaModel(Option.KeyValue annotation, U type) {
            super(annotation, type);
            paramInfoType = TypeInfo.of(TypeInfo.of(KeyValueInfo.class), type);
        }

        @Override
        public String name() {
            return annotation().name();
        }

        @Override
        public String description() {
            return annotation().description();
        }

        @Override
        public TypeInfo paramInfoType() {
            return paramInfoType;
        }
    }

    /**
     * Meta-model for the elements annotated with {@link Option.KeyValues}.
     *
     * @param <U> type info type
     */
    static class KeyValuesMetaModel<U extends TypeInfo> extends MetaModel<KeyValues, U>
            implements NamedOptionMetaModel, ParameterMetaModel {

        private final TypeInfo paramInfoType;

        /**
         * Create a new key-values meta-model.
         *
         * @param annotation annotation
         * @param type       type info of the annotated element
         */
        KeyValuesMetaModel(Option.KeyValues annotation, U type) {
            super(annotation, type);
            paramInfoType = TypeInfo.of(TypeInfo.of(KeyValuesInfo.class), type);
        }

        @Override
        public String description() {
            return annotation().description();
        }

        @Override
        public String name() {
            return annotation().name();
        }

        @Override
        public TypeInfo paramInfoType() {
            return paramInfoType;
        }
    }

    /**
     * Base meta-model for the elements annotated with {@link CommandFragment} and {@link Command}.
     *
     * @param <T> annotation type
     * @param <U> type info type
     */
    abstract static class ParametersMetaModel<T, U extends TypeInfo> extends MetaModel<T, U>
            implements ParameterMetaModel {

        private final List<ParameterMetaModel> params;
        private final List<String> paramNames;

        /**
         * Create a new parameters meta-model.
         *
         * @param annotation annotation
         * @param type       type info of the annotated element
         * @param params     parameters meta-model
         */
        protected ParametersMetaModel(T annotation, U type, List<ParameterMetaModel> params) {
            super(annotation, type);
            this.params = Objects.requireNonNull(params, "params is null");
            paramNames = params.stream()
                               .filter(NamedOptionMetaModel.class::isInstance)
                               .map(NamedOptionMetaModel.class::cast)
                               .map(NamedOptionMetaModel::name)
                               .collect(Collectors.toList());
        }

        /**
         * Get the parameters.
         *
         * @return list of models for the parameters
         */
        final List<ParameterMetaModel> params() {
            return params;
        }

        /**
         * Get the parameter names.
         *
         * @return list of parameter name
         */
        final List<String> paramNames() {
            return paramNames;
        }

        /**
         * Compute the duplicates between the given parameters name with the ones of this instance.
         *
         * @param names name of parameters to compare duplicates for
         * @return list of duplicated parameters name
         */
        final List<String> duplicates(List<String> names) {
            return names.stream()
                        .filter(this.paramNames::contains)
                        .collect(Collectors.toList());
        }
    }

    /**
     * Meta-model for the elements annotated with {@link CommandFragment}.
     *
     * @param <U> type info type
     */
    static final class FragmentMetaModel<U extends TypeInfo> extends ParametersMetaModel<CommandFragment, U> {

        private final TypeInfo paramInfoType;

        /**
         * Create a new command fragment meta-model.
         *
         * @param annotation annotation
         * @param type       type info of the annotated element
         * @param params     parameters meta-model
         */
        FragmentMetaModel(CommandFragment annotation, U type, List<ParameterMetaModel> params) {
            super(annotation, type, params);
            paramInfoType = TypeInfo.of(TypeInfo.of(CommandFragmentInfo.class), type);
        }

        @Override
        public TypeInfo paramInfoType() {
            return paramInfoType;
        }
    }

    /**
     * Meta-model for the elements annotated with {@link Command}.
     *
     * @param <U> type info type
     */
    static final class CommandMetaModel<U extends TypeInfo> extends ParametersMetaModel<Command, U>
            implements Comparable<CommandMetaModel<?>> {

        private final TypeInfo paramInfoType;

        /**
         * Create a new command meta-model.
         *
         * @param annotation annotation
         * @param type       type info of the annotated element
         * @param params     parameters meta-model
         */
        CommandMetaModel(Command annotation, U type, List<ParameterMetaModel> params) {
            super(annotation, type, params);
            paramInfoType = TypeInfo.of(CommandInfo.class);
        }

        @Override
        public int compareTo(CommandMetaModel<?> model) {
            return annotation().name().compareTo(model.annotation().name());
        }

        @Override
        public TypeInfo paramInfoType() {
            return paramInfoType;
        }
    }

    /**
     * Meta-model for the elements annotated with {@link CommandLineInterface}.
     *
     * @param <U> type info type
     */
    static final class CLIMetaModel<U extends TypeInfo> extends MetaModel<CommandLineInterface, U> {

        private final List<CommandMetaModel<U>> commands;

        /**
         * Create a new CLI meta-model.
         *
         * @param annotation annotation
         * @param type       type info of the annotated element
         * @param commands   command meta-models
         */
        CLIMetaModel(CommandLineInterface annotation, U type, List<CommandMetaModel<U>> commands) {
            super(annotation, type);
            this.commands = Objects.requireNonNull(commands, "commands is null");
        }

        /**
         * Get the commands.
         *
         * @return list of command meta-models
         */
        List<CommandMetaModel<U>> commands() {
            return commands;
        }
    }
}
