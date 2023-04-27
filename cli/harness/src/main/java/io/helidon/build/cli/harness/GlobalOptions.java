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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.cli.harness.CommandModel.FlagInfo;
import io.helidon.build.cli.harness.CommandParameters.ParameterInfo;

/**
 * Global option constants.
 */
public class GlobalOptions {

    /**
     * The --help flag name.
     */
    public static final String HELP_FLAG_NAME = "help";

    /**
     * The --help flag description.
     */
    public static final String HELP_FLAG_DESCRIPTION = "Display help information";

    /**
     * The --help flag info.
     */
    public static final FlagInfo HELP_FLAG_INFO = new FlagInfo(HELP_FLAG_NAME, HELP_FLAG_DESCRIPTION, false);

    /**
     * The --version flag name.
     */
    public static final String VERSION_FLAG_NAME = "version";

    /**
     * The --version flag argument.
     */
    public static final String VERSION_FLAG_ARGUMENT = "--" + VERSION_FLAG_NAME;

    /**
     * The --verbose flag name.
     */
    public static final String VERBOSE_FLAG_NAME = "verbose";

    /**
     * The --verbose flag description.
     */
    public static final String VERBOSE_FLAG_DESCRIPTION = "Produce verbose output";

    /**
     * The --verbose flag info.
     */
    public static final FlagInfo VERBOSE_FLAG_INFO = new FlagInfo(VERBOSE_FLAG_NAME, VERBOSE_FLAG_DESCRIPTION, false);

    /**
     * The --verbose flag argument.
     */
    public static final String VERBOSE_FLAG_ARGUMENT = "--" + VERBOSE_FLAG_NAME;

    /**
     * The --debug flag name.
     */
    public static final String DEBUG_FLAG_NAME = "debug";

    /**
     * The --debug flag description.
     */
    public static final String DEBUG_FLAG_DESCRIPTION = "Produce debug output";

    /**
     * The --debug flag info.
     */
    public static final FlagInfo DEBUG_FLAG_INFO = new FlagInfo(DEBUG_FLAG_NAME, DEBUG_FLAG_DESCRIPTION, false);

    /**
     * The --debug flag argument.
     */
    public static final String DEBUG_FLAG_ARGUMENT = "--" + DEBUG_FLAG_NAME;

    /**
     * The --error flag name.
     */
    public static final String ERROR_FLAG_NAME = "error";

    /**
     * The --error flag description.
     */
    public static final String ERROR_FLAG_DESCRIPTION = "Print error stack traces";

    /**
     * The --error flag info.
     */
    public static final FlagInfo ERROR_FLAG_INFO = new FlagInfo(ERROR_FLAG_NAME, ERROR_FLAG_DESCRIPTION, false);

    /**
     * The --error flag argument.
     */
    public static final String ERROR_FLAG_ARGUMENT = "--" + ERROR_FLAG_NAME;

    /**
     * The --plain flag name.
     */
    public static final String PLAIN_FLAG_NAME = "plain";

    /**
     * The --plain flag description.
     */
    public static final String PLAIN_FLAG_DESCRIPTION = "Do not use color or styles in output";

    /**
     * The --plain flag info.
     */
    public static final FlagInfo PLAIN_FLAG_INFO = new FlagInfo(PLAIN_FLAG_NAME, PLAIN_FLAG_DESCRIPTION, false);

    /**
     * The --plain flag argument.
     */
    public static final String PLAIN_FLAG_ARGUMENT = "--" + PLAIN_FLAG_NAME;

    /**
     * The --args-file option name.
     */
    public static final String ARGS_FILE_OPTION_NAME = "args-file";

    /**
     * The --args-file option description.
     */
    public static final String ARGS_FILE_OPTION_DESCRIPTION = "Path to a file with arguments for Helidon CLI tool";

    /**
     * The --args-file option argument.
     */
    public static final String ARGS_FILE_OPTION_ARGUMENT = "--" + ARGS_FILE_OPTION_NAME;

    /**
     * The --args-file option info.
     */
    public static final ParameterInfo<String> ARGS_FILE_OPTION_INFO = new CommandModel.KeyValueInfo<>(
            String.class,
            ARGS_FILE_OPTION_NAME,
            ARGS_FILE_OPTION_DESCRIPTION,
            null,
            false,
            false);

