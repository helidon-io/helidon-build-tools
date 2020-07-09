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

import java.io.File;
import java.util.Collection;
import java.util.Objects;

/**
 * Command model.
 */
public abstract class CommandModel extends CommandParameters {

    private final CommandInfo commandInfo;

    /**
     * Create a new command model.
     * @param commandInfo command info, must be non {@code null}
     */
    protected CommandModel(CommandInfo commandInfo) {
        this.commandInfo = Objects.requireNonNull(commandInfo, "commandInfo is null");
        // global options
        addParameter(GlobalOptions.HELP_FLAG_INFO);
        addParameter(GlobalOptions.VERBOSE_FLAG_INFO);
        addParameter(GlobalOptions.DEBUG_FLAG_INFO);
        addParameter(GlobalOptions.PLAIN_FLAG_INFO);
    }

    /**
     * Indicate if the command is visible.
     *
     * @return {@code true} if visible, {@code false} if not visible.
     */
    boolean visible() {
        return true;
    }

    /**
     * Get the command for this model.
     *
     * @return {@link Command}, never {@code null}
     */
    public final CommandInfo command() {
        return commandInfo;
    }

    /**
     * Create a {@link CommandExecution} for this model.
     *
     * @param parser command parser
     * @return new {@link CommandExecution} instance
     */
    public abstract CommandExecution createExecution(CommandParser parser);

    /**
     * Meta model for the {@link Command} annotation.
     */
    public static final class CommandInfo {

        private final String name;
        private final String description;

        /**
         * Create a new command info.
         *
         * @param name        command name.
         * @param description command description
         */
        public CommandInfo(String name, String description) {
            this.name = Objects.requireNonNull(name, "name is null");
            this.description = Objects.requireNonNull(description, "description is null");
        }

        /**
         * The command name.
         *
         * @return command name, never {@code null}
         */
        public String name() {
            return name;
        }

        /**
         * The command description.
         *
         * @return command description, never {@code null}
         */
        public String description() {
            return description;
        }
    }

    private static String valueUsage(Class<?> type) {
        String usage = "";
        if (String.class.equals(type)) {
            usage += "VALUE";
        } else if (Integer.class.equals(type)) {
            usage += "NUMBER";
        } else if (File.class.equals(type)) {
            usage += "PATH";
        } else if (Enum.class.isAssignableFrom(type)) {
            String choices = "";
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
            for (Enum e : enumClass.getEnumConstants()) {
                if (!choices.isEmpty()) {
                    choices += "|";
                }
                choices += e.name();
            }
            usage += choices;
        }
        return usage;
    }

    /**
     * Common meta-model for {@link Option.Argument} and {@link Option}.
     *
     * @param <T> mapped type
     */
    public abstract static class OptionInfo<T> implements ParameterInfo<T> {

        private final Class<T> type;
        private final String description;

        /**
         * Create a new option info.
         *
         * @param type        corresponding type
         * @param description description, must be non {@code null}
         */
        protected OptionInfo(Class<T> type, String description) {
            this.type = type;
            this.description = Objects.requireNonNull(description, "description is null");
        }

        /**
         * The attribute description.
         *
         * @return option description, never {@code null}
         */
        public final String description() {
            return description;
        }

        @Override
        public final Class<T> type() {
            return type;
        }

        /**
         * Get the usage for this option.
         *
         * @return usage, never {@code null}
         */
        abstract String usage();
    }

    /**
     * Meta model for the {@link Option.Argument} annotation.
     *
     * @param <T> mapped type
     */
    public static final class ArgumentInfo<T> extends OptionInfo<T> {

        private final boolean required;

        /**
         * Create a new argument info.
         *
         * @param type        argument field type
         * @param description argument description
         * @param required    argument required flag
         */
        public ArgumentInfo(Class<T> type, String description, boolean required) {
            super(type, description);
            this.required = required;
        }

        /**
         * The attribute required flag.
         *
         * @return required flag
         */
        public boolean required() {
            return required;
        }

        @Override
        String usage() {
            return (required ? "" : "[")
                    + description().toUpperCase()
                    + (required ? "" : "]");
        }
    }

    /**
     * Meta model for the {@link Option} annotation.
     *
     * @param <T> mapped type
     */
    public abstract static class NamedOptionInfo<T> extends OptionInfo<T> {

