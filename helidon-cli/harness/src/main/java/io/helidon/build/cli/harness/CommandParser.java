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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import io.helidon.build.cli.harness.CommandModel.ArgumentInfo;
import io.helidon.build.cli.harness.CommandModel.FlagInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValuesInfo;

/**
 * Command parser.
 */
public final class CommandParser {

    private static final List<String> GLOBAL_OPTIONS = List.of(
            "--" + CommandModel.HELP_OPTION.name(),
            "--" + CommandModel.VERBOSE_OPTION.name(),
            "--" + CommandModel.DEBUG_OPTION.name());

    static final String TOO_MANY_ARGUMENTS = "Too many arguments";
    static final String INVALID_REPEATING_OPTION = "Invalid repeating option";
    static final String INVALID_COMMAND_NAME = "Invalid command name";
    static final String INVALID_OPTION_NAME = "Invalid option name";
    static final String INVALID_PROPERTY = "Invalid property";
    static final String MISSING_REQUIRED_ARGUMENT = "Missing required argument";
    static final String INVALID_ARGUMENT_VALUE = "Invalid argument value";
    static final String INVALID_OPTION_VALUE = "Invalid option value";
    static final String INVALID_CHOICE = "Invalid choice";
    static final String UNREPEATABLE_OPTION = "Option cannot be repeated";
    static final String MISSING_REQUIRED_OPTION = "Missing required option";

    private final String[] rawArgs;
    private final Map<String, Parameter> params;
    private final String error;
    private final String commandName;
    private final Properties properties;

    private CommandParser(String[] rawArgs, String commandName, Map<String, Parameter> params, Properties properties,
            String error) {

        this.rawArgs = rawArgs;
        this.commandName = commandName;
        this.params = params;
        this.error = error;
        this.properties = properties;
    }

    /**
     * Parse the command line arguments.
     *
     * @param rawArgs arguments to parse
     * @return parser
     */
    static CommandParser create(String... rawArgs) {
        Objects.requireNonNull(rawArgs, "rawArgs is null");
        String error = null;
        String commandName = null;
        Map<String, Parameter> params = new HashMap<>();
        Properties properties = new Properties();
        for (int i = 0; i < rawArgs.length; i++) {
            String rawArg = rawArgs[i];
            if (rawArg == null || rawArg.isEmpty()) {
                continue;
            }
            rawArg = rawArg.trim();
            String arg = rawArg.trim().toLowerCase();
            if (rawArg.length() > 2 && rawArg.charAt(0) == '-' && rawArg.charAt(1) == 'D') {
                String prop = rawArg.substring(2);
                if (prop.length() >= 3) {
                    int index = prop.indexOf('=');
                    if (index >= 0) {
                        String propName = prop.substring(0, index);
                        String propValue = prop.substring(index + 1, prop.length());
                        properties.put(propName, propValue);
                        continue;
                    }
                }
                error = INVALID_PROPERTY + ": " + prop;
                break;
            } else if (!GLOBAL_OPTIONS.contains(arg) && commandName == null) {
                if (!Command.NAME_PREDICATE.test(arg)) {
                    error = INVALID_COMMAND_NAME + ": " + rawArg;
                    break;
                }
                commandName = arg;
            } else if (arg.length() > 2 && arg.charAt(0) == '-' && arg.charAt(1) == '-') {
                String optionName = arg.substring(2);
                if (!Option.NAME_PREDICATE.test(optionName)) {
                    error = INVALID_OPTION_NAME + ": " + optionName;
                    break;
                }
                Parameter param = params.get(optionName);
                if (i + 1 < rawArgs.length) {
                    // key-value(s)
                    String value = rawArgs[i + 1].trim();
                    if (value.charAt(0) != '-') {
                        String[] splitValues = value.split(",");
                        if (param == null && splitValues.length == 1) {
                            params.put(optionName, new KeyValueParam(optionName, value));
                        } else if (param == null) {
                            LinkedList<String> values = new LinkedList<>();
                            for (String splitValue : splitValues) {
                                values.add(splitValue);
                            }
                            params.put(optionName, new KeyValuesParam(optionName, values));
                        } else if (param instanceof KeyValueParam) {
                            LinkedList<String> values = new LinkedList<>();
                            values.add(((KeyValueParam) param).value);
                            values.add(value);
                            params.put(optionName, new KeyValuesParam(optionName, values));
                        } else if (param instanceof KeyValuesParam) {
                            for (String splitValue : splitValues) {
                                ((KeyValuesParam) param).values.add(splitValue);
                            }
                        } else {
                            error = INVALID_REPEATING_OPTION + ": " + optionName;
                            break;
                        }
                        i++;
                        continue;
                    }
                }
                // flag
                if (param == null) {
                    params.put(optionName, new FlagParam(optionName));
                } else {
                    error = INVALID_REPEATING_OPTION + ": " + optionName;
                    break;
                }
            } else if (params.containsKey("")) {
                error = TOO_MANY_ARGUMENTS;
                break;
            } else {
                params.put("", new ArgumentParam(arg));
            }
        }
        return new CommandParser(rawArgs, commandName, params, properties, error);
    }

