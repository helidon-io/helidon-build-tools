/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.cli.harness;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Model common to {@link Command} and {@link CommandFragment}.
 */
public class CommandParameters {

    private final List<ParameterInfo<?>> params;

    /**
     * Create a new command parameters.
     *
     * @param params parameters info
     */
    protected CommandParameters(ParameterInfo<?>... params) {
        this.params = new LinkedList<>();
        if (params != null) {
            Collections.addAll(this.params, params);
        }
    }

    /**
     * Create a new command parameters.
     *
     * @param globalOptions global options info
     * @param params      parameters info
     */
    protected CommandParameters(CommandParameters.ParameterInfo<?>[] globalOptions, ParameterInfo<?>... params) {
        this.params = new LinkedList<>();
        if (globalOptions != null) {
            Collections.addAll(this.params, globalOptions);
        }
        if (params != null) {
            Collections.addAll(this.params, params);
        }
    }

    /**
     * Get the parameters for this model.
     *
     * @return list of {@link ParameterInfo}, never {@code null}
     */
    public final List<ParameterInfo<?>> parameters() {
        return params;
    }

    /**
     * Get the parameters as a map.
     *
     * @return map of {@link ParameterInfo} keyed by their name.
     */
    public final Map<String, ParameterInfo<?>> parametersMap() {
        return params.stream()
                .flatMap(CommandParameters::paramStream)
                .distinct()
                .collect(Collectors.toMap(CommandParameters::paramName, Function.identity()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandParameters that = (CommandParameters) o;
        return params.equals(that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    /**
     * Meta model for parameters to retain the mapped type.
     *
     * @param <T> mapped type
     */
    public interface ParameterInfo<T> {

        /**
         * The parameter type.
         *
         * @return type
         */
        Class<T> type();

        /**
         * Indicate if the parameter is visible.
         *
         * @return {@code true} if visible, {@code false} if not visible.
         */
        default boolean visible() {
            return true;
        }
    }

    /**
     * Base class for meta-model implementations of {@link CommandFragment}.
     *
     * @param <T> mapped type
     */
    public abstract static class CommandFragmentInfo<T> extends CommandParameters implements ParameterInfo<T> {

        private final Class<T> type;

        /**
         * Create a new fragment info.
         *
         * @param type   fragment type
         * @param params fragment parameters
         */
        protected CommandFragmentInfo(Class<T> type, ParameterInfo<?>... params) {
            super(params);
            this.type = Objects.requireNonNull(type, "type is null");
        }

        @Override
        public final Class<T> type() {
            return type;
        }

        /**
         * Resolve a fragment instance.
         *
         * @param resolver command parser resolver
         * @return created fragment
         */
        public abstract T resolve(CommandParser.Resolver resolver);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            CommandFragmentInfo<?> that = (CommandFragmentInfo<?>) o;
            return type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), type);
        }

        @Override
        public String toString() {
            return "CommandFragmentInfo{"
                    + "params=" + parameters()
                    + ", type=" + type
                    + '}';
        }
    }

    private static String paramName(ParameterInfo<?> param) {
        if (param instanceof CommandModel.NamedOptionInfo) {
            return ((CommandModel.NamedOptionInfo<?>) param).name();
        }
        return "";
    }

    private static Stream<ParameterInfo<?>> paramStream(ParameterInfo<?> param) {
        if (param instanceof CommandFragmentInfo) {
            return ((CommandFragmentInfo<?>) param).parameters().stream();
        }
        return Stream.of(param);
    }
}
