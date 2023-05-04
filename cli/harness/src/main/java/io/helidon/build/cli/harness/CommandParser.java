/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import io.helidon.build.cli.harness.CommandModel.ArgumentInfo;
import io.helidon.build.cli.harness.CommandModel.FlagInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValuesInfo;
import io.helidon.build.cli.harness.CommandParameters.ParameterInfo;
import io.helidon.build.common.FileUtils;

import static io.helidon.build.cli.harness.GlobalOptions.ARGS_FILE_OPTION_ARGUMENT;
import static io.helidon.build.cli.harness.GlobalOptions.ARGS_FILE_OPTION_NAME;

/**
 * Command parser.
 */
public final class CommandParser {

    static final String TOO_MANY_ARGUMENTS = "Too many arguments";
    static final String INVALID_REPEATING_OPTION = "Invalid repeating option";
    static final String INVALID_COMMAND_NAME = "Invalid command name";
    static final String INVALID_OPTION_NAME = "Invalid option name";
    static final String MISSING_REQUIRED_ARGUMENT = "Missing required argument";
    static final String INVALID_ARGUMENT_VALUE = "Invalid argument value";
    static final String INVALID_OPTION_VALUE = "Invalid option value";
    static final String UNKNOWN_OPTION = "Unknown command option";
    static final String INVALID_CHOICE = "Invalid choice";
    static final String UNREPEATABLE_OPTION = "Option cannot be repeated";
    static final String MISSING_REQUIRED_OPTION = "Missing required option";

    private final List<String> argsList;
    private final String commandName;
    private final String error;
    private final Resolver globalResolver;

    private CommandParser(List<String> argsList, String commandName, Resolver globalResolver, String error) {
        this.argsList = argsList;
        this.commandName = commandName;
        this.globalResolver = globalResolver;
        this.error = error;
    }

    /**
     * Create a command parser.
     *
     * @param args arguments
     * @return parser
     */
    static CommandParser create(String... args) {
        Objects.requireNonNull(args, "args is null");
        String commandName = null;
        Properties properties = new Properties();
        Map<String, Parameter> params = new HashMap<>();
        String error = null;
        String[] processedArgs = preProcessArgs(params, args);
        List<String> argsList = mapArgs(processedArgs);
        Iterator<String> it = argsList.iterator();
        while (it.hasNext()) {
            String rawArg = it.next();
            if (rawArg.isEmpty()) {
                it.remove();
                continue;
            }
            rawArg = rawArg.trim();
            String arg = rawArg.trim().toLowerCase();
            if (isProperty(rawArg)) {
                if (commandName != null) {
                    continue;
                }
                String prop = rawArg.substring(2);
                Map.Entry<String, String> propEntry = parseProperty(prop);
                properties.put(propEntry.getKey(), propEntry.getValue());
                it.remove();
            } else if (!GlobalOptions.isGlobalFlag(arg)) {
                if (commandName == null) {
                    if (!Command.NAME_PREDICATE.test(arg)) {
                        error = INVALID_COMMAND_NAME + ": " + arg;
                        break;
                    } else {
                        commandName = arg;
                        it.remove();
                    }
                }
            } else {
                arg = arg.substring(2);
                params.put(arg, new FlagParam(arg));
                it.remove();
            }
        }
        return new CommandParser(argsList, commandName, new Resolver(params, properties), error);
    }