    /**
     * The --props-file option name.
     */
    public static final String PROPS_FILE_OPTION_NAME = "props-file";

    /**
     * The --props-file option description.
     */
    public static final String PROPS_FILE_OPTION_DESCRIPTION = "Path to a properties file with user inputs for Helidon CLI tool";

    /**
     * The --props-file option argument.
     */
    public static final String PROPS_FILE_OPTION_ARGUMENT = "--" + PROPS_FILE_OPTION_NAME;

    /**
     * The --props-file option info.
     */
    public static final ParameterInfo<String> PROPS_FILE_OPTION_INFO = new CommandModel.KeyValueInfo<>(
            String.class,
            PROPS_FILE_OPTION_NAME,
            PROPS_FILE_OPTION_DESCRIPTION,
            null,
            false,
            false);

    private final Map<String, String> options = new HashMap<>();

    GlobalOptions(Map<String, CommandParser.Parameter> parameters, Properties properties) {
        Objects.requireNonNull(parameters, "parameters is null");
        Objects.requireNonNull(properties, "properties is null");
        Map<String, String> props = parameters.entrySet().stream()
                .filter(entry -> GLOBAL_OPTIONS_NAME.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, param -> param.getValue().stringValue()));
        options.putAll(props);
        props = properties.entrySet().stream()
                .filter(entry -> GLOBAL_OPTIONS_NAME.contains(entry.getKey().toString()))
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        options.putAll(props);
    }

    /**
     * Tests whether the given argument is a global flag.
     *
     * @param argument The argument.
     * @return {@code true} if a global flag.
     */
    public static boolean isGlobalFlag(String argument) {
        return GLOBAL_OPTION_ARGUMENTS.contains(argument) && GLOBAL_OPTIONS.get(argument.substring(2)) instanceof FlagInfo;
    }

    /**
     * Tests whether the given argument is a global option.
     *
     * @param argument The argument.
     * @return {@code true} if a global option.
     */
    public static boolean isGlobal(String argument) {
        return GLOBAL_OPTIONS.containsKey(argument);
    }

    /**
     * Global options info.
     */
    static final ParameterInfo<?>[] GLOBAL_OPTIONS_INFO = new ParameterInfo[]{
            HELP_FLAG_INFO,
            VERBOSE_FLAG_INFO,
            DEBUG_FLAG_INFO,
            ERROR_FLAG_INFO,
            PLAIN_FLAG_INFO,
            PROPS_FILE_OPTION_INFO,
            ARGS_FILE_OPTION_INFO
    };

    /**
     * Global options name.
     */
    static final Set<String> GLOBAL_OPTIONS_NAME = Set.of(
            HELP_FLAG_NAME,
            VERSION_FLAG_NAME,
            PROPS_FILE_OPTION_NAME,
            ARGS_FILE_OPTION_NAME,
            PLAIN_FLAG_NAME,
            ERROR_FLAG_NAME,
            DEBUG_FLAG_NAME,
            VERBOSE_FLAG_NAME
    );

    private static final Set<String> GLOBAL_OPTION_ARGUMENTS = Stream.of(GLOBAL_OPTIONS_INFO)
                                                                     .map(info -> (CommandModel.NamedOptionInfo<?>) info)
                                                                     .map(info -> "--" + info.name())
                                                                     .collect(Collectors.toSet());

    /**
     * Global options.
     */
    static final Map<String, ParameterInfo<?>> GLOBAL_OPTIONS =
            Stream.of(GLOBAL_OPTIONS_INFO)
                  .map(info -> (CommandModel.NamedOptionInfo<?>) info)
                  .collect(Collectors.toMap(CommandModel.NamedOptionInfo::name, Function.identity()));

    /**
     * Get option value, can be {@code null}.
     *
     * @param key option key
     * @return value
     */
    public String get(String key) {
        return options.get(key);
    }

    /**
     * Get option value as {@link Boolean}.
     *
     * @param key option key
     * @return value as Boolean
     */
    public boolean getBoolean(String key) {
        return options.containsKey(key);
    }
}