    /**
     * Get the first parsing error if any.
     *
     * @return parsing error, or {@code null} if there is no error.
     */
    Optional<String> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Get the parsed command name.
     *
     * @return command name
     */
    Optional<String> commandName() {
        return Optional.ofNullable(commandName);
    }

    /**
     * Get the parsed parameters.
     *
     * @return map of parameter
     */
    Map<String, Parameter> params() {
        return params;
    }

    /**
     * Get the parsed properties.
     *
     * @return properties, never {@code null}
     */
    Properties properties() {
        return properties;
    }

    private static <T> T resolveValue(Class<T> type, String rawValue) {
        Objects.requireNonNull(rawValue, "rawValue is null");
        if (String.class.equals(type)) {
            return type.cast(rawValue);
        }
        if (Integer.class.equals(type)) {
            return type.cast(Integer.parseInt(rawValue));
        }
        if (File.class.equals(type)) {
            return type.cast(new File(rawValue));
        }
        if (Enum.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
            for (Enum e : enumClass.getEnumConstants()) {
                if (rawValue.equalsIgnoreCase(e.name())) {
                    return type.cast(e);
                }
            }
            throw new CommandParserException(INVALID_CHOICE + ": " + rawValue);
        }
        throw new IllegalArgumentException("Invalid value type: " + type);
    }

