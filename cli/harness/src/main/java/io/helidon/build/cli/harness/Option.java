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
package io.helidon.build.cli.harness;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Describes a command option.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface Option {

    /**
     * Name predicate to validate option names.
     */
    Predicate<String> VALID_NAME = Pattern.compile("^[a-zA-Z0-9]{1,}[-]?[a-zA-Z0-9]{0,}[-]?[a-zA-Z0-9]{0,}[-]?[a-zA-Z0-9]{0,}$")
                                          .asMatchPredicate();

    /**
     * Describes a unique key with a multiple values command line option. It is passed with two separate command line arguments:
     * {@code --key value} and can be repeated multiple times: {@code --key value1 --key value2}. Multiple values can also be
     * provided with the same argument by using {@code ,} as a separator: {@code --key value1,value2}. A key-values option is
     * never required, it values will default to an empty collection is not provided.
     * <p>
     * This annotation can only be used on a constructor argument of type {@link Collection} and the type parameter must be one of
     * the supported types listed in {@link #SUPPORTED_TYPES}
     * </p>
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface KeyValues {

        /**
         * The option name.
         *
         * @return option name
         * @see Option#VALID_NAME
         */
        String name();

        /**
         * The option description.
         *
         * @return description
         */
        String description();

        /**
         * The required flag. default is {@code false}
         *
         * @return {@code true} if optional, {@code false} if required
         */
        boolean required() default false;

        /**
         * Supported value types.
         */
        List<Class<?>> SUPPORTED_TYPES = List.of(String.class, Integer.class, File.class, Enum.class);
    }

    /**
     * Describes a key with a single value command line option. It is passed with two separate command line arguments:
     * {@code --key value}. If a {@link #defaultValue()} is set, the option is not required and the default value
     * will be converted to the declared type ; otherwise an error will be raised indicating that a required option is missing.
     * <p>
     * This annotation can only be used on a constructor argument with one of the supported types listed in {@link #SUPPORTED_TYPES}
     * </p>
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface KeyValue {

        /**
         * The option name.
         *
         * @return option name
         * @see Option#VALID_NAME
         */
        String name();

        /**
         * The option description.
         *
         * @return description
         */
        String description();

        /**
         * The default value for the option, if the option is not required.
         *
         * @return default value if the option is required
         */
        String defaultValue() default "";

        /**
         * The required flag. default is {@code false}
         *
         * @return {@code true} if optional, {@code false} if required
         */
        boolean required() default false;

        /**
         * The visible flag.
         *
         * @return {@code true} if visible, {@code false} if not
         */
        boolean visible() default true;

        /**
         * Supported value types.
         */
        List<Class<?>> SUPPORTED_TYPES = List.of(String.class, Integer.class, File.class, Enum.class);
    }

    /**
     * Describes a flag command line option. A flag represents something to enable, it is basically a {@code boolean} that
     * defaults to false when the option is not set, and is {@code true} when set. A flag is strictly intended for opt-in
     * features, it is not a required command line option.
     * <p>
     * This annotation can only be used on a constructor argument with the type {@link Boolean} or {@code boolean}.
     * </p>
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface Flag {

        /**
         * The option name.
         *
         * @return option name
         * @see Option#VALID_NAME
         */
        String name();

        /**
         * The option description.
         *
         * @return description
         */
        String description();

        /**
         * The visible flag.
         *
         * @return {@code true} if visible, {@code false} if not
         */
        boolean visible() default true;
    }

    /**
     * Describes an unnamed command line option. An argument represents a value to be passed to a command. An argument does not
     * have a default value, but may be optional. An optional argument is declared by having {@link #required()} set to
     * {@code false}. The value for an optional argument is {@code null}.
     * <p>
     * This annotation can only be used on a constructor argument with one of the supported types listed in {@link #SUPPORTED_TYPES}
     * </p>
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface Argument {

        /**
         * The option description.
         *
         * @return description
         */
        String description();

        /**
         * The required flag.
         *
         * @return {@code true} if optional, {@code false} if required
         */
        boolean required() default true;

        /**
         * Supported value types.
         */
        List<Class<?>> SUPPORTED_TYPES = List.of(String.class, File.class, Enum.class);
    }
}