        private final String name;
        private final boolean visible;

        /**
         * Create a new named option info.
         *
         * @param type        corresponding type
         * @param name        name, must be non {@code null}
         * @param description description, must be non {@code null}
         * @param visible     visible flag
         */
        protected NamedOptionInfo(Class<T> type, String name, String description, boolean visible) {
            super(type, description);
            this.name = Objects.requireNonNull(name, "name is null");
            this.visible = visible;
        }

        /**
         * Create a new visible named option info.
         *
         * @param type        corresponding type
         * @param name        name, must be non {@code null}
         * @param description description, must be non {@code null}
         */
        protected NamedOptionInfo(Class<T> type, String name, String description) {
            this(type, name, description, /* visible */ true);
        }

        /**
         * The option name.
         *
         * @return option name, never {@code null}
         */
        public String name() {
            return name;
        }

        @Override
        public boolean visible() {
            return visible;
        }
    }

    /**
     * Meta model for repeatable {@link Option.Flag} annotation.
     */
    public static final class FlagInfo extends NamedOptionInfo<Boolean> {

        /**
         * Create a new flag info.
         *
         * @param name        flag name
         * @param description flag description
         * @param visible     flag visible, {@code false} to make it hidden
         */
        public FlagInfo(String name, String description, boolean visible) {
            super(Boolean.class, name, description, visible);
        }

        /**
         * Create a new flag info.
         *
         * @param name        option name
         * @param description option description
         */
        public FlagInfo(String name, String description) {
            this(name, description, /* visible */ true);
        }

        @Override
        String usage() {
            return "[--" + name() + "]";
        }
    }

    /**
     * Meta model for the {@link Option.KeyValue} annotation.
     *
     * @param <T> item type
     */
    public static final class KeyValueInfo<T> extends NamedOptionInfo<T> {

        private final T defaultValue;
        private final boolean required;

        /**
         * Create a new key value info.
         *
         * @param type         option type
         * @param name         option name
         * @param description  option description
         * @param defaultValue default value, may be {@code null} if the option is not required
         * @param required     option required
         * @param visible      option visible
         */
        public KeyValueInfo(Class<T> type, String name, String description, T defaultValue, boolean required, boolean visible) {
            super(type, name, description, visible);
            this.defaultValue = defaultValue;
            this.required = required;
        }

        /**
         * Create a new key value info.
         *
         * @param type         option type
         * @param name         option name
         * @param description  option description
         * @param defaultValue default value, may be {@code null} if the option is not required
         * @param required     option required
         */
        public KeyValueInfo(Class<T> type, String name, String description, T defaultValue, boolean required) {
            this(type, name, description, defaultValue, required, true);
        }

        /**
         * The default value for the option, if the option is not required.
         *
         * @return default value or {@code null} if the option is required
         */
        public T defaultValue() {
            return defaultValue;
        }

        /**
         * The attribute required flag.
         *
         * @return required flag
         */
        public boolean required() {
            return required;
        }

        @Override
        String usage() {
            return (required ? "" : "[")
                    + "--" + name() + " " + valueUsage(type())
                    + (required ? "" : "]");
        }
    }

    /**
     * Meta model for the {@link Option.KeyValues} annotation.
     *
     * @param <T> item type
     */
    public static final class KeyValuesInfo<T> extends NamedOptionInfo<Collection<T>> {

        private final Class<T> paramType;
        private final boolean required;

        /**
         * Create a new key values info.
         *
         * @param paramType   option field type parameter type
         * @param name        option name
         * @param description option description
         * @param required    option required
         */
        public KeyValuesInfo(Class<T> paramType, String name, String description, boolean required) {
            super(null, name, description);
            this.required = required;
            this.paramType = Objects.requireNonNull(paramType, "paramType is null");
        }

        /**
         * Get the parameter type.
         *
         * @return type, never {@code null}
         */
        public Class<T> paramType() {
            return paramType;
        }

        /**
         * The attribute required flag.
         *
         * @return required flag
         */
        public boolean required() {
            return required;
        }

        @Override
        String usage() {
            return (required ? "" : "[")
                    + "--" + name() + " " + valueUsage(paramType)
                    + "[," + valueUsage(paramType) + "]"
                    + (required ? "" : "]");
        }
    }
}