    private static boolean isSupported(Class<?> type, List<Class<?>> supportedTypes) {
        for (Class<?> supportedType : supportedTypes) {
            if (supportedType.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the given flag option.
     *
     * @param option the option to resolve
     * @return {@code true} if the option is present, {@code false} otherwise
     * @throws CommandParserException if an error occurs while resolving the option
     */
    public boolean resolve(FlagInfo option) throws CommandParserException {
        Parameter resolved = params.get(option.name());
        if (resolved == null) {
            return false;
        } else if (resolved instanceof FlagParam) {
            return true;
        }
        throw new CommandParserException(INVALID_OPTION_VALUE + ": " + option.name());
    }

    /**
     * Resolve the given key-value option.
     *
     * @param <T>    option type
     * @param option the option to resolve
     * @return resolved value for the option
     * @throws CommandParserException if an error occurs while resolving the option
     */
    public <T> T resolve(KeyValueInfo<T> option) throws CommandParserException {
        Class<T> type = option.type();
        Parameter resolved = params.get(option.name());
        T defaultValue = option.defaultValue();
        if (resolved == null && option.required()) {
            throw new CommandParserException(MISSING_REQUIRED_OPTION + ": " + option.name());
        }
        if (isSupported(type, Option.KeyValue.SUPPORTED_TYPES)) {
            if (resolved == null) {
                return defaultValue;
            } else if (resolved instanceof KeyValueParam) {
                return resolveValue(type, ((KeyValueParam) resolved).value);
            } else if (resolved instanceof KeyValuesParam) {
                throw new CommandParserException(UNREPEATABLE_OPTION + ": " + option.name());
            }
        }
        throw new CommandParserException(INVALID_OPTION_VALUE + ": " + option.name());
    }

    /**
     * Resolve the given key-values option.
     *
     * @param <T>    item type
     * @param option the option to resolve
     * @return collection of resolved values for the option
     * @throws CommandParserException if an error occurs while resolving the option
     */
    public <T> Collection<T> resolve(KeyValuesInfo<T> option) throws CommandParserException {
        Class<T> type = option.paramType();
        Parameter resolved = params.get(option.name());
        if (resolved == null) {
            return Collections.emptyList();
        }
        if (isSupported(type, Option.KeyValues.SUPPORTED_TYPES)) {
            if (resolved instanceof KeyValueParam) {
                return List.of(resolveValue(type, ((KeyValueParam) resolved).value));
            } else if (resolved instanceof KeyValuesParam) {
                LinkedList<T> resolvedValues = new LinkedList<>();
                for (String value : ((KeyValuesParam) resolved).values) {
                    resolvedValues.add(resolveValue(type, value));
                }
                return resolvedValues;
            }
        }
        throw new CommandParserException(INVALID_OPTION_VALUE + ": " + option.name());
    }

    /**
     * Resolve the given argument option.
     *
     * @param <T>    argument type
     * @param option the argument to resolve
     * @return resolved value for the argument
     * @throws CommandParserException if an error occurs while resolving the option
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(ArgumentInfo<T> option) throws CommandParserException {
        Class<T> type = option.type();
        Parameter resolved = params.get("");
        if (resolved == null && option.required()) {
            throw new CommandParserException(MISSING_REQUIRED_ARGUMENT);
        }
        if (isSupported(type, Option.Argument.SUPPORTED_TYPES)) {
            if (resolved == null) {
                return (T) null;
            } else if (resolved instanceof ArgumentParam) {
                return type.cast(((ArgumentParam) resolved).value);
            }
        }
        throw new CommandParserException(INVALID_ARGUMENT_VALUE);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Arrays.deepHashCode(this.rawArgs);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CommandParser other = (CommandParser) obj;
        return Arrays.deepEquals(this.rawArgs, other.rawArgs);
    }

    /**
     * Parser error.
     */
    public static final class CommandParserException extends RuntimeException {

        private CommandParserException(String message) {
            super(message);
        }
    }

    /**
     * Base class for all parsed parameters.
     */
    public abstract static class Parameter {

        private final String name;

        private Parameter(String name) {
            this.name = Objects.requireNonNull(name, "name is null");
        }

        /**
         * Get the parameter name.
         * @return name, never {@code null}
         */
        public String name() {
            return name;
        }
    }

    /**
     * Named option with no explicit value, if present implies {@code true} value, if not present implies {@code false} value.
     */
    public static class FlagParam extends Parameter {

        private FlagParam(String name) {
            super(name);
        }
    }

    /**
     * Named option with only one value.
     */
    public static class KeyValueParam extends Parameter {

        private final String value;

        private KeyValueParam(String name, String value) {
            super(name);
            this.value = Objects.requireNonNull(value, "value is null");
        }

        /**
         * Get the value.
         * @return value, never {@code null}
         */
        public String value() {
            return value;
        }
    }

    /**
     * Named option with one or more values.
     */
    public static class KeyValuesParam extends Parameter {

        private final LinkedList<String> values;

        private KeyValuesParam(String name, LinkedList<String> values) {
            super(name);
            this.values = Objects.requireNonNull(values, "values is null");
        }

        /**
         * Get the values.
         *
         * @return values, never {@code null}
         */
        public LinkedList<String> values() {
            return values;
        }
    }

    /**
     * No-name local option with one value.
     */
    public static class ArgumentParam extends Parameter {

        private final String value;

        private ArgumentParam(String value) {
            super("");
            this.value = value;
        }

        /**
         * Get the value.
         *
         * @return value, never {@code null}
         */
        public String value() {
            return value;
        }
    }
}
