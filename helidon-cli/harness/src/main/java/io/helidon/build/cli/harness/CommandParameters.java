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
package io.helidon.build.cli.harness;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Model common to {@link Command} and {@link CommandFragment}.
 */
public class CommandParameters {

    private final List<ParameterInfo> params;

    /**
     * Create a new command parameters.
     */
    protected CommandParameters() {
        this.params = new LinkedList<>();
    }

    /**
     * Add an attribute to the command model.
     *
     * @param param parameter info to add
     */
    protected final void addParameter(ParameterInfo param) {
        params.add(Objects.requireNonNull(param, "param is null"));
    }

    /**
     * Get the parameters for this model.
     *
     * @return list of {@link ParameterInfo}, never {@code null}
     */
    public final List<ParameterInfo> parameters() {
        return params;
    }

    /**
     * Meta model for parameters to retain the mapped type.
     *
     * @param <T> mapped type
     */
    public interface ParameterInfo<T> {

        /**
         * The parameter type.
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
         * @param type fragment type
         */
        protected CommandFragmentInfo(Class<T> type) {
            super();
            this.type = Objects.requireNonNull(type, "type is null");
        }

        @Override
        public final Class<T> type() {
            return type;
        }

        /**
         * Resolve a fragment instance.
         *
         * @param parser command parser
         * @return created fragment
         */
        public abstract T resolve(CommandParser parser);
    }
}
