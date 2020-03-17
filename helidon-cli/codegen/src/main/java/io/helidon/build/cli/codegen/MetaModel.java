/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.TypeElement;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.Option;

/**
 * Meta model to represent the processed annotations.
 */
abstract class MetaModel<T> {

    private final TypeElement typeElt;
    private final T annotation;

    protected MetaModel(T annotation) {
        this.annotation = annotation;
        this.typeElt = null;
    }

    protected MetaModel(T option, TypeElement typeElt) {
        this.annotation = option;
        this.typeElt = typeElt;
    }

    /**
     * Get the annotation value.
     * @return annotation value
     */
    T annotation() {
        return annotation;
    }

    /**
     * Get the type for this meta-model.
     * @return type
     */
    TypeElement type() {
        return typeElt;
    }

    /**
     * A meta-model with a description.
     */
    interface DescribedOptionMetaModel {

        /**
         * Get the description.
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
         * @return name, never {@code null}
         */
        String name();
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.Option.Argument}.
     */
    static class ArgumentMetaModel extends MetaModel<Option.Argument> implements DescribedOptionMetaModel {

        private final Option.Argument option;

        /**
         * Create a new argument meta-model.
         * @param typeElt annotated variable type
         * @param option annotation
         */
        ArgumentMetaModel(TypeElement typeElt, Option.Argument option) {
            super(option, typeElt);
            this.option = Objects.requireNonNull(option, "option is null");
        }

        @Override
        public String description() {
            return option.description();
        }
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.Option.Flag}.
     */
    static class FlagMetaModel extends MetaModel<Option.Flag> implements NamedOptionMetaModel {

        private final Option.Flag option;

        /**
         * Create a new flag meta-model.
         * @param option annotation
         */
        FlagMetaModel(Option.Flag option) {
            super(option);
            this.option = Objects.requireNonNull(option, "option is null");
        }

        @Override
        public String description() {
            return option.description();
        }

        @Override
        public String name() {
            return option.name();
        }
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.Option.KeyValue}.
     */
    static class KeyValueMetaModel extends MetaModel<Option.KeyValue> implements NamedOptionMetaModel {

        private final Option.KeyValue option;

        /**
         * Create a new key-value meta-model.
         * @param typeElt annotated variable type
         * @param option annotation
         */
        KeyValueMetaModel(TypeElement typeElt, Option.KeyValue option) {
            super(option, typeElt);
            this.option = Objects.requireNonNull(option, "option is null");
        }

        @Override
        public String name() {
            return option.name();
        }

        @Override
        public String description() {
            return option.description();
        }
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.Option.KeyValues}.
     */
    static class KeyValuesMetaModel extends MetaModel<Option.KeyValues> implements NamedOptionMetaModel {

        private final Option.KeyValues option;
        private final TypeElement paramTypeElt;

        /**
         * Create a new key-values meta-model.
         * @param paramTypeElt annotated variable collection parameter type
         * @param option annotation
         */
        KeyValuesMetaModel(TypeElement paramTypeElt, Option.KeyValues option) {
            super(option);
            this.option = Objects.requireNonNull(option, "option is null");
            this.paramTypeElt = Objects.requireNonNull(paramTypeElt, "paramTypeElt is null");
        }

        @Override
        public String description() {
            return option.description();
        }

        @Override
        public String name() {
            return option.name();
        }

        /**
         * Get the generic parameter type.
         * @return type
         */
        TypeElement paramType() {
            return paramTypeElt;
        }
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.CommandFragment}.
     */
    abstract static class ParametersMetaModel<T> extends MetaModel<T> {

        private final List<MetaModel<?>> params;
        private final String pkg;

        protected ParametersMetaModel(T annotation, TypeElement typeElt, String pkg, List<MetaModel<?>> params) {
            super(annotation, Objects.requireNonNull(typeElt, "typeElt is null"));
            this.pkg = Objects.requireNonNull(pkg, "pk is null");
            this.params = params;
        }

        /**
         * Get the parameters.
         * @return list of models for the parameters
         */
        final List<MetaModel<?>> params() {
            return params;
        }

        /**
         * Get the java package.
         * @return java package name
         */
        final String pkg() {
            return pkg;
        }

        /**
         * Get the option names.
         * @return option names
         */
        final List<String> optionNames() {
            List<String> names = new ArrayList<>();
            for (MetaModel attr : params) {
                if (attr instanceof NamedOptionMetaModel) {
                    names.add(((NamedOptionMetaModel) attr).name());
                }
            }
            return names;
        }

        /**
         * Get the duplicates between the given option names and the current option names of this fragment.
         * @param optionNames option names to compare duplicates for
         * @return list of duplicated option names
         */
        final List<String> optionDuplicates(List<String> optionNames) {
            List<String> duplicates = new ArrayList<>();
            for (String optionName : optionNames()) {
                if (optionNames.contains(optionName)) {
                    duplicates.add(optionName);
                }
            }
            return duplicates;
        }
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.Command}.
     */
    static final class CommandFragmentMetaModel extends ParametersMetaModel<Void> {

        /**
         * Create a new command fragment meta-model.
         * @param typeElt annotated class type
         * @param pkg java package
         * @param params all options for this fragment
         */
        CommandFragmentMetaModel(TypeElement typeElt, String pkg, List<MetaModel<?>> params) {
            super(null, typeElt, pkg, params);
        }
    }

    /**
     * Meta-model for {@link io.helidon.build.cli.harness.Command}.
     */
    static final class CommandMetaModel extends ParametersMetaModel<Command> implements Comparable<CommandMetaModel> {

        /**
         * Create a new command meta-model.
         * @param optionsModel fragment meta model to compose this command model with
         * @param command command annotation
         */
        CommandMetaModel(TypeElement typeElt, String pkg, List<MetaModel<?>> params, Command annotation) {
            super(annotation, typeElt, pkg, params);
        }

        @Override
        public int compareTo(CommandMetaModel cmd) {
            return annotation().name().compareTo(cmd.annotation().name());
        }
    }
}