    static String[] preProcessArgs(Map<String, Parameter> params, String[] args) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (ARGS_FILE_OPTION_ARGUMENT.equals(args[i]) && (i < args.length - 1)) {
                String argsFile = args[i + 1];
                params.put(ARGS_FILE_OPTION_NAME, new KeyValueParam(ARGS_FILE_OPTION_NAME, argsFile));
                result.addAll(readArgsFile(argsFile));
                i++;
            } else {
                result.add(args[i]);
            }
        }
        return result.toArray(new String[0]);
    }

    static List<String> readArgsFile(String argsFile) {
        try {
            return Files.lines(Path.of(argsFile))
                        .filter(line -> !line.startsWith("#"))
                        .flatMap(line -> Arrays.stream(line.split("\\s+")))
                        .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> mapArgs(String... args) {
        List<String> result = new LinkedList<>(Arrays.asList(args));
        // Map version flag to version command if first argument
        if (args.length >= 1 && args[0].equals(GlobalOptions.VERSION_FLAG_ARGUMENT)) {
            result.set(0, GlobalOptions.VERSION_FLAG_NAME);
        }
        return result;
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
     * Get the global scope resolver.
     *
     * @return resolver, never {@code null}
     */
    Resolver globalResolver() {
        return globalResolver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandParser that = (CommandParser) o;
        return Objects.equals(argsList, that.argsList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(argsList);
    }

    /**
     * Parse the specified command.
     *
     * @param command command parameters for the command to parse
     * @return resolver that can be used to resolve the values for the parsed parameters
     */
    Resolver parseCommand(CommandParameters command) {
        return parseCommand(command.parametersMap());
    }

    /**
     * Finish parsing with no specified command.
     *
     * @return resolver that can be used to resolve the values for the parsed parameters
     */
    Resolver parseCommand() {
        return parseCommand(new CommandParameters(GlobalOptions.GLOBAL_OPTIONS_INFO));
    }

    private Resolver parseCommand(Map<String, ParameterInfo<?>> parametersMap) {
        Map<String, Parameter> parsedParams = new HashMap<>(globalResolver.params);
        Properties properties = new Properties();
        Iterator<String> it = argsList.iterator();
        while (it.hasNext()) {
            String rawArg = it.next();
            if (rawArg.isEmpty()) {
                continue;
            }
            rawArg = rawArg.trim();
            String arg = rawArg.toLowerCase();
            if (isParam(arg)) {
                String optionName = arg.substring(2);
                if (!Option.VALID_NAME.test(optionName)) {
                    throw new CommandParserException(INVALID_OPTION_NAME + ": " + optionName);
                }
                ParameterInfo<?> paramInfo = parameterInfo(optionName, parametersMap);
                if (paramInfo instanceof FlagInfo) {
                    parsedParams.put(optionName, new FlagParam(optionName));
                } else if (paramInfo instanceof KeyValueInfo) {
                    boolean required = ((KeyValueInfo<?>) paramInfo).required();
                    if (!it.hasNext()) {
                        if (required) {
                            throw new CommandParserException(MISSING_REQUIRED_OPTION + ": " + optionName);
                        } else {
                            continue;
                        }
                    }
                    KeyValueParam keyValueParam = new KeyValueParam(optionName, it.next().trim());
                    parsedParams.put(optionName, keyValueParam);
                    if (keyValueParam.name().equals(GlobalOptions.PROPS_FILE_OPTION_NAME)) {
                            Properties props = FileUtils.loadProperties(Path.of(keyValueParam.value));
                            props.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
                    }
                } else if (paramInfo instanceof KeyValuesInfo) {
                    boolean required = ((KeyValuesInfo<?>) paramInfo).required();
                    if (!it.hasNext()) {
                        if (required) {
                            throw new CommandParserException(MISSING_REQUIRED_OPTION + ": " + optionName);
                        } else {
                            continue;
                        }
                    }
                    String value = it.next().trim();
                    if (isParam(value) && parametersMap.containsKey(value.substring(2))) {
                        throw new CommandParserException(INVALID_REPEATING_OPTION + ": " + optionName);
                    }
                    String[] splitValues = value.split(",");
                    LinkedList<String> values = new LinkedList<>();
                    Collections.addAll(values, splitValues);
                    Parameter param = parsedParams.get(optionName);
                    if (param == null) {
                        parsedParams.put(optionName, new KeyValuesParam(optionName, values));
                    } else if (param instanceof KeyValuesParam) {
                        ((KeyValuesParam) param).values().addAll(values);
                    }
                } else {
                    throw new CommandParserException(UNKNOWN_OPTION + ": " + optionName);
                }
            } else if (isProperty(rawArg)) {
                String prop = rawArg.substring(2);
                Map.Entry<String, String> propEntry = parseProperty(prop);
                properties.put(propEntry.getKey(), propEntry.getValue());
                it.remove();
            } else if (parsedParams.containsKey("")) {
                throw new CommandParserException(TOO_MANY_ARGUMENTS);
            } else {
                parsedParams.put("", new ArgumentParam(rawArg));
            }
        }
        return new Resolver(parsedParams, properties);
    }

    private ParameterInfo<?> parameterInfo(String paramName, Map<String, ParameterInfo<?>> parametersMap) {
        if (GlobalOptions.isGlobal(paramName)) {
            return GlobalOptions.GLOBAL_OPTIONS.get(paramName);
        }
        return parametersMap.get(paramName);
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
     * Command parser resolver.
     */
    public static class Resolver {

        private final Map<String, Parameter> params;
        private final Properties properties;

        Resolver(Map<String, Parameter> params, Properties properties) {
            this.params = params;
            this.properties = properties;
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
         * @return properties
         */
        public Properties properties() {
            return properties;
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
         * @param <T> option type
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
         * @param <T> item type
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
                if (resolved instanceof KeyValuesParam) {
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
         * @param <T> argument type
         * @param option the argument to resolve
         * @return resolved value for the argument
         * @throws CommandParserException if an error occurs while resolving the option
         */
        public <T> T resolve(ArgumentInfo<T> option) throws CommandParserException {
            Class<T> type = option.type();
            Parameter resolved = params.get("");
            if (resolved == null && option.required()) {
                throw new CommandParserException(MISSING_REQUIRED_ARGUMENT);
            }
            if (isSupported(type, Option.Argument.SUPPORTED_TYPES)) {
                if (resolved == null) {
                    return null;
                } else if (resolved instanceof ArgumentParam) {
                    return resolveValue(type, ((ArgumentParam) resolved).value);
                }
            }
            throw new CommandParserException(INVALID_ARGUMENT_VALUE);
        }

        @SuppressWarnings("rawtypes")
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
         *
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
         *
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

    private static boolean isProperty(String arg) {
        return arg.length() > 2 && arg.charAt(0) == '-' && arg.charAt(1) == 'D';
    }

    private static Map.Entry<String, String> parseProperty(String prop) {
        if (prop.length() >= 3) {
            int index = prop.indexOf('=');
            if (index >= 0) {
                return Map.entry(prop.substring(0, index), prop.substring(index + 1));
            }
        }
        return Map.entry(prop, "");
    }

    private static boolean isParam(String arg) {
        return arg.length() > 2 && arg.charAt(0) == '-' && arg.charAt(1) == '-';
    }
}
